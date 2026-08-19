import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().trim().email('Введите корректный email').max(320),
  password: z.string().min(1, 'Введите пароль').max(72, 'Пароль слишком длинный'),
});

export const registerSchema = loginSchema.extend({
  displayName: z.string().trim().min(1, 'Как вас называть?').max(100, 'Имя слишком длинное'),
  password: z.string().min(12, 'Минимум 12 символов').max(72, 'Максимум 72 символа'),
});

export const emailSchema = z.string().trim().email('Введите корректный email').max(320);
export const tokenSchema = z.string().trim().min(1, 'Вставьте код из письма').max(128, 'Код слишком длинный');
export const passwordSchema = z.string().min(12, 'Минимум 12 символов').max(72, 'Максимум 72 символа');

export function validationMessage(result: z.SafeParseReturnType<unknown, unknown>) {
  return result.success ? null : result.error.issues[0]?.message ?? 'Проверьте введённые данные';
}
