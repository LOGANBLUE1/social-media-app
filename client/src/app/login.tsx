import { Link, Redirect } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';

import { Button } from '@/components/button';
import { TextField } from '@/components/text-field';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';

export default function LoginScreen() {
  const { login, session, hydrating } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // A successful login flips `session`, which lands here on the next render and navigates --
  // so submit() never has to call router.replace itself.
  if (!hydrating && session) return <Redirect href="/feed" />;

  async function submit() {
    setError(null);
    setBusy(true);
    try {
      await login(username.trim(), password);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not log in');
    } finally {
      setBusy(false);
    }
  }

  const canSubmit = username.trim().length > 0 && password.length > 0 && !busy;

  return (
    <ThemedView style={styles.container}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.form}>
        <TextField
          label="Username"
          value={username}
          onChangeText={setUsername}
          autoCapitalize="none"
          autoCorrect={false}
          autoComplete="username"
          returnKeyType="next"
        />
        <TextField
          label="Password"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          autoComplete="current-password"
          returnKeyType="go"
          onSubmitEditing={() => canSubmit && submit()}
        />

        {error && (
          <ThemedText type="small" themeColor="danger">
            {error}
          </ThemedText>
        )}

        <Button title="Log in" onPress={submit} loading={busy} disabled={!canSubmit} />

        <Link href="/signup" style={styles.link}>
          <ThemedText type="linkPrimary">No account yet? Create one</ThemedText>
        </Link>
      </KeyboardAvoidingView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.four,
  },
  form: {
    width: '100%',
    maxWidth: 420,
    gap: Spacing.three,
  },
  link: {
    alignSelf: 'center',
  },
});
