import { api } from './instance';
import type { CommentResponse } from './types';

export function listForPost(postId: number): Promise<CommentResponse[]> {
  return api.get<CommentResponse[]>(`/comments?postId=${postId}`);
}

export function getById(id: number): Promise<CommentResponse> {
  return api.get<CommentResponse>(`/comments/${id}`);
}

/** The author comes from the authenticated principal here, so no userId in the body. */
export function create(postId: number, text: string): Promise<CommentResponse> {
  return api.post<CommentResponse>('/comments', { postId, text });
}

export function update(id: number, text: string): Promise<CommentResponse> {
  return api.put<CommentResponse>(`/comments/${id}`, { text });
}

export function remove(id: number): Promise<void> {
  return api.delete<void>(`/comments/${id}`);
}
