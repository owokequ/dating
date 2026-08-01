import { apiRequest, jsonBody } from '../../../../shared/api/http'
import type { Event, EventPage, EventStatus } from '../../../events/api/eventsApi'

export type EventSyncFailure = { page: number; detail: string }
export type EventSyncResult = {
  pages: number
  received: number
  upserted: number
  skipped: number
  complete: boolean
  errors: EventSyncFailure[]
}

export type UpdateEventVenueInput = {
  venueName: string
  venueAddress: string
  latitude: number
  longitude: number
}

export function getAdminEvents(status: EventStatus, page = 0) {
  const params = new URLSearchParams({ status, page: String(page), size: '100' })
  return apiRequest<EventPage>(`/api/v1/admin/events?${params}`)
}

export const syncEvents = () => apiRequest<EventSyncResult>('/api/v1/admin/events/sync', { method: 'POST' })

export const updateEventVenue = (eventId: string, input: UpdateEventVenueInput) => apiRequest<Event>(
  `/api/v1/admin/events/${eventId}/venue`,
  { method: 'PATCH', body: jsonBody(input) },
)

export const changeEventStatus = (eventId: string, action: 'publish' | 'hide' | 'archive') => apiRequest<Event>(
  `/api/v1/admin/events/${eventId}/${action}`,
  { method: 'POST' },
)
