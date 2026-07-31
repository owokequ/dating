import { useQuery } from '@tanstack/react-query'
import { ApiError, apiRequest, jsonBody } from '../../../shared/api/http'

export type AccountProfile = {
  userId: string
  email: string | null
  displayName: string
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'DISABLED'
  telegramLinked: boolean
}

export type LoginInput = { email: string; password: string }
export type RegisterInput = LoginInput & { displayName: string }

export const register = (input: RegisterInput) =>
  apiRequest<void>('/api/v1/auth/register', { method: 'POST', body: jsonBody(input) }, false)

export const login = (input: LoginInput) =>
  apiRequest<void>('/api/v1/auth/login', { method: 'POST', body: jsonBody(input) }, false)

export const logout = () => apiRequest<void>('/api/v1/auth/logout', { method: 'POST' }, false)

export const verifyEmail = (token: string) => apiRequest<void>(
  '/api/v1/auth/email-verifications/confirm',
  { method: 'POST', body: jsonBody({ token }) },
  false,
)

export const requestPasswordReset = (email: string) => apiRequest<void>(
  '/api/v1/auth/password-reset/request',
  { method: 'POST', body: jsonBody({ email }) },
  false,
)

export const confirmPasswordReset = (token: string, newPassword: string) => apiRequest<void>(
  '/api/v1/auth/password-reset/confirm',
  { method: 'POST', body: jsonBody({ token, newPassword }) },
  false,
)

export const getProfile = () => apiRequest<AccountProfile>('/api/v1/users/me')

export function useSession() {
  return useQuery({
    queryKey: ['session'],
    queryFn: async () => {
      try {
        return await getProfile()
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) return null
        throw error
      }
    },
    retry: false,
  })
}

export function safeContinuePath(value: string | null): string {
  return value?.startsWith('/') && !value.startsWith('//') ? value : '/dashboard'
}
