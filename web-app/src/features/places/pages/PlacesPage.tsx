import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { getPlaces } from '../api/placesApi'

const categories = ['', 'CAFE', 'RESTAURANT', 'ENTERTAINMENT']

export function PlacesPage() {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const places = useQuery({ queryKey: ['places', category, query], queryFn: () => getPlaces({ category, query }) })

  return (
    <section>
      <PageTitle eyebrow="Казань" title="Куда пойдём?">Каталог ручных подборок и разрешённых данных 2GIS.</PageTitle>
      <div className="filters panel">
        <input aria-label="Поиск мест" placeholder="Название или адрес" value={query} onChange={(event) => setQuery(event.target.value)} />
        <select aria-label="Категория" value={category} onChange={(event) => setCategory(event.target.value)}>
          {categories.map((value) => <option key={value} value={value}>{value || 'Все категории'}</option>)}
        </select>
      </div>
      {places.isLoading ? <Loading /> : places.data?.items.length ? (
        <div className="card-grid">
          {places.data.items.map((place) => (
            <article className="place-card" key={place.id}>
              <div><span className="eyebrow">{place.category}</span><h2>{place.name}</h2></div>
              <p>{place.description || 'Описание появится после обновления каталога.'}</p>
              <address>{place.address}</address>
              <div className="card-actions">
                <span>{place.priceLevel ? '₽'.repeat(place.priceLevel) : 'Цена не указана'}</span>
                <a className="button small" href={`/dates/new?placeId=${place.id}`}>Выбрать</a>
              </div>
            </article>
          ))}
        </div>
      ) : <div className="panel empty-state">Места не найдены. Измените фильтры.</div>}
      <ErrorMessage error={places.error} />
    </section>
  )
}
