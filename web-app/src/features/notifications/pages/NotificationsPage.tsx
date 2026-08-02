import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { formatDateTime } from '../../../shared/lib/date'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { listNotifications, markNotificationRead } from '../api/notificationsApi'

export function NotificationsPage() {
  const queryClient = useQueryClient()
  const notifications = useQuery({ queryKey: ['notifications'], queryFn: listNotifications })
  const read = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })
  if (notifications.isLoading) return <Loading />

  return (
    <section>
      <PageTitle eyebrow="Ничего не потеряется" title="Ваши уведомления">Здесь собраны приглашения, ответы и напоминания о ваших планах.</PageTitle>
      <div className="notification-list">
        {notifications.data?.map((item) => (
          <article key={item.id} className={`panel notification ${item.readAt ? '' : 'unread'}`}>
            <div><span className="eyebrow">{item.type}</span><h2>{item.title}</h2><p>{item.body}</p><small>{formatDateTime(item.createdAt)}</small></div>
            <div className="button-row">
              {item.actionUrl && <a className="button small" href={item.actionUrl}>Открыть</a>}
              {!item.readAt && <button className="secondary small" onClick={() => read.mutate(item.id)}>Прочитано</button>}
            </div>
          </article>
        ))}
        {!notifications.data?.length && <div className="panel empty-state"><h2>Пока тихо</h2><p>Здесь появятся ответы вашего человека и напоминания о встречах.</p></div>}
      </div>
      <ErrorMessage error={notifications.error ?? read.error} />
    </section>
  )
}
