import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Place } from '../../../places/api/placesApi'
import { getAdminPlaces, savePlaceModeration, syncKudaGo, syncTwoGis } from './adminPlacesApi'

afterEach(() => vi.unstubAllGlobals())

describe('adminPlacesApi', () => {
  it('filters the admin catalog by status', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      items: [], page: 0, size: 100, totalElements: 0, totalPages: 0,
    }))
    vi.stubGlobal('fetch', fetchMock)

    await getAdminPlaces('DRAFT')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/admin/places?status=DRAFT&page=0&size=100',
      expect.objectContaining({ method: 'GET', credentials: 'include' }),
    )
  })

  it('returns synchronization counters and partial failures', async () => {
    vi.stubGlobal('document', { cookie: 'XSRF-TOKEN=csrf-token' })
    const result = {
      received: 20,
      created: 18,
      updated: 1,
      unchanged: 1,
      duplicates: 0,
      failures: [{ category: 'RESTAURANT', page: 1, reason: '2GIS returned HTTP 429' }],
    }
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(result))
    vi.stubGlobal('fetch', fetchMock)

    await expect(syncTwoGis()).resolves.toEqual(result)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/admin/places/sync/2gis',
      expect.objectContaining({ method: 'POST', credentials: 'include' }),
    )
  })

  it('uses the dedicated KudaGo synchronization endpoint', async () => {
    vi.stubGlobal('document', { cookie: 'XSRF-TOKEN=csrf-token' })
    const result = { received: 30, created: 30, updated: 0, unchanged: 0, duplicates: 0, failures: [] }
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(result))
    vi.stubGlobal('fetch', fetchMock)

    await expect(syncKudaGo()).resolves.toEqual(result)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/admin/places/sync/kudago',
      expect.objectContaining({ method: 'POST', credentials: 'include' }),
    )
  })

  it('publishes a draft without changing provider-owned fields', async () => {
    vi.stubGlobal('document', { cookie: 'XSRF-TOKEN=csrf-token' })
    const place: Place = {
      id: 'place-id',
      cityCode: 'KZN',
      name: 'Provider name',
      description: null,
      category: 'CAFE',
      address: 'Provider address',
      latitude: 55.79,
      longitude: 49.10,
      priceLevel: null,
      source: 'TWO_GIS',
      sourcePageUrl: null,
      attributionName: '2GIS',
      providerDescription: null,
      descriptionOverridden: false,
      status: 'DRAFT',
      coverMediaId: null,
      images: [],
    }
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ...place, status: 'ACTIVE' }))
    vi.stubGlobal('fetch', fetchMock)

    await savePlaceModeration(place, 'Own description', 3, 'ACTIVE')

    const [, request] = fetchMock.mock.calls[0]
    expect(JSON.parse(request.body)).toEqual(expect.objectContaining({
      name: 'Provider name',
      address: 'Provider address',
      description: 'Own description',
      priceLevel: 3,
      status: 'ACTIVE',
    }))
  })
})

function jsonResponse(value: unknown) {
  return new Response(JSON.stringify(value), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}
