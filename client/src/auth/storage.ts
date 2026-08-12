import type { Session } from '@/api';

import type { TokenStorage } from './session-store';

/**
 * In-memory fallback. Metro substitutes `storage.native.ts` on Android/iOS and
 * `storage.web.ts` in the browser, so this is what runs where neither applies -- notably
 * Node, during the `web.output: "static"` prerender.
 */
let memory: Session | null = null;

export const storage: TokenStorage = {
  async load() {
    return memory;
  },
  async save(session) {
    memory = session;
  },
  async clear() {
    memory = null;
  },
};
