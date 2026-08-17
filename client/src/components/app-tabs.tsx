import { useRouter } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useConversations } from '@/hooks/use-chat';
import { useIncomingRequests } from '@/hooks/use-friends';
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

  // Both hooks are already used by the screens these tabs sit on, and React Query dedupes by key,
  // so reading them here costs no extra requests -- the counts come from the same cache. The
  // conversations query polls on its own, which is what keeps the chat count live without opening
  // the tab; incoming requests refresh whenever a friend mutation invalidates them.
  const incoming = useIncomingRequests();
  const conversations = useConversations();

  const pendingRequests = (incoming.data ?? []).filter(
    (request) => request.status === 'PENDING',
  ).length;
  // People with something unread, not messages unread -- one friend sending ten lines is one
  // conversation waiting on you.
  const unreadChats = (conversations.data ?? []).filter(
    (conversation) => conversation.unreadCount > 0,
  ).length;

  const countFor = (key: TabKey) =>
    key === 'friends' ? pendingRequests : key === 'chats' ? unreadChats : 0;

  return (
    <View style={[styles.tabs, { backgroundColor: theme.backgroundElement }]}>
      {TABS.map((tab) => {
        const selected = tab.key === active;
        const count = countFor(tab.key);

        return (
          <Pressable
            key={tab.key}
            accessibilityRole="tab"
            accessibilityState={{ selected }}
            accessibilityLabel={count > 0 ? `${tab.label}, ${count} waiting` : tab.label}
            // replace: these are siblings, not a drill-down -- pushing would stack them forever.
            onPress={() => !selected && router.replace(tab.href)}
            style={[styles.tab, selected && { backgroundColor: theme.backgroundSelected }]}>
            <View style={styles.tabInner}>
              <ThemedText
                type="smallBold"
                themeColor={selected ? 'text' : 'textSecondary'}
                numberOfLines={1}>
                {tab.label}
              </ThemedText>
              {count > 0 && (
                // Hidden from screen readers -- the count is already in the tab's own label above,
                // and announcing it twice is worse than not at all.
                <View
                  accessibilityElementsHidden
                  importantForAccessibility="no-hide-descendants"
                  style={[styles.count, { backgroundColor: theme.tint }]}>
                  <ThemedText style={[styles.countText, { color: theme.onTint }]}>
                    {count > 9 ? '9+' : count}
                  </ThemedText>
                </View>
              )}
            </View>
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
  tabInner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.half,
  },
  count: {
    minWidth: 16,
    height: 16,
    borderRadius: 8,
    paddingHorizontal: 3,
    alignItems: 'center',
    justifyContent: 'center',
  },
  countText: {
    fontSize: 10,
    lineHeight: 13,
    fontWeight: '700',
  },
});
