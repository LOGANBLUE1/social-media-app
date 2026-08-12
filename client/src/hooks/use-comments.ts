import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { comments } from '@/api';
import { useAuth } from '@/auth/auth-context';

export const commentKeys = {
  forPost: (postId: number) => ['comments', postId] as const,
};

export function usePostComments(postId: number) {
  const { session } = useAuth();

  return useQuery({
    queryKey: commentKeys.forPost(postId),
    queryFn: () => comments.listForPost(postId),
    enabled: !!session && Number.isFinite(postId),
  });
}

export function useCreateComment(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (text: string) => comments.create(postId, text),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: commentKeys.forPost(postId) }),
  });
}

export function useDeleteComment(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (commentId: number) => comments.remove(commentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: commentKeys.forPost(postId) }),
  });
}
