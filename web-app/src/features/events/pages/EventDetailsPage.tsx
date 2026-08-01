import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { formatDateTime } from '../../../shared/lib/date'
import { ErrorMessage, Loading } from '../../../shared/ui/Feedback'
import { eventCover, getEvent } from '../api/eventsApi'
import { getEventMedia } from '../api/eventMediaApi'

export function EventDetailsPage() {
  const { eventId } = useParams({ strict: false }) as { eventId: string }
  const event = useQuery({ queryKey: ['event', eventId], queryFn: () => getEvent(eventId) })
  const media = useQuery({
    queryKey: ['event-media', eventId],
    queryFn: () => getEventMedia(eventId),
    retry: false,
  })
  if (event.isLoading) return <Loading />
  if (!event.data) return <ErrorMessage error={event.error} />
  const data = event.data
  const occurrences = data.occurrences.filter((occurrence) => occurrence.status === 'ACTIVE')
  const readyImages = media.data?.images.filter((image) => image.status === 'READY') ?? []
  const detailCover = readyImages.find((image) => image.cover)?.detailUrl ?? eventCover(data)

  return (
    <section className="place-details">
      <div className="place-detail-hero">
        <img src={detailCover} alt={`Обложка события ${data.title}`} />
        <div className="place-detail-overlay">
          <span className="eyebrow">{data.categories.join(' · ') || 'Событие'} · Казань</span>
          <h1>{data.title}</h1>
          <address>{data.venueName}{data.venueAddress ? ` · ${data.venueAddress}` : ''}</address>
        </div>
      </div>
      <div className="event-detail-layout">
        <article className="panel event-description">
          <p>{data.description || data.providerDescription || 'Описание события скоро появится.'}</p>
          <div className="place-facts">
            <span>{data.free ? 'Бесплатно' : data.priceText || 'Цена не указана'}</span>
            {data.ageRestriction && <span>{data.ageRestriction}</span>}
          </div>
          <a className="source-link" href={data.sourcePageUrl} target="_blank" rel="noopener noreferrer">
            Источник и подробности: KudaGo ↗
          </a>
        </article>
        <aside className="panel occurrence-panel">
          <h2>Выберите сеанс</h2>
          {occurrences.length ? occurrences.map((occurrence) => (
            <div className="occurrence-row" key={occurrence.id}>
              <div>
                <strong>{formatDateTime(occurrence.startsAt)}</strong>
                {occurrence.continuous && occurrence.endsAt && <small>Можно выбрать время до {formatDateTime(occurrence.endsAt)}</small>}
              </div>
              <a className="button small" href={`/dates/new?eventId=${data.id}&eventOccurrenceId=${occurrence.id}`}>
                Выбрать
              </a>
            </div>
          )) : <p className="muted">Доступных сеансов пока нет.</p>}
        </aside>
      </div>
      {readyImages.length > 1 && <div className="place-gallery event-gallery">
        {readyImages.map((image, index) => <figure key={image.mediaId}>
          <img src={image.detailUrl ?? image.thumbnailUrl ?? '/place-placeholder.svg'} alt={`${data.title}, фото ${index + 1}`} loading="lazy" />
          {image.source === 'REMOTE_URL' && image.sourceLink && <figcaption><a className="source-link" href={image.sourceLink} target="_blank" rel="noopener noreferrer">{image.sourceName || 'Источник'} ↗</a></figcaption>}
        </figure>)}
      </div>}
    </section>
  )
}
