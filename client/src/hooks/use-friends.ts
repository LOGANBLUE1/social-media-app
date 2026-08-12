import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { friends } from '@/api';
import { useAuth } from '@/auth/auth-context';

export const friendKeys = {
  friends: ['friends', 'list'] as const,
  incoming: ['friends', 'requests', 'incoming'] as const,
  outgoing: ['friends', 'requests', 'outgoing'] as const,
};

export function useFriends() {
  const { session } = useAuth();

  return useQuery({
    queryKey: friendKeys.friends,
    queryFn: friends.listFriends,
    enabled: !!session,
  });
}

/** Pending requests waiting on this user. */
export function useIncomingRequests() {
  const { session } = useAuth();

  return useQuery({
    queryKey: friendKeys.incoming,
    queryFn: friends.listIncoming,
    enabled: !!session,
  });
}

export function useOutgoingRequests() {
  const { session } = useAuth();

  return useQuery({
    queryKey: friendKeys.outgoing,
    queryFn: friends.listOutgoing,
    enabled: !!session,
  });
}

/**
 * Every friend mutation can move a row between all three lists -- sending accepts outright when
 * the other user already asked, and accepting empties an incoming row into the friends list --
 * so they are all refetched rather than reasoned about per case.
 */
function useInvalidateFriends() {
  const queryClient = useQueryClient();

  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: friendKeys.friends }),
      queryClient.invalidateQueries({ queryKey: friendKeys.incoming }),
      queryClient.invalidateQueries({ queryKey: friendKeys.outgoing }),
    ]);
}

/** Sends a request, or accepts theirs if they already asked -- the server decides which. */
export function useSendFriendRequest() {
  const invalidate = useInvalidateFriends();

  return useMutation({
    mutationFn: (addresseeId: number) => friends.send(addresseeId),
    onSuccess: invalidate,
  });
}

export function useRespondToRequest() {
  const invalidate = useInvalidateFriends();

  return useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'accept' | 'decline' }) =>
      action === 'accept' ? friends.accept(id) : friends.decline(id),
    onSuccess: invalidate,
  });
}

/** Unfriends by user id -- `DELETE /friends/{userId}`, which drops the accepted row either way. */
export function useRemoveFriend() {
  const invalidate = useInvalidateFriends();

  return useMutation({
    mutationFn: (userId: number) => friends.removeFriend(userId),
    onSuccess: invalidate,
  });
}

/** Cancels a pending request this user sent. Not for declining -- see useRespondToRequest. */
export function useDeleteFriendRequest() {
  const invalidate = useInvalidateFriends();

  return useMutation({
    mutationFn: (requestId: number) => friends.remove(requestId),
    onSuccess: invalidate,
  });
}
