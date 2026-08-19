import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { router, useLocalSearchParams } from 'expo-router';
import { useMemo, useRef, useState, type ComponentProps } from 'react';
import { Alert, Image, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, TextInput, View } from 'react-native';

import { AppHeader, AppText, Button, Card, Chip, Screen as BaseScreen, SectionHeader, useAppTheme } from '@/design-system';
import { createDate, createDateFromEvent, createIdempotencyKey, createPrivateDate, getDate, sendPrivateDate, uploadPrivateDatePhoto } from '@/features/api/mobile-api';

type Source = 'place' | 'event' | 'private';
const Screen = (props: ComponentProps<typeof BaseScreen>) => <BaseScreen scroll={false} {...props} />;
const initialDate = new Date(Date.now() + 86_400_000).toISOString().slice(0, 10);
const pause = (milliseconds: number) => new Promise((resolve) => setTimeout(resolve, milliseconds));

async function uploadPhotoWhenDraftIsReady(proposalId: string, photo: ImagePicker.ImagePickerAsset) {
  let lastError: unknown;
  for (let attempt = 0; attempt < 8; attempt += 1) {
    try {
      return await uploadPrivateDatePhoto(proposalId, photo);
    } catch (error) {
      lastError = error;
      await pause(500);
    }
  }
  throw lastError ?? new Error('Не удалось подготовить фото.');
}

async function waitForPhotoCover(proposalId: string, mediaId: string) {
  for (let attempt = 0; attempt < 12; attempt += 1) {
    const draft = await getDate(proposalId);
    if (draft.placeCoverMediaId === mediaId) return;
    await pause(500);
  }
  throw new Error('Фото ещё обрабатывается. Попробуйте отправить приглашение чуть позже.');
}

