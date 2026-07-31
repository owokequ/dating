import { PageTitle } from '../../../shared/ui/Feedback'

export function TelegramCallbackPage() {
  return (
    <section className="auth-card panel">
      <PageTitle eyebrow="Telegram" title="Завершаем вход">Backend проверяет state, nonce, PKCE и подпись ID token.</PageTitle>
      <a className="button" href="/dashboard">Продолжить</a>
    </section>
  )
}
