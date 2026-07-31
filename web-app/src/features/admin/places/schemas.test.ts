import { describe, expect, it } from 'vitest'
import { adminPlaceSchema } from './schemas'

const validPlace = {
  name: 'Кофейня у Кремля',
  description: 'Тихое место',
  category: 'CAFE' as const,
  address: 'Казань, улица Баумана, 1',
  latitude: 55.7973,
  longitude: 49.1063,
  priceLevel: 2,
}

describe('adminPlaceSchema', () => {
  it('accepts a valid Kazan place', () => {
    expect(adminPlaceSchema.safeParse(validPlace).success).toBe(true)
  })

  it('rejects invalid coordinates and price level', () => {
    expect(adminPlaceSchema.safeParse({ ...validPlace, latitude: 100, priceLevel: 5 }).success).toBe(false)
  })
})
