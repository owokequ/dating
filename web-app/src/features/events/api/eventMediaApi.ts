import { apiRequest } from '../../../shared/api/http'

export type EventMediaItem = {
  mediaId: string
  source: 'UPLOAD' | 'REMOTE_URL'
  status: string
  position: number
  cover: boolean
  thumbnailUrl: string | null
  cardUrl: string | null
  detailUrl: string | null
  sourceName: string | null
  sourceLink: string | null
}

export type EventMediaCollection = {
  ownerType: 'EVENT'
  ownerId: string
  coverMediaId: string | null
  version: number
  images: EventMediaItem[]
}

export const getEventMedia = (eventId: string) => apiRequest<EventMediaCollection>(
  `/api/v1/media/event-collections/${eventId}`,
)
