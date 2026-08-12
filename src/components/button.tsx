import { ActivityIndicator, Pressable, StyleSheet, type PressableProps } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

type Props = Omit<PressableProps, 'children'> & {
  title: string;
  loading?: boolean;
  variant?: 'primary' | 'secondary';
};

export function Button({ title, loading, variant = 'primary', disabled, style, ...rest }: Props) {
  const theme = useTheme();
  const inactive = disabled || loading;

  return (
    <Pressable
      accessibilityRole="button"
      disabled={inactive}
      style={[
        styles.base,
        variant === 'primary'
          ? { backgroundColor: theme.tint }
          : { backgroundColor: theme.backgroundElement },
        inactive && styles.inactive,
        typeof style === 'object' && style,
      ]}
      {...rest}>
      {loading ? (
        <ActivityIndicator color={variant === 'primary' ? theme.onTint : theme.text} />
      ) : (
        <ThemedText
          type="smallBold"
          style={{ color: variant === 'primary' ? theme.onTint : theme.text }}>
          {title}
        </ThemedText>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 44,
    borderRadius: Spacing.two,
    paddingHorizontal: Spacing.three,
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'stretch',
  },
  inactive: {
    opacity: 0.5,
  },
});
