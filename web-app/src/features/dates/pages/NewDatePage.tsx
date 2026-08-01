import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { toMoscowIso, toMoscowLocalInput } from '../../../shared/lib/date'
import { getPlaces } from '../../places/api/placesApi'
import { getEvent } from '../../events/api/eventsApi'
import { createDate, createDateFromEvent } from '../api/datesApi'

export function NewDatePage() {
  const initialPlace = new URLSearchParams(window.location.search).get('placeId') ?? ''
  const initialEvent = new URLSearchParams(window.location.search).get('eventId') ?? ''
  const initialOccurrence = new URLSearchParams(window.location.search).get('eventOccurrenceId') ?? ''
  const [placeId, setPlaceId] = useState(initialPlace)
  const [scheduledAt, setScheduledAt] = useState('')
  const [description, setDescription] = useState('')
  const places = useQuery({ queryKey: ['places', 'date-form'], queryFn: () => getPlaces() })
  const selectedEvent = useQuery({
    queryKey: ['event', initialEvent],
    queryFn: () => getEvent(initialEvent),
    enabled: Boolean(initialEvent && initialOccurrence),
  })
  const occurrence = selectedEvent.data?.occurrences.find((value) => value.id === initialOccurrence)
  const eventMode = Boolean(initialEvent && initialOccurrence)
  const create = useMutation({
    mutationFn: () => eventMode
      ? createDateFromEvent({
          eventOccurrenceId: initialOccurrence,
          visitAt: occurrence?.continuous ? toMoscowIso(scheduledAt) : undefined,
          description: description || undefined,
        })
      : createDate({ scheduledAt: toMoscowIso(scheduledAt), placeId, description: description || undefined }),
    onSuccess: (proposal) => window.location.assign(`/dates/${proposal.id}`),
  })
  if (places.isLoading || selectedEvent.isLoading) return <Loading />

  return (
    <section className="form-page">
      <PageTitle eyebrow="Новое свидание" title={eventMode ? 'Предложить событие' : 'Предложить время и место'}>Партнёр получит уведомление и должен подтвердить предложение.</PageTitle>
      <form className="panel" onSubmit={(event) => { event.preventDefault(); create.mutate() }}>
        {eventMode && selectedEvent.data ? <div className="selected-event-summary">
          <span className="eyebrow">KudaGo</span>
          <h2>{selectedEvent.data.title}</h2>
          <address>{selectedEvent.data.venueName}{selectedEvent.data.venueAddress ? ` · ${selectedEvent.data.venueAddress}` : ''}</address>
          {!occurrence?.continuous && occurrence && <strong>{new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long', timeStyle: 'short', timeZone: 'Europe/Moscow' }).format(new Date(occurrence.startsAt))}</strong>}
        </div> : <label>Место
          <select value={placeId} onChange={(event) => setPlaceId(event.target.value)} required>
            <option value="">Выберите место</option>
            {places.data?.items.map((place) => <option key={place.id} value={place.id}>{place.name} — {place.address}</option>)}
          </select>
        </label>}
        {(!eventMode || occurrence?.continuous) && <label>{eventMode ? 'Время посещения внутри периода' : 'Дата и время'} (Москва)<input
          type="datetime-local"
          value={scheduledAt}
          min={occurrence?.continuous ? toMoscowLocalInput(occurrence.startsAt) : undefined}
          max={occurrence?.continuous && occurrence.endsAt ? toMoscowLocalInput(occurrence.endsAt) : undefined}
          onChange={(event) => setScheduledAt(event.target.value)}
          required
        /></label>}
        {eventMode && selectedEvent.data && !occurrence && <p className="message warning">Этот сеанс больше недоступен. Вернитесь в афишу и выберите другой.</p>}
        <label>Описание<textarea maxLength={1000} rows={5} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Например: хочу спокойно поужинать и прогуляться" /></label>
        <small>{description.length}/1000</small>
        <ErrorMessage error={create.error ?? selectedEvent.error ?? places.error} />
        <button disabled={create.isPending || (eventMode && !occurrence)}>Отправить предложение</button>
      </form>
    </section>
  )
}
