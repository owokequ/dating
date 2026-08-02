import { useMutation } from '@tanstack/react-query'
import { ImagePlus, MapPin, Send } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { toMoscowIso } from '../../../shared/lib/date'
import { ErrorMessage, PageTitle } from '../../../shared/ui/Feedback'
import { createPrivatePlaceDraft, getDate, sendPrivatePlaceDraft, uploadPrivatePlaceCover } from '../api/datesApi'

const sleep = (milliseconds: number) => new Promise((resolve) => window.setTimeout(resolve, milliseconds))

export function PrivatePlaceDatePage() {
  const [placeName, setPlaceName] = useState('')
  const [address, setAddress] = useState('')
  const [scheduledAt, setScheduledAt] = useState('')
  const [description, setDescription] = useState('')
  const [cover, setCover] = useState<File | null>(null)
  const previewUrl = useMemo(() => cover ? URL.createObjectURL(cover) : null, [cover])
  useEffect(() => () => { if (previewUrl) URL.revokeObjectURL(previewUrl) }, [previewUrl])
  const create = useMutation({
    mutationFn: async () => {
      const draft = await createPrivatePlaceDraft({ scheduledAt: toMoscowIso(scheduledAt), placeName, placeAddress: address || undefined, description: description || undefined })
      if (cover) {
        await uploadPrivatePlaceCover(draft.id, cover)
        for (let attempt = 0; attempt < 20; attempt += 1) {
          await sleep(500)
          if ((await getDate(draft.id)).placeCoverMediaId) break
          if (attempt === 19) throw new Error('Не удалось подготовить фотографию. Попробуйте отправить приглашение ещё раз.')
        }
      }
      return sendPrivatePlaceDraft(draft.id)
    },
    onSuccess: (proposal) => window.location.assign(`/dates/${proposal.id}`),
  })
  return <section className="form-page">
    <PageTitle eyebrow="Только для вас двоих" title="Пригласить в своё место">Этого места не будет в общем каталоге — приглашение увидит только ваш человек.</PageTitle>
    <form className="panel date-proposal-form" onSubmit={(event) => { event.preventDefault(); create.mutate() }}>
      <div className="form-steps"><span className="active">1 · место</span><span className="active">2 · время</span><span className="active">3 · открытка</span></div>
      <section className="date-form-step"><span className="step-number">01</span><div className="step-content private-place-fields"><label>Как называется это место<input value={placeName} onChange={(event) => setPlaceName(event.target.value)} maxLength={300} required placeholder="Например, наша крыша" /></label><label>Адрес <small>необязательно</small><input value={address} onChange={(event) => setAddress(event.target.value)} maxLength={500} placeholder="Где вас ждать" /></label></div></section>
      <section className="date-form-step"><span className="step-number">02</span><div className="step-content"><label>Когда встречаемся (Москва)<input type="datetime-local" value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} required /></label></div></section>
      <section className="date-form-step"><span className="step-number">03</span><div className="step-content private-place-fields"><label>Личная записка <small>необязательно</small><textarea maxLength={1000} rows={4} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Почему именно сюда?" /></label><label className="private-cover-input"><span><ImagePlus size={18} /> Фотография-обложка <small>необязательно</small></span><input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => setCover(event.target.files?.[0] ?? null)} /></label>{previewUrl ? <img className="private-cover-preview" src={previewUrl} alt="Предпросмотр обложки" /> : <div className="private-cover-fallback"><MapPin size={25} /> Без фото используем фирменную открытку For my L</div>}<small>{description.length}/1000</small></div></section>
      <ErrorMessage error={create.error} /><button className="date-submit" disabled={create.isPending}>{create.isPending ? 'Готовим открытку…' : <><Send size={17} /> Отправить приглашение</>}</button>
    </form>
  </section>
}
