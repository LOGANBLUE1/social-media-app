/**
 * Below are the colors that are used in the app. The colors are defined in the light and dark mode.
 * There are many other ways to style your app. For example, [Nativewind](https://www.nativewind.dev/), [Tamagui](https://tamagui.dev/), [unistyles](https://reactnativeunistyles.vercel.app), etc.
 */

import '@/global.css';

import { Platform } from 'react-native';

/**
 * Light purple palette. Surfaces are near-white with a violet cast and step up in three barely
 * separated tones (background -> backgroundElement -> backgroundSelected); saturated purple is
 * reserved for `tint`, the accent every interactive surface uses (buttons, links, the like
 * button, the selected nav tab). `onTint` is the only text color guaranteed to be readable on
 * top of `tint`, so pair the two -- never put `text` on a tinted background.
 */
export const Colors = {
  light: {
    text: '#1E1533',
    background: '#F6F2FE',
    backgroundElement: '#EBE3FB',
    backgroundSelected: '#DCD0F8',
    textSecondary: '#615579',
    tint: '#6D42D9',
    onTint: '#FFFFFF',
    border: '#DCD0F8',
    danger: '#C62828',
  },
  dark: {
    text: '#F4F1FF',
    background: '#141024',
    backgroundElement: '#221B3A',
    backgroundSelected: '#31284F',
    textSecondary: '#B3A8D4',
    tint: '#A78BFA',
    onTint: '#1F1633',
    border: '#31284F',
    danger: '#FF8A80',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

export const Fonts = Platform.select({
  ios: {
    /** iOS `UIFontDescriptorSystemDesignDefault` */
    sans: 'system-ui',
    /** iOS `UIFontDescriptorSystemDesignSerif` */
    serif: 'ui-serif',
    /** iOS `UIFontDescriptorSystemDesignRounded` */
    rounded: 'ui-rounded',
    /** iOS `UIFontDescriptorSystemDesignMonospaced` */
    mono: 'ui-monospace',
  },
  default: {
    sans: 'normal',
    serif: 'serif',
    rounded: 'normal',
    mono: 'monospace',
  },
  web: {
    sans: 'var(--font-display)',
    serif: 'var(--font-serif)',
    rounded: 'var(--font-rounded)',
    mono: 'var(--font-mono)',
  },
});

export const Spacing = {
  half: 2,
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

export const BottomTabInset = Platform.select({ ios: 50, android: 80 }) ?? 0;
export const MaxContentWidth = 800;
