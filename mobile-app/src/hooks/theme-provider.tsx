import * as SecureStore from 'expo-secure-store';
import React, { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { Platform, useColorScheme } from 'react-native';
import { Colors, type Theme, type ThemeName, type ThemePreference } from '@/constants/theme';

const STORAGE_KEY = 'for-my-l.theme-preference';
type ThemeContextValue = { theme: Theme; colors: Theme; themeName: ThemeName; preference: ThemePreference; setPreference: (preference: ThemePreference) => Promise<void> };
export const ThemeContext = createContext<ThemeContextValue | null>(null);

async function readPreference(): Promise<ThemePreference | null> {
  try {
    const stored = Platform.OS === 'web' ? globalThis.localStorage?.getItem(STORAGE_KEY) : await SecureStore.getItemAsync(STORAGE_KEY);
    return stored === 'light' || stored === 'dark' || stored === 'system' ? stored : null;
  } catch { return null; }
}
async function writePreference(preference: ThemePreference) {
  try {
    if (Platform.OS === 'web') globalThis.localStorage?.setItem(STORAGE_KEY, preference);
    else await SecureStore.setItemAsync(STORAGE_KEY, preference);
  } catch { /* The active session still receives the selected theme. */ }
}

export function ThemeProvider({ children }: React.PropsWithChildren) {
  const systemScheme = useColorScheme();
  const [preference, setPreferenceState] = useState<ThemePreference>('system');
  useEffect(() => { let active = true; void readPreference().then((stored) => { if (active && stored) setPreferenceState(stored); }); return () => { active = false; }; }, []);
  const setPreference = useCallback(async (next: ThemePreference) => { setPreferenceState(next); await writePreference(next); }, []);
  const themeName: ThemeName = preference === 'system' ? systemScheme === 'dark' ? 'dark' : 'light' : preference;
  const value = useMemo(() => ({ theme: Colors[themeName], colors: Colors[themeName], themeName, preference, setPreference }), [preference, setPreference, themeName]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
