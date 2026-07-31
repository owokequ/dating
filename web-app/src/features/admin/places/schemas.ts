import { z } from 'zod'

export const placeCategories = ['CAFE', 'RESTAURANT', 'ENTERTAINMENT'] as const

export const adminPlaceSchema = z.object({
  name: z.string().trim().min(1, 'Укажите название').max(200),
  description: z.string().trim().max(2000, 'Максимум 2000 символов'),
  category: z.enum(placeCategories),
  address: z.string().trim().min(1, 'Укажите адрес').max(500),
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
  priceLevel: z.number().int().min(1).max(4),
})
