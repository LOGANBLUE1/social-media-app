# social-media-app-server

A Spring Boot REST API for a small social app: users, posts, comments, likes, and JWT authentication.

**Stack:** Java 17 · Spring Boot 3.0.2 · Spring Security 6 · Spring Data JPA / Hibernate · PostgreSQL · jjwt 0.11.5 · Lombok · Maven

## Running locally

Requires JDK 17 and a PostgreSQL database named `logandb` on `localhost:5432` (see `src/main/resources/application.properties`).

```bash
./mvnw spring-boot:run
```

The schema is created by Hibernate on startup (`spring.jpa.hibernate.ddl-auto=update`).

## Endpoints

| Method | Path | Auth |
|---|---|---|
| POST | `/auth/signup` | public |
| POST | `/auth/login` | public |
| POST | `/auth/refresh` | public |
| POST | `/auth/change-password` | required |
| GET | `/users` · `/users/me` · `/users/activity/me` · `/users/activity/{userId}` | required |
| PUT / DELETE | `/users/me` | required |
| GET | `/posts/me` · `/posts/{id}` | required |
| POST / PUT / DELETE | `/posts` · `/posts/{id}` | required |
| GET | `/comments?postId=` · `/comments/{id}` | required |
| POST / PUT / DELETE | `/comments` · `/comments/{id}` | required |
| GET | `/likes?postId=` · `/likes/{id}` | required |
| POST / DELETE | `/likes` · `/likes/{id}` | required |

## Response format

Every endpoint returns the `Response<T>` envelope (`utils/Response.java`), serialized with `@JsonInclude(NON_NULL)`:

```json
{ "success": true,  "message": "Login successful", "data": { } }
{ "success": false, "message": "Invalid username or password" }
```

Controllers return `Response<T>` directly and declare their status with `@ResponseStatus`; every non-happy path is **thrown**, and `exceptions/GlobalExceptionHandler` is the single place that maps an exception to a status and message.

| Thrown | Status |
|---|---|
| `IllegalArgumentException` | 400 |
| `UnauthorizedException`, Spring's `AuthenticationException` | 401 |
| `NotFoundException` | 404 |
| `ConflictException` | 409 |
| anything else | 500 |
| Spring MVC's own family (bad JSON, wrong `Content-Type`, wrong method, missing param, validation) | its own 4xx, via `handleExceptionInternal` |

---

# Code review — open issues

Findings from a full pass over `src/`, grouped by impact. Nothing below has been fixed yet.

## A. Security — exploitable today

- [ ] **A1. `userId` is client-supplied on create-post and create-like.** `CreatePostRequest.userId` and `CreateLikeRequest.userId` are trusted by `PostService.createPost:57` and `LikeService.createLike:48`, so any user can author content as any other user. `CommentController.java:40` already does it right — id from `@AuthenticationPrincipal`, field marked `@JsonProperty(access = READ_ONLY)` (`CreateCommentRequest.java:9`). OWASP API1, Broken Object Level Authorization.
- [ ] **A2. No ownership check on any update or delete.** `PUT/DELETE /posts/{id}`, `PUT/DELETE /comments/{id}`, `DELETE /likes/{id}` load by id and mutate without comparing the row's owner to the caller. Any logged-in user can delete anyone's data.
- [ ] **A3. The JWT signing key is regenerated on every startup.** `JWTTokenProvider.java:18` — `Keys.secretKeyFor(SignatureAlgorithm.HS256)`. Every restart invalidates all issued tokens, and two instances can never validate each other's. Move to a configured base64 secret via `Keys.hmacShaKeyFor(...)`.
- [ ] **A4. Database credentials committed to the repo.** `application.properties` ships `postgres`/`postgres`. Use `${DB_USERNAME}`/`${DB_PASSWORD}` placeholders backed by env vars.
- [ ] **A5. `/auth/refresh` trusts a client-supplied `userId`.** Look the row up by refresh token, not by user id. The token is also stored in plaintext (store a hash), and there is no logout/revoke endpoint.
- [ ] **A6. CORS is fully open with credentials.** `SecurityConfiguration.java:55-57` — `allowCredentials(true)` plus `allowedOriginPatterns("*")`, which deliberately sidesteps the spec's ban on `*` with credentials. Pin to the real front-end origins.
- [ ] **A7. Username enumeration by timing.** `UserDetailsServiceImpl.java:22-25` returns `JWTUserDetails.create(null)` for an unknown username → NPE → `InternalAuthenticationServiceException`. The client still sees a correct 401, but Spring's dummy-bcrypt timing mitigation is skipped, so "no such user" answers measurably faster than "wrong password". Fix: `throw new UsernameNotFoundException(username)`.

## B. Correctness / data integrity

