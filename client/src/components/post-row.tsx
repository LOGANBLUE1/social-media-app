import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, View } from 'react-native';

import type { PostResponse } from '@/api';
import { NEW_POST_HOURS, NewBadge } from '@/components/new-badge';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { formatDateTime } from '@/utils/time';

type Props = {
  post: PostResponse;
  onPress: () => void;
  /** Omit these to hide the row actions -- only the owner's own lists pass them. */
  onEdit?: () => void;
  onDelete?: () => void;
  deleting?: boolean;
};

/** One card in a post list -- shared by the feed and the my-posts screens. */
export function PostRow({ post, onPress, onEdit, onDelete, deleting }: Props) {
  const theme = useTheme();
  // Deleting is irreversible and there is no undo, so the button asks twice. Alert.alert is
  // deliberately avoided: it is a no-op on react-native-web, and this app ships to web.
  const [confirming, setConfirming] = useState(false);

  return (
    // A View, not a Pressable: the delete controls are Pressables themselves, and on
    // react-native-web nesting them inside one renders an invalid <button> in a <button>.
    <View style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
      <Pressable onPress={onPress} accessibilityRole="button" style={styles.body}>
        <View style={styles.titleRow}>
          <ThemedText type="smallBold" style={styles.title}>
            {post.title}
          </ThemedText>
          <NewBadge createdAt={post.createdAt} withinHours={NEW_POST_HOURS} />
        </View>
        <ThemedText type="small" themeColor="textSecondary">
          {post.author.username}
        </ThemedText>
        {!!post.description && (
          <ThemedText type="small" numberOfLines={2}>
            {post.description}
          </ThemedText>
        )}
        <ThemedText type="small" themeColor="textSecondary">
          {post.likeCount} {post.likeCount === 1 ? 'like' : 'likes'} · {post.commentCount}{' '}
          {post.commentCount === 1 ? 'comment' : 'comments'} · {formatDateTime(post.createdAt)}
        </ThemedText>
      </Pressable>

      {(onEdit || onDelete) && (
        <View style={styles.footer}>
          <View style={styles.actions}>
            {deleting ? (
              <ActivityIndicator />
            ) : confirming ? (
              <>
                <Pressable
                  accessibilityRole="button"
                  onPress={() => setConfirming(false)}
                  style={styles.action}>
                  <ThemedText type="smallBold" themeColor="textSecondary">
                    Cancel
                  </ThemedText>
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`Confirm deleting ${post.title}`}
                  onPress={() => {
                    setConfirming(false);
                    onDelete?.();
                  }}
                  style={[styles.action, { backgroundColor: theme.danger }]}>
                  <ThemedText type="smallBold" themeColor="onTint">
                    Confirm
                  </ThemedText>
                </Pressable>
              </>
            ) : (
              <>
                {onEdit && (
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`Edit ${post.title}`}
                    onPress={onEdit}
                    style={styles.action}>
                    <ThemedText type="smallBold" themeColor="tint">
                      Edit
                    </ThemedText>
                  </Pressable>
                )}
                {onDelete && (
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`Delete ${post.title}`}
                    onPress={() => setConfirming(true)}
                    style={styles.action}>
                    <ThemedText type="smallBold" themeColor="danger">
                      Delete
                    </ThemedText>
                  </Pressable>
                )}
              </>
            )}
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    gap: Spacing.two,
    padding: Spacing.three,
    borderRadius: Spacing.two,
  },
  body: {
    gap: Spacing.two,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  title: {
    // Shrinks so a long title truncates rather than pushing the badge off the card.
    flexShrink: 1,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  action: {
    minHeight: 32,
    paddingHorizontal: Spacing.two,
    borderRadius: Spacing.one + Spacing.half,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
