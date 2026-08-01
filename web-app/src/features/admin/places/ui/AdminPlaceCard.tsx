import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useCallback, useState } from 'react'
import type { Place } from '../../../places/api/placesApi'
import { ErrorMessage } from '../../../../shared/ui/Feedback'
import { savePlaceModeration, type PlaceStatus } from '../api/adminPlacesApi'
import type { MediaCollection } from '../api/adminMediaApi'
import { PlaceMediaManager } from './PlaceMediaManager'

type Props = {
  place: Place
  onChanged: () => Promise<void>
}

export function AdminPlaceCard({ place, onChanged }: Props) {
  const queryClient = useQueryClient()
  const [description, setDescription] = useState(place.description ?? '')
  const [priceLevel, setPriceLevel] = useState(place.priceLevel ?? 2)
  const [hasReadyCover, setHasReadyCover] = useState(place.coverMediaId !== null)
  const onCollectionChange = useCallback((collection: MediaCollection | undefined) => {
    setHasReadyCover(collection?.images.some((image) => image.cover && image.status === 'READY') ?? false)
  }, [])
  const update = useMutation({
    mutationFn: (status: PlaceStatus) => savePlaceModeration(
      place,
      description.trim() || null,
      priceLevel,
      status,
    ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['places'] })
      await onChanged()
    },
  })
  const canEdit = place.status !== 'ARCHIVED'
  const canPublish = place.status === 'DRAFT'
    && description.trim().length > 0
    && hasReadyCover

  return (
    <article className="admin-place-item">
      <div className="admin-place-summary">
        <div>
          <div className="admin-place-badges">
            <span className="eyebrow">{place.category}</span>
            <span className={`status ${place.status.toLowerCase()}`}>{place.status}</span>
            <span className="status">{place.source === 'TWO_GIS' ? '2GIS' : 'Вручную'}</span>
          </div>
          <h3>{place.name}</h3>
          <address>{place.address}</address>
          <small>{place.latitude}, {place.longitude}</small>
        </div>
      </div>

      {canEdit && (
        <div className="admin-place-editor">
          <label>
            Описание
            <textarea
              rows={4}
              maxLength={2000}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>
          <label>
            Уровень цены
            <select value={priceLevel} onChange={(event) => setPriceLevel(Number(event.target.value))}>
              {[1, 2, 3, 4].map((level) => (
                <option key={level} value={level}>{'₽'.repeat(level)}</option>
              ))}
            </select>
          </label>
          {place.source === 'TWO_GIS' && (
            <p className="muted admin-provider-note">
              Название, адрес, координаты и категория принадлежат 2GIS и обновляются только синхронизацией.
            </p>
          )}
        </div>
      )}

      {canEdit && (
        <PlaceMediaManager
          placeId={place.id}
          placeName={place.name}
          onCollectionChange={onCollectionChange}
        />
      )}

      {canEdit && (
        <div className="button-row admin-place-actions">
          <button
            type="button"
            className="secondary small"
            disabled={update.isPending}
            onClick={() => update.mutate(place.status)}
          >
            Сохранить
          </button>
          {place.status === 'DRAFT' && (
            <button
              type="button"
              className="small"
              disabled={update.isPending || !canPublish}
              title={!canPublish ? 'Добавьте описание и готовую фотографию-обложку' : undefined}
              onClick={() => update.mutate('ACTIVE')}
            >
              Опубликовать
            </button>
          )}
          <button
            type="button"
            className="danger small"
            disabled={update.isPending}
            onClick={() => update.mutate('ARCHIVED')}
          >
            Архивировать
          </button>
        </div>
      )}
      {place.status === 'DRAFT' && !canPublish && (
        <p className="message warning">Для публикации нужны описание и готовая фотография-обложка.</p>
      )}
      <ErrorMessage error={update.error} />
    </article>
  )
}