- [ ] **B1. Nothing is transactional.** Zero `@Transactional` in the codebase. `AuthService.changePassword`, `UserService.updateUserById`, `PostService.updatePostById`, `RefreshTokenService.createRefreshToken` all read-modify-write across separate auto-commit statements.
- [ ] **B2. Open Session In View is masking B1.** `spring.jpa.open-in-view` is unset, so it defaults to `true` (Boot warns about this at startup). It's the only reason `CommentResponse.java:15-16` can touch a `LAZY` association outside a transaction. Setting it to `false` turns the missing transactions into `LazyInitializationException` — so B1 must be fixed first.
- [ ] **B3. `RefreshToken` is `@ManyToOne` but read as a single row.** `RefreshTokenRepository.findByUserId` returns one `RefreshToken`; two rows for one user (`createRefreshToken` does a non-atomic check-then-insert) makes it throw `IncorrectResultSizeDataAccessException`. Make it `@OneToOne` with a unique constraint on `user_id`, or support multiple devices and return a `List`.
- [ ] **B4. The two token lifetimes use different units under identical naming.** `question.expires.in=350000` is consumed as **milliseconds** (`JWTTokenProvider.java:23`) → 5.8-minute access token; `refresh.token.expires.in=700000` is consumed as **seconds** (`RefreshTokenService.java:36`) → 8.1 days. Bind `Duration` types or rename with explicit units.
- [ ] **B5. `CommentService.getAllComments:47-50` NPEs** when both `Optional`s are empty (`comments = null`, then `.stream()`). Unreachable only because the endpoint is commented out at `CommentController.java:26-29`.
- [ ] **B6. `UserDetailsServiceImpl.loadUserById:28`** — `.get()` on an empty `Optional` throws `NoSuchElementException` inside the filter, which `JWTAuthenticationFilter.java:39` swallows into a **200 with an empty body**.
- [ ] **B7. "No activity" is reported as 400.** `UserController.activityOrThrow` turns "user has no posts" into `IllegalArgumentException`. An empty result isn't a client error — return 200 with empty lists.

### Auth failures that bypass the envelope

- [ ] **B8. `JWTAuthenticationFilter.java:39-42`** catches everything, writes no body, and returns without calling the chain — a malformed or expired token yields **200 with an empty body** instead of 401.
- [ ] **B9. `JWTAuthenticationEntryPoint.java:17`** uses `response.sendError(...)`, so an unauthenticated request to a protected route returns Tomcat's default error shape, not `Response`. Both live in the filter chain, so `@RestControllerAdvice` never sees them — clients need two error parsers.

## C. Spring / JPA convention violations

- [ ] **C1. `@Data` on all five entities.** Generates `equals`/`hashCode` over every field including associations, plus a `toString` that walks them — breaks JPA identity across detach/merge and triggers lazy loads. Use `@Getter @Setter` with id-based `equals`/`hashCode`.
- [ ] **C2. `Post.user` is `FetchType.EAGER`** (`Post.java:19`) while every other `@ManyToOne` is `LAZY`. EAGER on `@ManyToOne` is the classic JPA anti-pattern.
- [ ] **C3. N+1 query in `PostService.getAllPosts:40-42`** — one query for posts, then one per post for likes. Use `findByPostIdIn(ids)` and group in memory, or an `@EntityGraph`.
- [ ] **C4. Circular dependency `PostService` ↔ `LikeService`,** patched with `@Lazy` setter injection (`PostService.java:32-35`), which is what lets it slip past Boot's `allow-circular-references=false` default. Assembling a post's likes is one read concern and should live in one place.
- [ ] **C5. No pagination anywhere.** `GET /users` returns the whole table via `findAll()`; `findByUserId` is unbounded. Use `Pageable`.
- [ ] **C6. No validation, despite `spring-boot-starter-validation` being a dependency.** Not one `@Valid`/`@NotBlank`/`@NotNull` in the repo — a signup with a null username reaches bcrypt and 500s. `GlobalExceptionHandler.messageFor` already has a `MethodArgumentNotValidException` branch that can never fire.
- [ ] **C7. Field injection instead of constructor injection** — `WebConfig.java:10`, `JWTAuthenticationFilter.java:20-24`, `RefreshTokenService.java:15-16`. Every other class in the codebase already uses constructor injection.
- [ ] **C8. `JWTAuthenticationFilter` is registered twice** — as a `@Bean` (auto-registered with the servlet container) and inside the security chain (`SecurityConfiguration.java:82`). `OncePerRequestFilter` dedupes it today, but the servlet-level copy runs outside the security chain, so any ordering change turns this into "principal set, then cleared." Disable the auto registration with a `FilterRegistrationBean`.
- [ ] **C9. No indexes on the FK columns you filter by.** Postgres doesn't auto-index foreign keys, and `findByPostId` / `findByUserId` / both native activity queries all filter on them.
- [ ] **C10. `findTop5ByUserId` is a native query wearing a derived-query name** (`PostRepository.java:20`) — Spring Data would implement that name for you, so the `@Query` silently overrides it. Also the `LIMIT 5` in `findUserCommentsByPostId` caps the total across all posts, not per post.
- [ ] **C11. Deprecated security DSL.** `SecurityConfiguration.java:65-80` uses `.and()` chaining, deprecated in Spring Security 6.1 and removed in 7. Move to the lambda DSL during the Boot upgrade.
- [ ] **C12. `LikeController` returns the `Like` entity** while every other controller returns a `*Response` DTO. `@JsonIgnore` on both associations reduces it to `{"id":n}`, which is useless to clients anyway.

