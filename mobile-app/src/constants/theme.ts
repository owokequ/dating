import { Platform } from 'react-native';

const shared = { white: '#FFFFFF', black: '#160E12', success: '#2F7D5B', warning: '#A96512', danger: '#B7374F', info: '#5267A8' } as const;

export const Colors = {
  light: { ...shared, background: '#FFF4F7', backgroundSubtle: '#FFFAFB', surface: '#FFFFFF', surfaceElevated: '#FFFFFF', surfaceMuted: '#FFEFF4', backgroundElement: '#FFEFF4', backgroundSelected: '#FFE0E9', primary: '#A52C52', primaryPressed: '#84213F', primarySoft: '#FFE0E9', onPrimary: '#FFFFFF', text: '#3F2631', textSecondary: '#725965', textMuted: '#957F89', muted: '#957F89', border: '#ECD3DC', borderStrong: '#D9B5C2', overlay: 'rgba(63, 38, 49, 0.42)', skeleton: '#F0DCE3', skeletonHighlight: '#FFF7F9', shadow: '#6C2C43', disabled: '#D9C8CE', onDisabled: '#8D7A82' },
  dark: { ...shared, success: '#69C49A', warning: '#F2B45D', danger: '#FF8BA0', info: '#9DACED', background: '#21171D', backgroundSubtle: '#261A21', surface: '#2A1D25', surfaceElevated: '#34242D', surfaceMuted: '#3A2630', backgroundElement: '#34242D', backgroundSelected: '#4A2A38', primary: '#FF769E', primaryPressed: '#FF9AB8', primarySoft: '#4A2634', onPrimary: '#2A101A', text: '#F6E9EE', textSecondary: '#D5BDC7', textMuted: '#AA909B', muted: '#AA909B', border: '#4A3540', borderStrong: '#694A58', overlay: 'rgba(10, 5, 8, 0.7)', skeleton: '#3B2A33', skeletonHighlight: '#503844', shadow: '#000000', disabled: '#4B3B43', onDisabled: '#9B8991' },
} as const;

export type ThemeName = keyof typeof Colors;
export type ThemePreference = ThemeName | 'system';
export type Theme = (typeof Colors)[ThemeName];
export type ThemeColor = keyof Theme;

export const Fonts = Platform.select({ ios: { sans: 'System', serif: 'Georgia', rounded: 'System', mono: 'Menlo' }, default: { sans: 'sans-serif', serif: 'serif', rounded: 'sans-serif', mono: 'monospace' }, web: { sans: 'Manrope, system-ui, sans-serif', serif: 'Cormorant Garamond, Georgia, serif', rounded: 'Manrope, system-ui, sans-serif', mono: 'ui-monospace, monospace' } })!;
export const Typography = {
  hero: { fontFamily: Fonts.serif, fontSize: 44, lineHeight: 48, fontWeight: '600' as const },
  title: { fontFamily: Fonts.serif, fontSize: 34, lineHeight: 40, fontWeight: '600' as const },
  heading: { fontFamily: Fonts.sans, fontSize: 24, lineHeight: 30, fontWeight: '700' as const },
  subheading: { fontFamily: Fonts.sans, fontSize: 18, lineHeight: 24, fontWeight: '700' as const },
  body: { fontFamily: Fonts.sans, fontSize: 16, lineHeight: 24, fontWeight: '400' as const },
  bodyStrong: { fontFamily: Fonts.sans, fontSize: 16, lineHeight: 24, fontWeight: '700' as const },
  label: { fontFamily: Fonts.sans, fontSize: 14, lineHeight: 20, fontWeight: '600' as const },
  caption: { fontFamily: Fonts.sans, fontSize: 12, lineHeight: 16, fontWeight: '500' as const },
  displaySmall: { fontFamily: Fonts.serif, fontSize: 34, lineHeight: 40, fontWeight: '600' as const },
  title3: { fontFamily: Fonts.sans, fontSize: 20, lineHeight: 26, fontWeight: '700' as const },
  bodySmall: { fontFamily: Fonts.sans, fontSize: 14, lineHeight: 20, fontWeight: '400' as const },
  eyebrow: { fontFamily: Fonts.sans, fontSize: 12, lineHeight: 16, fontWeight: '700' as const, letterSpacing: 1.1, textTransform: 'uppercase' as const },
} as const;
export const Spacing = { none: 0, half: 2, one: 4, two: 8, three: 16, four: 24, five: 32, six: 48, seven: 64 } as const;
export const Radius = { small: 10, medium: 16, large: 24, card: 28, pill: 999 } as const;
export const TouchTarget = Platform.OS === 'ios' ? 44 : 48;
export const BottomTabInset = Platform.select({ ios: 50, android: 80 }) ?? 0;
export const MaxContentWidth = 800;
export const Shadows = { card: Platform.select({ ios: { shadowColor: '#6C2C43', shadowOpacity: 0.1, shadowRadius: 18, shadowOffset: { width: 0, height: 8 } }, android: { elevation: 3 }, default: {} }) } as const;
