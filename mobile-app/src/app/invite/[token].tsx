import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { AppText, Button, Card, EmptyState, LoadingView, Screen, useAppTheme } from '@/design-system';
import { acceptInvitation, getInvitation, type InvitationPreview } from '@/features/api/mobile-api';

export default function InvitationScreen() {
  const { token = '' } = useLocalSearchParams<{ token: string }>();
  const { theme } = useAppTheme();
  const [invitation, setInvitation] = useState<InvitationPreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void getInvitation(token).then(setInvitation).catch(() => setError('Приглашение недействительно или уже истекло.')).finally(() => setLoading(false));
  }, [token]);

  const accept = async () => {
    setAccepting(true); setError(null);
    try {
      await acceptInvitation(token);
      router.replace('/couple');
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : '';
      if (message.includes('401')) {
        router.replace({ pathname: '/', params: { continue: `/invite/${token}` } } as never);
      } else setError('Не удалось принять приглашение. Возможно, оно уже использовано.');
    } finally { setAccepting(false); }
  };

  if (loading) return <Screen scroll={false}><LoadingView label="Проверяем приглашение…" /></Screen>;
  if (!invitation) return <Screen><EmptyState title="Приглашение не найдено" description={error ?? 'Попросите партнёра создать новую ссылку.'} actionLabel="На главную" onAction={() => router.replace('/')} /></Screen>;
  return <Screen contentContainerStyle={styles.content}>
    <View style={[styles.mark, { backgroundColor: theme.primarySoft, borderColor: theme.borderStrong }]}><Ionicons color={theme.primary} name="heart" size={58} /></View>
    <View style={styles.copy}><AppText color="primary" variant="caption" align="center">ЛИЧНОЕ ПРИГЛАШЕНИЕ</AppText><AppText variant="hero" align="center">Вас приглашают{`\n`}в пространство для двоих</AppText><AppText color="textSecondary" align="center">Вместе выбирайте места, планируйте свидания и сохраняйте общие истории.</AppText></View>
    <Card><AppText variant="bodyStrong">Ссылка действует до</AppText><AppText color="textSecondary">{new Date(invitation.expiresAt).toLocaleString('ru-RU', { day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })}</AppText><AppText color="textMuted" variant="caption">Принять приглашение можно только один раз.</AppText></Card>
    {error ? <AppText color="danger" align="center" variant="caption">{error}</AppText> : null}
    <Button label="Принять приглашение" loading={accepting} onPress={() => void accept()} />
    <Button label="Не сейчас" variant="ghost" onPress={() => router.replace('/')} />
  </Screen>;
}

const styles = StyleSheet.create({ content: { flexGrow: 1, justifyContent: 'center', gap: 24 }, mark: { width: 132, height: 132, borderRadius: 66, borderWidth: 1, alignSelf: 'center', alignItems: 'center', justifyContent: 'center' }, copy: { gap: 10 } });
