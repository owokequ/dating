import { useQuery } from '@tanstack/react-query'
import { formatDateTime } from '../../../shared/lib/date'
import { Loading, PageTitle } from '../../../shared/ui/Feedback'
import { useSession } from '../../auth/api/authApi'
import { getCurrentCouple } from '../../couple/api/coupleApi'
import { listDates } from '../../dates/api/datesApi'

export function DashboardPage() {
  const session = useSession()
  const couple = useQuery({ queryKey: ['couple'], queryFn: getCurrentCouple, enabled: Boolean(session.data) })
  const dates = useQuery({ queryKey: ['dates'], queryFn: listDates, enabled: Boolean(session.data && couple.data?.status === 'ACTIVE') })
  if (session.isLoading) return <Loading />
  if (!session.data) {
    return (
      <section className="hero">
        <div><span className="eyebrow">Время для двоих</span><h1>Не откладывайте<br /><em>ваше свидание.</em></h1><p>Выберите место в Казани, предложите дату и получите ответ партнёра — без бесконечной переписки.</p><a className="button" href="/register">Начать вместе</a></div>
        <div className="hero-card"><span>Следующее свидание</span><strong>Суббота, 19:30</strong><p>Ужин и прогулка у Кремля</p><div className="pulse">Подтверждено</div></div>
      </section>
    )
  }

  const upcoming = dates.data?.filter((date) => ['PENDING_CONFIRMATION', 'ACCEPTED'].includes(date.status)) ?? []
  return (
    <section>
      <PageTitle eyebrow="Ваше пространство" title={`Привет, ${session.data.displayName}`}>Сегодня хороший день, чтобы придумать следующую встречу.</PageTitle>
      {!couple.data ? (
        <div className="panel callout"><div><h2>Пока вы здесь один</h2><p>Пригласите партнёра по защищённой одноразовой ссылке.</p></div><a className="button" href="/couple">Добавить партнёра</a></div>
      ) : couple.data.status === 'PENDING' ? (
        <div className="panel callout"><div><h2>Ждём партнёра</h2><p>Перешлите приглашение, чтобы активировать пару.</p></div><a className="button" href="/couple">Открыть приглашение</a></div>
      ) : (
        <>
          <div className="quick-actions"><a href="/dates/new">＋ Предложить свидание</a><a href="/places">⌖ Выбрать место</a><a href="/couple">♡ Ваша пара</a></div>
          <div className="section-heading"><h2>Ближайшие планы</h2><a href="/places">Смотреть места</a></div>
          {upcoming.length ? <div className="date-list">{upcoming.map((date) => (
            <a className="date-card" href={`/dates/${date.id}`} key={date.id}><span className={`status ${date.status.toLowerCase()}`}>{date.status === 'ACCEPTED' ? 'Подтверждено' : 'Ждёт ответа'}</span><h3>{date.placeName}</h3><strong>{formatDateTime(date.scheduledAt)}</strong><small>{date.placeAddress}</small></a>
          ))}</div> : <div className="panel empty-state"><h2>Планов пока нет</h2><p>Каталог уже ждёт — выберите место для первой встречи.</p><a className="button" href="/places">Найти место</a></div>}
        </>
      )}
    </section>
  )
}
