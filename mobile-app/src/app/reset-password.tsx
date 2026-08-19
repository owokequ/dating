import { MaterialCommunityIcons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { AppHeader, AppText, Button, Card, Screen, TextField, useAppTheme } from '@/design-system';
import { confirmPasswordReset, requestPasswordReset } from '@/features/auth/mobile-session';
import { emailSchema, passwordSchema, tokenSchema, validationMessage } from '@/features/auth/validation';

export default function ResetPasswordScreen() {
  const params = useLocalSearchParams<{ token?: string | string[] }>(); const initialToken = Array.isArray(params.token) ? params.token[0] ?? '' : params.token ?? '';
  const { theme } = useAppTheme(); const [email, setEmail] = useState(''); const [token, setToken] = useState(initialToken); const [password, setPassword] = useState('');
  const [requested, setRequested] = useState(false); const [passwordVisible, setPasswordVisible] = useState(false); const [loading, setLoading] = useState(false); const [error, setError] = useState<string | null>(null);
  const request = async () => {
    const message = validationMessage(emailSchema.safeParse(email)); if (message) { setError(message); return; }
    setLoading(true); setError(null); try { await requestPasswordReset(email.trim()); setRequested(true); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Не удалось отправить письмо.'); } finally { setLoading(false); }
  };
  const confirm = async () => {
    const tokenMessage = validationMessage(tokenSchema.safeParse(token)); const passwordMessage = validationMessage(passwordSchema.safeParse(password));
    if (tokenMessage || passwordMessage) { setError(tokenMessage ?? passwordMessage); return; }
    setLoading(true); setError(null); try { await confirmPasswordReset(token.trim(), password); router.replace('/'); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Не удалось обновить пароль.'); } finally { setLoading(false); }
  };
  const enteringPassword = Boolean(token);
  return <Screen>
    <AppHeader title="Восстановление" subtitle="Доступ к вашей истории" onBack={() => router.back()} />
    <View style={[styles.icon, { backgroundColor: theme.primarySoft }]}><MaterialCommunityIcons name="lock-reset" size={36} color={theme.primary} /></View>
    <View style={styles.intro}><AppText variant="title" align="center">{enteringPassword ? 'Придумайте новый пароль' : 'Всё поправим'}</AppText><AppText color="textSecondary" align="center">{enteringPassword ? 'Он должен содержать не менее 12 символов.' : 'Укажите email — мы отправим безопасную ссылку.'}</AppText></View>
    <Card style={styles.card}>
      {enteringPassword ? <>
        <TextField autoCapitalize="none" label="Код из письма" onChangeText={setToken} value={token} />
        <TextField autoComplete="new-password" label="Новый пароль" onChangeText={setPassword} onSubmitEditing={() => void confirm()} secureTextEntry={!passwordVisible} value={password} right={<Pressable hitSlop={10} onPress={() => setPasswordVisible(value => !value)}><MaterialCommunityIcons color={theme.textSecondary} name={passwordVisible ? 'eye-off-outline' : 'eye-outline'} size={21} /></Pressable>} />
        {error ? <AppText variant="caption" color="danger">{error}</AppText> : null}<Button label="Сохранить новый пароль" loading={loading} onPress={() => void confirm()} />
      </> : <>
        <TextField autoCapitalize="none" autoComplete="email" keyboardType="email-address" label="Email" onChangeText={setEmail} onSubmitEditing={() => void request()} placeholder="you@example.com" value={email} />
        {requested ? <View style={[styles.success, { backgroundColor: theme.primarySoft }]}><MaterialCommunityIcons name="email-check-outline" size={20} color={theme.primary} /><AppText variant="caption" color="primary" style={styles.flex}>Если аккаунт существует, письмо уже отправлено.</AppText></View> : null}
        {error ? <AppText variant="caption" color="danger">{error}</AppText> : null}<Button label={requested ? 'Отправить ещё раз' : 'Отправить ссылку'} loading={loading} onPress={() => void request()} />
        <Button label="У меня уже есть код" variant="ghost" onPress={() => setToken(' ')} />
      </>}
    </Card>
  </Screen>;
}

const styles = StyleSheet.create({ icon: { width: 80, height: 80, borderRadius: 40, alignItems: 'center', justifyContent: 'center', alignSelf: 'center' }, intro: { gap: 8 }, card: { gap: 16 }, success: { flexDirection: 'row', gap: 10, padding: 12, borderRadius: 14, alignItems: 'center' }, flex: { flex: 1 } });
