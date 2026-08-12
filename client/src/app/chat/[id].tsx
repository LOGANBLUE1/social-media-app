import { Redirect, Stack, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  View,
} from 'react-native';

import type { MessageResponse } from '@/api';
import { Button } from '@/components/button';
import { TextField } from '@/components/text-field';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { useAuth } from '@/auth/auth-context';
import { useMessages, useSendMessage } from '@/hooks/use-chat';
import { useTheme } from '@/hooks/use-theme';

export default function ChatScreen() {
  const { id, name } = useLocalSearchParams<{ id: string; name?: string }>();
  const conversationId = Number(id);
  const { session, user, hydrating } = useAuth();

  const messages = useMessages(conversationId);
  const send = useSendMessage(conversationId);

  const [body, setBody] = useState('');

  if (!hydrating && !session) return <Redirect href="/login" />;

  async function submit() {
    try {
      await send.mutateAsync(body.trim());
      setBody('');
    } catch {
      // Surfaced below via send.error -- the draft stays put so it is not lost.
    }
  }

  const canSend = body.trim().length > 0 && !send.isPending;
  // The server returns oldest first; `inverted` renders from the bottom, so feed it newest first.
  const newestFirst = [...(messages.data ?? [])].reverse();

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{ title: name || 'Chat' }} />
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <FlatList
          data={newestFirst}
          inverted
          keyExtractor={(message) => String(message.id)}
          contentContainerStyle={styles.list}
          ListEmptyComponent={
            messages.isLoading ? (
              <ActivityIndicator style={styles.spacer} />
            ) : messages.error ? (
              <View style={styles.spacer}>
                <ThemedText type="small" themeColor="danger">
                  {messages.error.message}
                </ThemedText>
                <Button
                  title="Retry"
                  variant="secondary"
                  onPress={() => messages.refetch()}
                />
              </View>
            ) : (
              <ThemedText type="small" themeColor="textSecondary" style={styles.spacer}>
                No messages yet. Say hello.
              </ThemedText>
            )
          }
          renderItem={({ item }) => <Bubble message={item} mine={item.senderId === user?.id} />}
        />

        <View style={styles.composer}>
          {send.error && (
            <ThemedText type="small" themeColor="danger">
              {send.error.message}
            </ThemedText>
          )}
          <TextField
            label="Message"
            value={body}
            onChangeText={setBody}
            multiline
            style={styles.input}
            onSubmitEditing={() => canSend && submit()}
          />
          <Button title="Send" onPress={submit} loading={send.isPending} disabled={!canSend} />
        </View>
      </KeyboardAvoidingView>
    </ThemedView>
  );
}

function Bubble({ message, mine }: { message: MessageResponse; mine: boolean }) {
  const theme = useTheme();

  return (
    <View style={[styles.bubbleRow, mine ? styles.rowMine : styles.rowTheirs]}>
      <View
        style={[
          styles.bubble,
          { backgroundColor: mine ? theme.tint : theme.backgroundElement },
        ]}>
        <ThemedText type="small" style={{ color: mine ? theme.onTint : theme.text }}>
          {message.body}
        </ThemedText>
        <ThemedText
          type="small"
          style={[styles.time, { color: mine ? theme.onTint : theme.textSecondary }]}>
          {formatTime(message.createdAt)}
        </ThemedText>
      </View>
    </View>
  );
}

/** No timezone offset on the wire, so Date parses it as local time -- which is what we want. */
function formatTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ''
    : date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  list: {
    padding: Spacing.three,
    gap: Spacing.two,
    maxWidth: 640,
    width: '100%',
    alignSelf: 'center',
  },
  bubbleRow: {
    flexDirection: 'row',
  },
  rowMine: {
    justifyContent: 'flex-end',
  },
  rowTheirs: {
    justifyContent: 'flex-start',
  },
  bubble: {
    maxWidth: '80%',
    gap: Spacing.half,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    borderRadius: Spacing.three,
  },
  time: {
    fontSize: 11,
    opacity: 0.8,
  },
  composer: {
    gap: Spacing.two,
    padding: Spacing.three,
    maxWidth: 640,
    width: '100%',
    alignSelf: 'center',
  },
  input: {
    minHeight: 44,
    maxHeight: 120,
    textAlignVertical: 'top',
  },
  spacer: {
    paddingVertical: Spacing.four,
    gap: Spacing.three,
    // The list is inverted, so its empty state would render upside down without this.
    transform: [{ scaleY: -1 }],
  },
});
