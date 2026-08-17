import { StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { isWithinHours } from '@/utils/time';

/**
 * How long something counts as new, by kind.
 *
 * These differ because the badge is only worth anything while it stays rare -- if most rows carry
 * one, it stops meaning "look here" and becomes decoration. Signing up is a once-per-person event,
 * so a day-long window still leaves the badge on a handful of rows at most. Posting is ordinary and
 * repeated, so the same window would light up most of an active feed; a few hours keeps it to
 * "since you last looked in" for someone who checks daily.
 *
 * Both are a single number to change if the app's rhythm turns out to be different.
 */
export const NEW_USER_HOURS = 24;
export const NEW_POST_HOURS = 6;

/**
 * Small "NEW" pill. Renders nothing when the timestamp is outside the window or missing, so callers
 * can drop it into a row unconditionally.
 */
export function NewBadge({
  createdAt,
  withinHours,
  label = 'NEW',
}: {
  createdAt: string | null | undefined;
  withinHours: number;
  label?: string;
}) {
  const theme = useTheme();

  if (!isWithinHours(createdAt, withinHours)) return null;

  return (
    <View style={[styles.badge, { backgroundColor: theme.tint }]}>
      <ThemedText type="small" style={[styles.label, { color: theme.onTint }]}>
        {label}
      </ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: Spacing.one + Spacing.half,
    paddingVertical: 1,
    borderRadius: Spacing.one,
    // Stops the pill stretching to the height of a taller flex row next to it.
    alignSelf: 'center',
  },
  label: {
    fontSize: 10,
    lineHeight: 14,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
});
