import { afterEach, describe, expect, it, vi } from 'vitest'
import { changeEventStatus, getAdminEvents, syncEvents, updateEventVenue } from './adminEventsApi'

afterEach(() => vi.unstubAllGlobals())

describe('adminEventsApi', () => {
  it('filters the moderation list by status', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ items: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }))
    vi.stubGlobal('fetch', fetchMock)
    await getAdminEvents('DRAFT')
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/events?status=DRAFT&page=0&size=100', expect.objectContaining({ method: 'GET' }))
  })

  it('starts sync and publishes an event', async () => {
    vi.stubGlobal('document', { cookie: 'XSRF-TOKEN=csrf-token' })
    const payload = { pages: 1, received: 2, upserted: 2, skipped: 0, complete: true, errors: [] }
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse(payload)))
    vi.stubGlobal('fetch', fetchMock)
    await syncEvents()
    await changeEventStatus('event-id', 'publish')
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/admin/events/sync')
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/admin/events/event-id/publish')
  })

  it('updates an incomplete event venue', async () => {
    vi.stubGlobal('document', { cookie: 'XSRF-TOKEN=csrf-token' })
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 'event-id' }))
    vi.stubGlobal('fetch', fetchMock)
    const input = { venueName: 'Театр', venueAddress: 'Казань, улица Пушкина, 1', latitude: 55.79, longitude: 49.12 }
    await updateEventVenue('event-id', input)
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual(input)
  })
})

function jsonResponse(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { 'Content-Type': 'application/json' } })
}
