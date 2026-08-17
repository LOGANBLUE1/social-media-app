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
import { conversationTitle, groupMemberSummary } from '@/utils/conversation';
import { formatDateTime } from '@/utils/time';

export default function ChatsScreen() {
  const { session, user, hydrating } = useAuth();
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
        ListHeaderComponent={
          <View style={styles.header}>
            <AppTabs active="chats" />
            <Button
              title="New group"
              variant="secondary"
              onPress={() => router.push('/new-group')}
            />
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
              No chats yet. Start one from the Friends tab.
            </ThemedText>
          )
        }
        renderItem={({ item }) => (
          <ConversationRow
            conversation={item}
            viewerId={user?.id}
            onPress={() =>
              router.push({
                pathname: '/chat/[id]',
                // The title is passed through so the header is right on the first frame; the chat
                // screen re-derives it from the conversation itself for deep links, where there
                // are no params to inherit.
                params: { id: String(item.id), name: conversationTitle(item) },
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
  viewerId,
  onPress,
}: {
  conversation: ConversationResponse;
  viewerId: number | undefined;
  onPress: () => void;
}) {
  const theme = useTheme();
  const unread = conversation.unreadCount;
  const isGroup = conversation.type === 'GROUP';
  const title = conversationTitle(conversation);

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      // Read out as one label so the count is announced with the name rather than as a loose
      // number after it.
      accessibilityLabel={
        unread > 0
          ? `${title}, ${unread} unread ${unread === 1 ? 'message' : 'messages'}`
          : title
      }
      style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
      {/* A group has no single face, so the fallback initial comes from its name. */}
      <Avatar
        username={title}
        image={isGroup ? null : conversation.otherUser?.image}
        size={40}
      />
      <View style={styles.details}>
        <View style={styles.titleRow}>
          <ThemedText type="smallBold" numberOfLines={1} style={styles.title}>
            {title}
          </ThemedText>
          {isGroup && (
            <View style={[styles.groupTag, { backgroundColor: theme.backgroundSelected }]}>
              <ThemedText type="small" themeColor="textSecondary" style={styles.groupTagText}>
                {conversation.participants.length}
              </ThemedText>
            </View>
          )}
        </View>
        {isGroup && (
          <ThemedText type="small" themeColor="textSecondary" numberOfLines={1}>
            {groupMemberSummary(conversation, viewerId)}
          </ThemedText>
        )}
        <ThemedText
          type={unread > 0 ? 'smallBold' : 'small'}
          themeColor={unread > 0 ? 'text' : 'textSecondary'}>
          {/* Still keyed on lastSeq rather than just printing the timestamp: lastMessageAt is
              seeded to the creation time, so an empty chat would otherwise show a time no
              message was ever sent at. */}
          {conversation.lastSeq === 0
            ? 'No messages yet'
            : formatDateTime(conversation.lastMessageAt)}
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
  header: {
    gap: Spacing.three,
  },
  details: {
    flex: 1,
    gap: Spacing.half,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  title: {
    flexShrink: 1,
  },
  groupTag: {
    minWidth: 20,
    paddingHorizontal: Spacing.one,
    borderRadius: Spacing.one,
    alignItems: 'center',
  },
  groupTagText: {
    fontSize: 11,
    lineHeight: 16,
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
