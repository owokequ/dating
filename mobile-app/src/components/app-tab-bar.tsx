import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { router, usePathname } from 'expo-router';
import { Platform, Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppText } from '@/design-system';
import { useAppTheme } from '@/hooks/use-theme';

const items = [
  { href: '/dashboard', label: 'Сегодня', icon: 'heart-outline', activeIcon: 'heart' },
  { href: '/places', label: 'Места', icon: 'location-outline', activeIcon: 'location' },
  { href: '/events', label: 'Афиша', icon: 'sparkles-outline', activeIcon: 'sparkles' },
  { href: '/dates', label: 'Свидания', icon: 'calendar-outline', activeIcon: 'calendar' },
  { href: '/profile', label: 'Ещё', icon: 'ellipsis-horizontal-circle-outline', activeIcon: 'ellipsis-horizontal-circle' },
] as const;

const rootPaths = new Set(items.map((item) => item.href));

export function shouldShowTabBar(pathname: string) {
  return rootPaths.has(pathname as (typeof items)[number]['href']);
}

export function AppTabBar() {
  const pathname = usePathname();
  const insets = useSafeAreaInsets();
  const { theme } = useAppTheme();

  if (!shouldShowTabBar(pathname)) return null;

  const navigate = (href: string) => {
    if (Platform.OS !== 'web') void Haptics.selectionAsync();
    router.replace(href as never);
  };

  return (
    <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>
      <Pressable
        accessibilityLabel="Предложить свидание"
        accessibilityRole="button"
        onPress={() => navigate('/new-date')}
        style={({ pressed }) => [
          styles.create,
          { backgroundColor: theme.primary, bottom: 62 + insets.bottom, shadowColor: theme.shadow },
          pressed && styles.pressed,
        ]}>
        <Ionicons color={theme.onPrimary} name="add" size={30} />
      </Pressable>
      <View
        style={[
          styles.bar,
          {
            paddingBottom: Math.max(insets.bottom, 8),
            backgroundColor: theme.surfaceElevated,
            borderColor: theme.border,
            shadowColor: theme.shadow,
          },
        ]}>
        {items.map((item, index) => {
          const active = pathname === item.href;
          return (
            <Pressable
              accessibilityRole="tab"
              accessibilityState={{ selected: active }}
              key={item.href}
              onPress={() => navigate(item.href)}
              style={styles.item}>
              <Ionicons
                color={active ? theme.primary : theme.textMuted}
                name={(active ? item.activeIcon : item.icon) as never}
                size={22}
              />
              <AppText style={{ color: active ? theme.primary : theme.textMuted }} variant="caption">
                {item.label}
              </AppText>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    position: 'absolute',
    right: 10,
    bottom: 8,
    left: 10,
    minHeight: 68,
    flexDirection: 'row',
    alignItems: 'flex-start',
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 28,
    paddingTop: 9,
    shadowOpacity: 0.14,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 8 },
    elevation: 10,
  },
  item: { flex: 1, minHeight: 48, alignItems: 'center', justifyContent: 'center', gap: 2 },
  create: {
    position: 'absolute',
    zIndex: 4,
    alignSelf: 'center',
    width: 58,
    height: 58,
    borderRadius: 29,
    alignItems: 'center',
    justifyContent: 'center',
    shadowOpacity: 0.26,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 8 },
    elevation: 12,
  },
  pressed: { transform: [{ scale: 0.95 }], opacity: 0.92 },
});
