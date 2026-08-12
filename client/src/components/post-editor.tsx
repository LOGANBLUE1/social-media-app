import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import type { PostResponse } from '@/api';
import { Button } from '@/components/button';
import { TextField } from '@/components/text-field';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useUpdatePost } from '@/hooks/use-posts';
import { useTheme } from '@/hooks/use-theme';

/** Replaces a PostRow in place while its post is being edited. */
export function PostEditor({ post, onDone }: { post: PostResponse; onDone: () => void }) {
  const theme = useTheme();
  const update = useUpdatePost();

  const [title, setTitle] = useState(post.title);
  const [description, setDescription] = useState(post.description ?? '');

  async function save() {
    try {
      await update.mutateAsync({ id: post.id, title: title.trim(), description: description.trim() });
      onDone();
    } catch {
      // Surfaced below via update.error -- the editor stays open so the text is not lost.
    }
  }

  const canSave = title.trim().length > 0 && !update.isPending;

  return (
    <View style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
      <ThemedText type="smallBold">Edit post</ThemedText>
      <TextField label="Title" value={title} onChangeText={setTitle} />
      <TextField
        label="Description"
        value={description}
        onChangeText={setDescription}
        multiline
        numberOfLines={3}
        style={styles.multiline}
      />
      {update.error && (
        <ThemedText type="small" themeColor="danger">
          {update.error.message}
        </ThemedText>
      )}
      <View style={styles.actions}>
        <View style={styles.action}>
          <Button title="Cancel" variant="secondary" onPress={onDone} disabled={update.isPending} />
        </View>
        <View style={styles.action}>
          <Button title="Save" onPress={save} loading={update.isPending} disabled={!canSave} />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    gap: Spacing.two,
    padding: Spacing.three,
    borderRadius: Spacing.two,
  },
  multiline: {
    minHeight: 80,
    textAlignVertical: 'top',
  },
  actions: {
    flexDirection: 'row',
    gap: Spacing.two,
  },
  action: {
    flex: 1,
  },
});
