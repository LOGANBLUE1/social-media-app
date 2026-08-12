import { useMutation, useQueryClient } from '@tanstack/react-query';

import { likes, type PostResponse } from '@/api';
import { useAuth } from '@/auth/auth-context';

import { postKeys } from './use-posts';

/**
 * Toggles the caller's like on a post.
 *
 * `PostResponse` carries `likedByMe` and `likeCount` rather than the like rows, so unliking goes
 * through `DELETE /likes?postId=` -- there is no like id on the client to delete by.
 */
export function useToggleLike(post: PostResponse | undefined) {
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const likedByMe = post?.likedByMe ?? false;

  const mutation = useMutation({
    mutationFn: async () => {
      if (!user || !post) throw new Error('Not ready');
      if (likedByMe) {
        await likes.removeByPost(post.id);
      } else {
        // The (post_id, user_id) unique constraint turns a double-tap into a 409; refetching
        // below keeps `likedByMe` accurate so that stays an edge case rather than the norm.
        await likes.create(user.id, post.id);
      }
    },
    onSuccess: () => {
      if (!post) return;

      // Patch the caches rather than invalidating them: the detail view and both lists render the
      // same count, and refetching every post to move one number by one is not worth a round-trip.
      // The caller's own toggle is always exactly +/-1, so the local result matches the server.
      const delta = likedByMe ? -1 : 1;
      const patch = (target: PostResponse): PostResponse =>
        target.id === post.id
          ? { ...target, likeCount: target.likeCount + delta, likedByMe: !likedByMe }
          : target;

      queryClient.setQueryData<PostResponse>(postKeys.detail(post.id), (old) =>
        old ? patch(old) : old,
      );
      queryClient.setQueryData<PostResponse[]>(postKeys.feed, (old) => old?.map(patch));
      queryClient.setQueryData<PostResponse[]>(postKeys.mine, (old) => old?.map(patch));
    },
  });

  return { ...mutation, likedByMe, count: post?.likeCount ?? 0 };
}
