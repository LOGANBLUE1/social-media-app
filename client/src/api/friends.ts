import { api } from './instance';
import type { FriendRequestResponse, UserResponse } from './types';

/**
 * `POST /friends/requests` -- sends a request, or accepts theirs if they already asked. 409s when
 * a request is already pending or the two are already friends, 400 when addresseeId is the caller.
 */
export function send(addresseeId: number): Promise<FriendRequestResponse> {
  return api.post<FriendRequestResponse>('/friends/requests', { addresseeId });
}

/** Pending requests waiting on the caller -- "people who sent me a friend request". */
export function listIncoming(): Promise<FriendRequestResponse[]> {
  return api.get<FriendRequestResponse[]>('/friends/requests/incoming');
}

/** Everything the caller sent, answered or not -- so a row here may be ACCEPTED or DECLINED. */
export function listOutgoing(): Promise<FriendRequestResponse[]> {
  return api.get<FriendRequestResponse[]>('/friends/requests/outgoing');
}

export function accept(requestId: number): Promise<FriendRequestResponse> {
  return api.post<FriendRequestResponse>(`/friends/requests/${requestId}/accept`);
}

export function decline(requestId: number): Promise<FriendRequestResponse> {
  return api.post<FriendRequestResponse>(`/friends/requests/${requestId}/decline`);
}

/**
 * Cancels a request the caller sent, or unfriends. Declining someone else's request is
 * `decline()` instead -- that keeps the row as history, this deletes it.
 */
export function remove(requestId: number): Promise<void> {
  return api.delete<void>(`/friends/requests/${requestId}`);
}

/**
 * Unfriends by user id rather than request id: `GET /friends` returns bare users, so the caller
 * has no request id to hand for a friendship the other person started.
 */
export function removeFriend(userId: number): Promise<void> {
  return api.delete<void>(`/friends/${userId}`);
}

/** `GET /friends` -- the other party from every accepted request, whichever side the caller is on. */
export function listFriends(): Promise<UserResponse[]> {
  return api.get<UserResponse[]>('/friends');
}
