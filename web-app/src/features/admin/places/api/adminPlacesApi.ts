import { apiRequest, jsonBody } from '../../../../shared/api/http'
import type { Place, PlacePage } from '../../../places/api/placesApi'

export type PlaceStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'

export type SyncFailure = {
  category: string
  page: number
  reason: string
}

export type TwoGisSyncResult = {
  received: number
  created: number
  updated: number
  unchanged: number
  duplicates: number
  failures: SyncFailure[]
}

export type CreatePlaceInput = {
  name: string
  description: string | null
  category: 'CAFE' | 'RESTAURANT' | 'ENTERTAINMENT'
  address: string
  latitude: number
  longitude: number
  priceLevel: number
}

export type UpdatePlaceInput = {
  name: string
  description: string | null
  category: string
  address: string
  latitude: number
  longitude: number
  priceLevel: number | null
  status: PlaceStatus
}

export function getAdminPlaces(status: PlaceStatus, page = 0) {
  const params = new URLSearchParams({ status, page: String(page), size: '100' })
  return apiRequest<PlacePage>(`/api/v1/admin/places?${params}`)
}

export const syncTwoGis = () => apiRequest<TwoGisSyncResult>(
  '/api/v1/admin/places/sync',
  { method: 'POST' },
)

export const createPlace = (input: CreatePlaceInput) => apiRequest<Place>(
  '/api/v1/admin/places',
  { method: 'POST', body: jsonBody(input) },
)

export const updatePlace = (placeId: string, input: UpdatePlaceInput) => apiRequest<Place>(
  `/api/v1/admin/places/${placeId}`,
  { method: 'PUT', body: jsonBody(input) },
)

export const archivePlace = (place: Place) => updatePlace(place.id, {
  name: place.name,
  description: place.description,
  category: place.category,
  address: place.address,
  latitude: place.latitude,
  longitude: place.longitude,
  priceLevel: place.priceLevel,
  status: 'ARCHIVED',
})

export const savePlaceModeration = (
  place: Place,
  description: string | null,
  priceLevel: number | null,
  status: PlaceStatus,
) => updatePlace(place.id, {
  name: place.name,
  description,
  category: place.category,
  address: place.address,
  latitude: place.latitude,
  longitude: place.longitude,
  priceLevel,
  status,
})
