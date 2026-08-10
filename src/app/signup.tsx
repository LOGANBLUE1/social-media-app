import { Link, Redirect } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';

import { Button } from '@/components/button';
import { TextField } from '@/components/text-field';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';

export default function SignupScreen() {
  const { signup, session, hydrating } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [image, setImage] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!hydrating && session) return <Redirect href="/feed" />;

  async function submit() {
    setError(null);
    setBusy(true);
    try {
      // Signup logs you straight in -- register() authenticates the new credentials and
      // returns tokens, so there is no separate login step.
      await signup(username.trim(), password, image.trim() || null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not create account');
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
        />
        <TextField
          label="Password"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          autoComplete="new-password"
        />
        <TextField
          label="Avatar URL (optional)"
          value={image}
          onChangeText={setImage}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
        />

        {error && (
          <ThemedText type="small" style={styles.error}>
            {error}
          </ThemedText>
        )}

        <Button title="Create account" onPress={submit} loading={busy} disabled={!canSubmit} />

        <Link href="/login" style={styles.link}>
          <ThemedText type="linkPrimary">Already registered? Log in</ThemedText>
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
  error: {
    color: '#d93025',
  },
  link: {
    alignSelf: 'center',
  },
});
