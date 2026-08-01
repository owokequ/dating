import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useState } from 'react'
import type { z } from 'zod'
import { ErrorMessage, Loading, PageTitle } from '../../../../shared/ui/Feedback'
import { useSession } from '../../../auth/api/authApi'
import {
  createPlace,
  getAdminPlaces,
  syncTwoGis,
  type PlaceStatus,
} from '../api/adminPlacesApi'
import { uploadPlaceImageWhenProjectionIsReady } from '../api/adminMediaApi'
import { adminPlaceSchema, placeCategories } from '../schemas'
import { AdminPlaceCard } from '../ui/AdminPlaceCard'
import { PlaceImagePicker } from '../ui/PlaceImagePicker'

type FormValues = z.infer<typeof adminPlaceSchema>
type CreateCommand = { values: FormValues; images: File[] }

const defaultValues: FormValues = {
  name: '',
  description: '',
  category: 'CAFE',
  address: '',
  latitude: 55.7963,
  longitude: 49.1064,
  priceLevel: 2,
}

const statusTabs: { status: PlaceStatus; label: string }[] = [
  { status: 'DRAFT', label: 'Черновики' },
  { status: 'ACTIVE', label: 'Активные' },
  { status: 'ARCHIVED', label: 'Архив' },
]

export function AdminPlacesPage() {
  const session = useSession()
  const isAdmin = session.data?.role === 'ADMIN'
  const queryClient = useQueryClient()
  const [images, setImages] = useState<File[]>([])
  const [status, setStatus] = useState<PlaceStatus>('DRAFT')
  const form = useForm<FormValues>({ resolver: zodResolver(adminPlaceSchema), defaultValues })
  const places = useQuery({
    queryKey: ['admin-places', status],
    queryFn: () => getAdminPlaces(status),
    enabled: isAdmin,
  })
  const sync = useMutation({
    mutationFn: syncTwoGis,
    onSuccess: async () => {
      setStatus('DRAFT')
      await queryClient.invalidateQueries({ queryKey: ['admin-places'] })
    },
  })
  const create = useMutation({
    mutationFn: async ({ values, images: selectedImages }: CreateCommand) => {
      const place = await createPlace({
        ...values,
        description: values.description || null,
      })
      let uploadedImages = 0
      let uploadError: string | null = null
      for (const image of selectedImages) {
        try {
          await uploadPlaceImageWhenProjectionIsReady(place.id, image)
          uploadedImages++
        } catch (error) {
          // The place is already committed. Failed files can be retried from its media manager.
          uploadError ??= error instanceof Error ? error.message : 'Неизвестная ошибка загрузки'
        }
      }
      return { place, requestedImages: selectedImages.length, uploadedImages, uploadError }
    },
    onSuccess: async () => {
      form.reset(defaultValues)
      setImages([])
      setStatus('ACTIVE')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['places'] }),
        queryClient.invalidateQueries({ queryKey: ['admin-places'] }),
      ])
    },
  })

  if (session.isLoading) return <Loading />
  if (!session.data) {
    return (
      <section className="panel empty-state">
        <h1>Нужна авторизация</h1>
        <a className="button" href="/login?continue=%2Fadmin%2Fplaces">Войти</a>
      </section>
    )
  }
  if (!isAdmin) {
    return (
      <section className="panel empty-state">
        <span className="eyebrow">403</span>
        <h1>Доступ закрыт</h1>
        <p>Управлять каталогом может только администратор.</p>
        <a className="button secondary" href="/places">Открыть каталог</a>
      </section>
    )
  }

  return (
    <section>
      <PageTitle eyebrow="Admin" title="Управление местами">
        Импортируйте места из 2GIS в черновики, добавьте собственные фотографии и публикуйте только проверенные карточки.
      </PageTitle>
      <section className="panel admin-sync-panel">
        <div>
          <h2>Импорт из 2GIS</h2>
          <p className="muted">Ручная синхронизация получает кафе, рестораны и развлечения Казани. Новые места не видны пользователям до публикации.</p>
        </div>
        <button type="button" disabled={sync.isPending} onClick={() => sync.mutate()}>
          {sync.isPending ? 'Синхронизируем…' : 'Синхронизировать с 2GIS'}
        </button>
        {sync.data && (
          <div className="sync-result" role="status">
            <span>Получено: <strong>{sync.data.received}</strong></span>
            <span>Создано: <strong>{sync.data.created}</strong></span>
            <span>Обновлено: <strong>{sync.data.updated}</strong></span>
            <span>Без изменений: <strong>{sync.data.unchanged}</strong></span>
            <span>Дубликаты: <strong>{sync.data.duplicates}</strong></span>
          </div>
        )}
        {sync.data?.failures.map((failure) => (
          <p className="message warning" key={`${failure.category}-${failure.page}`}>
            {failure.category}, страница {failure.page}: {failure.reason}
          </p>
        ))}
        <ErrorMessage error={sync.error} />
      </section>
      <div className="admin-grid">
        <form className="panel admin-place-form" onSubmit={form.handleSubmit((values) => create.mutate({ values, images }))}>
          <h2>Новое место</h2>
          <label>Название<input {...form.register('name')} /></label>
          <small>{form.formState.errors.name?.message}</small>
          <label>Категория
            <select {...form.register('category')}>
              {placeCategories.map((category) => <option key={category}>{category}</option>)}
            </select>
          </label>
          <label>Адрес<input {...form.register('address')} /></label>
          <small>{form.formState.errors.address?.message}</small>
          <label>Описание<textarea rows={4} {...form.register('description')} /></label>
          <small>{form.formState.errors.description?.message}</small>
          <div className="coordinate-grid">
            <label>Широта<input type="number" step="any" {...form.register('latitude', { valueAsNumber: true })} /></label>
            <label>Долгота<input type="number" step="any" {...form.register('longitude', { valueAsNumber: true })} /></label>
          </div>
          <small>{form.formState.errors.latitude?.message ?? form.formState.errors.longitude?.message}</small>
          <label>Уровень цены
            <select {...form.register('priceLevel', { valueAsNumber: true })}>
              {[1, 2, 3, 4].map((level) => <option key={level} value={level}>{'₽'.repeat(level)}</option>)}
            </select>
          </label>
          <PlaceImagePicker files={images} disabled={create.isPending} onChange={setImages} />
          {create.data && create.data.uploadedImages === create.data.requestedImages && (
            <p className="message success">
              Место создано{create.data.uploadedImages ? `, фотографий загружено: ${create.data.uploadedImages}` : ''}.
            </p>
          )}
          {create.data && create.data.uploadedImages < create.data.requestedImages && (
            <p className="message warning">
              Место создано, но загружено {create.data.uploadedImages} из {create.data.requestedImages} фотографий.
              {' '}Повторите недостающие загрузки в карточке места.
              {create.data.uploadError && <> Причина: {create.data.uploadError}.</>}
            </p>
          )}
          <ErrorMessage error={create.error} />
          <button disabled={create.isPending}>{create.isPending ? 'Создаём место и загружаем фото…' : 'Добавить место'}</button>
        </form>

        <div className="panel admin-place-list">
          <div className="section-heading"><h2>Каталог</h2><span>{places.data?.totalElements ?? 0}</span></div>
          <div className="admin-status-tabs" role="tablist" aria-label="Статус места">
            {statusTabs.map((tab) => (
              <button
                type="button"
                role="tab"
                aria-selected={status === tab.status}
                className={status === tab.status ? 'small' : 'secondary small'}
                key={tab.status}
                onClick={() => setStatus(tab.status)}
              >
                {tab.label}
              </button>
            ))}
          </div>
          {places.isLoading ? <Loading /> : places.data?.items.length ? places.data.items.map((place) => (
            <AdminPlaceCard
              key={place.id}
              place={place}
              onChanged={() => queryClient.invalidateQueries({ queryKey: ['admin-places'] })}
            />
          )) : <p className="muted">В этом разделе мест пока нет.</p>}
          <ErrorMessage error={places.error} />
        </div>
      </div>
    </section>
  )
}
