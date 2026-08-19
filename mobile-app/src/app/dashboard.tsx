import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Pressable, RefreshControl, StyleSheet, View } from 'react-native';

import { AppHeader, AppText, Button, Card, EmptyState, InitialsAvatar, LoadingView, Screen, SectionHeader, StatusPill, useAppTheme } from '@/design-system';
import { getCouple, getProfile, listDates, type Couple, type DateProposal, type Profile } from '@/features/api/mobile-api';

const statusLabel = (status: string) => status === 'ACCEPTED' ? 'Подтверждено' : status === 'PENDING_CONFIRMATION' ? 'Ждёт ответа' : status;
const statusTone = (status: string) => status === 'ACCEPTED' ? 'success' as const : 'warning' as const;

export default function DashboardScreen() {
  const { theme } = useAppTheme();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [couple, setCouple] = useState<Couple | null>(null);
  const [dates, setDates] = useState<DateProposal[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    try {
      const [nextProfile, nextCouple] = await Promise.all([getProfile(), getCouple()]);
      const nextDates = nextCouple?.status === 'ACTIVE' ? await listDates() : [];
      setProfile(nextProfile);
      setCouple(nextCouple);
      setDates(nextDates);
    } catch (value) {
      setError(value instanceof Error ? value.message : 'Не удалось загрузить ваше пространство.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);
  const upcoming = useMemo(() => dates.filter((item) => ['ACCEPTED', 'PENDING_CONFIRMATION'].includes(item.status)).sort((a, b) => Date.parse(a.scheduledAt) - Date.parse(b.scheduledAt)), [dates]);

  if (loading) return <Screen scroll={false}><LoadingView label="Открываем ваше пространство…" /></Screen>;
  if (!profile || error) return <Screen><EmptyState title="Не удалось загрузить главную" description={error ?? 'Попробуйте ещё раз.'} actionLabel="Повторить" onAction={() => void load()} /></Screen>;

  const firstName = profile.displayName.trim().split(/\s+/)[0];
  const nextDate = upcoming[0];
  return (
    <Screen contentContainerStyle={styles.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void load(true)} tintColor={theme.primary} />}>
      <AppHeader title="For my L" subtitle="наше место" right={<Pressable accessibilityLabel="Уведомления" hitSlop={10} onPress={() => router.push('/notifications')}><Ionicons color={theme.primary} name="notifications-outline" size={24} /></Pressable>} />
      <View style={styles.greeting}>
        <View style={styles.greetingCopy}><AppText color="primary" variant="caption">НАШ ДНЕВНИК · КАЗАНЬ</AppText><AppText variant="title">С возвращением, {firstName}</AppText><AppText color="textSecondary">Сегодня хороший день, чтобы придумать следующую встречу.</AppText></View>
        <InitialsAvatar name={profile.displayName} size={62} />
      </View>

      {nextDate ? (
        <Card onPress={() => router.push({ pathname: '/date/[id]', params: { id: nextDate.id } })} style={[styles.hero, { backgroundColor: theme.primary }]}>
          <AppText style={styles.watermark}>L</AppText>
          <StatusPill label={statusLabel(nextDate.status)} tone={statusTone(nextDate.status)} />
          <View style={styles.heroCopy}><AppText color="onPrimary" variant="caption">БЛИЖАЙШАЯ СТРАНИЦА</AppText><AppText color="onPrimary" variant="heading">{nextDate.placeName}</AppText><AppText color="onPrimary" variant="bodyStrong">{new Date(nextDate.scheduledAt).toLocaleString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })}</AppText><AppText color="onPrimary" numberOfLines={2}>{nextDate.placeAddress}</AppText></View>
        </Card>
      ) : (
        <Card style={{ backgroundColor: theme.primarySoft }}><AppText variant="subheading">Первая страница ещё не написана</AppText><AppText color="textSecondary">Выберите место или событие, которое хочется разделить вдвоём.</AppText><Button label="Найти идею" onPress={() => router.push('/places')} /></Card>
      )}

      {!couple || couple.status !== 'ACTIVE' ? (
        <Card><AppText variant="subheading">{couple ? 'Ждём вашего человека' : 'Пока вы здесь один'}</AppText><AppText color="textSecondary">Создайте защищённую ссылку и отправьте её партнёру.</AppText><Button label="Открыть пространство пары" variant="secondary" onPress={() => router.push('/couple')} /></Card>
      ) : null}

      <SectionHeader title="Что хочется сегодня?" />
      <View style={styles.actions}>
        <QuickAction icon="calendar-outline" label="Предложить свидание" onPress={() => router.push('/new-date')} />
        <QuickAction icon="location-outline" label="Выбрать место" onPress={() => router.push('/places')} />
        <QuickAction icon="sparkles-outline" label="Открыть афишу" onPress={() => router.push('/events')} />
        <QuickAction icon="heart-outline" label="Наше пространство" onPress={() => router.push('/couple')} />
      </View>

      {upcoming.length > 1 ? <><SectionHeader title="Ближайшие свидания" actionLabel="Все" onAction={() => router.push('/dates')} />{upcoming.slice(1, 4).map((item) => <Card key={item.id} onPress={() => router.push({ pathname: '/date/[id]', params: { id: item.id } })}><View style={styles.dateRow}><View style={styles.dateCopy}><AppText variant="subheading">{item.placeName}</AppText><AppText color="textSecondary">{new Date(item.scheduledAt).toLocaleString('ru-RU', { day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })}</AppText></View><Ionicons color={theme.primary} name="chevron-forward" size={22} /></View></Card>)}</> : null}
    </Screen>
  );
}

function QuickAction({ icon, label, onPress }: { icon: keyof typeof Ionicons.glyphMap; label: string; onPress: () => void }) {
  const { theme } = useAppTheme();
  return <Card accessibilityLabel={label} onPress={onPress} style={styles.action}><Ionicons color={theme.primary} name={icon} size={25} /><AppText variant="bodyStrong">{label}</AppText></Card>;
}

const styles = StyleSheet.create({
  content: { paddingBottom: 130 },
  greeting: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  greetingCopy: { flex: 1, gap: 5 },
  hero: { minHeight: 245, justifyContent: 'space-between', overflow: 'hidden' },
  heroCopy: { gap: 7, maxWidth: '88%' },
  watermark: { position: 'absolute', right: -12, bottom: -58, color: 'rgba(255,255,255,0.10)', fontFamily: 'serif', fontSize: 190, fontStyle: 'italic', lineHeight: 210 },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  action: { width: '48%', minHeight: 128, justifyContent: 'space-between' },
  dateRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  dateCopy: { flex: 1, gap: 4 },
});
