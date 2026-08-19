import * as Haptics from 'expo-haptics';
import React, { useEffect, useMemo, useRef } from 'react';
import {
  ActivityIndicator,
  Animated,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type DimensionValue,
  type PressableProps,
  type ScrollViewProps,
  type StyleProp,
  type TextInputProps,
  type TextProps,
  type TextStyle,
  type ViewProps,
  type ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import type { Edge } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { Radius, Shadows, Spacing, TouchTarget, Typography, type ThemeColor } from '@/constants/theme';
import { useReducedMotion } from '@/hooks/use-reduced-motion';
import { useTheme } from '@/hooks/use-theme';

export type AppTextVariant = keyof typeof Typography;
export type AppTextProps = TextProps & { variant?: AppTextVariant; color?: ThemeColor; align?: TextStyle['textAlign'] };
export function AppText({ variant = 'body', color = 'text', align, style, ...props }: AppTextProps) {
  const theme = useTheme();
  return <Text maxFontSizeMultiplier={2} style={[Typography[variant], { color: theme[color], textAlign: align }, style]} {...props} />;
}

export type ScreenProps = ViewProps & {
  scroll?: boolean;
  edges?: Edge[];
  contentContainerStyle?: StyleProp<ViewStyle>;
  keyboardShouldPersistTaps?: ScrollViewProps['keyboardShouldPersistTaps'];
  refreshControl?: ScrollViewProps['refreshControl'];
};
export function Screen({ scroll = true, edges = ['top', 'left', 'right'], style, contentContainerStyle, keyboardShouldPersistTaps = 'handled', refreshControl, children, ...props }: ScreenProps) {
  const theme = useTheme();
  const content = scroll ? (
    <ScrollView contentContainerStyle={[styles.screenContent, contentContainerStyle]} keyboardShouldPersistTaps={keyboardShouldPersistTaps} refreshControl={refreshControl} showsVerticalScrollIndicator={false}>{children}</ScrollView>
  ) : <View style={[styles.screenContent, styles.flex, contentContainerStyle]}>{children}</View>;
  return <SafeAreaView edges={edges} style={[styles.flex, { backgroundColor: theme.background }, style]} {...props}><KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>{content}</KeyboardAvoidingView></SafeAreaView>;
}

export type AppHeaderProps = ViewProps & { title: string; subtitle?: string; onBack?: () => void; showBack?: boolean; right?: React.ReactNode; actionIcon?: string; onAction?: () => void };
export function AppHeader({ title, subtitle, onBack, showBack = false, right, actionIcon, onAction, style, ...props }: AppHeaderProps) {
  const theme = useTheme();
  const backAction = onBack ?? (showBack ? () => router.back() : undefined);
  return <View style={[styles.header, style]} {...props}>
    {backAction ? <Pressable accessibilityRole="button" accessibilityLabel="Назад" hitSlop={8} onPress={backAction} style={({ pressed }) => [styles.headerButton, { backgroundColor: pressed ? theme.primarySoft : 'transparent' }]}><AppText variant="heading" color="primary">‹</AppText></Pressable> : <View style={styles.headerButton} />}
    <View style={styles.headerCopy}><AppText variant="heading" numberOfLines={2}>{title}</AppText>{subtitle ? <AppText variant="caption" color="textSecondary" numberOfLines={2}>{subtitle}</AppText> : null}</View>
    <View style={styles.headerButton}>{right ?? (actionIcon && onAction ? <Pressable accessibilityRole="button" accessibilityLabel="Действие" onPress={onAction} hitSlop={8}><AppText variant="heading" color="primary">{actionIcon === 'add' ? '+' : '•'}</AppText></Pressable> : null)}</View>
  </View>;
}

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type ButtonProps = Omit<PressableProps, 'children' | 'style'> & { label: string; variant?: ButtonVariant; loading?: boolean; left?: React.ReactNode; icon?: React.ReactNode; haptic?: boolean; fullWidth?: boolean; style?: StyleProp<ViewStyle> };
export function Button({ label, variant = 'primary', loading = false, left, icon, haptic = true, fullWidth = true, disabled, onPress, style, ...props }: ButtonProps) {
  const theme = useTheme();
  const palette = variant === 'primary' ? { bg: theme.primary, pressed: theme.primaryPressed, text: theme.onPrimary, border: theme.primary }
    : variant === 'danger' ? { bg: theme.danger, pressed: theme.danger, text: theme.white, border: theme.danger }
      : variant === 'secondary' ? { bg: theme.primarySoft, pressed: theme.backgroundSelected, text: theme.primary, border: theme.borderStrong }
        : { bg: 'transparent', pressed: theme.primarySoft, text: theme.primary, border: 'transparent' };
  const inactive = disabled || loading;
  return <Pressable accessibilityRole="button" accessibilityState={{ disabled: inactive, busy: loading }} disabled={inactive} onPress={(event) => { if (haptic) void Haptics.selectionAsync().catch(() => undefined); onPress?.(event); }} style={({ pressed }) => [styles.button, fullWidth && styles.fullWidth, { backgroundColor: inactive ? theme.disabled : pressed ? palette.pressed : palette.bg, borderColor: inactive ? theme.disabled : palette.border }, style]} {...props}>
    {loading ? <ActivityIndicator color={theme.onDisabled} /> : <>{left ?? (typeof icon === 'string' ? <AppText style={{ color: inactive ? theme.onDisabled : palette.text }}>♥</AppText> : icon)}{<AppText variant="bodyStrong" style={{ color: inactive ? theme.onDisabled : palette.text }}>{label}</AppText>}</>}
  </Pressable>;
}

export type TextFieldProps = TextInputProps & { label?: string; error?: string; hint?: string; left?: React.ReactNode; right?: React.ReactNode; containerStyle?: StyleProp<ViewStyle> };
export function TextField({ label, error, hint, left, right, containerStyle, style, editable = true, ...props }: TextFieldProps) {
  const theme = useTheme();
  const helpId = useMemo(() => `field-help-${Math.random().toString(36).slice(2)}`, []);
  return <View style={[styles.fieldGroup, containerStyle]}>
    {label ? <AppText variant="label">{label}</AppText> : null}
    <View style={[styles.field, { backgroundColor: theme.surface, borderColor: error ? theme.danger : theme.border }, !editable && { backgroundColor: theme.surfaceMuted }]}>
      {left}<TextInput accessibilityState={{ disabled: !editable }} editable={editable} placeholderTextColor={theme.textMuted} selectionColor={theme.primary} style={[styles.input, { color: editable ? theme.text : theme.onDisabled }, style]} {...props} />{right}
    </View>
    {error || hint ? <AppText nativeID={helpId} variant="caption" color={error ? 'danger' : 'textSecondary'}>{error ?? hint}</AppText> : null}
  </View>;
}

export type CardProps = ViewProps & { onPress?: () => void; padded?: boolean; padding?: number; accessibilityLabel?: string };
export function Card({ onPress, padded = true, padding, accessibilityLabel, style, children, ...props }: CardProps) {
  const theme = useTheme();
  const cardStyle = [styles.card, padded && styles.cardPadded, padding !== undefined && { padding }, Shadows.card, { backgroundColor: theme.surface, borderColor: theme.border }, style];
  if (onPress) return <Pressable accessibilityRole="button" accessibilityLabel={accessibilityLabel} onPress={onPress} style={({ pressed }) => [cardStyle, pressed && { opacity: 0.86 }]}>{children}</Pressable>;
  return <View style={cardStyle} {...props}>{children}</View>;
}

export type ChipProps = { label: string; selected?: boolean; onPress?: () => void; disabled?: boolean; icon?: React.ReactNode; style?: StyleProp<ViewStyle> };
export function Chip({ label, selected = false, onPress, disabled, icon, style }: ChipProps) {
  const theme = useTheme();
  return <Pressable accessibilityRole={onPress ? 'button' : 'text'} accessibilityState={{ selected, disabled }} disabled={disabled || !onPress} onPress={onPress} style={({ pressed }) => [styles.chip, { backgroundColor: selected ? theme.primary : theme.surface, borderColor: selected ? theme.primary : theme.border, opacity: disabled ? 0.5 : pressed ? 0.8 : 1 }, style]}>{icon}<AppText variant="label" style={{ color: selected ? theme.onPrimary : theme.text }}>{label}</AppText></Pressable>;
}

export type StatusTone = 'neutral' | 'primary' | 'success' | 'warning' | 'danger' | 'info';
export function StatusPill({ label, tone = 'neutral', style }: { label: string; tone?: StatusTone; style?: StyleProp<ViewStyle> }) {
  const theme = useTheme(); const color = tone === 'neutral' ? theme.textSecondary : theme[tone];
  return <View accessibilityRole="text" style={[styles.pill, { borderColor: color, backgroundColor: `${color}18` }, style]}><AppText variant="caption" style={{ color }}>{label}</AppText></View>;
}

export function SectionHeader({ title, subtitle, actionLabel, onAction, style }: { title: string; subtitle?: string; actionLabel?: string; onAction?: () => void; style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.sectionHeader, style]}><View style={styles.flex}><AppText variant="subheading">{title}</AppText>{subtitle ? <AppText variant="caption" color="textSecondary">{subtitle}</AppText> : null}</View>{actionLabel && onAction ? <Pressable accessibilityRole="button" onPress={onAction} hitSlop={8}><AppText variant="label" color="primary">{actionLabel}</AppText></Pressable> : null}</View>;
}

