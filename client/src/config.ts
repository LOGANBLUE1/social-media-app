import { Platform } from 'react-native';

/**
 * Where the Spring server lives.
 *
 * Set EXPO_PUBLIC_API_URL in `.env` to override -- required when testing on a *physical*
 * Android device, which needs your machine's LAN address (e.g. http://192.168.1.24:8080)
 * because `localhost` there means the phone itself.
 */
function resolveBaseUrl(): string {
  const override = process.env.EXPO_PUBLIC_API_URL;
  if (override) return override.replace(/\/$/, '');

  // 10.0.2.2 is the Android emulator's alias for the host machine's loopback.
  return Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
}

export const API_BASE_URL = resolveBaseUrl();
