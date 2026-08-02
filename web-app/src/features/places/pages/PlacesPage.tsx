import { useQuery } from '@tanstack/react-query'
import { ArrowUpRight, Heart, MapPin, Plus, Search, WalletCards } from 'lucide-react'
import { useState } from 'react'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { getPlaces } from '../api/placesApi'

const categories = [
  ['', 'Все идеи'],
  ['CAFE', 'Кофе и десерты'],
  ['RESTAURANT', 'Ужин вдвоём'],
  ['ENTERTAINMENT', 'Впечатления'],
] as const

export function PlacesPage() {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const places = useQuery({ queryKey: ['places', category, query], queryFn: () => getPlaces({ category, query }) })

  return (
    <section className="catalog-page">
      <div className="catalog-heading">
        <PageTitle eyebrow="Идеи для двоих · Казань" title="Найдите ваше место">Не рейтинг и не бесконечный список — только идеи, из которых хочется сложить общий вечер.</PageTitle>
        {places.data && <span className="catalog-count">{places.data.items.length}<small>идей сейчас</small></span>}
      </div>
      <div className="catalog-toolbar panel">
        <label className="search-field">
          <Search size={19} aria-hidden="true" />
          <span className="visually-hidden">Поиск мест</span>
          <input aria-label="Поиск мест" placeholder="Название, настроение или адрес" value={query} onChange={(event) => setQuery(event.target.value)} />
        </label>
        <div className="category-chips" aria-label="Категории мест">
          {categories.map(([value, label]) => <button type="button" aria-pressed={category === value} className={category === value ? 'active' : ''} key={value} onClick={() => setCategory(value)}>{label}</button>)}
        </div>
        <a className="button secondary private-place-link" href="/dates/new/private-place"><Plus size={17} /> Не нашли? Добавить своё место</a>
      </div>
      {places.isLoading ? <Loading /> : places.data?.items.length ? (
        <div className="card-grid">
          {places.data.items.map((place) => (
            <article className="place-card" key={place.id}>
              <a className="place-cover" href={`/places/${place.id}`} aria-label={`Открыть ${place.name}`}>
                <img src={place.images.find((image) => image.cover)?.cardUrl ?? '/place-placeholder.svg'}
                  alt={place.coverMediaId ? `Обложка места ${place.name}` : ''} loading="lazy" />
                <span className="cover-heart" aria-hidden="true"><Heart size={17} /></span>
              </a>
              <div><span className="eyebrow">{categories.find(([value]) => value === place.category)?.[1] ?? place.category}</span><h2>{place.name}</h2></div>
              <p>{place.description || 'Описание появится после обновления каталога.'}</p>
              <address className="icon-line"><MapPin size={16} aria-hidden="true" />{place.address}</address>
              <div className="card-actions">
                <span className="icon-line"><WalletCards size={16} aria-hidden="true" />{place.priceLevel ? '₽'.repeat(place.priceLevel) : 'Цена не указана'}</span>
                <div className="card-button-group">
                  <a className="round-link" href={`/places/${place.id}`} aria-label={`Подробнее о ${place.name}`}><ArrowUpRight size={18} /></a>
                  <a className="button small" href={`/dates/new?placeId=${place.id}`}>Позвать сюда</a>
                </div>
              </div>
              {place.sourcePageUrl && <a className="source-link" href={place.sourcePageUrl} target="_blank" rel="noopener noreferrer">Источник: {place.attributionName || place.source} ↗</a>}
            </article>
          ))}
        </div>
      ) : <div className="panel empty-state"><Heart className="empty-state-icon" size={38} strokeWidth={1.5} /><h2>Пока ничего не нашли</h2><p>Попробуйте другое слово или настроение.</p></div>}
      <ErrorMessage error={places.error} />
    </section>
  )
}
