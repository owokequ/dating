import { useQuery } from '@tanstack/react-query'
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
    <section>
      <PageTitle eyebrow="Афиша Казани" title="Куда пойдём вместе?">
        Выберите событие и подходящий сеанс — дата и площадка попадут в предложение автоматически.
      </PageTitle>
      <div className="filters panel event-filters">
        <select aria-label="Категория события" value={category} onChange={(event) => { setCategory(event.target.value); setPage(0) }}>
          {categories.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
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
                </a>
                <div><span className="eyebrow">{event.categories[0] ?? 'Событие'}</span><h2>{event.title}</h2></div>
                <p>{event.description || event.providerDescription || 'Описание события скоро появится.'}</p>
                <address>{event.venueName}{event.venueAddress ? ` · ${event.venueAddress}` : ''}</address>
                {nextOccurrence && <strong className="event-date">{formatDateTime(nextOccurrence.startsAt)}</strong>}
                <div className="card-actions">
                  <span>{event.free ? 'Бесплатно' : event.priceText || 'Цена не указана'}</span>
                  <a className="button small" href={`/events/${event.id}`}>Выбрать сеанс</a>
                </div>
                <a className="source-link" href={event.sourcePageUrl} target="_blank" rel="noopener noreferrer">
                  Источник: KudaGo ↗
                </a>
              </article>
            )
          })}
        </div>
      ) : <div className="panel empty-state">Событий по выбранным фильтрам пока нет.</div>}
      {events.data && events.data.totalPages > 1 && <nav className="pagination" aria-label="Страницы афиши">
        <button className="secondary small" disabled={events.data.page === 0} onClick={() => setPage((value) => value - 1)}>← Назад</button>
        <span>Страница {events.data.page + 1} из {events.data.totalPages}</span>
        <button className="secondary small" disabled={events.data.page + 1 >= events.data.totalPages} onClick={() => setPage((value) => value + 1)}>Вперёд →</button>
      </nav>}
      <ErrorMessage error={events.error} />
    </section>
  )
}
