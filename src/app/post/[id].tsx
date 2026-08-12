import { Redirect, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, View } from 'react-native';

import { Button } from '@/components/button';
import { TextField } from '@/components/text-field';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import { useCreateComment, usePostComments } from '@/hooks/use-comments';
import { useToggleLike } from '@/hooks/use-likes';
import { usePost } from '@/hooks/use-posts';
import { useTheme } from '@/hooks/use-theme';

export default function PostDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const postId = Number(id);
  const { session, hydrating } = useAuth();
  const theme = useTheme();

  const post = usePost(postId);
  const comments = usePostComments(postId);
  const like = useToggleLike(post.data);
  const createComment = useCreateComment(postId);

  const [text, setText] = useState('');

  if (!hydrating && !session) return <Redirect href="/login" />;

  async function submitComment() {
    try {
      await createComment.mutateAsync(text.trim());
      setText('');
    } catch {
      // Surfaced below via createComment.error.
    }
  }

  if (post.isLoading) {
    return (
      <ThemedView style={styles.centered}>
        <ActivityIndicator />
      </ThemedView>
    );
  }

  if (post.error || !post.data) {
    return (
      <ThemedView style={styles.centered}>
        <ThemedText type="small" themeColor="danger">
          {post.error?.message ?? 'Post not found'}
        </ThemedText>
        <Button title="Retry" variant="secondary" onPress={() => post.refetch()} />
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <ThemedText type="subtitle">{post.data.title}</ThemedText>
        {!!post.data.description && <ThemedText>{post.data.description}</ThemedText>}

        <View style={styles.likeRow}>
          <Pressable
            accessibilityRole="button"
            disabled={like.isPending}
            onPress={() => like.mutate()}
            style={[
              styles.likeButton,
              { backgroundColor: like.likedByMe ? theme.tint : theme.backgroundElement },
              like.isPending && styles.pending,
            ]}>
            <ThemedText
              type="smallBold"
              style={{ color: like.likedByMe ? theme.onTint : theme.text }}>
              {like.likedByMe ? 'Liked' : 'Like'}
            </ThemedText>
          </Pressable>
          <ThemedText type="small" themeColor="textSecondary">
            {like.count} {like.count === 1 ? 'like' : 'likes'}
          </ThemedText>
        </View>

        {like.error && (
          <ThemedText type="small" themeColor="danger">
            {like.error.message}
          </ThemedText>
        )}

        <ThemedText type="smallBold" style={styles.sectionTitle}>
          Comments
        </ThemedText>

        {comments.isLoading && <ActivityIndicator />}
        {comments.error && (
          <ThemedText type="small" themeColor="danger">
            {comments.error.message}
          </ThemedText>
        )}
        {comments.data?.length === 0 && (
          <ThemedText type="small" themeColor="textSecondary">
            No comments yet.
          </ThemedText>
        )}

        {comments.data?.map((comment) => (
          <View
            key={comment.id}
            style={[styles.comment, { backgroundColor: theme.backgroundElement }]}>
            <ThemedText type="smallBold">{comment.username}</ThemedText>
            <ThemedText type="small">{comment.text}</ThemedText>
          </View>
        ))}

        <View style={styles.composer}>
          <TextField
            label="Add a comment"
            value={text}
            onChangeText={setText}
            multiline
            style={styles.multiline}
          />
          {createComment.error && (
            <ThemedText type="small" themeColor="danger">
              {createComment.error.message}
            </ThemedText>
          )}
          <Button
            title="Comment"
            onPress={submitComment}
            loading={createComment.isPending}
            disabled={text.trim().length === 0 || createComment.isPending}
          />
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.three,
    padding: Spacing.four,
  },
  content: {
    padding: Spacing.three,
    gap: Spacing.three,
    maxWidth: 640,
    width: '100%',
    alignSelf: 'center',
  },
  likeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
  },
  likeButton: {
    minHeight: 40,
    paddingHorizontal: Spacing.four,
    borderRadius: Spacing.two,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pending: {
    opacity: 0.5,
  },
  sectionTitle: {
    marginTop: Spacing.three,
  },
  comment: {
    gap: Spacing.half,
    padding: Spacing.three,
    borderRadius: Spacing.two,
  },
  composer: {
    marginTop: Spacing.three,
    gap: Spacing.three,
  },
  multiline: {
    minHeight: 72,
    textAlignVertical: 'top',
  },
});
