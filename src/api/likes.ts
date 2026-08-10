import { api } from './instance';
import type { CreatedLike, LikeResponse } from './types';

export function listForPost(postId: number): Promise<LikeResponse[]> {
  return api.get<LikeResponse[]>(`/likes?postId=${postId}`);
}

/**
 * Returns only `{ id }` -- the server responds with the raw Like entity whose post and user
 * are @JsonIgnore'd. Refetch the post if you need the full like list.
 *
 * The (post_id, user_id) unique constraint means a second like by the same user is a 409.
 */
export function create(userId: number, postId: number): Promise<CreatedLike> {
  return api.post<CreatedLike>('/likes', { userId, postId });
}

export function remove(likeId: number): Promise<void> {
  return api.delete<void>(`/likes/${likeId}`);
}
