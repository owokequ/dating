import { apiRequest } from '../../../shared/api/http'

export type EventStatus = 'DRAFT' | 'ACTIVE' | 'HIDDEN' | 'ARCHIVED'
export type EventOccurrenceStatus = 'ACTIVE' | 'EXPIRED'

export type EventImage = {
  providerAssetKey: string
  remoteUrl: string
  thumbnailUrl: string | null
  sourceName: string
  sourceLink: string
}

export type EventOccurrence = {
  id: string
  startsAt: string
  endsAt: string | null
  continuous: boolean
  status: EventOccurrenceStatus
}

export type Event = {
  id: string
  title: string
  description: string | null
  providerDescription: string | null
  descriptionOverridden: boolean
  categories: string[]
  priceText: string | null
  free: boolean
  ageRestriction: string | null
  sourcePageUrl: string
  venueName: string | null
  venueAddress: string | null
  latitude: number | null
  longitude: number | null
  localPlaceId: string | null
  status: EventStatus
  occurrences: EventOccurrence[]
  images: EventImage[]
  createdAt: string
  updatedAt: string
  version: number
}

export type EventPage = {
  items: Event[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type EventFilters = {
  category?: string
  free?: boolean
  from?: string
  to?: string
  page?: number
  size?: number
}

export function getEvents(filters: EventFilters = {}) {
  const params = new URLSearchParams({
    page: String(filters.page ?? 0),
    size: String(filters.size ?? 24),
  })
  if (filters.category) params.set('category', filters.category)
  if (filters.free !== undefined) params.set('free', String(filters.free))
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  return apiRequest<EventPage>(`/api/v1/events?${params}`)
}

export const getEvent = (eventId: string) => apiRequest<Event>(`/api/v1/events/${eventId}`)

export function eventCover(event: Event): string {
  return event.images[0]?.thumbnailUrl
    ?? event.images[0]?.remoteUrl
    ?? '/place-placeholder.svg'
}
