import type { ConversationResponse } from '@/api';

/**
 * What to call a conversation on screen.
 *
 * A group has a name; a direct chat is named after the other person. Both the chat list and the
 * chat header need this, and getting it wrong shows a blank title rather than failing loudly, so
 * it lives here instead of being branched at each call site.
 */
export function conversationTitle(conversation: ConversationResponse): string {
  if (conversation.type === 'GROUP') return conversation.name ?? 'Group';
  return conversation.otherUser?.username ?? 'Chat';
}

/** Subtitle for a group row: who is in it, from the caller's point of view. */
export function groupMemberSummary(
  conversation: ConversationResponse,
  viewerId: number | undefined,
): string {
  const others = conversation.participants.filter((participant) => participant.id !== viewerId);
  if (others.length === 0) return 'Just you';

  const names = others.map((participant) => participant.username);
  // Past a couple of names the row turns into a wall of text, so the tail becomes a count.
  if (names.length <= 2) return `You and ${names.join(' and ')}`;
  return `You, ${names.slice(0, 2).join(', ')} and ${names.length - 2} more`;
}
