import { apiRequest } from '../../../shared/api/http'

export type Place = {
  id: string
  cityCode: 'KZN'
  name: string
  description: string | null
  category: string
  address: string
  latitude: number
  longitude: number
  priceLevel: number | null
  source: 'MANUAL' | 'TWO_GIS'
  status: 'ACTIVE' | 'ARCHIVED'
}

export type PlacePage = { items: Place[]; page: number; size: number; totalElements: number; totalPages: number }

export function getPlaces(filters: { category?: string; query?: string; page?: number } = {}) {
  const params = new URLSearchParams({ page: String(filters.page ?? 0), size: '24' })
  if (filters.category) params.set('category', filters.category)
  if (filters.query) params.set('query', filters.query)
  return apiRequest<PlacePage>(`/api/v1/places?${params}`)
}
