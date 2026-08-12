import { Redirect, useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, View } from 'react-native';

import { Button } from '@/components/button';
import { AppTabs } from '@/components/app-tabs';
import { PostRow } from '@/components/post-row';
import { TextField } from '@/components/text-field';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import { useCreatePost, useFeed } from '@/hooks/use-posts';
import { useTheme } from '@/hooks/use-theme';

export default function FeedScreen() {
  const { session, hydrating } = useAuth();
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
            <AppTabs active="feed" />

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
                <ThemedText type="small" themeColor="danger">
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
              <ThemedText type="small" themeColor="danger">
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
});
