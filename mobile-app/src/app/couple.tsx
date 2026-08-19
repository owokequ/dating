import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { Alert, Share, StyleSheet, View } from 'react-native';

import { AppHeader, AppText, Button, Card, EmptyState, InitialsAvatar, LoadingView, Screen, StatusPill, useAppTheme } from '@/design-system';
import { createInvitation, getCouple, type Couple } from '@/features/api/mobile-api';

type DetailedCouple = Couple & { activatedAt?: string | null; createdAt?: string };

export default function CoupleScreen() {
  const { theme } = useAppTheme();
  const [couple, setCouple] = useState<DetailedCouple | null>(null);
  const [loading, setLoading] = useState(true);
  const [inviting, setInviting] = useState(false);
  const [invite, setInvite] = useState<{ inviteUrl: string; expiresAt: string } | null>(null);
  const load = useCallback(async () => { try { setCouple(await getCouple()); } finally { setLoading(false); } }, []);
  useEffect(() => { void load(); }, [load]);

  const shareInvite = async () => {
    setInviting(true);
    try {
      const value = invite ?? await createInvitation();
      setInvite(value);
      await Share.share({ title: 'Приглашение в For my L', message: `Присоединяйся ко мне в For my L: ${value.inviteUrl}`, url: value.inviteUrl });
    } catch (error) {
      Alert.alert('Не удалось создать приглашение', error instanceof Error ? error.message : 'Попробуйте ещё раз.');
    } finally { setInviting(false); }
  };

  if (loading) return <Screen scroll={false}><LoadingView label="Открываем пространство пары…" /></Screen>;
  return <Screen contentContainerStyle={styles.content}>
    <AppHeader title="Мы" subtitle="ваше закрытое пространство" onBack={() => router.back()} />
    {couple ? <>
      <Card style={[styles.coupleCard, { backgroundColor: theme.primarySoft }]}>
        <AppText style={[styles.letter, { color: theme.primary }]}>L</AppText>
        <StatusPill label={couple.status === 'ACTIVE' ? 'Вы вместе' : 'Ожидаем подтверждение'} tone={couple.status === 'ACTIVE' ? 'success' : 'warning'} />
        <View style={styles.members}>{couple.members.map((member) => <View key={member.userId} style={styles.member}><InitialsAvatar name={member.displayName ?? 'Партнёр'} size={72} /><AppText variant="bodyStrong" align="center">{member.displayName ?? 'Партнёр'}</AppText><AppText color="textSecondary" variant="caption">{member.role === 'OWNER' ? 'Создатель пары' : 'Партнёр'}</AppText></View>)}</View>
        {couple.activatedAt ? <AppText color="textSecondary" align="center">Вместе в For my L с {new Date(couple.activatedAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })}</AppText> : null}
      </Card>
      {couple.status !== 'ACTIVE' ? <Card><AppText variant="subheading">Приглашение ждёт вашего человека</AppText><AppText color="textSecondary">Ссылка одноразовая и ограничена по времени.</AppText><Button label="Поделиться приглашением" loading={inviting} left={<Ionicons color={theme.onPrimary} name="share-outline" size={19} />} onPress={() => void shareInvite()} /></Card> : <Card><AppText variant="subheading">Ваша следующая история</AppText><AppText color="textSecondary">Найдите место, выберите событие или придумайте что-то только ваше.</AppText><Button label="Предложить свидание" onPress={() => router.push('/new-date')} /></Card>}
    </> : <EmptyState icon={<Ionicons color={theme.primary} name="heart-circle-outline" size={64} />} title="Пригласите своего человека" description="Создайте личную ссылку и отправьте её тому, с кем хотите планировать свидания." actionLabel={inviting ? 'Создаём…' : 'Создать приглашение'} onAction={() => void shareInvite()} />}
  </Screen>;
}

const styles = StyleSheet.create({ content: { paddingBottom: 120 }, coupleCard: { minHeight: 300, justifyContent: 'space-between', overflow: 'hidden' }, letter: { position: 'absolute', right: -10, bottom: -45, opacity: 0.09, fontFamily: 'serif', fontSize: 180, fontStyle: 'italic' }, members: { flexDirection: 'row', justifyContent: 'space-around', gap: 16 }, member: { flex: 1, alignItems: 'center', gap: 7 } });
