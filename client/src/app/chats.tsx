import { Redirect, useRouter } from 'expo-router';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, View } from 'react-native';

import type { ConversationResponse } from '@/api';
import { AppTabs } from '@/components/app-tabs';
import { Avatar } from '@/components/avatar';
import { Button } from '@/components/button';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import { useConversations } from '@/hooks/use-chat';
import { useTheme } from '@/hooks/use-theme';

export default function ChatsScreen() {
  const { session, hydrating } = useAuth();
  const router = useRouter();
  const query = useConversations();

  if (!hydrating && !session) return <Redirect href="/login" />;

  return (
    <ThemedView style={styles.container}>
      <FlatList
        data={query.data ?? []}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        refreshing={query.isRefetching}
        onRefresh={query.refetch}
        ListHeaderComponent={<AppTabs active="chats" />}
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
              No chats yet. Start one from the Friends tab.
            </ThemedText>
          )
        }
        renderItem={({ item }) => (
          <ConversationRow
            conversation={item}
            onPress={() =>
              router.push({
                pathname: '/chat/[id]',
                params: { id: String(item.id), name: item.otherUser.username },
              })
            }
          />
        )}
      />
    </ThemedView>
  );
}

function ConversationRow({
  conversation,
  onPress,
}: {
  conversation: ConversationResponse;
  onPress: () => void;
}) {
  const theme = useTheme();
  const unread = conversation.unreadCount;

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      // Read out as one label so the count is announced with the name rather than as a loose
      // number after it.
      accessibilityLabel={
        unread > 0
          ? `${conversation.otherUser.username}, ${unread} unread ${unread === 1 ? 'message' : 'messages'}`
          : conversation.otherUser.username
      }
      style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
      <Avatar
        username={conversation.otherUser.username}
        image={conversation.otherUser.image}
        size={40}
      />
      <View style={styles.details}>
        <ThemedText type="smallBold">{conversation.otherUser.username}</ThemedText>
        <ThemedText
          type={unread > 0 ? 'smallBold' : 'small'}
          themeColor={unread > 0 ? 'text' : 'textSecondary'}>
          {/* Still keyed on lastSeq rather than just printing the timestamp: lastMessageAt is
              seeded to the creation time, so an empty chat would otherwise show a time no
              message was ever sent at. */}
          {conversation.lastSeq === 0
            ? 'No messages yet'
            : formatTimestamp(conversation.lastMessageAt)}
        </ThemedText>
      </View>
      {unread > 0 && (
        // Already covered by the row's accessibilityLabel, so hide it from screen readers rather
        // than have the count announced twice.
        <View
          accessibilityElementsHidden
          importantForAccessibility="no-hide-descendants"
          style={[styles.badge, { backgroundColor: theme.tint }]}>
          <ThemedText type="smallBold" style={[styles.badgeCount, { color: theme.onTint }]}>
            {unread > 99 ? '99+' : unread}
          </ThemedText>
        </View>
      )}
    </Pressable>
  );
}

/** No timezone offset on the wire, so Date parses it as local time -- which is what we want. */
function formatTimestamp(value: string): string {
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
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    padding: Spacing.three,
    borderRadius: Spacing.two,
  },
  details: {
    flex: 1,
    gap: Spacing.half,
  },
  badge: {
    // minWidth rather than width so "99+" widens the pill instead of overflowing it; the matching
    // borderRadius keeps it a circle at one digit and a capsule beyond that.
    minWidth: 24,
    height: 24,
    borderRadius: 12,
    paddingHorizontal: Spacing.one,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeCount: {
    // The shared line-height of `smallBold` is taller than the pill and would push the digits low.
    lineHeight: 16,
  },
  spacer: {
    paddingVertical: Spacing.four,
    gap: Spacing.three,
  },
});
