import { afterEach, describe, expect, it, vi } from 'vitest'
import { createDateFromEvent } from './datesApi'

afterEach(() => vi.unstubAllGlobals())

describe('datesApi', () => {
  it('creates an event based proposal with idempotency and CSRF', async () => {
    vi.stubGlobal('document', { cookie: 'XSRF-TOKEN=csrf-token' })
    vi.stubGlobal('crypto', { randomUUID: () => 'idempotency-key' })
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 'proposal-id' }))
    vi.stubGlobal('fetch', fetchMock)

    await createDateFromEvent({ eventOccurrenceId: 'occurrence-id', description: 'Пойдём?' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/date-proposals/from-event',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        headers: expect.any(Headers),
        body: JSON.stringify({ eventOccurrenceId: 'occurrence-id', description: 'Пойдём?' }),
      }),
    )
    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Idempotency-Key')).toBe('idempotency-key')
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
  })
})

function jsonResponse(value: unknown) {
  return new Response(JSON.stringify(value), { status: 200, headers: { 'Content-Type': 'application/json' } })
}
