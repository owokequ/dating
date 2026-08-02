import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { formatDateTime } from '../../../shared/lib/date'
import { useSession } from '../../auth/api/authApi'
import { createTelegramLink, updateProfile } from '../api/settingsApi'

export function SettingsPage() {
  const session = useSession()
  const queryClient = useQueryClient()
  const profile = useMutation({
    mutationFn: updateProfile,
    onSuccess: (value) => queryClient.setQueryData(['session'], value),
  })
  const telegram = useMutation({
    mutationFn: createTelegramLink,
    onSuccess: (link) => window.open(link.url, '_blank', 'noopener,noreferrer'),
  })
  if (session.isLoading) return <Loading />

  return (
    <section>
      <PageTitle eyebrow="Ваш уголок" title="Настройки">Имя, уведомления и связь с Telegram — всё, что помогает оставаться на связи.</PageTitle>
      <div className="settings-grid">
        <form className="panel" onSubmit={(event) => {
          event.preventDefault()
          profile.mutate(String(new FormData(event.currentTarget).get('displayName') ?? ''))
        }}>
          <h2>Личные данные</h2>
          <label>Имя<input name="displayName" defaultValue={session.data?.displayName} maxLength={100} required /></label>
          <label>Email<input value={session.data?.email ?? ''} disabled /></label>
          {profile.isSuccess && <p className="message success">Профиль обновлён.</p>}
          <ErrorMessage error={profile.error} />
          <button disabled={profile.isPending}>Сохранить</button>
        </form>
        <div className="panel">
          <h2>Уведомления в Telegram</h2>
          <p>{session.data?.telegramLinked ? 'Telegram уже привязан. Бот сможет присылать предложения и напоминания.' : 'Откроем бота с одноразовым токеном на 10 минут.'}</p>
          <button className="telegram" disabled={telegram.isPending || session.data?.telegramLinked} onClick={() => telegram.mutate()}>
            {session.data?.telegramLinked ? 'Telegram подключён' : 'Подключить Telegram'}
          </button>
          {telegram.data && <small>Ссылка действует до {formatDateTime(telegram.data.expiresAt)}</small>}
          <ErrorMessage error={telegram.error} />
        </div>
      </div>
    </section>
  )
}
