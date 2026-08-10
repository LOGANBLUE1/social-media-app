# social-media-app-client

Expo + TypeScript client for the [social-media-app-server](../social-media-app-server) Spring
Boot API. One codebase targets **Android**, **web** and (for free) iOS.

## Running it

The server must be up first — Postgres on `:5432` with database `logandb`, then
`./mvnw spring-boot:run` in the server repo.

```bash
npm run web        # browser
npm run android    # Android emulator or connected device
npm start          # dev server; press a / w / i to pick a target
```

### Pointing at the server

`src/config.ts` resolves the base URL per platform:

| Target | Default |
| --- | --- |
| web, iOS simulator | `http://localhost:8080` |
| Android emulator | `http://10.0.2.2:8080` — the emulator's alias for the host loopback |
| Physical Android device | **must** be set explicitly |

For a physical device, copy `.env.example` to `.env` and set your machine's LAN address:

```bash
echo "EXPO_PUBLIC_API_URL=http://$(ipconfig getifaddr en0):8080" > .env
```

`android.usesCleartextTraffic` is enabled in `app.json` because the dev server is plain HTTP
and Android 9+ blocks cleartext by default. Turn it off before shipping anything real.

## Layout

```
src/
  api/          transport + endpoint modules. No React, no platform APIs, no window.
    client.ts   bearer header, envelope unwrap, single-flight refresh on 401
    types.ts    hand-mirrored from the Java DTOs
    instance.ts the shared ApiClient (separate module to avoid a require cycle)
  auth/
    session-store.ts   TokenStorage interface + shared helpers
    storage.native.ts  expo-secure-store (Keystore / Keychain)
    storage.web.ts     localStorage
    storage.ts         in-memory fallback (used by the static web prerender in Node)
    auth-context.tsx   session state, login / signup / logout
  hooks/        TanStack Query wrappers
  app/          Expo Router routes
```

The portability rule: **nothing under `src/api/` may import React or touch a platform API.**
Persistence reaches it only through `api.onSessionChange`. That is what lets the same
transport serve Android, the browser, and any future client.

Metro picks `storage.native.ts` / `storage.web.ts` automatically from the file extension, so
`import { storage } from './storage'` resolves correctly per platform.

## Server behaviour worth knowing

These are quirks of the current API that the client works around — verified against a running
server, not inferred:

- **`POST /auth/refresh` returns only `accessToken`.** `refreshToken` and `user` come back
  `null`, so `doRefresh()` spreads the existing session instead of replacing it.
- **`POST /posts` and `POST /likes` read the author from the request body**, not the token, so
  the client sends `userId`. Delete that field once the server uses
  `@AuthenticationPrincipal` (comments already do).
- **401s from the security filter have an empty body.** `JWTAuthenticationEntryPoint` calls
  `response.sendError(...)`, which bypasses the `Response` envelope entirely, so `unwrap()`
  tolerates a non-envelope, non-JSON, zero-length response.
- **`POST /likes` returns the bare `Like` entity**, whose `post` and `user` are `@JsonIgnore`d
  — only `{ id }` survives. Liking therefore refetches the post rather than patching the cache.
- **Access tokens last ~6 minutes** (`question.expires.in=350000` ms), so the refresh path is
  exercised constantly rather than rarely.
- **The JWT signing key is regenerated on every server restart**
  (`JWTTokenProvider` uses `Keys.secretKeyFor(...)` in a field initialiser). Every restart
  invalidates every stored session, so expect to log in again after each server reload.
- **The server stores one refresh token per user, not per device.** Logging in on Android
  invalidates the browser's refresh token and vice versa. Fixing this is a prerequisite for
  genuinely using both clients at once.
- **There is no feed endpoint and no pagination** — `GET /posts/me` returns all of your own
  posts and nothing from anyone else.

## Not built yet

- Push notifications (`expo-notifications` → FCM for Android; Web Push + a service worker for
  the browser, which `expo-notifications` does not cover). Needs a `device_tokens` table and a
  registration endpoint server-side first.
- Editing and deleting posts/comments — the API modules (`posts.update`, `comments.remove`, …)
  are written and typed, but no screen calls them.
- Profile screen, other users' posts, avatar upload.