## D. Structure & naming

| What | Now | Convention |
|---|---|---|
| Repository package | `dataAccess` | `repository` — package names are all-lowercase, no camelCase (JLS §6.1) |
| Config package | `configuration` | `config` |
| Entity package | `entities` | `entity` / `domain` (singular) |
| DTOs | `requests` + `responses` + `dto` | one `dto` package with `request` / `response` / `projection` — "DTO" currently means three folders |
| API envelope | `utils/Response` | belongs in `dto` or `web`; `utils` is where classes go to avoid a naming decision |
| Interceptor | `MyInterceptor` | name it for what it does — `RequestLoggingInterceptor` |

- [ ] **D1. Four names for one app.** `artifactId=questionapp`, `spring.application.name=logan`, `LoganApplicationTests`, package `com.example.app`, directory `social-media-app-server`. The property `question.expires.in` is from the same lineage. Pick one and move off `com.example`.
- [ ] **D2. No API versioning or prefix.** Controllers sit at `/users`, `/posts`. Use `/api/v1/...`.
- [ ] **D3. `MyInterceptor` uses `org.apache.commons.logging`** (`MyInterceptor.java:5-6`) with string concatenation. SLF4J is the Boot standard and is what `GlobalExceptionHandler` already uses.
- [ ] **D4. `UserRequest` is reused for signup and profile update,** with a `password` field `UserService.updateUserById` deliberately ignores. Two operations with different required fields can't share one validated DTO — split it.
- [ ] **D5. Inconsistent field visibility.** `RefreshToken`, `AuthenticationResponse`, `UserRequest` use package-private fields; everything else uses `private`. `JWTUserDetails.id` is `public`, and the class carries `@Getter @Setter @Data` (`@Data` already includes both).
- [ ] **D6. Dead code:** `LikeService.getUserLikes` (endpoint commented out at `LikeController.java:28-31`), `CommentService.getAllComments`, and the commented blocks in `UserController.java:60-82`. Git remembers them.

## E. Build & operations

- [ ] **E1. Spring Boot 3.0.2 is past end-of-life** (OSS support ended Nov 2023) — missing security patches across Boot, Framework, Security, Hibernate and Tomcat. Upgrading also means jjwt 0.11.5 → 0.12.x (`parserBuilder()` → `parser()`, `setSubject` → `subject`).
- [ ] **E2. `ddl-auto=update`.** Schema drift accumulates with no record of what changed. Move to Flyway with `ddl-auto=validate`.
- [ ] **E3. No profiles.** One `application.properties` hardcoding `localhost:5432`. Split into `local` / `prod`.
- [ ] **E4. `pom.xml` cleanup:** `spring-boot-devtools` is declared twice; jjwt's version is repeated across three artifacts instead of a `<jjwt.version>` property; `jakarta.xml.bind-api` is a javax-era leftover nothing needs under Boot 3.
- [ ] **E5. Lombok's version is pinned** (`1.18.34`) while the Boot parent already manages it — drop the `<version>`.

## F. Testing

- [ ] **F1. One test, and it can't pass on a clean machine.** `LoganApplicationTests.contextLoads` is `@SpringBootTest`, so it boots the full context against `localhost:5432/logandb`. No H2 profile, no Testcontainers — CI fails on checkout.
- [ ] **F2. Nothing covers what's most likely to break:** no `@WebMvcTest` over the envelope/status mapping, no `@DataJpaTest` over the two native queries, no test that a wrong password returns 401 — and nothing that would have caught A2.

---

## Suggested order

1. **A1, A2** — anyone with an account can write and delete other people's data.
2. **A3** — the signing key blocks any real deployment.
3. **A4, A6** — secrets and CORS.
4. **B1, then B2** — transactions first, then turn off OSIV.
5. **C6** — validation annotations; the handler for them already exists.
6. **E1, E2** — Boot upgrade and Flyway together, since both touch startup.
7. **D** — the renames, in one commit after the above so the diff stays readable.
