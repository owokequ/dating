import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { ErrorMessage, PageTitle } from '../../../shared/ui/Feedback'
import { safeContinuePath, verifyEmail } from '../api/authApi'

export function VerifyEmailPage() {
  const params = new URLSearchParams(window.location.search)
  const [token, setToken] = useState(params.get('token') ?? '')
  const continuePath = safeContinuePath(params.get('continue'))
  const mutation = useMutation({
    mutationFn: verifyEmail,
    onSuccess: () => window.location.assign(`/login?continue=${encodeURIComponent(continuePath)}`),
  })

  return (
    <section className="auth-card panel">
      <PageTitle eyebrow="Проверьте почту" title="Подтвердите email">
        Мы отправили одноразовую ссылку. Она действует 24 часа.
      </PageTitle>
      <form onSubmit={(event) => { event.preventDefault(); mutation.mutate(token) }}>
        <label>Токен подтверждения<input value={token} onChange={(event) => setToken(event.target.value)} required /></label>
        <ErrorMessage error={mutation.error} />
        <button disabled={mutation.isPending || !token}>Подтвердить</button>
      </form>
    </section>
  )
}
