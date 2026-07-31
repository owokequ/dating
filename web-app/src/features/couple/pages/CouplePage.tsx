import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { formatDateTime } from '../../../shared/lib/date'
import { closeCouple, createInvitation, getCurrentCouple, type Invitation } from '../api/coupleApi'

export function CouplePage() {
  const queryClient = useQueryClient()
  const [invitation, setInvitation] = useState<Invitation | null>(null)
  const couple = useQuery({ queryKey: ['couple'], queryFn: getCurrentCouple })
  const invite = useMutation({ mutationFn: createInvitation, onSuccess: setInvitation })
  const close = useMutation({
    mutationFn: closeCouple,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['couple'] }),
  })
  if (couple.isLoading) return <Loading />

  return (
    <section>
      <PageTitle eyebrow="Вы вдвоём" title="Ваша пара">Одна активная пара, два участника и общая история свиданий.</PageTitle>
      {!couple.data ? (
        <div className="panel empty-state">
          <h2>Добавьте партнёра</h2>
          <p>Создадим одноразовую ссылку на 7 дней. Отправьте её только своему партнёру.</p>
          <button onClick={() => invite.mutate()} disabled={invite.isPending}>Создать приглашение</button>
          <ErrorMessage error={invite.error} />
          {invitation && (
            <div className="invite-box">
              <code>{invitation.inviteUrl}</code>
              <button className="secondary" onClick={() => navigator.clipboard.writeText(invitation.inviteUrl)}>Копировать</button>
              <small>До {formatDateTime(invitation.expiresAt)}</small>
            </div>
          )}
        </div>
      ) : (
        <div className="panel">
          <div className="status-line"><span className={`status ${couple.data.status.toLowerCase()}`}>{couple.data.status}</span></div>
          <div className="member-grid">
            {couple.data.members.map((member) => (
              <article key={member.userId} className="member-card">
                <span className="eyebrow">{member.role === 'OWNER' ? 'Создатель' : 'Партнёр'}</span>
                <strong>{member.userId}</strong>
                <small>В паре с {formatDateTime(member.joinedAt)}</small>
              </article>
            ))}
          </div>
          {couple.data.status === 'PENDING' && (
            <button onClick={() => invite.mutate()} disabled={invite.isPending}>Перевыпустить приглашение</button>
          )}
          {invitation && <div className="invite-box"><code>{invitation.inviteUrl}</code></div>}
          <button className="danger" onClick={() => {
            if (window.confirm('Закрыть пару? История сохранится, но действие нельзя отменить.')) close.mutate()
          }}>Завершить пару</button>
          <ErrorMessage error={close.error ?? invite.error} />
        </div>
      )}
    </section>
  )
}
