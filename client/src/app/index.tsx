import { Redirect } from 'expo-router';
import { ActivityIndicator, StyleSheet } from 'react-native';

import { ThemedView } from '@/components/themed-view';
import { useAuth } from '@/auth/auth-context';

/**
 * Entry gate. Rendering a spinner while hydrating matters on native, where reading the
 * Keychain is async -- redirecting first would bounce a signed-in user to the login screen.
 */
export default function Index() {
  const { session, hydrating } = useAuth();

  if (hydrating) {
    return (
      <ThemedView style={styles.centered}>
        <ActivityIndicator />
      </ThemedView>
    );
  }

  return <Redirect href={session ? '/feed' : '/login'} />;
}

const styles = StyleSheet.create({
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
