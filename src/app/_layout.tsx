import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DefaultTheme, Stack, ThemeProvider } from 'expo-router';

import { AuthProvider } from '@/auth/auth-context';
import { HeaderUser } from '@/components/header-user';
import { Colors } from '@/constants/theme';

/**
 * React Navigation keeps its own palette for the header and screen background, so the app
 * colors have to be handed to it explicitly -- otherwise the stack header stays default
 * blue-on-white above our purple screens.
 */
const navigationTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    primary: Colors.light.tint,
    background: Colors.light.background,
    card: Colors.light.background,
    text: Colors.light.text,
    border: Colors.light.border,
  },
};

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // The api client already retries once after refreshing an expired access token, so a
      // 401 that reaches here is final -- retrying it would just replay a dead session.
      retry: 1,
    },
  },
});

export default function RootLayout() {
  return (
    // QueryClientProvider must be outside AuthProvider: AuthProvider calls useQueryClient to
    // flush cached data when a session ends.
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        {/* Light only -- the app opts out of the device dark scheme, see useTheme(). */}
        <ThemeProvider value={navigationTheme}>
          <Stack>
            <Stack.Screen name="index" options={{ headerShown: false }} />
            <Stack.Screen name="login" options={{ title: 'Log in' }} />
            <Stack.Screen name="signup" options={{ title: 'Create account' }} />
            {/* headerRight, not a screen element: it belongs to the navbar and must not scroll. */}
            <Stack.Screen
              name="feed"
              options={{ title: 'Feed', headerRight: () => <HeaderUser /> }}
            />
            <Stack.Screen
              name="my-posts"
              options={{ title: 'My posts', headerRight: () => <HeaderUser /> }}
            />
            <Stack.Screen
              name="users"
              options={{ title: 'People', headerRight: () => <HeaderUser /> }}
            />
            <Stack.Screen
              name="friends"
              options={{ title: 'Friends', headerRight: () => <HeaderUser /> }}
            />
            <Stack.Screen
              name="chats"
              options={{ title: 'Chats', headerRight: () => <HeaderUser /> }}
            />
            {/* The title is set by the screen itself, from the other participant's name. */}
            <Stack.Screen name="chat/[id]" options={{ title: 'Chat' }} />
            <Stack.Screen name="profile" options={{ title: 'Profile' }} />
            <Stack.Screen
              name="post/[id]"
              options={{ title: 'Post', headerRight: () => <HeaderUser /> }}
            />
          </Stack>
        </ThemeProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
