import { afterEach, describe, expect, it, vi } from 'vitest'
import { getEvent, getEvents } from './eventsApi'

afterEach(() => vi.unstubAllGlobals())

describe('eventsApi', () => {
  it('serializes public event filters', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      items: [], page: 1, size: 12, totalElements: 0, totalPages: 0,
    }))
    vi.stubGlobal('fetch', fetchMock)

    await getEvents({ category: 'cinema', free: true, page: 1, size: 12 })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/events?page=1&size=12&category=cinema&free=true',
      expect.objectContaining({ method: 'GET', credentials: 'include' }),
    )
  })

  it('loads one event by id', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 'event-id' }))
    vi.stubGlobal('fetch', fetchMock)
    await getEvent('event-id')
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/events/event-id', expect.objectContaining({ method: 'GET' }))
  })
})

function jsonResponse(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { 'Content-Type': 'application/json' } })
}
