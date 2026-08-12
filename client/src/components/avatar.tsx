import { Image } from 'expo-image';
import { StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { useTheme } from '@/hooks/use-theme';

type Props = {
  username?: string | null;
  image?: string | null;
  size?: number;
};

/** Circular user image, falling back to the first letter of the username. */
export function Avatar({ username, image, size = 32 }: Props) {
  const theme = useTheme();
  const shape = { width: size, height: size, borderRadius: size / 2 };

  if (image) {
    return <Image source={image} style={shape} contentFit="cover" transition={150} />;
  }

  return (
    <View style={[styles.fallback, shape, { backgroundColor: theme.backgroundSelected }]}>
      <ThemedText style={{ fontSize: size * 0.45, fontWeight: '700', lineHeight: size }}>
        {(username?.trim()[0] ?? '?').toUpperCase()}
      </ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  fallback: {
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
});