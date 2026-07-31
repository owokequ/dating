import { describe, expect, it } from 'vitest'
import { toMoscowIso } from './date'

describe('date conversion', () => {
  it('converts a local datetime to an ISO instant', () => {
    expect(toMoscowIso('2030-01-01T12:30')).toBe('2030-01-01T09:30:00.000Z')
  })
})
