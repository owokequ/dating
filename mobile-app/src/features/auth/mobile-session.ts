import * as Linking from 'expo-linking';
import * as SecureStore from 'expo-secure-store';

export type MobileSession = {
  tokenType: 'Bearer';
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
};

const refreshTokenKey = 'owoke.mobile.refresh-token';
const apiUrl = process.env.EXPO_PUBLIC_API_URL?.replace(/\/$/, '');

let accessToken: string | null = null;
let refreshInFlight: Promise<MobileSession | null> | null = null;

function requireApiUrl() {
  if (!apiUrl) {
    throw new Error('EXPO_PUBLIC_API_URL is not configured');
  }
  return apiUrl;
}

async function requestSession(path: string, body: object): Promise<MobileSession> {
  const response = await fetch(`${requireApiUrl()}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(await responseError(response, 'Не удалось выполнить вход. Проверьте данные.'));
  }
  return (await response.json()) as MobileSession;
}

async function responseError(response: Response, fallback: string) {
  try {
    const body = await response.json() as { detail?: string; message?: string; title?: string };
    return body.detail || body.message || body.title || fallback;
  } catch {
    return fallback;
  }
}

async function publicJsonRequest(path: string, body: object, fallback: string) {
  const response = await fetch(`${requireApiUrl()}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(await responseError(response, fallback));
}

async function saveSession(session: MobileSession) {
  accessToken = session.accessToken;
  await SecureStore.setItemAsync(refreshTokenKey, session.refreshToken);
  return session;
}

export async function login(email: string, password: string) {
  return saveSession(await requestSession('/api/v1/auth/mobile/login', { email, password }));
}

export async function register(input: { displayName: string; email: string; password: string }) {
  await publicJsonRequest('/api/v1/auth/register', input, 'Не удалось создать аккаунт.');
}

export async function verifyEmail(token: string) {
  await publicJsonRequest('/api/v1/auth/email-verifications/confirm', { token }, 'Ссылка неверна или уже истекла.');
}

export async function requestPasswordReset(email: string) {
  await publicJsonRequest('/api/v1/auth/password-reset/request', { email }, 'Не удалось отправить письмо.');
}

export async function confirmPasswordReset(token: string, newPassword: string) {
  await publicJsonRequest('/api/v1/auth/password-reset/confirm', { token, newPassword }, 'Не удалось сохранить новый пароль.');
}

export async function restoreSession() {
  return refreshSession();
}

export async function refreshSession(): Promise<MobileSession | null> {
  if (refreshInFlight) return refreshInFlight;

  refreshInFlight = (async () => {
    try {
      const refreshToken = await SecureStore.getItemAsync(refreshTokenKey);
      if (!refreshToken) return null;
      return await saveSession(await requestSession('/api/v1/auth/mobile/refresh', { refreshToken }));
    } catch {
      accessToken = null;
      await SecureStore.deleteItemAsync(refreshTokenKey);
      return null;
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

export async function logout() {
  const refreshToken = await SecureStore.getItemAsync(refreshTokenKey);
  try {
    if (refreshToken) {
      await fetch(`${requireApiUrl()}/api/v1/auth/mobile/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
    }
  } finally {
    accessToken = null;
    await SecureStore.deleteItemAsync(refreshTokenKey);
  }
}

export async function authenticatedFetch(input: string, init: RequestInit = {}) {
  const request = async () => {
    const headers = new Headers(init.headers);
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
    return fetch(`${requireApiUrl()}${input}`, { ...init, headers });
  };

  let response = await request();
  if (response.status !== 401 || !(await refreshSession())) return response;
  response = await request();
  return response;
}

export async function startTelegramLogin() {
  await Linking.openURL(`${requireApiUrl()}/api/v1/auth/mobile/telegram/authorize`);
}

export async function exchangeTelegramCode(url: string) {
  const parsed = Linking.parse(url);
  const route = [parsed.hostname, parsed.path].filter(Boolean).join('/').replace(/^\/+|\/+$/g, '');
  if (route !== 'auth/telegram') return null;
  const code = parsed.queryParams?.code;
  if (typeof code !== 'string' || !code) return null;
  return saveSession(await requestSession('/api/v1/auth/mobile/telegram/exchange', { code }));
}
