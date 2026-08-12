import { Redirect, useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, View } from 'react-native';

import type { FriendRequestResponse, UserResponse } from '@/api';
import { AppTabs } from '@/components/app-tabs';
import { Avatar } from '@/components/avatar';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import {
  useDeleteFriendRequest,
  useFriends,
  useIncomingRequests,
  useOutgoingRequests,
  useRemoveFriend,
  useRespondToRequest,
} from '@/hooks/use-friends';
import { useOpenConversation } from '@/hooks/use-chat';
import { useTheme } from '@/hooks/use-theme';

export default function FriendsScreen() {
  const { session, hydrating } = useAuth();
  const router = useRouter();

  const friends = useFriends();
  const incoming = useIncomingRequests();
  const outgoing = useOutgoingRequests();

  const respond = useRespondToRequest();
  const cancel = useDeleteFriendRequest();
  const removeFriend = useRemoveFriend();
  const openChat = useOpenConversation();

  /** Opening is idempotent server-side, so this is safe to call every time the button is hit. */
  async function openConversation(userId: number, username: string) {
    try {
      const conversation = await openChat.mutateAsync(userId);
      router.push({
        pathname: '/chat/[id]',
        params: { id: String(conversation.id), name: username },
      });
    } catch {
      // Surfaced via openChat.error -- 403 when the two are no longer friends.
    }
  }

  if (!hydrating && !session) return <Redirect href="/login" />;

  // The outgoing list also returns answered rows; only the pending ones are still cancellable.
  const sent = (outgoing.data ?? []).filter((request) => request.status === 'PENDING');
  const loading = friends.isLoading || incoming.isLoading || outgoing.isLoading;
  const error = friends.error ?? incoming.error ?? outgoing.error;
  const actionError = respond.error ?? cancel.error ?? removeFriend.error ?? openChat.error;

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <AppTabs active="friends" />

        {actionError && (
          <ThemedText type="small" themeColor="danger">
            {actionError.message}
          </ThemedText>
        )}
        {error && (
          <ThemedText type="small" themeColor="danger">
            {error.message}
          </ThemedText>
        )}
        {loading && <ActivityIndicator style={styles.spacer} />}

        <Section title={`Requests received (${incoming.data?.length ?? 0})`}>
          {incoming.data?.length ? (
            incoming.data.map((request) => (
              <PersonCard
                key={request.id}
                user={request.requester}
                caption="Wants to be friends"
                busy={respond.isPending && respond.variables?.id === request.id}>
                <Action
                  label="Decline"
                  onPress={() => respond.mutate({ id: request.id, action: 'decline' })}
                />
                <Action
                  label="Accept"
                  filled
                  onPress={() => respond.mutate({ id: request.id, action: 'accept' })}
                />
              </PersonCard>
            ))
          ) : (
            <Empty text="No one has sent you a request." />
          )}
        </Section>

        <Section title={`Requests sent (${sent.length})`}>
          {sent.length ? (
            sent.map((request) => (
              <PersonCard
                key={request.id}
                user={request.addressee}
                caption="Waiting for a reply"
                busy={cancel.isPending && cancel.variables === request.id}>
                <Action label="Cancel" onPress={() => cancel.mutate(request.id)} />
              </PersonCard>
            ))
          ) : (
            <Empty text="You have no pending requests." />
          )}
        </Section>

        <Section title={`Friends (${friends.data?.length ?? 0})`}>
          {friends.data?.length ? (
            friends.data.map((friend) => (
              <FriendCard
                key={friend.id}
                user={friend}
                busy={
                  (removeFriend.isPending && removeFriend.variables === friend.id) ||
                  (openChat.isPending && openChat.variables === friend.id)
                }
                onMessage={() => openConversation(friend.id, friend.username)}
                onRemove={() => removeFriend.mutate(friend.id)}
              />
            ))
          ) : (
            <Empty text="No friends yet. Add someone from the People tab." />
          )}
        </Section>
      </ScrollView>
    </ThemedView>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={styles.section}>
      <ThemedText type="smallBold">{title}</ThemedText>
      {children}
    </View>
  );
}

/** A friend row, whose Remove action asks twice -- unfriending has no undo and no confirmation UI. */
function FriendCard({
  user,
  busy,
  onMessage,
  onRemove,
}: {
  user: UserResponse;
  busy: boolean;
  onMessage: () => void;
  onRemove: () => void;
}) {
  const theme = useTheme();
  const [confirming, setConfirming] = useState(false);

  return (
    <PersonCard user={user} caption={`User #${user.id}`} busy={busy}>
      {confirming ? (
        <>
          <Action label="Keep" onPress={() => setConfirming(false)} />
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Confirm removing ${user.username}`}
            onPress={() => {
              setConfirming(false);
              onRemove();
            }}
            style={[styles.action, { backgroundColor: theme.danger }]}>
            <ThemedText type="smallBold" themeColor="onTint">
              Remove
            </ThemedText>
          </Pressable>
        </>
      ) : (
        <>
          <Action label="Message" filled onPress={onMessage} />
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`Remove ${user.username} from friends`}
            onPress={() => setConfirming(true)}
            style={styles.action}>
            <ThemedText type="smallBold" themeColor="danger">
              Remove
            </ThemedText>
          </Pressable>
        </>
      )}
    </PersonCard>
  );
}

function PersonCard({
  user,
  caption,
  busy,
  children,
}: {
  user: UserResponse;
  caption: string;
  busy?: boolean;
  children?: React.ReactNode;
}) {
  const theme = useTheme();

  return (
    <View style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
      <Avatar username={user.username} image={user.image} size={40} />
      <View style={styles.details}>
        <ThemedText type="smallBold">{user.username}</ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          {caption}
        </ThemedText>
      </View>
      {busy ? <ActivityIndicator /> : <View style={styles.actions}>{children}</View>}
    </View>
  );
}

function Action({ label, onPress, filled }: { label: string; onPress: () => void; filled?: boolean }) {
  const theme = useTheme();

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={[styles.action, filled && { backgroundColor: theme.tint }]}>
      <ThemedText type="smallBold" themeColor={filled ? 'onTint' : 'textSecondary'}>
        {label}
      </ThemedText>
    </Pressable>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <ThemedText type="small" themeColor="textSecondary">
      {text}
    </ThemedText>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: Spacing.three,
    gap: Spacing.four,
    maxWidth: 640,
    width: '100%',
    alignSelf: 'center',
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
  spacer: {
    paddingVertical: Spacing.four,
  },
});
