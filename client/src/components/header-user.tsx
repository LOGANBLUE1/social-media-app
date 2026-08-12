import { useRouter } from 'expo-router';
import { Pressable, StyleSheet } from 'react-native';

import { Avatar } from '@/components/avatar';
import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';

/**
 * The signed-in user, rendered on the right of the stack header via `headerRight`. Lives in the
 * navigation chrome rather than in a screen so it stays put while the lists scroll.
 */
export function HeaderUser() {
  const { user } = useAuth();
  const router = useRouter();

  return (
    <Pressable
      onPress={() => router.push('/profile')}
      accessibilityRole="button"
      accessibilityLabel="Open your profile"
      style={styles.button}>
      <ThemedText type="smallBold" themeColor="textSecondary" numberOfLines={1} style={styles.name}>
        {user?.username ?? 'Profile'}
      </ThemedText>
      <Avatar username={user?.username} image={user?.image} size={28} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    paddingLeft: Spacing.two,
  },
  name: {
    maxWidth: 120,
  },
});
