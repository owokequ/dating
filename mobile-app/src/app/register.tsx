import { MaterialCommunityIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { AppHeader, AppText, Button, Card, Screen, TextField, useAppTheme } from '@/design-system';
import { register } from '@/features/auth/mobile-session';
import { registerSchema, validationMessage } from '@/features/auth/validation';

export default function RegisterScreen() {
  const { theme } = useAppTheme();
  const [displayName, setDisplayName] = useState(''); const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false); const [loading, setLoading] = useState(false); const [error, setError] = useState<string | null>(null);
  const submit = async () => {
    const input = { displayName, email, password }; const message = validationMessage(registerSchema.safeParse(input));
    if (message) { setError(message); return; }
    setLoading(true); setError(null);
    try { await register({ displayName: displayName.trim(), email: email.trim(), password }); router.replace({ pathname: '/verify-email', params: { email: email.trim() } } as never); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Не удалось создать аккаунт.'); }
    finally { setLoading(false); }
  };
  return <Screen>
    <AppHeader title="Создать аккаунт" subtitle="Ваше личное место для двоих" onBack={() => router.back()} />
    <View style={[styles.heroIcon, { backgroundColor: theme.primarySoft }]}><MaterialCommunityIcons name="heart-plus-outline" size={34} color={theme.primary} /></View>
    <View style={styles.intro}><AppText variant="title" align="center">Начните вашу{`\n`}историю</AppText><AppText color="textSecondary" align="center">Партнёр сможет присоединиться по вашей личной ссылке.</AppText></View>
    <Card style={styles.form}>
      <TextField autoComplete="name" label="Как вас называть" maxLength={100} onChangeText={setDisplayName} placeholder="Ваше имя" value={displayName} />
      <TextField autoCapitalize="none" autoComplete="email" keyboardType="email-address" label="Email" maxLength={320} onChangeText={setEmail} placeholder="you@example.com" value={email} />
      <TextField autoComplete="new-password" label="Пароль" hint="Не менее 12 символов" maxLength={72} onChangeText={setPassword} onSubmitEditing={() => void submit()} placeholder="Придумайте надёжный пароль" returnKeyType="done" secureTextEntry={!passwordVisible} value={password} right={<Pressable hitSlop={10} onPress={() => setPasswordVisible(value => !value)}><MaterialCommunityIcons color={theme.textSecondary} name={passwordVisible ? 'eye-off-outline' : 'eye-outline'} size={21} /></Pressable>} />
      {error ? <AppText variant="caption" color="danger" accessibilityLiveRegion="polite">{error}</AppText> : null}
      <Button label="Создать аккаунт" loading={loading} onPress={() => void submit()} />
      <AppText variant="caption" color="textMuted" align="center">Нажимая кнопку, вы соглашаетесь с условиями сервиса.</AppText>
    </Card>
    <Button label="Уже есть аккаунт" variant="ghost" onPress={() => router.replace('/')} />
  </Screen>;
}

const styles = StyleSheet.create({ heroIcon: { width: 72, height: 72, borderRadius: 36, alignItems: 'center', justifyContent: 'center', alignSelf: 'center' }, intro: { gap: 8 }, form: { gap: 16 } });
