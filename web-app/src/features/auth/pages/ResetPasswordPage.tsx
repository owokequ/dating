import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { ErrorMessage, PageTitle } from '../../../shared/ui/Feedback'
import { confirmPasswordReset, requestPasswordReset } from '../api/authApi'

export function ResetPasswordPage() {
  const initialToken = new URLSearchParams(window.location.search).get('token') ?? ''
  const [email, setEmail] = useState('')
  const [token, setToken] = useState(initialToken)
  const [password, setPassword] = useState('')
  const [requested, setRequested] = useState(false)
  const request = useMutation({ mutationFn: requestPasswordReset, onSuccess: () => setRequested(true) })
  const confirm = useMutation({
    mutationFn: () => confirmPasswordReset(token, password),
    onSuccess: () => window.location.assign('/login'),
  })

  return (
    <section className="auth-card panel">
      <PageTitle eyebrow="Восстановление" title="Новый пароль">Ответ не раскрывает, зарегистрирован ли email.</PageTitle>
      {!token ? (
        <form onSubmit={(event) => { event.preventDefault(); request.mutate(email) }}>
          <label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
          {requested && <p className="message success">Если аккаунт существует, письмо уже отправлено.</p>}
          <ErrorMessage error={request.error} />
          <button disabled={request.isPending}>Отправить ссылку</button>
          <label>Или вставьте токен<input value={token} onChange={(event) => setToken(event.target.value)} /></label>
        </form>
      ) : (
        <form onSubmit={(event) => { event.preventDefault(); confirm.mutate() }}>
          <label>Новый пароль<input type="password" minLength={12} maxLength={72} value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
          <ErrorMessage error={confirm.error} />
          <button disabled={confirm.isPending}>Сохранить пароль</button>
        </form>
      )}
    </section>
  )
}
