import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { formatDateTime } from '../../../shared/lib/date'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { useSession } from '../../auth/api/authApi'
import { decideDate, getDate } from '../api/datesApi'

export function DateDetailsPage() {
  const { dateId } = useParams({ strict: false }) as { dateId: string }
  const queryClient = useQueryClient()
  const session = useSession()
  const proposal = useQuery({ queryKey: ['date', dateId], queryFn: () => getDate(dateId) })
  const decide = useMutation({
    mutationFn: (action: 'accept' | 'decline' | 'cancel') => decideDate(dateId, action),
    onSuccess: (value) => {
      queryClient.setQueryData(['date', dateId], value)
      queryClient.invalidateQueries({ queryKey: ['dates'] })
    },
  })
  if (proposal.isLoading || session.isLoading) return <Loading />
  if (!proposal.data) return <ErrorMessage error={proposal.error} />
  const isResponder = session.data?.userId === proposal.data.responderId
  const isProposer = session.data?.userId === proposal.data.proposerId
  const canRespond = proposal.data.status === 'PENDING_CONFIRMATION' && isResponder
  const canCancel = proposal.data.status === 'ACCEPTED'
    || (proposal.data.status === 'PENDING_CONFIRMATION' && isProposer)

  return (
    <section>
      <PageTitle eyebrow="Свидание" title={proposal.data.eventTitle || proposal.data.placeName}>{formatDateTime(proposal.data.scheduledAt)}</PageTitle>
      <article className="panel date-detail">
        <img className="date-detail-cover"
          src={proposal.data.placeCoverMediaId
            ? `/api/v1/media/assets/${proposal.data.placeCoverMediaId}/content?variant=DETAIL`
            : '/place-placeholder.svg'}
          alt={`Обложка места ${proposal.data.placeName}`} />
        <span className={`status ${proposal.data.status.toLowerCase()}`}>{proposal.data.status.replaceAll('_', ' ')}</span>
        <address>{proposal.data.placeAddress}</address>
        {proposal.data.eventPrice && <p><strong>{proposal.data.eventPrice}</strong></p>}
        {proposal.data.eventSourceUrl && <a className="source-link" href={proposal.data.eventSourceUrl} target="_blank" rel="noopener noreferrer">Источник: KudaGo ↗</a>}
        {proposal.data.description && <blockquote>{proposal.data.description}</blockquote>}
        <dl>
          <div><dt>Часовой пояс</dt><dd>{proposal.data.timezone}</dd></div>
          <div><dt>Создано</dt><dd>{formatDateTime(proposal.data.createdAt)}</dd></div>
        </dl>
        <div className="button-row">
          {canRespond && <><button onClick={() => decide.mutate('accept')}>Принять</button><button className="secondary" onClick={() => decide.mutate('decline')}>Отклонить</button></>}
          {canCancel && <button className="danger" onClick={() => decide.mutate('cancel')}>Отменить</button>}
        </div>
        <ErrorMessage error={decide.error} />
      </article>
    </section>
  )
}
