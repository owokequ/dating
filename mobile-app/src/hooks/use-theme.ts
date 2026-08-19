import { useContext } from 'react';
import { useColorScheme } from 'react-native';
import { Colors } from '@/constants/theme';
import { ThemeContext } from '@/hooks/theme-provider';

export function useThemeContext() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useThemeContext must be used inside ThemeProvider');
  return context;
}
export const useAppTheme = useThemeContext;
/** Safe legacy color access before the provider is wired at the root. */
export function useTheme() {
  const context = useContext(ThemeContext);
  const systemScheme = useColorScheme();
  return context?.theme ?? Colors[systemScheme === 'dark' ? 'dark' : 'light'];
}
