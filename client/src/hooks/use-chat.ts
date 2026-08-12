import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { conversations } from '@/api';
import { useAuth } from '@/auth/auth-context';

export const chatKeys = {
  conversations: ['conversations'] as const,
  messages: (id: number) => ['conversations', id, 'messages'] as const,
};

export function useConversations() {
  const { session } = useAuth();

  return useQuery({
    queryKey: chatKeys.conversations,
    queryFn: conversations.list,
    enabled: !!session,
    // Nothing pushes from the server, so the list is polled while it is on screen.
    refetchInterval: 15_000,
  });
}

export function useMessages(conversationId: number) {
  const { session } = useAuth();

  return useQuery({
    queryKey: chatKeys.messages(conversationId),
    queryFn: () => conversations.listMessages(conversationId),
    enabled: !!session && Number.isFinite(conversationId),
    // The only way to see the other person's replies -- there is no websocket.
    refetchInterval: 5_000,
    // A chat is worth showing stale-then-fresh; the default 30s would leave it frozen.
    staleTime: 0,
  });
}

/** Opens (or reuses) the chat with a friend. 403s when the two are not friends. */
export function useOpenConversation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userId: number) => conversations.open(userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: chatKeys.conversations }),
  });
}

export function useSendMessage(conversationId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: string) => conversations.sendMessage(conversationId, body),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: chatKeys.messages(conversationId) }),
        // lastMessageAt drives the chat list's order, so it moves this thread to the top.
        queryClient.invalidateQueries({ queryKey: chatKeys.conversations }),
      ]),
  });
}
