import { Redirect } from 'expo-router';
import { ActivityIndicator, ScrollView, StyleSheet, View } from 'react-native';

import { Avatar } from '@/components/avatar';
import { Button } from '@/components/button';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import { useMyPosts } from '@/hooks/use-posts';
import { useMe, useMyActivity } from '@/hooks/use-users';
import { useTheme } from '@/hooks/use-theme';

export default function ProfileScreen() {
  const { session, hydrating, logout } = useAuth();
  const theme = useTheme();

  const me = useMe();
  const activity = useMyActivity();
  const myPosts = useMyPosts();

  if (!hydrating && !session) return <Redirect href="/login" />;

  // `me` starts from the session snapshot, so this only shows before hydration finishes.
  if (!me.data) {
    return (
      <ThemedView style={styles.centered}>
        {me.error ? (
          <>
            <ThemedText type="small" themeColor="danger">
              {me.error.message}
            </ThemedText>
            <Button title="Retry" variant="secondary" onPress={() => me.refetch()} />
          </>
        ) : (
          <ActivityIndicator />
        )}
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.identity}>
          <Avatar username={me.data.username} image={me.data.image} size={88} />
          <ThemedText type="subtitle">{me.data.username}</ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            User #{me.data.id}
          </ThemedText>
        </View>

        <View style={styles.stats}>
          <Stat label="Posts" value={myPosts.data?.length} loading={myPosts.isLoading} />
          <Stat label="Likes" value={activity.data?.likes.length} loading={activity.isLoading} />
          <Stat
            label="Comments"
            value={activity.data?.comments.length}
            loading={activity.isLoading}
          />
        </View>

        {(activity.error || myPosts.error) && (
          <ThemedText type="small" themeColor="danger">
            {(activity.error ?? myPosts.error)?.message}
          </ThemedText>
        )}

        <View style={[styles.card, { backgroundColor: theme.backgroundElement }]}>
          <Row label="Username" value={me.data.username} />
          <Row label="User ID" value={String(me.data.id)} />
          <Row label="Profile image" value={me.data.image ? 'Set' : 'Not set'} />
        </View>

        <Button title="Log out" variant="secondary" onPress={logout} />
      </ScrollView>
    </ThemedView>
  );
}

function Stat({ label, value, loading }: { label: string; value?: number; loading: boolean }) {
  const theme = useTheme();

  return (
    <View style={[styles.stat, { backgroundColor: theme.backgroundElement }]}>
      {loading && value === undefined ? (
        <ActivityIndicator />
      ) : (
        <ThemedText type="subtitle">{value ?? '--'}</ThemedText>
      )}
      <ThemedText type="small" themeColor="textSecondary">
        {label}
      </ThemedText>
    </View>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.row}>
      <ThemedText type="small" themeColor="textSecondary">
        {label}
      </ThemedText>
      <ThemedText type="smallBold">{value}</ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.three,
    padding: Spacing.four,
  },
  content: {
    padding: Spacing.three,
    gap: Spacing.four,
    maxWidth: 640,
    width: '100%',
    alignSelf: 'center',
  },
  identity: {
    alignItems: 'center',
    gap: Spacing.two,
    paddingTop: Spacing.three,
  },
  stats: {
    flexDirection: 'row',
    gap: Spacing.three,
  },
  stat: {
    flex: 1,
    alignItems: 'center',
    gap: Spacing.one,
    paddingVertical: Spacing.three,
    borderRadius: Spacing.two,
  },
  card: {
    gap: Spacing.three,
    padding: Spacing.three,
    borderRadius: Spacing.two,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: Spacing.three,
  },
});