export function EmptyState({ title, description, icon, actionLabel, onAction, style }: { title: string; description?: string; icon?: React.ReactNode; actionLabel?: string; onAction?: () => void; style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.centerState, style]}>{icon}<AppText variant="subheading" align="center">{title}</AppText>{description ? <AppText color="textSecondary" align="center">{description}</AppText> : null}{actionLabel && onAction ? <Button label={actionLabel} onPress={onAction} fullWidth={false} /> : null}</View>;
}

export function LoadingView({ label = 'Загрузка…', style }: { label?: string; style?: StyleProp<ViewStyle> }) {
  const theme = useTheme(); return <View accessibilityRole="progressbar" accessibilityLabel={label} style={[styles.centerState, style]}><ActivityIndicator size="large" color={theme.primary} /><AppText color="textSecondary">{label}</AppText></View>;
}

export function Skeleton({ width = '100%', height = 20, radius = Radius.medium, style }: { width?: DimensionValue; height?: number; radius?: number; style?: StyleProp<ViewStyle> }) {
  const theme = useTheme(); const reducedMotion = useReducedMotion(); const opacity = useRef(new Animated.Value(0.48)).current;
  useEffect(() => { if (reducedMotion) { opacity.setValue(0.65); return; } const animation = Animated.loop(Animated.sequence([Animated.timing(opacity, { toValue: 0.9, duration: 850, useNativeDriver: true }), Animated.timing(opacity, { toValue: 0.48, duration: 850, useNativeDriver: true })])); animation.start(); return () => animation.stop(); }, [opacity, reducedMotion]);
  return <Animated.View accessibilityElementsHidden importantForAccessibility="no-hide-descendants" style={[{ width, height, borderRadius: radius, backgroundColor: theme.skeleton, opacity }, style]} />;
}

