import { useRouter } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const TABS = [
  { key: 'feed', label: 'Feed', href: '/feed' },
  { key: 'my-posts', label: 'My posts', href: '/my-posts' },
  { key: 'users', label: 'People', href: '/users' },
  { key: 'friends', label: 'Friends', href: '/friends' },
  { key: 'chats', label: 'Chats', href: '/chats' },
] as const;

type TabKey = (typeof TABS)[number]['key'];

/**
 * Segmented switch between the top-level lists. Rendered as a FlatList header rather than a real
 * navigator so the lists keep their own scroll and pull-to-refresh. The signed-in user lives in
 * the stack header instead -- see HeaderUser.
 */
export function AppTabs({ active }: { active: TabKey }) {
  const router = useRouter();
  const theme = useTheme();

  return (
    <View style={[styles.tabs, { backgroundColor: theme.backgroundElement }]}>
      {TABS.map((tab) => {
        const selected = tab.key === active;

        return (
          <Pressable
            key={tab.key}
            accessibilityRole="tab"
            accessibilityState={{ selected }}
            // replace: these are siblings, not a drill-down -- pushing would stack them forever.
            onPress={() => !selected && router.replace(tab.href)}
            style={[styles.tab, selected && { backgroundColor: theme.backgroundSelected }]}>
            <ThemedText
              type="smallBold"
              themeColor={selected ? 'text' : 'textSecondary'}
              numberOfLines={1}>
              {tab.label}
            </ThemedText>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  tabs: {
    flexDirection: 'row',
    padding: Spacing.one,
    borderRadius: Spacing.two,
    gap: Spacing.one,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 36,
    borderRadius: Spacing.one + Spacing.half,
  },
});
