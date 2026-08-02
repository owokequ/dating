import { useQuery } from '@tanstack/react-query'
import { ArrowRight, CalendarPlus, Heart, MapPin, Sparkles } from 'lucide-react'
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
        <div><span className="eyebrow">Время для двоих</span><h1>Не откладывайте<br /><em>ваше свидание.</em></h1><p>Личное место, где идеи превращаются в планы: выберите уголок Казани, назначьте время и сохраните ещё одну вашу историю.</p><a className="button" href="/register">Начать вместе <ArrowRight size={17} /></a></div>
        <div className="hero-card"><span>Следующая страница</span><strong>Суббота, 19:30</strong><p>Ужин и прогулка у Кремля</p><div className="pulse">♡ вы договорились</div></div>
      </section>
    )
  }

  const upcoming = dates.data?.filter((date) => ['PENDING_CONFIRMATION', 'ACCEPTED'].includes(date.status)) ?? []
  return (
    <section>
      <div className="dashboard-intro"><PageTitle eyebrow="Наш дневник" title={`С возвращением, ${session.data.displayName}`}>Сегодня хороший день, чтобы придумать следующую встречу.</PageTitle><span className="love-seal" aria-hidden="true">L</span></div>
      {!couple.data ? (
        <div className="panel callout"><div><h2>Пока вы здесь один</h2><p>Пригласите партнёра по защищённой одноразовой ссылке.</p></div><a className="button" href="/couple">Добавить партнёра</a></div>
      ) : couple.data.status === 'PENDING' ? (
        <div className="panel callout"><div><h2>Ждём партнёра</h2><p>Перешлите приглашение, чтобы активировать пару.</p></div><a className="button" href="/couple">Открыть приглашение</a></div>
      ) : (
        <>
          <div className="quick-actions"><a href="/dates/new"><CalendarPlus size={22} />Предложить свидание</a><a href="/places"><MapPin size={22} />Выбрать место</a><a href="/events"><Sparkles size={22} />Открыть афишу</a><a href="/couple"><Heart size={22} />Наше пространство</a></div>
          <div className="section-heading"><h2>Ближайшие страницы</h2><a href="/places">Найти идею</a></div>
          {upcoming.length ? <div className="date-list ribbon-list">{upcoming.map((date) => (
            <a className="date-card" href={`/dates/${date.id}`} key={date.id}><span className={`status ${date.status.toLowerCase()}`}>{date.status === 'ACCEPTED' ? 'Подтверждено' : 'Ждёт ответа'}</span><h3>{date.eventTitle || date.placeName}</h3><strong>{formatDateTime(date.scheduledAt)}</strong><small>{date.placeAddress}</small></a>
          ))}</div> : <div className="panel empty-state"><h2>Первая страница ещё не написана</h2><p>Выберите место или событие, которое хочется разделить вдвоём.</p><a className="button" href="/places">Найти идею <ArrowRight size={17} /></a></div>}
        </>
      )}
    </section>
  )
}
