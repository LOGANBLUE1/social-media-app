import { api } from './instance';
import type { AuthenticationResponse, ChangePasswordRequest, Session, UserRequest } from './types';

/**
 * Login and signup both return a full AuthenticationResponse, so both can produce a Session.
 * They are sent unauthenticated: a stale bearer token on these endpoints is at best ignored
 * and at worst triggers a pointless refresh attempt.
 */
async function authenticate(path: '/auth/login' | '/auth/signup', body: UserRequest): Promise<Session> {
  const res = await api.postPublic<AuthenticationResponse>(path, body);

  if (!res?.accessToken || !res.refreshToken || !res.user) {
    throw new Error('Server returned an incomplete authentication response');
  }

  return { accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user };
}

export function login(username: string, password: string): Promise<Session> {
  return authenticate('/auth/login', { username, password });
}

export function signup(username: string, password: string, image?: string | null): Promise<Session> {
  return authenticate('/auth/signup', { username, password, image: image ?? null });
}

export function changePassword(body: ChangePasswordRequest): Promise<void> {
  return api.post<void>('/auth/change-password', body);
}
