import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { ErrorMessage, Loading } from '../../../../shared/ui/Feedback'
import {
  deleteImage,
  getMedia,
  reorderMedia,
  uploadImage,
  type MediaCollection,
  type MediaOwnerType,
} from '../api/adminMediaApi'

type Props = {
  ownerType: MediaOwnerType
  ownerId: string
  ownerName: string
  onCollectionChange?: (collection: MediaCollection | undefined) => void
}

export function MediaManager({ ownerType, ownerId, ownerName, onCollectionChange }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const queryClient = useQueryClient()
  const mediaKey = ['admin-media', ownerType, ownerId]
  const media = useQuery({
    queryKey: mediaKey,
    queryFn: () => getMedia(ownerType, ownerId),
    retry: 8,
    retryDelay: (attempt) => Math.min(250 * 2 ** attempt, 1500),
    refetchInterval: (query) => query.state.data?.images.some((image) =>
      image.status === 'UPLOADED' || image.status === 'PROCESSING') ? 1500 : false,
  })
  useEffect(() => onCollectionChange?.(media.data), [media.data, onCollectionChange])
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: mediaKey })
    await queryClient.invalidateQueries({ queryKey: [ownerType === 'PLACE' ? 'places' : 'events'] })
  }
  const upload = useMutation({
    mutationFn: async (files: File[]) => {
      for (const file of files) await uploadImage(ownerType, ownerId, file)
    },
    onSuccess: refresh,
  })
  const reorder = useMutation({
    mutationFn: (next: { coverMediaId: string; orderedMediaIds: string[] }) =>
      reorderMedia(ownerType, ownerId, next.coverMediaId, next.orderedMediaIds),
    onSuccess: refresh,
  })
  const remove = useMutation({
    mutationFn: (mediaId: string) => deleteImage(ownerType, ownerId, mediaId),
    onSuccess: refresh,
  })

  const applyOrder = (collection: MediaCollection, mediaId: string, direction: -1 | 1) => {
    const ids = collection.images.map((image) => image.mediaId)
    const index = ids.indexOf(mediaId)
    const nextIndex = index + direction
    if (nextIndex < 0 || nextIndex >= ids.length || !collection.coverMediaId) return
    ;[ids[index], ids[nextIndex]] = [ids[nextIndex], ids[index]]
    reorder.mutate({ coverMediaId: collection.coverMediaId, orderedMediaIds: ids })
  }

  if (media.isLoading) return <Loading />
  const collection = media.data
  const atLimit = (collection?.images.length ?? 0) >= 5

  return <div className="media-manager">
    <div className="media-manager-heading">
      <strong>Фотографии · {collection?.images.length ?? 0}/5</strong>
      <button className="secondary small" type="button" disabled={atLimit || upload.isPending} onClick={() => inputRef.current?.click()}>
        {upload.isPending ? 'Загружаем…' : 'Добавить фото'}
      </button>
      <input ref={inputRef} className="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={(event) => {
        const remaining = 5 - (collection?.images.length ?? 0)
        const files = Array.from(event.target.files ?? []).slice(0, remaining)
        if (files.length) upload.mutate(files)
        event.target.value = ''
      }} />
    </div>
    {collection?.images.length ? <div className="media-admin-grid">
      {collection.images.map((image, index) => <article className="media-admin-item" key={image.mediaId}>
        {image.thumbnailUrl ? <img src={image.thumbnailUrl} alt={`${ownerName}, фото ${index + 1}`} />
          : <div className="media-processing">{image.status === 'FAILED' ? 'Ошибка' : 'Обработка…'}</div>}
        <div className="media-admin-actions">
          <button type="button" className={image.cover ? 'small' : 'secondary small'} disabled={image.cover || reorder.isPending || image.status !== 'READY'}
            onClick={() => reorder.mutate({ coverMediaId: image.mediaId, orderedMediaIds: collection.images.map((item) => item.mediaId) })}>{image.cover ? 'Обложка' : 'На обложку'}</button>
          <button type="button" className="secondary media-icon" aria-label="Переместить влево" disabled={index === 0 || reorder.isPending}
            onClick={() => applyOrder(collection, image.mediaId, -1)}>←</button>
          <button type="button" className="secondary media-icon" aria-label="Переместить вправо" disabled={index === collection.images.length - 1 || reorder.isPending}
            onClick={() => applyOrder(collection, image.mediaId, 1)}>→</button>
          <button type="button" className="danger media-icon" aria-label="Удалить фотографию" disabled={remove.isPending}
            onClick={() => remove.mutate(image.mediaId)}>×</button>
        </div>
        {image.source === 'REMOTE_URL' && image.sourceLink && <a className="source-link media-source" href={image.sourceLink} target="_blank" rel="noopener noreferrer">{image.sourceName || 'Источник'} ↗</a>}
      </article>)}
    </div> : <p className="muted">Добавьте обложку — она появится в каталоге и карточке свидания.</p>}
    <ErrorMessage error={media.error ?? upload.error ?? reorder.error ?? remove.error} />
  </div>
}
