import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { z } from 'zod'
import { ErrorMessage, PageTitle } from '../../../shared/ui/Feedback'
import { register, safeContinuePath } from '../api/authApi'
import { registerSchema } from '../schemas'

type FormValues = z.infer<typeof registerSchema>

export function RegisterPage() {
  const continuePath = safeContinuePath(new URLSearchParams(window.location.search).get('continue'))
  const form = useForm<FormValues>({ resolver: zodResolver(registerSchema) })
  const mutation = useMutation({
    mutationFn: register,
    onSuccess: () => window.location.assign(`/verify-email?continue=${encodeURIComponent(continuePath)}`),
  })

  return (
    <section className="auth-card panel">
      <PageTitle eyebrow="Новая история" title="Начать вместе">Создайте своё пространство. Ссылка-приглашение сохранится после регистрации.</PageTitle>
      <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
        <label>Имя<input autoComplete="name" {...form.register('displayName')} /></label>
        <small>{form.formState.errors.displayName?.message}</small>
        <label>Email<input type="email" autoComplete="email" {...form.register('email')} /></label>
        <small>{form.formState.errors.email?.message}</small>
        <label>Пароль<input type="password" autoComplete="new-password" {...form.register('password')} /></label>
        <small>{form.formState.errors.password?.message}</small>
        <ErrorMessage error={mutation.error} />
        <button disabled={mutation.isPending}>{mutation.isPending ? 'Создаём…' : 'Зарегистрироваться'}</button>
      </form>
      <p className="muted">Уже есть аккаунт? <a href={`/login?continue=${encodeURIComponent(continuePath)}`}>Войти</a></p>
    </section>
  )
}
