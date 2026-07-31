import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { z } from 'zod'
import { ErrorMessage, Loading, PageTitle } from '../../../../shared/ui/Feedback'
import { useSession } from '../../../auth/api/authApi'
import { getPlaces } from '../../../places/api/placesApi'
import { archivePlace, createPlace } from '../api/adminPlacesApi'
import { adminPlaceSchema, placeCategories } from '../schemas'

type FormValues = z.infer<typeof adminPlaceSchema>

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
  const form = useForm<FormValues>({ resolver: zodResolver(adminPlaceSchema), defaultValues })
  const places = useQuery({
    queryKey: ['places', 'admin'],
    queryFn: () => getPlaces(),
    enabled: isAdmin,
  })
  const create = useMutation({
    mutationFn: (values: FormValues) => createPlace({
      ...values,
      description: values.description || null,
    }),
    onSuccess: async () => {
      form.reset(defaultValues)
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
        <form className="panel admin-place-form" onSubmit={form.handleSubmit((values) => create.mutate(values))}>
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
          {create.isSuccess && <p className="message success">Место создано и поставлено в outbox.</p>}
          <ErrorMessage error={create.error} />
          <button disabled={create.isPending}>{create.isPending ? 'Сохраняем…' : 'Добавить место'}</button>
        </form>

        <div className="panel admin-place-list">
          <div className="section-heading"><h2>Активные места</h2><span>{places.data?.totalElements ?? 0}</span></div>
          {places.isLoading ? <Loading /> : places.data?.items.length ? places.data.items.map((place) => (
            <article key={place.id}>
              <div>
                <span className="eyebrow">{place.category}</span>
                <h3>{place.name}</h3>
                <address>{place.address}</address>
              </div>
              <button
                className="danger small"
                disabled={archive.isPending}
                onClick={() => archive.mutate(place)}
              >Архивировать</button>
            </article>
          )) : <p className="muted">Активных мест пока нет.</p>}
          <ErrorMessage error={places.error ?? archive.error} />
        </div>
      </div>
    </section>
  )
}
