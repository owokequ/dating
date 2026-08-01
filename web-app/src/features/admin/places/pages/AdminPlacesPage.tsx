import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useState } from 'react'
import type { z } from 'zod'
import { ErrorMessage, Loading, PageTitle } from '../../../../shared/ui/Feedback'
import { useSession } from '../../../auth/api/authApi'
import { getPlaces } from '../../../places/api/placesApi'
import { archivePlace, createPlace } from '../api/adminPlacesApi'
import { uploadPlaceImageWhenProjectionIsReady } from '../api/adminMediaApi'
import { adminPlaceSchema, placeCategories } from '../schemas'
import { PlaceImagePicker } from '../ui/PlaceImagePicker'
import { PlaceMediaManager } from '../ui/PlaceMediaManager'

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

export function AdminPlacesPage() {
  const session = useSession()
  const isAdmin = session.data?.role === 'ADMIN'
  const queryClient = useQueryClient()
  const [images, setImages] = useState<File[]>([])
  const form = useForm<FormValues>({ resolver: zodResolver(adminPlaceSchema), defaultValues })
  const places = useQuery({
    queryKey: ['places', 'admin'],
    queryFn: () => getPlaces(),
    enabled: isAdmin,
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
      await queryClient.invalidateQueries({ queryKey: ['places'] })
    },
  })
  const archive = useMutation({
    mutationFn: archivePlace,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['places'] }),
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
        Создание и архивирование проходят через Places Service, поэтому каждое изменение публикуется в Kafka.
      </PageTitle>
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
          <div className="section-heading"><h2>Активные места</h2><span>{places.data?.totalElements ?? 0}</span></div>
          {places.isLoading ? <Loading /> : places.data?.items.length ? places.data.items.map((place) => (
            <article className="admin-place-item" key={place.id}>
              <div className="admin-place-summary">
                <div>
                <span className="eyebrow">{place.category}</span>
                <h3>{place.name}</h3>
                <address>{place.address}</address>
                </div>
                <button className="danger small" disabled={archive.isPending}
                  onClick={() => archive.mutate(place)}>Архивировать</button>
              </div>
              <PlaceMediaManager placeId={place.id} placeName={place.name} />
            </article>
          )) : <p className="muted">Активных мест пока нет.</p>}
          <ErrorMessage error={places.error ?? archive.error} />
        </div>
      </div>
    </section>
  )
}
