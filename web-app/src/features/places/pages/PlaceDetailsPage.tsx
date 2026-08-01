import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { ErrorMessage, Loading } from '../../../shared/ui/Feedback'
import { getPlace } from '../api/placesApi'

export function PlaceDetailsPage() {
  const { placeId } = useParams({ strict: false }) as { placeId: string }
  const place = useQuery({ queryKey: ['place', placeId], queryFn: () => getPlace(placeId) })
  if (place.isLoading) return <Loading />
  if (!place.data) return <ErrorMessage error={place.error} />
  const images = place.data.images
  return (
    <section className="place-details">
      <div className="place-detail-hero">
        <img src={images.find((image) => image.cover)?.detailUrl ?? '/place-placeholder.svg'}
          alt={`Обложка места ${place.data.name}`} />
        <div className="place-detail-overlay">
          <span className="eyebrow">{place.data.category} · Казань</span>
          <h1>{place.data.name}</h1>
          <address>{place.data.address}</address>
        </div>
      </div>
      <div className="place-detail-content">
        <article className="panel">
          <p>{place.data.description || 'Описание места скоро появится.'}</p>
          <div className="place-facts">
            <span>{place.data.priceLevel ? '₽'.repeat(place.data.priceLevel) : 'Цена не указана'}</span>
            <span>{images.length} фото</span>
          </div>
          <a className="button" href={`/dates/new?placeId=${place.data.id}`}>Предложить свидание 💌</a>
          {place.data.sourcePageUrl && <a className="source-link" href={place.data.sourcePageUrl} target="_blank" rel="noopener noreferrer">Источник: {place.data.attributionName || place.data.source} ↗</a>}
        </article>
        {images.length > 1 && <div className="place-gallery">
          {images.map((image, index) => <img key={image.mediaId} src={image.detailUrl}
            alt={`${place.data.name}, фото ${index + 1}`} loading="lazy" />)}
        </div>}
      </div>
    </section>
  )
}
