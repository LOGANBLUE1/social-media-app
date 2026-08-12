/**
 * Learn more about light and dark modes:
 * https://docs.expo.dev/guides/color-schemes/
 */

import { Colors } from '@/constants/theme';

/**
 * The app is light-only by design: it always renders the purple-on-white palette, regardless
 * of the device color scheme. Switch this back to reading `useColorScheme()` (and restore the
 * dark branch in the root layout's navigation theme) if dark mode is ever wanted.
 */
export function useTheme() {
  return Colors.light;
}
