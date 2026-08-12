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

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
      <Avatar
        username={conversation.otherUser.username}
        image={conversation.otherUser.image}
        size={40}
      />
      <View style={styles.details}>
        <ThemedText type="smallBold">{conversation.otherUser.username}</ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          {conversation.lastSeq === 0
            ? 'No messages yet'
            : `${conversation.lastSeq} ${conversation.lastSeq === 1 ? 'message' : 'messages'} · ${formatTimestamp(conversation.lastMessageAt)}`}
        </ThemedText>
      </View>
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
  spacer: {
    paddingVertical: Spacing.four,
    gap: Spacing.three,
  },
});
