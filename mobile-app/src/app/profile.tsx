import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, Linking, StyleSheet, Switch, View } from 'react-native';

import { AppHeader, AppText, Button, Card, Chip, InitialsAvatar, LoadingView, Screen, SectionHeader, TextField, useAppTheme, type ThemePreference } from '@/design-system';
import { getNotificationPreferences, getProfile, updateNotificationPreferences, updateProfile, type NotificationPreferences, type Profile } from '@/features/api/mobile-api';
import { logout } from '@/features/auth/mobile-session';

const preferenceLabels: [keyof NotificationPreferences, string, string][] = [
  ['inAppEnabled', 'В приложении', 'Хранить события в центре уведомлений'],
  ['pushEnabled', 'Push', 'Показывать приглашения и напоминания на телефоне'],
  ['telegramEnabled', 'Telegram', 'Дублировать сообщения в Telegram'],
  ['emailEnabled', 'Email', 'Получать важные сообщения на почту'],
];

const defaultPreferences: NotificationPreferences = {
  inAppEnabled: true,
  pushEnabled: true,
  telegramEnabled: false,
  emailEnabled: true,
};

export default function ProfileScreen() {
  const { theme, preference, setPreference } = useAppTheme();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [name, setName] = useState('');
  const [prefs, setPrefs] = useState<NotificationPreferences | null>(null);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const value = await getProfile();
        const notificationPrefs = await getNotificationPreferences().catch(() => {
          if (active) setLoadError('Настройки уведомлений пока недоступны. Можно продолжить и повторить попытку позже.');
          return defaultPreferences;
        });
        if (!active) return;
        setProfile(value);
        setName(value.displayName);
        setPrefs(notificationPrefs);
      } catch (error) {
        if (active) setLoadError(error instanceof Error ? error.message : 'Не удалось загрузить профиль.');
      }
    };
    void load();
    return () => { active = false; };
  }, []);
  const save = async () => { if (!prefs || !name.trim()) return; setSaving(true); try { const [nextProfile, nextPrefs] = await Promise.all([updateProfile(name.trim()), updateNotificationPreferences(prefs)]); setProfile(nextProfile); setPrefs(nextPrefs); Alert.alert('Сохранено', 'Профиль и уведомления обновлены.'); } catch (error) { Alert.alert('Не удалось сохранить', error instanceof Error ? error.message : 'Попробуйте ещё раз.'); } finally { setSaving(false); } };
  const exit = () => Alert.alert('Выйти из For my L?', 'На этом телефоне потребуется войти снова.', [{ text: 'Отмена', style: 'cancel' }, { text: 'Выйти', style: 'destructive', onPress: () => void logout().then(() => router.replace('/')) }]);
  if (!profile || !prefs) return <Screen scroll={false}><LoadingView label={loadError ?? 'Загружаем настройки…'} /></Screen>;
  return <Screen contentContainerStyle={styles.content}>
    <AppHeader title="Ещё" subtitle="ваш уголок" right={<InitialsAvatar name={profile.displayName} size={42} />} />
    {loadError ? <Card style={[styles.notice, { borderColor: theme.warning }]}><AppText color="warning" variant="caption">{loadError}</AppText></Card> : null}
    <Card style={styles.profileCard}><InitialsAvatar name={profile.displayName} size={78} /><View style={styles.profileCopy}><AppText variant="heading">{profile.displayName}</AppText><AppText color="textSecondary">{profile.email}</AppText>{profile.telegramLinked ? <AppText color="success" variant="caption">Telegram подключён</AppText> : null}</View></Card>
    <Button label="Пространство пары" variant="secondary" left={<Ionicons color={theme.primary} name="heart-outline" size={20} />} onPress={() => router.push('/couple')} />

    <SectionHeader title="Личные данные" />
    <Card><TextField label="Имя" value={name} maxLength={100} onChangeText={setName} /><TextField editable={false} label="Email" value={profile.email ?? ''} /></Card>

    <SectionHeader title="Уведомления" subtitle="Каждый канал работает независимо" />
    <Card>{preferenceLabels.map(([key, title, description], index) => <View key={key} style={[styles.settingRow, index > 0 && { borderTopColor: theme.border, borderTopWidth: StyleSheet.hairlineWidth }]}><View style={styles.settingCopy}><AppText variant="bodyStrong">{title}</AppText><AppText color="textSecondary" variant="caption">{description}</AppText></View><Switch accessibilityLabel={title} trackColor={{ false: theme.disabled, true: theme.primarySoft }} thumbColor={prefs[key] ? theme.primary : theme.textMuted} value={prefs[key]} onValueChange={(value) => setPrefs({ ...prefs, [key]: value })} /></View>)}</Card>

    <SectionHeader title="Оформление" subtitle="Можно следовать настройкам телефона" />
    <View style={styles.chips}>{([['system', 'Как на телефоне'], ['light', 'Светлая'], ['dark', 'Тёмная']] as [ThemePreference, string][]).map(([value, label]) => <Chip key={value} label={label} selected={preference === value} onPress={() => void setPreference(value)} />)}</View>

    <Button label="Сохранить изменения" loading={saving} onPress={() => void save()} />
    <Card onPress={() => void Linking.openSettings()}><View style={styles.linkRow}><Ionicons color={theme.primary} name="phone-portrait-outline" size={22} /><View style={styles.settingCopy}><AppText variant="bodyStrong">Настройки телефона</AppText><AppText color="textSecondary" variant="caption">Разрешения, push и системные параметры</AppText></View><Ionicons color={theme.textMuted} name="open-outline" size={19} /></View></Card>
    <Button label="Выйти" variant="danger" onPress={exit} />
    <AppText align="center" color="textMuted" variant="caption">For my L · сделано для двоих</AppText>
  </Screen>;
}

const styles = StyleSheet.create({ content: { paddingBottom: 130 }, notice: { borderWidth: 1 }, profileCard: { flexDirection: 'row', alignItems: 'center', gap: 16 }, profileCopy: { flex: 1, gap: 3 }, settingRow: { minHeight: 70, flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 10 }, settingCopy: { flex: 1, gap: 3 }, chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 }, linkRow: { flexDirection: 'row', alignItems: 'center', gap: 12 } });
