import { ImagePlus } from 'lucide-react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { ErrorMessage, Loading, PageTitle } from '../../../shared/ui/Feedback'
import { toMoscowIso, toMoscowLocalInput } from '../../../shared/lib/date'
import { getPlaces } from '../../places/api/placesApi'
import { getEvent } from '../../events/api/eventsApi'
import { createDate, createDateFromEvent } from '../api/datesApi'

export function NewDatePage() {
  const initialPlace = new URLSearchParams(window.location.search).get('placeId') ?? ''
  const initialEvent = new URLSearchParams(window.location.search).get('eventId') ?? ''
  const initialOccurrence = new URLSearchParams(window.location.search).get('eventOccurrenceId') ?? ''
  const shouldUsePrivatePlace = !initialPlace && !initialEvent
  useEffect(() => {
    if (shouldUsePrivatePlace) window.location.replace('/dates/new/private-place')
  }, [shouldUsePrivatePlace])
  const [placeId, setPlaceId] = useState(initialPlace)
  const [scheduledAt, setScheduledAt] = useState('')
  const [description, setDescription] = useState('')
  const places = useQuery({ queryKey: ['places', 'date-form'], queryFn: () => getPlaces(), enabled: !shouldUsePrivatePlace })
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
  if (shouldUsePrivatePlace || places.isLoading || selectedEvent.isLoading) return <Loading />

  return (
    <section className="form-page">
      <PageTitle eyebrow="Новая страница" title={eventMode ? 'Позвать на событие' : 'Оставить приглашение'}>Соберите красивый план — ваш человек получит его и сможет ответить.</PageTitle>
      <a className="button secondary private-date-link" href="/dates/new/private-place"><ImagePlus size={18} /> Добавить своё место и фотографию</a>
      <form className="panel date-proposal-form" onSubmit={(event) => { event.preventDefault(); create.mutate() }}>
        <div className="form-steps" aria-label="Шаги предложения"><span className="active">1 · идея</span><span>2 · время</span><span>3 · записка</span></div>
        <section className="date-form-step"><span className="step-number">01</span><div className="step-content">{eventMode && selectedEvent.data ? <div className="selected-event-summary">
          <span className="eyebrow">KudaGo</span>
          <h2>{selectedEvent.data.title}</h2>
          <address>{selectedEvent.data.venueName}{selectedEvent.data.venueAddress ? ` · ${selectedEvent.data.venueAddress}` : ''}</address>
          {!occurrence?.continuous && occurrence && <strong>{new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long', timeStyle: 'short', timeZone: 'Europe/Moscow' }).format(new Date(occurrence.startsAt))}</strong>}
        </div> : <label>Куда пойдём
          <select value={placeId} onChange={(event) => setPlaceId(event.target.value)} required>
            <option value="">Выберите место</option>
            {places.data?.items.map((place) => <option key={place.id} value={place.id}>{place.name} — {place.address}</option>)}
          </select>
        </label>}</div></section>
        <section className="date-form-step"><span className="step-number">02</span><div className="step-content">{(!eventMode || occurrence?.continuous) && <label>{eventMode ? 'Когда придём' : 'Когда встречаемся'} (Москва)<input
          type="datetime-local"
          value={scheduledAt}
          min={occurrence?.continuous ? toMoscowLocalInput(occurrence.startsAt) : undefined}
          max={occurrence?.continuous && occurrence.endsAt ? toMoscowLocalInput(occurrence.endsAt) : undefined}
          onChange={(event) => setScheduledAt(event.target.value)}
          required
        /></label>}
        {eventMode && selectedEvent.data && !occurrence && <p className="message warning">Этот сеанс больше недоступен. Вернитесь в афишу и выберите другой.</p>}
        </div></section>
        <section className="date-form-step"><span className="step-number">03</span><div className="step-content"><label>Личная записка<textarea maxLength={1000} rows={5} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Например: хочу провести этот вечер только с тобой" /></label>
        <small>{description.length}/1000</small></div></section>
        <ErrorMessage error={create.error ?? selectedEvent.error ?? places.error} />
        <button className="date-submit" disabled={create.isPending || (eventMode && !occurrence)}>{create.isPending ? 'Отправляем…' : 'Отправить приглашение ♡'}</button>
      </form>
    </section>
  )
}
