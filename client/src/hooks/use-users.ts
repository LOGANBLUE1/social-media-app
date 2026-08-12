import { useQuery } from '@tanstack/react-query';

import { users } from '@/api';
import { useAuth } from '@/auth/auth-context';

export const userKeys = {
  all: ['users', 'list'] as const,
  me: ['users', 'me'] as const,
  myActivity: ['users', 'activity', 'me'] as const,
};

/** Everyone registered on the server -- `GET /users`, including the caller. */
export function useUsers() {
  const { session } = useAuth();

  return useQuery({
    queryKey: userKeys.all,
    queryFn: users.list,
    enabled: !!session,
  });
}

/**
 * The signed-in user's profile. The session already carries a `user` snapshot taken at login,
 * so screens should render that first and let this query correct it once it lands.
 */
export function useMe() {
  const { session } = useAuth();

  return useQuery({
    queryKey: userKeys.me,
    queryFn: users.me,
    enabled: !!session,
    initialData: session?.user,
  });
}

/** Everything the user has liked and commented on -- the counts shown on the profile. */
export function useMyActivity() {
  const { session } = useAuth();

  return useQuery({
    queryKey: userKeys.myActivity,
    queryFn: users.myActivity,
    enabled: !!session,
  });
}