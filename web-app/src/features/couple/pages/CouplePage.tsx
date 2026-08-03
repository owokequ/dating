import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Copy, HeartHandshake, Link2 } from 'lucide-react'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { formatDateTime } from '../../../shared/lib/date'
import { closeCouple, createInvitation, getCurrentCouple, type Invitation } from '../api/coupleApi'
import { useSession } from '../../auth/api/authApi'

export function CouplePage() {
  const queryClient = useQueryClient()
  const session = useSession()
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
      <PageTitle eyebrow="Только ваше" title="Наше пространство">Здесь начинается общая история планов, мест и встреч.</PageTitle>
      {!couple.data ? (
        <div className="panel empty-state">
          <HeartHandshake className="empty-state-icon" size={38} strokeWidth={1.5} />
          <h2>Пригласите своего человека</h2>
          <p>Создадим личную одноразовую ссылку на 7 дней. Отправьте её только тому, с кем хотите строить планы.</p>
          <button onClick={() => invite.mutate()} disabled={invite.isPending}><Link2 size={17} />Создать приглашение</button>
          <ErrorMessage error={invite.error} />
          {invitation && (
            <div className="invite-box">
              <code>{invitation.inviteUrl}</code>
              <button className="secondary" onClick={() => navigator.clipboard.writeText(invitation.inviteUrl)}><Copy size={16} />Копировать ссылку</button>
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
                <span className="eyebrow">{member.role === 'OWNER' ? 'Начало истории' : 'Вместе с вами'}</span>
                <strong>{member.userId === session.data?.userId
                  ? session.data?.displayName
                  : member.displayName || 'Ваш человек'}</strong>
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
