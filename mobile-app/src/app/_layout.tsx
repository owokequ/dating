import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import * as Notifications from 'expo-notifications';
import { router, Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';

import { AppTabBar } from '@/components/app-tab-bar';
import { ThemeProvider } from '@/hooks/theme-provider';
import { useAppTheme } from '@/hooks/use-theme';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldPlaySound: true,
    shouldSetBadge: true,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

function routeNotification(data: Record<string, unknown> | undefined) {
  if (!data) return;
  const route = data.route;
  const referenceId = data.referenceId;
  if ((route === 'date' || route === 'reminder') && typeof referenceId === 'string') {
    router.push({ pathname: '/date/[id]', params: { id: referenceId } });
  } else if (route === 'notifications') {
    router.push('/notifications');
  }
}

function Navigation() {
  const { themeName } = useAppTheme();

  useEffect(() => {
    const subscription = Notifications.addNotificationResponseReceivedListener((response) => {
      routeNotification(response.notification.request.content.data);
    });
    void Notifications.getLastNotificationResponseAsync().then((response) => {
      if (response) routeNotification(response.notification.request.content.data);
    });
    return () => subscription.remove();
  }, []);

  return (
    <>
      <Stack screenOptions={{ animation: 'slide_from_right', headerShown: false }} />
      <AppTabBar />
      <StatusBar style={themeName === 'dark' ? 'light' : 'dark'} />
    </>
  );
}

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <Navigation />
      </ThemeProvider>
    </QueryClientProvider>
  );
}
