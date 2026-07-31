import { apiRequest, jsonBody } from '../../../../shared/api/http'
import type { Place } from '../../../places/api/placesApi'

export type CreatePlaceInput = {
  name: string
  description: string | null
  category: 'CAFE' | 'RESTAURANT' | 'ENTERTAINMENT'
  address: string
  latitude: number
  longitude: number
  priceLevel: number
}

type UpdatePlaceInput = {
  name: string
  description: string | null
  category: string
  address: string
  latitude: number
  longitude: number
  priceLevel: number | null
  status: 'ACTIVE' | 'ARCHIVED'
}

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
