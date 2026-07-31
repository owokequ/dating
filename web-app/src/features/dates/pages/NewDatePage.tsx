import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { toMoscowIso } from '../../../shared/lib/date'
import { getPlaces } from '../../places/api/placesApi'
import { createDate } from '../api/datesApi'

export function NewDatePage() {
  const initialPlace = new URLSearchParams(window.location.search).get('placeId') ?? ''
  const [placeId, setPlaceId] = useState(initialPlace)
  const [scheduledAt, setScheduledAt] = useState('')
  const [description, setDescription] = useState('')
  const places = useQuery({ queryKey: ['places', 'date-form'], queryFn: () => getPlaces() })
  const create = useMutation({
    mutationFn: () => createDate({ scheduledAt: toMoscowIso(scheduledAt), placeId, description: description || undefined }),
    onSuccess: (proposal) => window.location.assign(`/dates/${proposal.id}`),
  })
  if (places.isLoading) return <Loading />

  return (
    <section className="form-page">
      <PageTitle eyebrow="Новое свидание" title="Предложить время и место">Партнёр получит уведомление и должен подтвердить предложение.</PageTitle>
      <form className="panel" onSubmit={(event) => { event.preventDefault(); create.mutate() }}>
        <label>Место
          <select value={placeId} onChange={(event) => setPlaceId(event.target.value)} required>
            <option value="">Выберите место</option>
            {places.data?.items.map((place) => <option key={place.id} value={place.id}>{place.name} — {place.address}</option>)}
          </select>
        </label>
        <label>Дата и время (Москва)<input type="datetime-local" value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} required /></label>
        <label>Описание<textarea maxLength={1000} rows={5} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Например: хочу спокойно поужинать и прогуляться" /></label>
        <small>{description.length}/1000</small>
        <ErrorMessage error={create.error ?? places.error} />
        <button disabled={create.isPending}>Отправить предложение</button>
      </form>
    </section>
  )
}
