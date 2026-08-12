import { parseSession, SESSION_KEY, type TokenStorage } from './session-store';

/**
 * localStorage is readable by any script on the origin, so an XSS here means a stolen refresh
 * token. The enterprise fix is a BFF holding tokens in an httpOnly cookie; until then this is
 * the pragmatic option, since there is nowhere in a browser that JS can write but not read.
 *
 * Guarded because `web.output: "static"` prerenders these screens in Node, which has no
 * localStorage.
 */
const available = typeof localStorage !== 'undefined';

export const storage: TokenStorage = {
  async load() {
    return available ? parseSession(localStorage.getItem(SESSION_KEY)) : null;
  },

  async save(session) {
    if (available) localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  },

  async clear() {
    if (available) localStorage.removeItem(SESSION_KEY);
  },
};
