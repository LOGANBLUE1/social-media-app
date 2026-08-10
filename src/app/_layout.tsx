import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import { useColorScheme } from 'react-native';

import { AuthProvider } from '@/auth/auth-context';

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
  const colorScheme = useColorScheme();

  return (
    // QueryClientProvider must be outside AuthProvider: AuthProvider calls useQueryClient to
    // flush cached data when a session ends.
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
          <Stack>
            <Stack.Screen name="index" options={{ headerShown: false }} />
            <Stack.Screen name="login" options={{ title: 'Log in' }} />
            <Stack.Screen name="signup" options={{ title: 'Create account' }} />
            <Stack.Screen name="feed" options={{ title: 'My posts' }} />
            <Stack.Screen name="post/[id]" options={{ title: 'Post' }} />
          </Stack>
        </ThemeProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
