import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { posts } from '@/api';
import { useAuth } from '@/auth/auth-context';

export const postKeys = {
  feed: ['posts', 'feed'] as const,
  mine: ['posts', 'mine'] as const,
  detail: (id: number) => ['posts', 'detail', id] as const,
};

/** The homepage list: every user's posts, newest first. */
export function useFeed() {
  const { session } = useAuth();

  return useQuery({
    queryKey: postKeys.feed,
    queryFn: posts.listFeed,
    enabled: !!session,
  });
}

export function useMyPosts() {
  const { session } = useAuth();

  return useQuery({
    queryKey: postKeys.mine,
    queryFn: posts.listMine,
    enabled: !!session,
  });
}

export function usePost(id: number) {
  const { session } = useAuth();

  return useQuery({
    queryKey: postKeys.detail(id),
    queryFn: () => posts.getById(id),
    enabled: !!session && Number.isFinite(id),
  });
}

export function useCreatePost() {
  const queryClient = useQueryClient();
  const { user } = useAuth();

  return useMutation({
    mutationFn: ({ title, description }: { title: string; description: string }) => {
      if (!user) throw new Error('Not signed in');
      // The author is read from the body server-side, not from the token -- see CreatePostRequest.
      return posts.create(user.id, title, description);
    },
    // Both lists contain the new post -- the feed is what the user is looking at when they publish.
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: postKeys.feed }),
        queryClient.invalidateQueries({ queryKey: postKeys.mine }),
      ]),
  });
}

export function useUpdatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    // Note the field rename: the update endpoint takes `text` where create takes `description`,
    // and both write Post.description -- see UpdatePostRequest.
    mutationFn: ({ id, title, description }: { id: number; title: string; description: string }) =>
      posts.update(id, { title, text: description }),
    onSuccess: (_result, { id }) =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: postKeys.feed }),
        queryClient.invalidateQueries({ queryKey: postKeys.mine }),
        queryClient.invalidateQueries({ queryKey: postKeys.detail(id) }),
      ]),
  });
}

export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => posts.remove(id),
    onSuccess: (_result, id) => {
      queryClient.removeQueries({ queryKey: postKeys.detail(id) });
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: postKeys.feed }),
        queryClient.invalidateQueries({ queryKey: postKeys.mine }),
      ]);
    },
  });
}
