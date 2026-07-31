import { apiRequest } from '../../../shared/api/http'

export type Notification = {
  id: string
  type: string
  title: string
  body: string
  actionUrl: string | null
  readAt: string | null
  createdAt: string
}

export const listNotifications = () => apiRequest<Notification[]>('/api/v1/notifications?limit=50')
export const markNotificationRead = (id: string) =>
  apiRequest<void>(`/api/v1/notifications/${id}/read`, { method: 'POST' })
