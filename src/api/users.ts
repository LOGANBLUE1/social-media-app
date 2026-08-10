import { api } from './instance';
import type { UserActivityResponse, UserRequest, UserResponse } from './types';

export function list(): Promise<UserResponse[]> {
  return api.get<UserResponse[]>('/users');
}

export function me(): Promise<UserResponse> {
  return api.get<UserResponse>('/users/me');
}

export function updateMe(body: Partial<UserRequest>): Promise<UserResponse> {
  return api.put<UserResponse>('/users/me', body);
}

export function deleteMe(): Promise<void> {
  return api.delete<void>('/users/me');
}

export function myActivity(): Promise<UserActivityResponse> {
  return api.get<UserActivityResponse>('/users/activity/me');
}

export function activityOf(userId: number): Promise<UserActivityResponse> {
  return api.get<UserActivityResponse>(`/users/activity/${userId}`);
}
