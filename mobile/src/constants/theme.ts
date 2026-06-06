/**
 * AeroStride Design System - Theme & Colors
 * Premium dark-mode-first design with brand colors
 */

import '@/global.css';

import { Platform } from 'react-native';

// Brand palette
export const Brand = {
  primary: '#208AEF',
  primaryLight: '#4DA6FF',
  primaryDark: '#1565C0',
  accent: '#FF6B35',
  accentLight: '#FF8F5E',
  success: '#10B981',
  warning: '#F59E0B',
  error: '#EF4444',
  info: '#06B6D4',
} as const;

export const Colors = {
  light: {
    text: '#0F172A',
    textSecondary: '#64748B',
    textTertiary: '#94A3B8',
    background: '#F8FAFC',
    backgroundElement: '#FFFFFF',
    backgroundSelected: '#E2E8F0',
    surface: '#FFFFFF',
    surfaceElevated: '#FFFFFF',
    border: '#E2E8F0',
    borderLight: '#F1F5F9',
    primary: Brand.primary,
    primaryText: '#FFFFFF',
    cardShadow: 'rgba(0,0,0,0.08)',
    overlay: 'rgba(0,0,0,0.5)',
    tabBar: '#FFFFFF',
    tabBarBorder: '#E2E8F0',
    statusBar: 'dark-content' as const,
  },
  dark: {
    text: '#F1F5F9',
    textSecondary: '#94A3B8',
    textTertiary: '#64748B',
    background: '#0B1120',
    backgroundElement: '#151D2E',
    backgroundSelected: '#1E293B',
    surface: '#151D2E',
    surfaceElevated: '#1E293B',
    border: '#1E293B',
    borderLight: '#151D2E',
    primary: Brand.primary,
    primaryText: '#FFFFFF',
    cardShadow: 'rgba(0,0,0,0.3)',
    overlay: 'rgba(0,0,0,0.7)',
    tabBar: '#0B1120',
    tabBarBorder: '#1E293B',
    statusBar: 'light-content' as const,
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

export const Gradients = {
  primary: ['#208AEF', '#1565C0'],
  hero: ['#0B1120', '#152238', '#1E3A5F'],
  card: ['rgba(32,138,239,0.1)', 'rgba(32,138,239,0.02)'],
  accent: ['#FF6B35', '#FF8F5E'],
  dark: ['#0B1120', '#151D2E'],
} as const;

export const Fonts = Platform.select({
  ios: {
    sans: 'system-ui',
    serif: 'ui-serif',
    rounded: 'ui-rounded',
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

export const FontSizes = {
  xs: 11,
  sm: 13,
  base: 15,
  md: 17,
  lg: 20,
  xl: 24,
  '2xl': 30,
  '3xl': 36,
  '4xl': 48,
} as const;

export const FontWeights = {
  regular: '400' as const,
  medium: '500' as const,
  semibold: '600' as const,
  bold: '700' as const,
  extrabold: '800' as const,
};

export const Spacing = {
  half: 2,
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 48,
  seven: 64,
  eight: 80,
} as const;

export const BorderRadius = {
  sm: 6,
  md: 12,
  lg: 16,
  xl: 20,
  '2xl': 24,
  full: 9999,
} as const;

export const BottomTabInset = Platform.select({ ios: 50, android: 80 }) ?? 0;
export const MaxContentWidth = 800;
