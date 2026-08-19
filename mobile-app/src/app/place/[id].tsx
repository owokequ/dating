import { useCallback, useEffect, useState, type ComponentProps } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { router, useLocalSearchParams } from 'expo-router';
import { AppHeader, AppText, Button, Card, EmptyState, LoadingView, Screen as BaseScreen, useAppTheme } from '@/design-system';
import { getPlace, type Place } from '@/features/api/mobile-api';

const Screen = (props: ComponentProps<typeof BaseScreen>) => <BaseScreen scroll={false} {...props} />;

export default function PlaceDetailsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>(); const theme = useAppTheme();
  const [place, setPlace] = useState<Place | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(false);
  const load = useCallback(async () => { if (!id) return; setLoading(true); setError(false); try { setPlace(await getPlace(id)); } catch { setError(true); } finally { setLoading(false); } }, [id]);
  useEffect(() => { void load(); }, [load]);
  if (loading) return <Screen><LoadingView label="Открываем место…" /></Screen>;
  if (!place || error) return <Screen><AppHeader title="Место" showBack /><EmptyState title="Место не загрузилось" description="Попробуйте ещё раз через несколько секунд." actionLabel="Повторить" onAction={() => void load()} /></Screen>;
  const hero = place.images[0]?.detailUrl ?? place.images[0]?.cardUrl ?? place.images[0]?.thumbnailUrl;
  return <Screen edges={['top']} style={styles.screen}><AppHeader title="Место" showBack /><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
    <View style={[styles.hero, { backgroundColor: theme.colors.primarySoft }]}>{hero ? <Image source={hero} style={StyleSheet.absoluteFill} contentFit="cover" transition={250} /> : <Ionicons name="heart" size={54} color={theme.colors.primary} />}</View>
    <View style={styles.heading}><AppText variant="eyebrow" color="primary">{place.category}</AppText><AppText variant="displaySmall">{place.name}</AppText><View style={styles.row}><Ionicons name="location-outline" size={18} color={theme.colors.muted} /><AppText color="muted" style={styles.flex}>{place.address}</AppText></View></View>
    {place.description ? <Card><AppText variant="title3">Об этом месте</AppText><AppText color="muted" style={styles.description}>{place.description}</AppText></Card> : null}
    {place.images.length > 1 ? <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.gallery}>{place.images.slice(1).map((image, index) => <Image key={`${image.thumbnailUrl}-${index}`} source={image.cardUrl ?? image.thumbnailUrl} style={styles.galleryImage} contentFit="cover" transition={200} />)}</ScrollView> : null}
  </ScrollView><View style={[styles.footer, { backgroundColor: theme.colors.background, borderTopColor: theme.colors.border }]}><Button label="Позвать сюда" icon="heart" onPress={() => router.push({ pathname: '/new-date', params: { placeId: place.id, name: place.name, address: place.address } })} /></View></Screen>;
}
const styles = StyleSheet.create({ screen: { paddingHorizontal: 0 }, content: { paddingBottom: 120, gap: 20 }, hero: { height: 310, alignItems: 'center', justifyContent: 'center' }, heading: { paddingHorizontal: 20, gap: 9 }, row: { flexDirection: 'row', alignItems: 'center', gap: 7 }, flex: { flex: 1 }, description: { marginTop: 8, lineHeight: 23 }, gallery: { paddingHorizontal: 20, gap: 10 }, galleryImage: { width: 220, height: 150, borderRadius: 20 }, footer: { position: 'absolute', left: 0, right: 0, bottom: 0, paddingHorizontal: 20, paddingTop: 12, paddingBottom: 24, borderTopWidth: StyleSheet.hairlineWidth } });
