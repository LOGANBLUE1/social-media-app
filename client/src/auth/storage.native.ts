import * as SecureStore from 'expo-secure-store';

import { parseSession, SESSION_KEY, type TokenStorage } from './session-store';

/**
 * Android Keystore / iOS Keychain. Unlike AsyncStorage this survives on-device inspection,
 * which matters because the refresh token stored here stays valid for days.
 */
export const storage: TokenStorage = {
  async load() {
    return parseSession(await SecureStore.getItemAsync(SESSION_KEY));
  },

  async save(session) {
    await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(session));
  },

  async clear() {
    await SecureStore.deleteItemAsync(SESSION_KEY);
  },
};