export function InitialsAvatar({ name, initials, size = 48, style }: { name?: string; initials?: string; size?: number; style?: StyleProp<ViewStyle> }) {
  const theme = useTheme(); const value = (initials ?? name?.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('') ?? 'L').toLocaleUpperCase();
  return <View accessibilityRole="image" accessibilityLabel={name ? `Аватар: ${name}` : 'Аватар'} style={[styles.avatar, { width: size, height: size, borderRadius: size / 2, backgroundColor: theme.primarySoft, borderColor: theme.borderStrong }, style]}><AppText variant={size >= 64 ? 'heading' : 'bodyStrong'} color="primary">{value}</AppText></View>;
}

const styles = StyleSheet.create({
  flex: { flex: 1 }, fullWidth: { alignSelf: 'stretch' },
  screenContent: { flexGrow: 1, paddingHorizontal: Spacing.three, paddingBottom: Spacing.five, gap: Spacing.three },
  header: { minHeight: 60, flexDirection: 'row', alignItems: 'center', gap: Spacing.two, paddingVertical: Spacing.two },
  headerButton: { width: TouchTarget, minHeight: TouchTarget, borderRadius: Radius.pill, alignItems: 'center', justifyContent: 'center' },
  headerCopy: { flex: 1, alignItems: 'center', gap: 2 },
  button: { minHeight: TouchTarget, borderRadius: Radius.pill, borderWidth: 1, paddingHorizontal: Spacing.four, paddingVertical: 10, flexDirection: 'row', gap: Spacing.two, alignItems: 'center', justifyContent: 'center' },
  fieldGroup: { gap: 6 }, field: { minHeight: TouchTarget, borderWidth: 1, borderRadius: Radius.medium, flexDirection: 'row', alignItems: 'center', gap: Spacing.two, paddingHorizontal: Spacing.three }, input: { ...Typography.body, flex: 1, paddingVertical: Platform.OS === 'ios' ? 12 : 8 },
  card: { borderWidth: StyleSheet.hairlineWidth, borderTopLeftRadius: Radius.card, borderTopRightRadius: Radius.card, borderBottomRightRadius: Radius.card, borderBottomLeftRadius: Radius.small, overflow: Platform.OS === 'android' ? 'hidden' : 'visible' }, cardPadded: { padding: Spacing.three, gap: Spacing.three },
  chip: { minHeight: TouchTarget, borderWidth: 1, borderRadius: Radius.pill, paddingHorizontal: Spacing.three, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6 },
  pill: { minHeight: 28, borderWidth: 1, borderRadius: Radius.pill, paddingHorizontal: 10, paddingVertical: 4, alignSelf: 'flex-start', justifyContent: 'center' },
  sectionHeader: { minHeight: TouchTarget, flexDirection: 'row', alignItems: 'center', gap: Spacing.three },
  centerState: { flex: 1, minHeight: 220, padding: Spacing.four, alignItems: 'center', justifyContent: 'center', gap: Spacing.three },
  avatar: { borderWidth: 1, alignItems: 'center', justifyContent: 'center', overflow: 'hidden' },
});
