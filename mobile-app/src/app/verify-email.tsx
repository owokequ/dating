import { MaterialCommunityIcons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { AppHeader, AppText, Button, Card, Screen, TextField, useAppTheme } from '@/design-system';
import { verifyEmail } from '@/features/auth/mobile-session';
import { tokenSchema, validationMessage } from '@/features/auth/validation';

const param = (value: string | string[] | undefined) => Array.isArray(value) ? value[0] ?? '' : value ?? '';

export default function VerifyEmailScreen() {
  const params = useLocalSearchParams<{ token?: string | string[]; email?: string | string[] }>();
  const { theme } = useAppTheme();
  const [token, setToken] = useState(param(params.token));
  const [loading, setLoading] = useState(false); const [success, setSuccess] = useState(false); const [error, setError] = useState<string | null>(null);
  const submit = async () => {
    const message = validationMessage(tokenSchema.safeParse(token)); if (message) { setError(message); return; }
    setLoading(true); setError(null);
    try { await verifyEmail(token.trim()); setSuccess(true); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Не удалось подтвердить email.'); }
    finally { setLoading(false); }
  };
  return <Screen contentContainerStyle={styles.screen}>
    <AppHeader title="Подтверждение email" onBack={() => router.back()} />
    <View style={[styles.icon, { backgroundColor: theme.primarySoft }]}><MaterialCommunityIcons name={success ? 'email-check-outline' : 'email-heart-outline'} size={40} color={theme.primary} /></View>
    <AppText variant="title" align="center">{success ? 'Почта подтверждена' : 'Загляните в почту'}</AppText>
    <AppText color="textSecondary" align="center">{success ? 'Теперь можно войти и начать планировать ваши встречи.' : `Мы отправили ссылку${param(params.email) ? ` на ${param(params.email)}` : ''}. Она действует 24 часа.`}</AppText>
    <Card style={styles.card}>
      {success ? <Button label="Перейти ко входу" onPress={() => router.replace('/')} /> : <>
        <TextField autoCapitalize="none" label="Код из письма" onChangeText={setToken} onSubmitEditing={() => void submit()} placeholder="Вставьте код или токен" value={token} />
        {error ? <AppText variant="caption" color="danger" accessibilityLiveRegion="polite">{error}</AppText> : null}
        <Button label="Подтвердить email" loading={loading} onPress={() => void submit()} />
      </>}
    </Card>
    {!success ? <Button label="Вернуться ко входу" variant="ghost" onPress={() => router.replace('/')} /> : null}
  </Screen>;
}

const styles = StyleSheet.create({ screen: { justifyContent: 'center' }, icon: { width: 88, height: 88, borderRadius: 44, alignItems: 'center', justifyContent: 'center', alignSelf: 'center' }, card: { gap: 16 } });
