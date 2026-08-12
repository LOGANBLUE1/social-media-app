import { Redirect, useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, View } from 'react-native';

import { Button } from '@/components/button';
import { PostEditor } from '@/components/post-editor';
import { AppTabs } from '@/components/app-tabs';
import { PostRow } from '@/components/post-row';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import { useDeletePost, useMyPosts } from '@/hooks/use-posts';

export default function MyPostsScreen() {
  const { session, hydrating } = useAuth();
  const router = useRouter();
  const query = useMyPosts();
  const deletePost = useDeletePost();

  /** Id of the post whose row is currently swapped for the editor, if any. */
  const [editingId, setEditingId] = useState<number | null>(null);

  if (!hydrating && !session) return <Redirect href="/login" />;

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
            <AppTabs active="my-posts" />
            {deletePost.error && (
              <ThemedText type="small" themeColor="danger">
                {deletePost.error.message}
              </ThemedText>
            )}
          </View>
        }
        ListEmptyComponent={
          query.isLoading ? (
            <ActivityIndicator style={styles.spacer} />
          ) : query.error ? (
            <View style={styles.spacer}>
              <ThemedText type="small" themeColor="danger">
                {query.error.message}
              </ThemedText>
              <Button title="Retry" variant="secondary" onPress={() => query.refetch()} />
            </View>
          ) : (
            <View style={styles.spacer}>
              <ThemedText type="small" themeColor="textSecondary">
                You have not posted anything yet.
              </ThemedText>
              <Button
                title="Write a post"
                variant="secondary"
                onPress={() => router.replace('/feed')}
              />
            </View>
          )
        }
        renderItem={({ item }) =>
          editingId === item.id ? (
            <PostEditor post={item} onDone={() => setEditingId(null)} />
          ) : (
            <PostRow
              post={item}
              onPress={() =>
                router.push({ pathname: '/post/[id]', params: { id: String(item.id) } })
              }
              onEdit={() => setEditingId(item.id)}
              onDelete={() => deletePost.mutate(item.id)}
              // `variables` is the id passed to the in-flight mutation, so only that row spins.
              deleting={deletePost.isPending && deletePost.variables === item.id}
            />
          )
        }
      />
    </ThemedView>
  );
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
    gap: Spacing.two,
  },
  spacer: {
    paddingVertical: Spacing.four,
    gap: Spacing.three,
  },
});