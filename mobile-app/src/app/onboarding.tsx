import { MaterialCommunityIcons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Share, StyleSheet, View } from 'react-native';

import { AppText, Button, Card, Chip, Screen, useAppTheme } from '@/design-system';
import { createInvitation } from '@/features/api/mobile-api';
import { registerPushDevice } from '@/features/push/register-device';

const totalSteps = 3;

export default function OnboardingScreen() {
  const { theme } = useAppTheme(); const [step, setStep] = useState(0); const [loading, setLoading] = useState(false); const [message, setMessage] = useState<string | null>(null); const [error, setError] = useState<string | null>(null);
  const next = () => { setError(null); setMessage(null); setStep(value => Math.min(value + 1, totalSteps - 1)); };
  const finish = () => router.replace('/dashboard');
  const invite = async () => {
    setLoading(true); setError(null);
    try {
      const invitation = await createInvitation();
      await Share.share({ title: 'Наше место For my L', message: `Присоединяйся ко мне в For my L ❤\ufe0f\n${invitation.inviteUrl}`, url: invitation.inviteUrl });
      next();
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Не удалось создать приглашение.'); }
    finally { setLoading(false); }
  };
  const enablePush = async () => {
    setLoading(true); setError(null); setMessage(null);
    try {
      const enabled = await registerPushDevice();
      if (!enabled) { setMessage('Уведомления остались выключены. Их можно включить позже в настройках.'); return; }
      setMessage('Готово! Мы напомним о самом важном.');
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Не удалось включить push-уведомления.'); }
    finally { setLoading(false); }
  };

  return <Screen contentContainerStyle={styles.screen}>
    <View style={styles.topRow}><AppText variant="label" color="primary">FOR MY L</AppText><Chip label={`${step + 1} из ${totalSteps}`} /></View>
    <View accessibilityLabel={`Шаг ${step + 1} из ${totalSteps}`} style={styles.progress}>{Array.from({ length: totalSteps }, (_, index) => <View key={index} style={[styles.progressDot, { backgroundColor: index <= step ? theme.primary : theme.border }]} />)}</View>
    {step === 0 ? <WelcomeStep onNext={next} /> : null}
    {step === 1 ? <PartnerStep error={error} loading={loading} onInvite={() => void invite()} onSkip={next} /> : null}
    {step === 2 ? <PushStep error={error} loading={loading} message={message} onEnable={() => void enablePush()} onFinish={finish} /> : null}
  </Screen>;
}

function WelcomeStep({ onNext }: { onNext: () => void }) {
  const { theme } = useAppTheme();
  return <View style={styles.step}>
    <View style={[styles.illustration, { backgroundColor: theme.primarySoft, borderColor: theme.borderStrong }]}><MaterialCommunityIcons name="heart-multiple-outline" size={70} color={theme.primary} /></View>
    <View style={styles.copy}><AppText variant="hero" align="center">Место для{`\n`} вашей <AppText variant="hero" color="primary">истории</AppText></AppText><AppText color="textSecondary" align="center">Выбирайте места, зовите друг друга на свидания и не теряйте маленькие важные моменты.</AppText></View>
    <Card style={styles.benefits}><Benefit icon="map-marker-outline" text="Идеи для встреч в Казани" /><Benefit icon="calendar-heart" text="Красивые личные приглашения" /><Benefit icon="bell-alert-outline" text="Напоминания без лишнего шума" /></Card>
    <Button label="Начать" onPress={onNext} />
  </View>;
}

function PartnerStep({ error, loading, onInvite, onSkip }: { error: string | null; loading: boolean; onInvite: () => void; onSkip: () => void }) {
  const { theme } = useAppTheme();
  return <View style={styles.step}><View style={[styles.illustration, { backgroundColor: theme.primarySoft, borderColor: theme.borderStrong }]}><MaterialCommunityIcons name="account-heart-outline" size={70} color={theme.primary} /></View><View style={styles.copy}><AppText variant="title" align="center">Пригласите своего человека</AppText><AppText color="textSecondary" align="center">Мы создадим личную ссылку. Отправьте её партнёру любым удобным способом.</AppText></View><Card style={styles.privacy}><MaterialCommunityIcons name="shield-lock-outline" size={24} color={theme.primary} /><View style={styles.flex}><AppText variant="bodyStrong">Только для двоих</AppText><AppText variant="caption" color="textSecondary">Ваше пространство останется закрытым и личным.</AppText></View></Card>{error ? <AppText variant="caption" color="danger" align="center">{error}</AppText> : null}<View style={styles.actions}><Button label="Создать и поделиться" loading={loading} onPress={onInvite} /><Button label="Сделаю позже" variant="ghost" disabled={loading} onPress={onSkip} /></View></View>;
}

function PushStep({ error, loading, message, onEnable, onFinish }: { error: string | null; loading: boolean; message: string | null; onEnable: () => void; onFinish: () => void }) {
  const { theme } = useAppTheme();
  return <View style={styles.step}><View style={[styles.illustration, { backgroundColor: theme.primarySoft, borderColor: theme.borderStrong }]}><MaterialCommunityIcons name="bell-ring-outline" size={66} color={theme.primary} /></View><View style={styles.copy}><AppText variant="title" align="center">Не пропускайте важное</AppText><AppText color="textSecondary" align="center">Push напомнит о новом приглашении, ответе партнёра и приближающемся свидании.</AppText></View><Card style={styles.privacy}><MaterialCommunityIcons name="shield-lock-outline" size={24} color={theme.primary} /><View style={styles.flex}><AppText variant="bodyStrong">Без лишних деталей</AppText><AppText variant="caption" color="textSecondary">На экране блокировки не показываем адреса и личные заметки.</AppText></View></Card>{message ? <AppText variant="caption" color="success" align="center">{message}</AppText> : null}{error ? <AppText variant="caption" color="danger" align="center">{error}</AppText> : null}<View style={styles.actions}>{!message ? <Button label="Разрешить push-уведомления" loading={loading} onPress={onEnable} /> : null}<Button label={message ? 'Открыть For my L' : 'Пока не сейчас'} variant={message ? 'primary' : 'ghost'} disabled={loading} onPress={onFinish} /></View></View>;
}

function Benefit({ icon, text }: { icon: keyof typeof MaterialCommunityIcons.glyphMap; text: string }) { const { theme } = useAppTheme(); return <View style={styles.benefit}><MaterialCommunityIcons name={icon} size={22} color={theme.primary} /><AppText style={styles.flex}>{text}</AppText></View>; }

const styles = StyleSheet.create({ screen: { justifyContent: 'space-between', paddingTop: 12 }, topRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }, progress: { flexDirection: 'row', gap: 7 }, progressDot: { height: 4, flex: 1, borderRadius: 2 }, step: { flex: 1, justifyContent: 'center', gap: 24 }, illustration: { width: 144, height: 144, borderRadius: 72, borderWidth: 1, alignSelf: 'center', alignItems: 'center', justifyContent: 'center' }, copy: { gap: 10 }, benefits: { gap: 16 }, benefit: { flexDirection: 'row', gap: 12, alignItems: 'center' }, privacy: { flexDirection: 'row', gap: 14, alignItems: 'center' }, actions: { gap: 8 }, flex: { flex: 1 } });
