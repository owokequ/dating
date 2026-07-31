import { apiRequest, jsonBody } from '../../../shared/api/http'
import type { AccountProfile } from '../../auth/api/authApi'

export type TelegramLink = { url: string; expiresAt: string }
export const updateProfile = (displayName: string) => apiRequest<AccountProfile>(
  '/api/v1/users/me', { method: 'PATCH', body: jsonBody({ displayName }) },
)
export const createTelegramLink = () => apiRequest<TelegramLink>(
  '/api/v1/users/me/telegram-link', { method: 'POST' },
)
