import Constants from 'expo-constants';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

import { authenticatedFetch } from '@/features/auth/mobile-session';

export async function registerPushDevice() {
  const permission = await Notifications.requestPermissionsAsync();
  if (permission.status !== 'granted') return false;

  if (Platform.OS === 'android') {
    await Promise.all([
      Notifications.setNotificationChannelAsync('dates', { name: 'Свидания', importance: Notifications.AndroidImportance.HIGH }),
      Notifications.setNotificationChannelAsync('reminders', { name: 'Напоминания', importance: Notifications.AndroidImportance.HIGH }),
      Notifications.setNotificationChannelAsync('general', { name: 'Общие', importance: Notifications.AndroidImportance.DEFAULT }),
    ]);
  }

  const projectId = Constants.expoConfig?.extra?.eas?.projectId;
  if (!projectId) throw new Error('Expo project ID is not configured');
  const expoPushToken = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
  const response = await authenticatedFetch('/api/v1/mobile/devices', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ expoPushToken, platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID' }),
  });
  if (!response.ok) throw new Error('Не удалось зарегистрировать устройство для уведомлений');
  return true;
}
