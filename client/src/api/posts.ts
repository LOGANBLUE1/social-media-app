import { api } from './instance';
import type { PostResponse, UpdatePostRequest } from './types';

/** `GET /feed` -- every user's posts, newest first. What the homepage renders. */
export function listFeed(): Promise<PostResponse[]> {
  return api.get<PostResponse[]>('/feed');
}

/** `GET /posts/me` -- only the caller's own posts. */
export function listMine(): Promise<PostResponse[]> {
  return api.get<PostResponse[]>('/posts/me');
}

export function getById(id: number): Promise<PostResponse> {
  return api.get<PostResponse>(`/posts/${id}`);
}

/** `userId` is required in the body because the server does not read the author from the token. */
export function create(userId: number, title: string, description: string): Promise<PostResponse> {
  return api.post<PostResponse>('/posts', { userId, title, description });
}

export function update(id: number, body: UpdatePostRequest): Promise<PostResponse> {
  return api.put<PostResponse>(`/posts/${id}`, body);
}

export function remove(id: number): Promise<void> {
  return api.delete<void>(`/posts/${id}`);
}
