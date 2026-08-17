import { api } from './instance';
import type { ConversationResponse, MessageResponse } from './types';

/** The caller's chats, most recently active first. */
export function list(): Promise<ConversationResponse[]> {
  return api.get<ConversationResponse[]>('/conversations');
}

/**
 * Opens the chat with another user, creating it on first use. Idempotent -- calling it twice
 * returns the same conversation. 403s when the two are not friends.
 */
export function open(userId: number): Promise<ConversationResponse> {
  return api.post<ConversationResponse>('/conversations', { userId });
}

/**
 * Creates a group chat containing the caller and the friends they picked.
 *
 * Not idempotent, unlike `open` -- two calls with the same name and members make two groups, which
 * is the intended behaviour.
 */
export function createGroup(name: string, memberIds: number[]): Promise<ConversationResponse> {
  return api.post<ConversationResponse>('/conversations/groups', { name, memberIds });
}

/** The whole conversation, oldest first. */
export function listMessages(conversationId: number): Promise<MessageResponse[]> {
  return api.get<MessageResponse[]>(`/conversations/${conversationId}/messages`);
}

/**
 * History stays readable after unfriending but the conversation stops accepting messages, so
 * this 403s where `listMessages` still succeeds.
 */
export function sendMessage(conversationId: number, body: string): Promise<MessageResponse> {
  return api.post<MessageResponse>(`/conversations/${conversationId}/messages`, { body });
}

/**
 * Clears the caller's unread badge up to `lastReadSeq`. Returns the updated conversation, so the
 * chat list can be settled without re-listing.
 *
 * The pointer only moves forward server-side, so this is idempotent and safe to fire on every new
 * message that arrives while the chat is open.
 */
export function markRead(
  conversationId: number,
  lastReadSeq: number,
): Promise<ConversationResponse> {
  return api.post<ConversationResponse>(`/conversations/${conversationId}/read`, { lastReadSeq });
}
