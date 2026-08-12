/**
 * Hand-mirrored from the Spring DTOs in `com.example.app.{requests,responses}`.
 *
 * These are maintained by hand until the server exposes an OpenAPI spec (springdoc-openapi),
 * at which point this file should be generated instead. Until then, a server-side DTO change
 * is a silent break here -- grep this file whenever you touch a `*Response`/`*Request` class.
 */

/** The envelope every controller returns (`com.example.app.utils.Response`). */
export interface ApiEnvelope<T> {
  success: boolean;
  message?: string | null;
  data?: T | null;
}

// --- responses ---

export interface UserResponse {
  id: number;
  username: string;
  image: string | null;
}

export interface AuthenticationResponse {
  accessToken: string;
  /** Null on `POST /auth/refresh` -- that endpoint only re-issues the access token. */
  refreshToken: string | null;
  /** Null on `POST /auth/refresh`. */
  user: UserResponse | null;
}

export interface LikeResponse {
  id: number;
  userId: number;
  postId: number;
}

export interface PostResponse {
  id: number;
  title: string;
  description: string;
  /** ISO-8601 local date-time, e.g. "2026-08-10T14:03:11.482" -- no timezone offset. */
  createdAt: string;
  author: UserResponse;
  likeCount: number;
  commentCount: number;
  /** Whether the caller has liked this post. Drives the like button without a second request. */
  likedByMe: boolean;
}

export interface CommentResponse {
  id: number;
  userId: number;
  username: string;
  text: string;
}

export interface UserCommentProjection {
  postId: number;
  text: string;
  username: string;
  image: string | null;
}

export interface UserLikeProjection {
  postId: number;
  username: string;
  image: string | null;
}

/** Persisted as a string server-side (`FriendRequestStatus`), so these spellings are the wire format. */
export type FriendRequestStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

/**
 * Carries both sides of the request rather than "the other user", because the incoming and
 * outgoing lists render opposite sides of the same row.
 */
export interface FriendRequestResponse {
  id: number;
  requester: UserResponse;
  addressee: UserResponse;
  status: FriendRequestStatus;
  /** ISO-8601 local date-time, no timezone offset -- same shape as PostResponse.createdAt. */
  createdAt: string;
  /** Null while the request is still PENDING. */
  respondedAt: string | null;
}

/** A 1:1 chat seen from the caller's side -- the server resolves which participant is "the other". */
export interface ConversationResponse {
  id: number;
  otherUser: UserResponse;
  /** Seeded to the creation time, so it is never null even before the first message. */
  lastMessageAt: string;
  /** How many messages the conversation holds -- also the seq of its newest message. */
  lastSeq: number;
}

/**
 * Carries only `senderId`, not the whole user: a 1:1 chat has two participants and the client
 * already knows both from the conversation.
 */
export interface MessageResponse {
  id: number;
  /** Position in the conversation. Sort on this, not createdAt. */
  seq: number;
  senderId: number;
  body: string;
  createdAt: string;
}

export interface UserActivityResponse {
  comments: UserCommentProjection[];
  likes: UserLikeProjection[];
}

/**
 * `POST /likes` returns the bare `Like` entity rather than `LikeResponse`, and its `post`/`user`
 * fields are `@JsonIgnore`d -- so only the id survives serialization.
 */
export interface CreatedLike {
  id: number;
}

// --- requests ---

export interface UserRequest {
  username: string;
  password: string;
  image?: string | null;
}

export interface CreatePostRequest {
  /**
   * The server reads the author from the body instead of the authenticated principal
   * (`PostController.createPost`). Send the logged-in user's id; delete this field once the
   * server takes it from `@AuthenticationPrincipal`.
   */
  userId: number;
  title: string;
  description: string;
}

/** Note the asymmetry: create takes `description`, update takes `text`. Both set Post.description. */
export interface UpdatePostRequest {
  title: string;
  text: string;
}

export interface CreateCommentRequest {
  postId: number;
  text: string;
  // userId is READ_ONLY server-side -- taken from the principal, so we must not send it.
}

export interface UpdateCommentRequest {
  text: string;
}

/** Same principal-vs-body issue as CreatePostRequest. */
export interface CreateLikeRequest {
  userId: number;
  postId: number;
}

/** The other participant. The caller is taken from the principal. */
export interface CreateConversationRequest {
  userId: number;
}

export interface CreateMessageRequest {
  body: string;
}

/** The requester is taken from the token, never the body -- only the addressee is sent. */
export interface CreateFriendRequest {
  addresseeId: number;
}

export interface RefreshTokenRequest {
  userId: number;
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// --- client-side session ---

/** What we persist between launches. */
export interface Session {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
}
