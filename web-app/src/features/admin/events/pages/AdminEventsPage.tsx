import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ErrorMessage, Loading, PageTitle } from '../../../../shared/ui/Feedback'
import { useSession } from '../../../auth/api/authApi'
import type { Event, EventStatus } from '../../../events/api/eventsApi'
import { changeEventStatus, getAdminEvents, syncEvents, updateEventVenue } from '../api/adminEventsApi'
import { MediaManager } from '../../media/ui/MediaManager'

const statusTabs: { status: EventStatus; label: string }[] = [
  { status: 'DRAFT', label: 'Черновики' },
  { status: 'ACTIVE', label: 'Активные' },
  { status: 'HIDDEN', label: 'Скрытые' },
  { status: 'ARCHIVED', label: 'Архив' },
]

export function AdminEventsPage() {
  const session = useSession()
  const isAdmin = session.data?.role === 'ADMIN'
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<EventStatus>('DRAFT')
  const events = useQuery({
    queryKey: ['admin-events', status],
    queryFn: () => getAdminEvents(status),
    enabled: isAdmin,
  })
  const sync = useMutation({
    mutationFn: syncEvents,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-events'] }),
  })

  if (session.isLoading) return <Loading />
  if (!session.data) return <AdminAccess title="Нужна авторизация" body="Войдите, чтобы открыть управление афишей." href="/login?continue=%2Fadmin%2Fevents" />
  if (!isAdmin) return <AdminAccess title="Доступ закрыт" body="Управлять афишей может только администратор." href="/events" />

  return (
    <section>
      <PageTitle eyebrow="Admin" title="Управление афишей">
        События KudaGo синхронизируются автоматически. Здесь можно проверить черновики, исправить площадку и скрыть неподходящую карточку.
      </PageTitle>
      <section className="panel admin-sync-panel">
        <div><h2>Синхронизация KudaGo</h2><p className="muted">Загружаются события Казани на ближайшие 90 дней.</p></div>
        <button type="button" disabled={sync.isPending} onClick={() => sync.mutate()}>
          {sync.isPending ? 'Синхронизируем…' : 'Синхронизировать афишу'}
        </button>
        {sync.data && <div className="sync-result" role="status">
          <span>Получено: <strong>{sync.data.received}</strong></span>
          <span>Обработано: <strong>{sync.data.upserted}</strong></span>
          <span>Пропущено: <strong>{sync.data.skipped}</strong></span>
          <span>Страниц: <strong>{sync.data.pages}</strong></span>
          <span>{sync.data.complete ? 'Полная синхронизация' : 'Частичный результат'}</span>
        </div>}
        {sync.data?.errors.map((failure) => <p className="message warning" key={`${failure.page}-${failure.detail}`}>Страница {failure.page}: {failure.detail}</p>)}
        <ErrorMessage error={sync.error} />
      </section>
      <section className="panel admin-event-list">
        <div className="section-heading"><h2>События</h2><span>{events.data?.totalElements ?? 0}</span></div>
        <div className="admin-status-tabs" role="tablist" aria-label="Статус события">
          {statusTabs.map((tab) => <button type="button" role="tab" aria-selected={status === tab.status}
            className={status === tab.status ? 'small' : 'secondary small'} key={tab.status} onClick={() => setStatus(tab.status)}>{tab.label}</button>)}
        </div>
        {events.isLoading ? <Loading /> : events.data?.items.length ? events.data.items.map((event) => (
          <AdminEventCard key={event.id} event={event} onChanged={() => queryClient.invalidateQueries({ queryKey: ['admin-events'] })} />
        )) : <p className="muted">В этом разделе событий пока нет.</p>}
        <ErrorMessage error={events.error} />
      </section>
    </section>
  )
}

function AdminAccess({ title, body, href }: { title: string; body: string; href: string }) {
  return <section className="panel empty-state"><h1>{title}</h1><p>{body}</p><a className="button" href={href}>Продолжить</a></section>
}

function AdminEventCard({ event, onChanged }: { event: Event; onChanged: () => Promise<void> }) {
  const [expanded, setExpanded] = useState(false)
  const [venueName, setVenueName] = useState(event.venueName ?? '')
  const [venueAddress, setVenueAddress] = useState(event.venueAddress ?? '')
  const [latitude, setLatitude] = useState<number | ''>(event.latitude ?? '')
  const [longitude, setLongitude] = useState<number | ''>(event.longitude ?? '')
  const venue = useMutation({
    mutationFn: () => updateEventVenue(event.id, {
      venueName,
      venueAddress,
      latitude: Number(latitude),
      longitude: Number(longitude),
    }),
    onSuccess: onChanged,
  })
  const status = useMutation({
    mutationFn: (action: 'publish' | 'hide' | 'archive') => changeEventStatus(event.id, action),
    onSuccess: onChanged,
  })
  const canPublish = venueName.trim().length > 0
    && venueAddress.trim().length > 0
    && latitude !== ''
    && longitude !== ''

  return <article className="admin-event-item">
    <div className="admin-place-badges"><span className="eyebrow">KudaGo</span><span className={`status ${event.status.toLowerCase()}`}>{event.status}</span></div>
    <h3>{event.title}</h3>
    <p className="muted">{event.description || event.providerDescription}</p>
    <a className="source-link" href={event.sourcePageUrl} target="_blank" rel="noopener noreferrer">Открыть источник ↗</a>
    {event.status !== 'ARCHIVED' && <button type="button" className="secondary small admin-event-expand" onClick={() => setExpanded((value) => !value)}>
      {expanded ? 'Свернуть редактор' : 'Редактировать карточку'}
    </button>}
    {event.status !== 'ARCHIVED' && expanded && <>
      <div className="admin-event-venue">
        <label>Площадка<input value={venueName} onChange={(e) => setVenueName(e.target.value)} /></label>
        <label>Адрес<input value={venueAddress} onChange={(e) => setVenueAddress(e.target.value)} /></label>
        <label>Широта<input type="number" min="-90" max="90" step="any" value={latitude} onChange={(e) => setLatitude(e.target.value === '' ? '' : Number(e.target.value))} /></label>
        <label>Долгота<input type="number" min="-180" max="180" step="any" value={longitude} onChange={(e) => setLongitude(e.target.value === '' ? '' : Number(e.target.value))} /></label>
      </div>
      <MediaManager ownerType="EVENT" ownerId={event.id} ownerName={event.title} />
    </>}
    <div className="button-row">
      {event.status !== 'ARCHIVED' && expanded && <button className="secondary small" disabled={venue.isPending || !canPublish} onClick={() => venue.mutate()}>Сохранить площадку</button>}
      {(event.status === 'DRAFT' || event.status === 'HIDDEN') && <button className="small" disabled={status.isPending || !canPublish} onClick={() => status.mutate('publish')}>Опубликовать</button>}
      {event.status === 'ACTIVE' && <button className="secondary small" disabled={status.isPending} onClick={() => status.mutate('hide')}>Скрыть</button>}
      {event.status !== 'ARCHIVED' && <button className="danger small" disabled={status.isPending} onClick={() => status.mutate('archive')}>Архивировать</button>}
    </div>
    <ErrorMessage error={venue.error ?? status.error} />
  </article>
}
