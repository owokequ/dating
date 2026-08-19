import { MaterialCommunityIcons } from '@expo/vector-icons';
import * as Linking from 'expo-linking';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { AppText, Button, Card, LoadingView, Screen, TextField, useAppTheme } from '@/design-system';
import { getProfile } from '@/features/api/mobile-api';
import { exchangeTelegramCode, login, restoreSession, startTelegramLogin } from '@/features/auth/mobile-session';
import { loginSchema, validationMessage } from '@/features/auth/validation';

function safeContinuePath(value: string | string[] | undefined) {
  const path = Array.isArray(value) ? value[0] : value;
  return path && /^\/invite\/[A-Za-z0-9_-]+$/.test(path) ? path : null;
}

async function openAuthenticatedApp(continuePath?: string | null) {
  if (continuePath) {
    router.replace(continuePath as never);
    return;
  }
  try {
    const profile = await getProfile();
    router.replace((profile.onboardingCompleted ? '/dashboard' : '/onboarding') as never);
  } catch {
    router.replace('/dashboard');
  }
}

export default function LoginScreen() {
  const params = useLocalSearchParams<{ continue?: string | string[] }>();
  const continuePath = safeContinuePath(params.continue);
  const { theme } = useAppTheme();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [restoring, setRestoring] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const handleUrl = async (url: string) => {
      try {
        const session = await exchangeTelegramCode(url);
        if (active && session) await openAuthenticatedApp(continuePath);
      } catch (reason) {
        if (active) setError(reason instanceof Error ? reason.message : 'Не удалось завершить вход через Telegram.');
      }
    };
    const subscription = Linking.addEventListener('url', ({ url }) => void handleUrl(url));
    void (async () => {
      const initialUrl = await Linking.getInitialURL();
      if (initialUrl && await exchangeTelegramCode(initialUrl)) {
        if (active) await openAuthenticatedApp(continuePath);
      } else if (await restoreSession()) {
        if (active) await openAuthenticatedApp(continuePath);
      }
      if (active) setRestoring(false);
    })().catch(() => { if (active) setRestoring(false); });
    return () => { active = false; subscription.remove(); };
  }, [continuePath]);

  const submit = async () => {
    const parsed = loginSchema.safeParse({ email, password });
    const message = validationMessage(parsed);
    if (message) { setError(message); return; }
    setLoading(true); setError(null);
    try {
      await login(email.trim(), password);
      await openAuthenticatedApp(continuePath);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Не удалось войти.');
    } finally { setLoading(false); }
  };

  const telegram = async () => {
    setError(null);
    try { await startTelegramLogin(); }
    catch { setError('Не удалось открыть Telegram. Попробуйте ещё раз.'); }
  };

  if (restoring) return <Screen scroll={false}><LoadingView label="Возвращаем вас к вашей истории…" /></Screen>;

  return <Screen contentContainerStyle={styles.screen}>
    <View style={styles.brandBlock}>
      <View style={[styles.mark, { backgroundColor: theme.primarySoft, borderColor: theme.borderStrong }]}><AppText variant="title" color="primary">L</AppText></View>
      <AppText variant="label" color="primary">FOR MY L</AppText>
      <AppText variant="hero" align="center">Ваше время{`\n`}<AppText variant="hero" color="primary">для двоих.</AppText></AppText>
      <AppText color="textSecondary" align="center">Идеи, приглашения и тёплые воспоминания — в одном личном пространстве.</AppText>
    </View>
    <Card style={styles.form}>
      <AppText variant="heading">С возвращением</AppText>
      <AppText color="textSecondary">Войдите, чтобы продолжить вашу историю.</AppText>
      <TextField autoCapitalize="none" autoComplete="email" keyboardType="email-address" label="Email" onChangeText={setEmail} placeholder="you@example.com" returnKeyType="next" value={email} />
      <TextField autoComplete="current-password" label="Пароль" onChangeText={setPassword} onSubmitEditing={() => void submit()} placeholder="Ваш пароль" returnKeyType="go" secureTextEntry={!passwordVisible} value={password} right={<Pressable accessibilityLabel={passwordVisible ? 'Скрыть пароль' : 'Показать пароль'} hitSlop={10} onPress={() => setPasswordVisible(value => !value)}><MaterialCommunityIcons color={theme.textSecondary} name={passwordVisible ? 'eye-off-outline' : 'eye-outline'} size={21} /></Pressable>} />
      {error ? <AppText color="danger" variant="caption" accessibilityLiveRegion="polite">{error}</AppText> : null}
      <Button label="Войти" loading={loading} onPress={() => void submit()} />
      <Button label="Войти через Telegram" variant="secondary" disabled={loading} left={<MaterialCommunityIcons color={theme.primary} name="send" size={18} />} onPress={() => void telegram()} />
      <Pressable hitSlop={8} onPress={() => router.push('/reset-password' as never)}><AppText align="center" variant="label" color="primary">Забыли пароль?</AppText></Pressable>
    </Card>
    <View style={styles.signup}><AppText color="textSecondary">Впервые здесь?</AppText><Pressable hitSlop={8} onPress={() => router.push('/register' as never)}><AppText variant="bodyStrong" color="primary">Создать аккаунт</AppText></Pressable></View>
  </Screen>;
}

const styles = StyleSheet.create({
  screen: { justifyContent: 'center', paddingTop: 28 },
  brandBlock: { alignItems: 'center', gap: 10, paddingHorizontal: 12, marginBottom: 8 },
  mark: { width: 64, height: 64, borderRadius: 32, borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
  form: { gap: 16 },
  signup: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'center', gap: 6, paddingBottom: 16 },
});
