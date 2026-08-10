import { Redirect, useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, View } from 'react-native';

import type { PostResponse } from '@/api';
import { Button } from '@/components/button';
import { TextField } from '@/components/text-field';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import { useCreatePost, useFeed } from '@/hooks/use-posts';
import { useTheme } from '@/hooks/use-theme';

export default function FeedScreen() {
  const { session, user, hydrating, logout } = useAuth();
  const router = useRouter();
  const theme = useTheme();
  const query = useFeed();
  const createPost = useCreatePost();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  if (!hydrating && !session) return <Redirect href="/login" />;

  async function submit() {
    try {
      await createPost.mutateAsync({ title: title.trim(), description: description.trim() });
      setTitle('');
      setDescription('');
    } catch {
      // Surfaced below via createPost.error -- nothing to do here.
    }
  }

  const canSubmit = title.trim().length > 0 && !createPost.isPending;

  return (
    <ThemedView style={styles.container}>
      <FlatList
        data={query.data ?? []}
        keyExtractor={(post) => String(post.id)}
        contentContainerStyle={styles.list}
        refreshing={query.isRefetching}
        onRefresh={query.refetch}
        ListHeaderComponent={
          <View style={styles.header}>
            <View style={styles.headerRow}>
              <ThemedText type="smallBold" themeColor="textSecondary">
                {user ? `Signed in as ${user.username}` : ''}
              </ThemedText>
              <Pressable onPress={logout} accessibilityRole="button">
                <ThemedText type="linkPrimary">Log out</ThemedText>
              </Pressable>
            </View>

            <View style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
              <ThemedText type="smallBold">New post</ThemedText>
              <TextField label="Title" value={title} onChangeText={setTitle} />
              <TextField
                label="Description"
                value={description}
                onChangeText={setDescription}
                multiline
                numberOfLines={3}
                style={styles.multiline}
              />
              {createPost.error && (
                <ThemedText type="small" style={styles.error}>
                  {createPost.error.message}
                </ThemedText>
              )}
              <Button
                title="Publish"
                onPress={submit}
                loading={createPost.isPending}
                disabled={!canSubmit}
              />
            </View>
          </View>
        }
        ListEmptyComponent={
          query.isLoading ? (
            <ActivityIndicator style={styles.spacer} />
          ) : query.error ? (
            <View style={styles.spacer}>
              <ThemedText type="small" style={styles.error}>
                {query.error.message}
              </ThemedText>
              <Button title="Retry" variant="secondary" onPress={() => query.refetch()} />
            </View>
          ) : (
            <ThemedText type="small" themeColor="textSecondary" style={styles.spacer}>
              No posts yet. Publish your first one above.
            </ThemedText>
          )
        }
        renderItem={({ item }) => (
          <PostRow
            post={item}
            onPress={() =>
              router.push({ pathname: '/post/[id]', params: { id: String(item.id) } })
            }
          />
        )}
      />
    </ThemedView>
  );
}

function PostRow({ post, onPress }: { post: PostResponse; onPress: () => void }) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
      <ThemedText type="smallBold">{post.title}</ThemedText>
      {!!post.description && (
        <ThemedText type="small" numberOfLines={2}>
          {post.description}
        </ThemedText>
      )}
      <ThemedText type="small" themeColor="textSecondary">
        {post.likes?.length ?? 0} likes · {formatCreatedAt(post.createdAt)}
      </ThemedText>
    </Pressable>
  );
}

/** `createdAt` has no timezone offset, so Date parses it as local time -- which is what we want. */
function formatCreatedAt(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString();
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  list: {
    padding: Spacing.three,
    gap: Spacing.three,
    maxWidth: 640,
    width: '100%',
    alignSelf: 'center',
  },
  header: {
    gap: Spacing.three,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  card: {
    gap: Spacing.two,
    padding: Spacing.three,
    borderRadius: Spacing.two,
  },
  multiline: {
    minHeight: 80,
    textAlignVertical: 'top',
  },
  spacer: {
    paddingVertical: Spacing.four,
    gap: Spacing.three,
  },
  error: {
    color: '#d93025',
  },
});
