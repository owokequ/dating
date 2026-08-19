import { authenticatedFetch } from '@/features/auth/mobile-session';

export type Profile = { userId: string; email: string | null; displayName: string; status: string; onboardingCompleted: boolean; telegramLinked: boolean };
export type PlaceImage = { thumbnailUrl: string; cardUrl?: string; detailUrl?: string };
export type Place = { id: string; name: string; address: string; description: string | null; category: string; priceLevel?: number | null; sourcePageUrl?: string | null; images: PlaceImage[] };
export type EventOccurrence = { id: string; startsAt: string; endsAt?: string | null; continuous?: boolean; status?: string };
export type Event = { id: string; title: string; description: string | null; categories?: string[]; priceText?: string | null; free?: boolean; ageRestriction?: string | null; sourcePageUrl?: string | null; venueName: string | null; venueAddress: string | null; occurrences: EventOccurrence[]; images: { thumbnailUrl: string | null; remoteUrl: string }[] };
export type DateProposal = { id: string; proposerId?: string; responderId?: string; placeId?: string | null; placeName: string; placeAddress: string; placeCoverMediaId?: string | null; eventId?: string | null; eventOccurrenceId?: string | null; eventTitle?: string | null; eventPrice?: string | null; scheduledAt: string; status: string; description: string | null; createdAt?: string; referenceId?: string | null };
export type AppNotification = { id: string; type: string; title: string; body: string; readAt: string | null; createdAt: string };
export type Couple = { id: string; status: string; members: { userId: string; displayName?: string; role: string }[] };
export type InvitationPreview = { invitationId: string; expiresAt: string };

async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await authenticatedFetch(path, init);
  if (!response.ok) throw new Error(`Запрос не выполнен (${response.status})`);
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
const json = (body: unknown) => ({ headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
// Expo Go does not expose the Web Crypto global on every supported runtime.
// The server only needs a unique, opaque value to make a repeated submit safe.
export const createIdempotencyKey = (): string =>
  `mobile-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 14)}`;
const key = (value: string = createIdempotencyKey()) => ({ 'Idempotency-Key': value });

export const getProfile = () => api<Profile>('/api/v1/users/me');
export const updateProfile = (displayName: string) => api<Profile>('/api/v1/users/me', { method: 'PATCH', ...json({ displayName }) });
export const getPlaces = (filters: { query?: string; category?: string } = {}) => {
  const params = new URLSearchParams({ page: '0', size: '50' });
  if (filters.query) params.set('query', filters.query);
  if (filters.category) params.set('category', filters.category);
  return api<{ items: Place[] }>(`/api/v1/places?${params.toString()}`);
};
export const getPlace = (id: string) => api<Place>(`/api/v1/places/${id}`);
export const getEvents = (filters: { category?: string; free?: boolean } = {}) => {
  const params = new URLSearchParams({ page: '0', size: '50', from: new Date().toISOString() });
  if (filters.category) params.set('category', filters.category);
  if (filters.free !== undefined) params.set('free', String(filters.free));
  return api<{ items: Event[] }>(`/api/v1/events?${params.toString()}`);
};
export const getEvent = (id: string) => api<Event>(`/api/v1/events/${id}`);
export const listDates = () => api<DateProposal[]>('/api/v1/date-proposals');
export const getDate = (id: string) => api<DateProposal>(`/api/v1/date-proposals/${id}`);
export const decideDate = (id: string, action: 'accept' | 'decline' | 'cancel', idempotencyKey?: string) => api<DateProposal>(`/api/v1/date-proposals/${id}/${action}`, { method: 'POST', headers: key(idempotencyKey) });
export const createDate = (input: { scheduledAt: string; placeId: string; description?: string }, idempotencyKey?: string) => api<DateProposal>('/api/v1/date-proposals', { method: 'POST', headers: { ...key(idempotencyKey), 'Content-Type': 'application/json' }, body: JSON.stringify(input) });
export const createDateFromEvent = (input: { eventOccurrenceId: string; visitAt?: string; description?: string }, idempotencyKey?: string) => api<DateProposal>('/api/v1/date-proposals/from-event', { method: 'POST', headers: { ...key(idempotencyKey), 'Content-Type': 'application/json' }, body: JSON.stringify(input) });
export const createPrivateDate = (input: { scheduledAt: string; placeName: string; placeAddress?: string; description?: string }, idempotencyKey?: string) => api<DateProposal>('/api/v1/date-proposals/private-place/drafts', { method: 'POST', headers: { ...key(idempotencyKey), 'Content-Type': 'application/json' }, body: JSON.stringify(input) });
export type LocalImageUpload = { uri: string; fileName?: string | null; mimeType?: string | null };
export const uploadPrivateDatePhoto = async (proposalId: string, image: LocalImageUpload) => {
  const form = new FormData();
  form.append('file', {
    uri: image.uri,
    name: image.fileName || `place-${Date.now()}.jpg`,
    type: image.mimeType || 'image/jpeg',
  } as unknown as Blob);
  return api<{ mediaId: string; status: string }>(`/api/v1/media/date-proposals/${proposalId}/assets`, {
    method: 'POST',
    body: form,
  });
};
export const sendPrivateDate = (id: string, idempotencyKey?: string) => api<DateProposal>(`/api/v1/date-proposals/${id}/send`, { method: 'POST', headers: key(idempotencyKey) });
export const mediaContentUrl = (mediaId: string, variant: 'THUMBNAIL' | 'CARD' | 'DETAIL' = 'CARD') => {
  const baseUrl = process.env.EXPO_PUBLIC_API_URL?.replace(/\/$/, '');
  return baseUrl ? `${baseUrl}/api/v1/media/assets/${mediaId}/content?variant=${variant}` : '';
};
export const listNotifications = () => api<AppNotification[]>('/api/v1/notifications?limit=50');
export const markNotificationRead = (id: string) => api<void>(`/api/v1/notifications/${id}/read`, { method: 'POST' });
export const getCouple = async () => { const response = await authenticatedFetch('/api/v1/couples/current'); return response.status === 404 ? null : (response.ok ? response.json() as Promise<Couple> : Promise.reject(new Error('Не удалось загрузить пару'))); };
export const createInvitation = () => api<{ inviteUrl: string; expiresAt: string }>('/api/v1/couple-invitations', { method: 'POST' });
export const getInvitation = (token: string) => api<InvitationPreview>(`/api/v1/couple-invitations/${encodeURIComponent(token)}`);
export const acceptInvitation = (token: string) => api<Couple>(`/api/v1/couple-invitations/${encodeURIComponent(token)}/accept`, { method: 'POST' });
export type NotificationPreferences = { inAppEnabled: boolean; pushEnabled: boolean; telegramEnabled: boolean; emailEnabled: boolean };
export const getNotificationPreferences = () => api<NotificationPreferences>('/api/v1/notification-preferences');
export const updateNotificationPreferences = (value: NotificationPreferences) => api<NotificationPreferences>('/api/v1/notification-preferences', { method: 'PATCH', ...json(value) });
