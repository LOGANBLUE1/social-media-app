import { useQueryClient } from '@tanstack/react-query';
import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';

import { api, auth, type Session, type UserResponse } from '@/api';

import { storage } from './storage';

interface AuthContextValue {
  session: Session | null;
  user: UserResponse | null;
  /** True until the persisted session has been read; screens should not redirect before then. */
  hydrating: boolean;
  login(username: string, password: string): Promise<void>;
  signup(username: string, password: string, image?: string | null): Promise<void>;
  logout(): Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [hydrating, setHydrating] = useState(true);
  const queryClient = useQueryClient();

  // The client calls onSessionChange from inside doRefresh(), i.e. outside React's control
  // flow, so it needs a stable reference to the latest queryClient without re-registering.
  const queryClientRef = useRef(queryClient);
  queryClientRef.current = queryClient;

  useEffect(() => {
    let cancelled = false;

    // Registered before hydration so a token refresh that lands mid-startup is still persisted.
    api.onSessionChange = (next) => {
      setSession(next);
      if (next) {
        void storage.save(next);
      } else {
        // Refresh failed and the client cleared itself: drop cached data belonging to the
        // signed-out user so the next account does not briefly see it.
        void storage.clear();
        queryClientRef.current.clear();
      }
    };

    void (async () => {
      const stored = await storage.load();
      if (cancelled) return;
      if (stored) {
        // persist: false -- this value came *from* storage, so writing it back is redundant.
        api.setSession(stored, { persist: false });
        setSession(stored);
      }
      setHydrating(false);
    })();

    return () => {
      cancelled = true;
      api.onSessionChange = () => {};
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      user: session?.user ?? null,
      hydrating,
      async login(username, password) {
        api.setSession(await auth.login(username, password));
      },
      async signup(username, password, image) {
        api.setSession(await auth.signup(username, password, image));
      },
      async logout() {
        // Nothing to call server-side: there is no logout endpoint, and refresh tokens are
        // not revocable per device yet, so this is a local-only sign-out.
        api.setSession(null);
      },
    }),
    [session, hydrating],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
