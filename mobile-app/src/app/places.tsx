import { useCallback, useEffect, useMemo, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import { AppHeader, AppText, Card, Chip, EmptyState, LoadingView, Screen, useAppTheme } from '@/design-system';
import { getPlaces, type Place } from '@/features/api/mobile-api';

const categories = [{ value: '', label: 'Все' }, { value: 'CAFE', label: 'Кофе и десерты' }, { value: 'RESTAURANT', label: 'Ужин вдвоём' }, { value: 'ENTERTAINMENT', label: 'Впечатления' }];
const cover = (place: Place) => place.images[0]?.cardUrl ?? place.images[0]?.thumbnailUrl;

export default function PlacesScreen() {
  const theme = useAppTheme();
  const [items, setItems] = useState<Place[]>([]);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(false);
  const load = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true); else setLoading(true); setError(false);
    try { setItems((await getPlaces({ category: category || undefined })).items); }
    catch { setError(true); }
    finally { setLoading(false); setRefreshing(false); }
  }, [category]);
  useEffect(() => { void load(); }, [load]);
  const visible = useMemo(() => { const value = query.trim().toLocaleLowerCase('ru'); return value ? items.filter(item => `${item.name} ${item.address}`.toLocaleLowerCase('ru').includes(value)) : items; }, [items, query]);
  return <Screen scroll={false} edges={['top']} style={styles.screen}>
    <AppHeader title="Места для двоих" subtitle="Казань · выберите настроение вечера" />
    <View style={[styles.search, { backgroundColor: theme.colors.surface, borderColor: theme.colors.border }]}><Ionicons name="search" size={20} color={theme.colors.muted} /><TextInput accessibilityLabel="Поиск мест" placeholder="Название или адрес" placeholderTextColor={theme.colors.muted} value={query} onChangeText={setQuery} style={[styles.input, { color: theme.colors.text }]} returnKeyType="search" /></View>
    <FlatList horizontal data={categories} keyExtractor={item => item.value} renderItem={({ item }) => <Chip label={item.label} selected={category === item.value} onPress={() => setCategory(item.value)} />} contentContainerStyle={styles.chips} showsHorizontalScrollIndicator={false} style={styles.chipList} />
    {loading ? <LoadingView label="Подбираем красивые места…" /> : <FlatList data={visible} keyExtractor={item => item.id} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void load(true)} tintColor={theme.colors.primary} />} contentContainerStyle={styles.list} showsVerticalScrollIndicator={false} ListEmptyComponent={<EmptyState title={error ? 'Не удалось загрузить места' : 'Ничего не найдено'} description={error ? 'Проверьте соединение и попробуйте ещё раз.' : 'Попробуйте другой запрос или категорию.'} actionLabel={error ? 'Повторить' : undefined} onAction={error ? () => void load() : undefined} />} renderItem={({ item }) => <Pressable accessibilityRole="button" accessibilityLabel={`${item.name}, ${item.address}`} onPress={() => router.push({ pathname: '/place/[id]' as never, params: { id: item.id } } as never)}><Card style={styles.card} padding={0}><View style={[styles.photo, { backgroundColor: theme.colors.primarySoft }]}>{cover(item) ? <Image source={cover(item)} style={StyleSheet.absoluteFill} contentFit="cover" transition={250} /> : <Ionicons name="heart" size={38} color={theme.colors.primary} />}<View style={styles.badge}><AppText variant="caption" style={styles.badgeText}>{item.category}</AppText></View></View><View style={styles.cardBody}><AppText variant="title3" numberOfLines={1}>{item.name}</AppText><View style={styles.row}><Ionicons name="location-outline" size={16} color={theme.colors.muted} /><AppText variant="bodySmall" color="muted" numberOfLines={1} style={styles.address}>{item.address}</AppText></View><View style={styles.rowBetween}><AppText variant="bodySmall" color="primary">{'₽'.repeat(Math.max(1, item.priceLevel ?? 1))}</AppText><AppText variant="label" color="primary">Посмотреть →</AppText></View></View></Card></Pressable>} />}
  </Screen>;
}
const styles = StyleSheet.create({ screen: { paddingHorizontal: 0 }, search: { height: 50, marginHorizontal: 20, borderWidth: 1, borderRadius: 18, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', gap: 10 }, input: { flex: 1, fontSize: 16 }, chipList: { flexGrow: 0 }, chips: { paddingHorizontal: 20, paddingVertical: 14, gap: 8 }, list: { paddingHorizontal: 20, paddingBottom: 120, gap: 16, flexGrow: 1 }, card: { overflow: 'hidden' }, photo: { height: 190, alignItems: 'center', justifyContent: 'center' }, badge: { position: 'absolute', left: 12, top: 12, borderRadius: 999, backgroundColor: 'rgba(32,21,28,0.78)', paddingHorizontal: 11, paddingVertical: 6 }, badgeText: { color: '#fff' }, cardBody: { padding: 16, gap: 9 }, row: { flexDirection: 'row', alignItems: 'center', gap: 6 }, rowBetween: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }, address: { flex: 1 } });
