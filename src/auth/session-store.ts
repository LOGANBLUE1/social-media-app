import type { Session } from '@/api';

/**
 * The contract the three `storage.*` implementations satisfy. It lives in its own module
 * because a `storage.native.ts` that imported `./storage` would resolve to *itself* on
 * native -- Metro prefers the platform extension over the bare `.ts`.
 */
export interface TokenStorage {
  load(): Promise<Session | null>;
  save(session: Session): Promise<void>;
  clear(): Promise<void>;
}

/** SecureStore keys are limited to alphanumerics plus `.`, `-` and `_`. */
export const SESSION_KEY = 'social_app_session';

export function parseSession(raw: string | null): Session | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as Session;
    // Guard against a partially-written or schema-drifted blob leaving the app convinced it
    // is logged in while unable to authenticate.
    if (!parsed?.accessToken || !parsed.refreshToken || !parsed.user?.id) return null;
    return parsed;
  } catch {
    return null;
  }
}