export default function NewDateScreen() {
  const params = useLocalSearchParams<{ placeId?: string; occurrenceId?: string; name?: string; address?: string; scheduledAt?: string }>();
  const theme = useAppTheme();
  const initialSource: Source = params.placeId ? 'place' : params.occurrenceId ? 'event' : 'private';
  const [source, setSource] = useState<Source>(initialSource);
  const [step, setStep] = useState<1 | 2>(1);
  const [place, setPlace] = useState(params.name ?? '');
  const [address, setAddress] = useState(params.address ?? '');
  const eventDate = params.scheduledAt ? new Date(params.scheduledAt) : null;
  const [date, setDate] = useState(eventDate ? eventDate.toISOString().slice(0, 10) : initialDate);
  const [time, setTime] = useState(eventDate ? eventDate.toTimeString().slice(0, 5) : '19:00');
  const [note, setNote] = useState('');
  const [photo, setPhoto] = useState<ImagePicker.ImagePickerAsset | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const requestKey = useRef(createIdempotencyKey());
  const sendKey = useRef(createIdempotencyKey());
  const when = useMemo(() => {
    const result = new Date(`${date}T${time}:00`);
    return Number.isNaN(result.getTime()) ? null : result;
  }, [date, time]);
  const canContinue = Boolean(place.trim() && when && when > new Date());
  const inputStyle = [styles.input, { color: theme.colors.text, backgroundColor: theme.colors.surface, borderColor: theme.colors.border }];

  const pickPhoto = async (fromCamera: boolean) => {
    const permission = fromCamera ? await ImagePicker.requestCameraPermissionsAsync() : await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Нужен доступ', fromCamera ? 'Разрешите камеру в настройках телефона, чтобы сделать снимок.' : 'Разрешите доступ к фото в настройках телефона, чтобы выбрать снимок.');
      return;
    }
    const options = { allowsEditing: true, aspect: [4, 3] as [number, number], quality: 0.8, mediaTypes: ['images'] as ImagePicker.MediaType[] };
    const result = fromCamera
      ? await ImagePicker.launchCameraAsync(options)
      : await ImagePicker.launchImageLibraryAsync({ ...options, preferredAssetRepresentationMode: ImagePicker.UIImagePickerPreferredAssetRepresentationMode.Compatible });
    if (!result.canceled) setPhoto(result.assets[0]);
  };
  const choosePhoto = () => Alert.alert('Фото места', 'Оно будет видно только вам и вашему партнёру.', [
    { text: 'Сделать фото', onPress: () => void pickPhoto(true) },
    { text: 'Выбрать из галереи', onPress: () => void pickPhoto(false) },
    { text: 'Отмена', style: 'cancel' },
  ]);

  const submit = async () => {
    if (!when || !canContinue || submitting) return;
    setSubmitting(true);
    try {
      if (source === 'place' && params.placeId) {
        await createDate({ placeId: params.placeId, scheduledAt: when.toISOString(), description: note.trim() || undefined }, requestKey.current);
      } else if (source === 'event' && params.occurrenceId) {
        await createDateFromEvent({ eventOccurrenceId: params.occurrenceId, visitAt: when.toISOString(), description: note.trim() || undefined }, requestKey.current);
      } else {
        const draft = await createPrivateDate({ placeName: place.trim(), placeAddress: address.trim() || undefined, scheduledAt: when.toISOString(), description: note.trim() || undefined }, requestKey.current);
        if (photo) {
          const uploaded = await uploadPhotoWhenDraftIsReady(draft.id, photo);
          await waitForPhotoCover(draft.id, uploaded.mediaId);
        }
        await sendPrivateDate(draft.id, sendKey.current);
      }
      requestKey.current = createIdempotencyKey();
      sendKey.current = createIdempotencyKey();
      Alert.alert('Приглашение отправлено 💗', 'Теперь осталось дождаться ответа.', [{ text: 'Посмотреть свидания', onPress: () => router.replace('/dates') }]);
    } catch (error) {
      Alert.alert('Не удалось отправить', error instanceof Error ? error.message : 'Проверьте соединение и попробуйте снова.');
    } finally {
      setSubmitting(false);
    }
  };

  const SourceCard = ({ icon, title, description, selected, onPress }: { icon: keyof typeof Ionicons.glyphMap; title: string; description: string; selected?: boolean; onPress: () => void }) => (
    <Pressable accessibilityRole="button" accessibilityState={{ selected }} onPress={onPress} style={[styles.source, { backgroundColor: selected ? theme.colors.primarySoft : theme.colors.surface, borderColor: selected ? theme.colors.primary : theme.colors.border }]}>
      <Ionicons name={icon} size={24} color={theme.colors.primary} /><AppText variant="label">{title}</AppText><AppText variant="caption" color="muted" style={styles.center}>{description}</AppText>
    </Pressable>
  );

  return <Screen edges={['top']} style={styles.screen}>
    <AppHeader title="Новое свидание" showBack subtitle={step === 1 ? 'Шаг 1 из 2 · детали' : 'Шаг 2 из 2 · приглашение'} />
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
        {!params.placeId && !params.occurrenceId ? <><SectionHeader title="С чего начнём?" subtitle="Выберите идею для свидания" /><View style={styles.sources}><SourceCard icon="location" title="Место" description="Из нашей подборки" onPress={() => router.replace('/places')} /><SourceCard icon="ticket" title="Событие" description="Концерт, театр, выставка" onPress={() => router.replace('/events')} /><SourceCard icon="sparkles" title="Своё" description="Любое особенное место" selected={source === 'private'} onPress={() => setSource('private')} /></View></> : null}
        {step === 1 ? <><SectionHeader title="Детали вечера" /><Card style={styles.form}>
          <AppText variant="label">Место</AppText><TextInput accessibilityLabel="Название места" style={inputStyle} value={place} onChangeText={setPlace} editable={source === 'private'} placeholder="Например, набережная у Кремля" placeholderTextColor={theme.colors.muted} maxLength={300} />
          {source === 'private' ? <><AppText variant="label">Адрес — необязательно</AppText><TextInput accessibilityLabel="Адрес места" style={inputStyle} value={address} onChangeText={setAddress} placeholder="Где встречаемся?" placeholderTextColor={theme.colors.muted} maxLength={500} /><AppText variant="label">Фото места — необязательно</AppText>{photo ? <View style={styles.photoPreview}><Image source={{ uri: photo.uri }} style={styles.photo} /><Pressable accessibilityLabel="Удалить выбранное фото" onPress={() => setPhoto(null)} style={[styles.removePhoto, { backgroundColor: theme.colors.surfaceElevated }]}><Ionicons color={theme.colors.text} name="close" size={20} /></Pressable><Pressable accessibilityRole="button" onPress={choosePhoto} style={styles.changePhoto}><AppText color="onPrimary" variant="caption">Изменить фото</AppText></Pressable></View> : <Pressable accessibilityRole="button" accessibilityLabel="Добавить фото места" onPress={choosePhoto} style={[styles.addPhoto, { backgroundColor: theme.colors.primarySoft, borderColor: theme.colors.primary }]}><Ionicons color={theme.colors.primary} name="camera-outline" size={25} /><View style={styles.flex}><AppText variant="bodyStrong">Добавить фото</AppText><AppText color="muted" variant="caption">Камера или галерея</AppText></View><Ionicons color={theme.colors.primary} name="chevron-forward" size={20} /></Pressable>}</> : null}
          <AppText variant="label">Дата и время</AppText><View style={styles.dateRow}><TextInput accessibilityLabel="Дата в формате год месяц день" style={[...inputStyle, styles.dateInput]} value={date} onChangeText={setDate} placeholder="ГГГГ-ММ-ДД" placeholderTextColor={theme.colors.muted} keyboardType="numbers-and-punctuation" /><TextInput accessibilityLabel="Время в формате часы минуты" style={[...inputStyle, styles.timeInput]} value={time} onChangeText={setTime} placeholder="19:00" placeholderTextColor={theme.colors.muted} keyboardType="numbers-and-punctuation" /></View>{when && when <= new Date() ? <AppText variant="caption" color="danger">Выберите время в будущем</AppText> : null}
          <AppText variant="label">Личная заметка — необязательно</AppText><TextInput accessibilityLabel="Личная заметка" style={[...inputStyle, styles.note]} value={note} onChangeText={setNote} multiline textAlignVertical="top" placeholder="Напишите пару тёплых слов…" placeholderTextColor={theme.colors.muted} maxLength={1000} />
        </Card><Button label="Посмотреть приглашение" disabled={!canContinue} onPress={() => setStep(2)} /></> : <><SectionHeader title="Всё верно?" subtitle="Так приглашение будет выглядеть для партнёра" /><View style={[styles.preview, { backgroundColor: theme.colors.primarySoft }]}>{photo ? <Image source={{ uri: photo.uri }} style={styles.previewImage} /> : <Ionicons name="heart" size={38} color={theme.colors.primary} />}<Chip label={source === 'event' ? 'Событие' : source === 'place' ? 'Место из каталога' : 'Своё место'} selected /><AppText variant="displaySmall" style={styles.center}>{place}</AppText><AppText color="muted" style={styles.center}>{when ? new Intl.DateTimeFormat('ru-RU', { weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' }).format(when) : ''}</AppText>{address ? <AppText variant="bodySmall" color="muted" style={styles.center}>{address}</AppText> : null}{note ? <AppText style={[styles.center, styles.previewNote]}>«{note}»</AppText> : null}</View><View style={styles.buttons}><Button label="Изменить" variant="secondary" style={styles.flex} onPress={() => setStep(1)} /><Button label={photo ? 'Загрузить и отправить' : 'Отправить'} loading={submitting} style={styles.flex} onPress={() => void submit()} /></View></>}
      </ScrollView>
    </KeyboardAvoidingView>
  </Screen>;
}

const styles = StyleSheet.create({
  screen: { paddingHorizontal: 0 }, flex: { flex: 1 }, content: { padding: 20, paddingBottom: 80, gap: 18 }, sources: { flexDirection: 'row', gap: 9 }, source: { flex: 1, minHeight: 118, borderWidth: 1, borderRadius: 20, padding: 10, alignItems: 'center', justifyContent: 'center', gap: 7 }, form: { gap: 9 }, input: { minHeight: 50, borderWidth: 1, borderRadius: 16, paddingHorizontal: 14, fontSize: 16 }, dateRow: { flexDirection: 'row', gap: 9 }, dateInput: { flex: 1.4 }, timeInput: { flex: 0.8 }, note: { minHeight: 104, paddingTop: 14 }, addPhoto: { minHeight: 72, borderWidth: 1, borderRadius: 16, paddingHorizontal: 14, flexDirection: 'row', alignItems: 'center', gap: 12 }, photoPreview: { height: 180, borderRadius: 18, overflow: 'hidden', position: 'relative' }, photo: { width: '100%', height: '100%' }, removePhoto: { position: 'absolute', top: 10, right: 10, width: 34, height: 34, borderRadius: 17, alignItems: 'center', justifyContent: 'center' }, changePhoto: { position: 'absolute', right: 0, bottom: 0, left: 0, alignItems: 'center', paddingVertical: 9, backgroundColor: 'rgba(0,0,0,0.45)' }, preview: { minHeight: 360, borderRadius: 32, borderBottomLeftRadius: 8, padding: 28, alignItems: 'center', justifyContent: 'center', gap: 14, overflow: 'hidden' }, previewImage: { width: '100%', height: 150, borderRadius: 20 }, center: { textAlign: 'center' }, previewNote: { fontStyle: 'italic', marginTop: 5 }, buttons: { flexDirection: 'row', gap: 10 },
});
