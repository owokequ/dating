import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().email('Введите корректный email').max(320),
  password: z.string().min(1, 'Введите пароль').max(72),
})

export const registerSchema = loginSchema.extend({
  displayName: z.string().trim().min(1, 'Как вас называть?').max(100),
  password: z.string().min(12, 'Минимум 12 символов').max(72),
})

export const resetSchema = z.object({
  email: z.string().email('Введите корректный email').max(320),
})

export const newPasswordSchema = z.object({
  password: z.string().min(12, 'Минимум 12 символов').max(72),
})
