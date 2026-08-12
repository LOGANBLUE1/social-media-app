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
