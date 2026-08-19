import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, View } from 'react-native';

import { AppHeader, AppText, Card, EmptyState, LoadingView, Screen, useAppTheme } from '@/design-system';
import { listNotifications, markNotificationRead, type AppNotification } from '@/features/api/mobile-api';

export default function NotificationsScreen() {
  const { theme } = useAppTheme();
  const [items, setItems] = useState<AppNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const load = useCallback(async (refresh = false) => { if (refresh) setRefreshing(true); else setLoading(true); try { setItems(await listNotifications()); } finally { setLoading(false); setRefreshing(false); } }, []);
  useEffect(() => { void load(); }, [load]);
  const open = async (item: AppNotification) => {
    if (!item.readAt) {
      await markNotificationRead(item.id);
      setItems((old) => old.map((value) => value.id === item.id ? { ...value, readAt: new Date().toISOString() } : value));
    }
    const referenceId = (item as AppNotification & { referenceId?: string }).referenceId;
    if (referenceId && item.type.startsWith('DATE_')) router.push({ pathname: '/date/[id]', params: { id: referenceId } });
  };
  return <Screen scroll={false} contentContainerStyle={styles.content}>
    <AppHeader title="Уведомления" subtitle={`${items.filter((item) => !item.readAt).length} непрочитанных`} onBack={() => router.back()} />
    {loading ? <LoadingView /> : <FlatList
      data={items}
      keyExtractor={(item) => item.id}
      contentContainerStyle={items.length ? styles.list : styles.empty}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void load(true)} tintColor={theme.primary} />}
      ListEmptyComponent={<EmptyState icon={<Ionicons color={theme.primary} name="notifications-outline" size={56} />} title="Здесь пока тихо" description="Приглашения, ответы и напоминания появятся в этом разделе." />}
      renderItem={({ item }) => <Pressable accessibilityRole="button" onPress={() => void open(item)}><Card style={!item.readAt && { borderLeftColor: theme.primary, borderLeftWidth: 4 }}><View style={styles.notificationRow}><View style={[styles.icon, { backgroundColor: theme.primarySoft }]}><Ionicons color={theme.primary} name={item.type.includes('REMINDER') ? 'alarm-outline' : 'heart-outline'} size={20} /></View><View style={styles.copy}><View style={styles.titleRow}><AppText variant="bodyStrong" numberOfLines={2}>{item.title}</AppText>{!item.readAt ? <View accessibilityLabel="Не прочитано" style={[styles.dot, { backgroundColor: theme.primary }]} /> : null}</View><AppText color="textSecondary" numberOfLines={3}>{item.body}</AppText><AppText color="textMuted" variant="caption">{new Date(item.createdAt).toLocaleString('ru-RU', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</AppText></View></View></Card></Pressable>}
    />}
  </Screen>;
}
const styles = StyleSheet.create({ content: { paddingBottom: 20 }, list: { gap: 10, paddingBottom: 30 }, empty: { flexGrow: 1 }, notificationRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 12 }, icon: { width: 42, height: 42, borderRadius: 21, alignItems: 'center', justifyContent: 'center' }, copy: { flex: 1, gap: 5 }, titleRow: { flexDirection: 'row', alignItems: 'center', gap: 8 }, dot: { width: 8, height: 8, borderRadius: 4 } });
