import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { z } from 'zod'
import { Send } from 'lucide-react'
import { ErrorMessage, PageTitle } from '../../../shared/ui/Feedback'
import { login, safeContinuePath } from '../api/authApi'
import { loginSchema } from '../schemas'

type FormValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const queryClient = useQueryClient()
  const params = new URLSearchParams(window.location.search)
  const continuePath = safeContinuePath(params.get('continue'))
  const form = useForm<FormValues>({ resolver: zodResolver(loginSchema) })
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['session'] })
      window.location.assign(continuePath)
    },
  })

  return (
    <section className="auth-card panel">
      <PageTitle eyebrow="С возвращением" title="Войти в For my L">Ваши планы и маленькие истории уже ждут.</PageTitle>
      <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
        <label>Email<input type="email" autoComplete="email" {...form.register('email')} /></label>
        <small>{form.formState.errors.email?.message}</small>
        <label>Пароль<input type="password" autoComplete="current-password" {...form.register('password')} /></label>
        <small>{form.formState.errors.password?.message}</small>
        <ErrorMessage error={mutation.error} />
        <button disabled={mutation.isPending}>{mutation.isPending ? 'Входим…' : 'Войти'}</button>
      </form>
      <button className="button secondary telegram" onClick={() => {
        window.location.href = `/api/v1/auth/telegram/authorize?continue=${encodeURIComponent(continuePath)}`
      }}><Send size={17} />Войти через Telegram</button>
      <div className="auth-links">
        <a href={`/register?continue=${encodeURIComponent(continuePath)}`}>Создать аккаунт</a>
        <a href="/reset-password">Забыли пароль?</a>
      </div>
    </section>
  )
}
