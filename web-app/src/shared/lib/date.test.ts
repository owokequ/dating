import { describe, expect, it } from 'vitest'
import { toMoscowIso, toMoscowLocalInput } from './date'

describe('date conversion', () => {
  it('converts a local datetime to an ISO instant', () => {
    expect(toMoscowIso('2030-01-01T12:30')).toBe('2030-01-01T09:30:00.000Z')
  })

  it('converts a UTC timestamp to a Moscow datetime-local value', () => {
    expect(toMoscowLocalInput('2026-08-15T13:30:00Z')).toBe('2026-08-15T16:30')
  })
})
