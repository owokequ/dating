export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly details?: unknown,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

let csrfRequest: Promise<void> | null = null
let refreshRequest: Promise<void> | null = null

export function readCookie(name: string, cookie = document.cookie): string | null {
  const prefix = `${encodeURIComponent(name)}=`
  const part = cookie.split(';').map((value) => value.trim()).find((value) => value.startsWith(prefix))
  return part ? decodeURIComponent(part.slice(prefix.length)) : null
}

async function ensureCsrf(): Promise<void> {
  if (readCookie('XSRF-TOKEN')) return
  csrfRequest ??= fetch('/api/v1/security/csrf', { credentials: 'include' })
    .then((response) => {
      if (!response.ok) throw new ApiError(response.status, 'Не удалось подготовить защищённый запрос')
    })
    .finally(() => { csrfRequest = null })
  return csrfRequest
}

async function refreshSession(): Promise<void> {
  refreshRequest ??= (async () => {
    await ensureCsrf()
    const response = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      credentials: 'include',
      headers: { 'X-XSRF-TOKEN': readCookie('XSRF-TOKEN') ?? '' },
    })
    if (!response.ok) throw new ApiError(response.status, 'Сессия истекла')
  })().finally(() => { refreshRequest = null })
  return refreshRequest
}

const unsafeMethods = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export async function apiRequest<T>(path: string, options: RequestInit = {}, allowRefresh = true): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  if (unsafeMethods.has(method)) await ensureCsrf()

  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (unsafeMethods.has(method)) headers.set('X-XSRF-TOKEN', readCookie('XSRF-TOKEN') ?? '')

  let response = await fetch(path, { ...options, method, headers, credentials: 'include' })
  const isAuthFlow = path.startsWith('/api/v1/auth/')
  if (response.status === 401 && allowRefresh && !isAuthFlow) {
    try {
      await refreshSession()
      response = await fetch(path, { ...options, method, headers, credentials: 'include' })
    } catch {
      throw new ApiError(401, 'Войдите в аккаунт, чтобы продолжить')
    }
  }
  if (!response.ok) throw await toApiError(response)
  if (response.status === 204 || response.headers.get('content-length') === '0') return undefined as T
  return response.json() as Promise<T>
}

async function toApiError(response: Response): Promise<ApiError> {
  const contentType = response.headers.get('content-type') ?? ''
  const details = contentType.includes('json') ? await response.json().catch(() => null) : null
  const message = details && typeof details === 'object' && 'detail' in details
    ? String(details.detail)
    : `Запрос завершился с кодом ${response.status}`
  return new ApiError(response.status, message, details)
}

export const jsonBody = (value: unknown) => JSON.stringify(value)
export const idempotencyKey = () => crypto.randomUUID()
