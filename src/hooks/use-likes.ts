import { useMutation, useQueryClient } from '@tanstack/react-query';

import { likes, type PostResponse } from '@/api';
import { useAuth } from '@/auth/auth-context';

import { postKeys } from './use-posts';

/**
 * Toggles the caller's like on a post.
 *
 * There is no "unlike by postId" endpoint -- deletion is by like id -- so the id is recovered
 * from the post's embedded like list. That means this depends on the post detail query having
 * loaded, which it has whenever the button is on screen.
 */
export function useToggleLike(post: PostResponse | undefined) {
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const own = user ? post?.likes?.find((like) => like.userId === user.id) : undefined;

  const mutation = useMutation({
    mutationFn: async () => {
      if (!user || !post) throw new Error('Not ready');
      if (own) {
        await likes.remove(own.id);
      } else {
        // The (post_id, user_id) unique constraint turns a double-tap into a 409; refetching
        // below keeps `own` accurate so that stays an edge case rather than the norm.
        await likes.create(user.id, post.id);
      }
    },
    onSuccess: () => {
      if (post) {
        // Refetch rather than patch the cache: POST /likes returns only { id }, so the client
        // cannot construct the LikeResponse the post detail expects.
        return queryClient.invalidateQueries({ queryKey: postKeys.detail(post.id) });
      }
    },
  });

  return { ...mutation, likedByMe: !!own, count: post?.likes?.length ?? 0 };
}
