import { Redirect, Stack, useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, View } from 'react-native';

import type { UserResponse } from '@/api';
import { useAuth } from '@/auth/auth-context';
import { Avatar } from '@/components/avatar';
import { Button } from '@/components/button';
import { TextField } from '@/components/text-field';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useCreateGroup } from '@/hooks/use-chat';
import { useFriends } from '@/hooks/use-friends';
import { useTheme } from '@/hooks/use-theme';

/**
 * Pick a name and some friends, get a group chat.
 *
 * The candidate list is the friends list rather than all users, because that is what the server
 * accepts -- adding a non-friend is a 403, so offering one would be an invitation to fail.
 */
export default function NewGroupScreen() {
  const { session, hydrating } = useAuth();
  const router = useRouter();
  const friends = useFriends();
  const create = useCreateGroup();

  const [name, setName] = useState('');
  const [selected, setSelected] = useState<number[]>([]);

  if (!hydrating && !session) return <Redirect href="/login" />;

  function toggle(userId: number) {
    setSelected((current) =>
      current.includes(userId)
        ? current.filter((id) => id !== userId)
        : [...current, userId],
    );
  }

  const trimmedName = name.trim();
  const canCreate = trimmedName.length > 0 && selected.length > 0 && !create.isPending;

  async function submit() {
    try {
      const conversation = await create.mutateAsync({ name: trimmedName, memberIds: selected });
      // replace, not push: coming back to a half-filled create form after making the group is
      // never what you want.
      router.replace({
        pathname: '/chat/[id]',
        // Carry the name so the header is right immediately -- the chat list is being refetched
        // at this point, so deriving it from the cache would flash "Chat" first.
        params: { id: String(conversation.id), name: trimmedName },
      });
    } catch {
      // Surfaced below via create.error; the form keeps its contents so nothing is retyped.
    }
  }

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{ title: 'New group' }} />
      <FlatList
        data={friends.data ?? []}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <View style={styles.header}>
            <TextField
              label="Group name"
              value={name}
              onChangeText={setName}
              placeholder="Weekend plans"
              maxLength={120}
            />
            <ThemedText type="smallBold" themeColor="textSecondary">
              {selected.length === 0
                ? 'Add friends'
                : `${selected.length} selected`}
            </ThemedText>
            {create.error && (
              <ThemedText type="small" themeColor="danger">
                {create.error.message}
              </ThemedText>
            )}
          </View>
        }
        ListEmptyComponent={
          friends.isLoading ? (
            <ActivityIndicator style={styles.spacer} />
          ) : friends.error ? (
            <View style={styles.spacer}>
              <ThemedText type="small" themeColor="danger">
                {friends.error.message}
              </ThemedText>
              <Button title="Retry" variant="secondary" onPress={() => friends.refetch()} />
            </View>
          ) : (
            <ThemedText type="small" themeColor="textSecondary" style={styles.spacer}>
              You need friends before you can make a group. Add some from the People tab.
            </ThemedText>
          )
        }
        renderItem={({ item }) => (
          <FriendRow
            friend={item}
            selected={selected.includes(item.id)}
            onToggle={() => toggle(item.id)}
          />
        )}
      />

      <View style={styles.footer}>
        <Button
          title={create.isPending ? 'Creating…' : 'Create group'}
          onPress={submit}
          loading={create.isPending}
          disabled={!canCreate}
        />
      </View>
    </ThemedView>
  );
}

function FriendRow({
  friend,
  selected,
  onToggle,
}: {
  friend: UserResponse;
  selected: boolean;
  onToggle: () => void;
}) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onToggle}
      accessibilityRole="checkbox"
      accessibilityState={{ checked: selected }}
      accessibilityLabel={friend.username}
      style={[
        styles.card,
        { backgroundColor: selected ? theme.backgroundSelected : theme.backgroundElement },
      ]}>
      <Avatar username={friend.username} image={friend.image} size={40} />
      <ThemedText type="smallBold" style={styles.name}>
        {friend.username}
      </ThemedText>
      {/* A drawn box rather than a platform checkbox: react-native has none that works on web,
          native and iOS alike without pulling in a dependency for one screen. */}
      <View
        style={[
          styles.check,
          selected
            ? { backgroundColor: theme.tint, borderColor: theme.tint }
            : { borderColor: theme.border },
        ]}>
        {selected && (
          <ThemedText style={[styles.tick, { color: theme.onTint }]}>✓</ThemedText>
        )}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  list: {
    padding: Spacing.three,
    gap: Spacing.two,
    maxWidth: 640,
    width: '100%',
    alignSelf: 'center',
  },
  header: {
    gap: Spacing.three,
    paddingBottom: Spacing.two,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    padding: Spacing.three,
    borderRadius: Spacing.two,
  },
  name: {
    flex: 1,
  },
  check: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tick: {
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
  },
  footer: {
    padding: Spacing.three,
    maxWidth: 640,
    width: '100%',
    alignSelf: 'center',
  },
  spacer: {
    paddingVertical: Spacing.four,
    gap: Spacing.three,
  },
});
