import { Redirect } from 'expo-router';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, View } from 'react-native';

import type { FriendRequestResponse, UserResponse } from '@/api';
import { AppTabs } from '@/components/app-tabs';
import { Avatar } from '@/components/avatar';
import { Button } from '@/components/button';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import {
  useDeleteFriendRequest,
  useFriends,
  useIncomingRequests,
  useOutgoingRequests,
  useRespondToRequest,
  useSendFriendRequest,
} from '@/hooks/use-friends';
import { useUsers } from '@/hooks/use-users';
import { useTheme } from '@/hooks/use-theme';

export default function UsersScreen() {
  const { session, user, hydrating } = useAuth();
  const query = useUsers();

  const friends = useFriends();
  const incoming = useIncomingRequests();
  const outgoing = useOutgoingRequests();

  const send = useSendFriendRequest();
  const respond = useRespondToRequest();
  const cancel = useDeleteFriendRequest();

  if (!hydrating && !session) return <Redirect href="/login" />;

  const friendIds = new Set((friends.data ?? []).map((friend) => friend.id));
  // Keyed by the *other* user so a row can find its own request without scanning the list.
  const incomingByRequester = new Map(
    (incoming.data ?? []).map((request) => [request.requester.id, request]),
  );
  const outgoingPendingByAddressee = new Map(
    (outgoing.data ?? [])
      .filter((request) => request.status === 'PENDING')
      .map((request) => [request.addressee.id, request]),
  );

  const actionError = send.error ?? respond.error ?? cancel.error;

  return (
    <ThemedView style={styles.container}>
      <FlatList
        data={query.data ?? []}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        refreshing={query.isRefetching}
        onRefresh={() => {
          void query.refetch();
          void friends.refetch();
          void incoming.refetch();
          void outgoing.refetch();
        }}
        ListHeaderComponent={
          <View style={styles.header}>
            <AppTabs active="users" />

            {actionError && (
              <ThemedText type="small" themeColor="danger">
                {actionError.message}
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
            <ThemedText type="small" themeColor="textSecondary" style={styles.spacer}>
              Nobody has signed up yet.
            </ThemedText>
          )
        }
        renderItem={({ item }) => {
          const sent = outgoingPendingByAddressee.get(item.id);
          const received = incomingByRequester.get(item.id);

          return (
            <UserRow
              user={item}
              isMe={item.id === user?.id}
              isFriend={friendIds.has(item.id)}
              sentRequest={sent}
              receivedRequest={received}
              busy={
                (send.isPending && send.variables === item.id) ||
                (respond.isPending && respond.variables?.id === received?.id) ||
                (cancel.isPending && cancel.variables === sent?.id)
              }
              onAdd={() => send.mutate(item.id)}
              onAccept={() => received && respond.mutate({ id: received.id, action: 'accept' })}
              onCancel={() => sent && cancel.mutate(sent.id)}
            />
          );
        }}
      />
    </ThemedView>
  );
}

/** `GET /users` includes the caller, so their own row is labelled rather than filtered out. */
function UserRow({
  user,
  isMe,
  isFriend,
  sentRequest,
  receivedRequest,
  busy,
  onAdd,
  onAccept,
  onCancel,
}: {
  user: UserResponse;
  isMe: boolean;
  isFriend: boolean;
  sentRequest?: FriendRequestResponse;
  receivedRequest?: FriendRequestResponse;
  busy: boolean;
  onAdd: () => void;
  onAccept: () => void;
  onCancel: () => void;
}) {
  const theme = useTheme();

  return (
    <View style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
      <Avatar username={user.username} image={user.image} size={40} />
      <View style={styles.details}>
        <ThemedText type="smallBold">{user.username}</ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          User #{user.id}
        </ThemedText>
      </View>

      {isMe ? (
        <View style={[styles.badge, { backgroundColor: theme.backgroundSelected }]}>
          <ThemedText type="small" themeColor="textSecondary">
            You
          </ThemedText>
        </View>
      ) : busy ? (
        <ActivityIndicator />
      ) : isFriend ? (
        <View style={[styles.badge, { backgroundColor: theme.backgroundSelected }]}>
          <ThemedText type="small" themeColor="tint">
            Friends
          </ThemedText>
        </View>
      ) : receivedRequest ? (
        // They asked first. Sending back would accept anyway, but say so plainly.
        <Action label="Accept" filled onPress={onAccept} />
      ) : sentRequest ? (
        <Action label="Cancel request" color="textSecondary" onPress={onCancel} />
      ) : (
        <Action label="Add friend" filled onPress={onAdd} />
      )}
    </View>
  );
}

function Action({
  label,
  onPress,
  filled,
  color = 'text',
}: {
  label: string;
  onPress: () => void;
  filled?: boolean;
  color?: 'text' | 'textSecondary';
}) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={[styles.action, filled && { backgroundColor: theme.tint }]}>
      <ThemedText type="smallBold" themeColor={filled ? 'onTint' : color}>
        {label}
      </ThemedText>
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
  header: {
    gap: Spacing.three,
  },
  section: {
    gap: Spacing.two,
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
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  action: {
    minHeight: 34,
    paddingHorizontal: Spacing.three,
    borderRadius: Spacing.one + Spacing.half,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badge: {
    paddingHorizontal: Spacing.two,
    paddingVertical: Spacing.half,
    borderRadius: Spacing.two,
  },
  spacer: {
    paddingVertical: Spacing.four,
    gap: Spacing.three,
  },
});
