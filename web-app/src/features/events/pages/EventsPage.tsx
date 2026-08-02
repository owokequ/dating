import { useQuery } from '@tanstack/react-query'
import { ArrowRight, CalendarDays, MapPin, Sparkles, Ticket } from 'lucide-react'
import { useState } from 'react'
import { formatDateTime } from '../../../shared/lib/date'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { eventCover, getEvents } from '../api/eventsApi'

const categories = [
  ['', 'Все события'],
  ['cinema', 'Кино'],
  ['concert', 'Концерты'],
  ['entertainment', 'Развлечения'],
  ['exhibition', 'Выставки'],
  ['festival', 'Фестивали'],
  ['party', 'Вечеринки'],
  ['quest', 'Квесты'],
  ['recreation', 'Отдых'],
  ['theater', 'Театр'],
  ['tour', 'Экскурсии'],
] as const

export function EventsPage() {
  const [category, setCategory] = useState('')
  const [onlyFree, setOnlyFree] = useState(false)
  const [page, setPage] = useState(0)
  const events = useQuery({
    queryKey: ['events', category, onlyFree, page],
    queryFn: () => getEvents({ category, free: onlyFree || undefined, page }),
  })

  return (
    <section className="catalog-page">
      <div className="catalog-heading">
        <PageTitle eyebrow="Афиша Казани" title="Один вечер — одна история">
          Выберите событие и сеанс, а For my L превратит их в красивое приглашение для вашего человека.
        </PageTitle>
        <span className="catalog-mark" aria-hidden="true"><Sparkles size={28} />L</span>
      </div>
      <div className="catalog-toolbar panel event-filters">
        <div className="category-chips category-chips-scroll" aria-label="Категории событий">
          {categories.map(([value, label]) => <button type="button" aria-pressed={category === value} className={category === value ? 'active' : ''} key={value} onClick={() => { setCategory(value); setPage(0) }}>{label}</button>)}
        </div>
        <label className="checkbox-label">
          <input type="checkbox" checked={onlyFree} onChange={(event) => { setOnlyFree(event.target.checked); setPage(0) }} />
          Только бесплатные
        </label>
      </div>
      {events.isLoading ? <Loading /> : events.data?.items.length ? (
        <div className="card-grid">
          {events.data.items.map((event) => {
            const nextOccurrence = event.occurrences.find((occurrence) => occurrence.status === 'ACTIVE')
            return (
              <article className="place-card event-card" key={event.id}>
                <a className="place-cover" href={`/events/${event.id}`}>
                  <img src={eventCover(event)} alt={`Обложка события ${event.title}`} loading="lazy" />
                  {nextOccurrence && <span className="cover-date"><CalendarDays size={14} />{formatDateTime(nextOccurrence.startsAt)}</span>}
                </a>
                <div><span className="eyebrow">{event.categories[0] ?? 'Событие'}</span><h2>{event.title}</h2></div>
                <p>{event.description || event.providerDescription || 'Описание события скоро появится.'}</p>
                <address className="icon-line"><MapPin size={16} />{event.venueName}{event.venueAddress ? ` · ${event.venueAddress}` : ''}</address>
                <div className="card-actions">
                  <span className="icon-line"><Ticket size={16} />{event.free ? 'Бесплатно' : event.priceText || 'Цена не указана'}</span>
                  <a className="button small" href={`/events/${event.id}`}>Выбрать <ArrowRight size={16} /></a>
                </div>
                <a className="source-link" href={event.sourcePageUrl} target="_blank" rel="noopener noreferrer">
                  Источник: KudaGo ↗
                </a>
              </article>
            )
          })}
        </div>
      ) : <div className="panel empty-state"><CalendarDays className="empty-state-icon" size={38} strokeWidth={1.5} /><h2>На эти даты тишина</h2><p>Попробуйте другую категорию — хорошая идея точно найдётся.</p></div>}
      {events.data && events.data.totalPages > 1 && <nav className="pagination" aria-label="Страницы афиши">
        <button className="secondary small" disabled={events.data.page === 0} onClick={() => setPage((value) => value - 1)}>← Назад</button>
        <span>Страница {events.data.page + 1} из {events.data.totalPages}</span>
        <button className="secondary small" disabled={events.data.page + 1 >= events.data.totalPages} onClick={() => setPage((value) => value + 1)}>Вперёд →</button>
      </nav>}
      <ErrorMessage error={events.error} />
    </section>
  )
}
