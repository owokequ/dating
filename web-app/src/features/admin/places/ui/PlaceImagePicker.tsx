import { useEffect, useMemo, useRef, useState } from 'react'
import {
  MAX_PLACE_IMAGES,
  PLACE_IMAGE_ACCEPT,
  mergePlaceImages,
} from '../mediaFiles'

type Props = {
  files: File[]
  disabled?: boolean
  onChange: (files: File[]) => void
}

export function PlaceImagePicker({ files, disabled = false, onChange }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  const previews = useMemo(() => files.map((file) => ({
    file,
    url: URL.createObjectURL(file),
  })), [files])

  useEffect(() => () => previews.forEach(({ url }) => URL.revokeObjectURL(url)), [previews])

  const move = (index: number, direction: -1 | 1) => {
    const nextIndex = index + direction
    if (nextIndex < 0 || nextIndex >= files.length) return
    const next = [...files]
    ;[next[index], next[nextIndex]] = [next[nextIndex], next[index]]
    onChange(next)
  }

  return (
    <fieldset className="place-image-picker" disabled={disabled}>
      <div className="place-image-picker-heading">
        <div>
          <strong>Фотографии · {files.length}/{MAX_PLACE_IMAGES}</strong>
          <small>Первая фотография станет обложкой.</small>
        </div>
        <button className="secondary small" type="button"
          disabled={disabled || files.length >= MAX_PLACE_IMAGES}
          onClick={() => inputRef.current?.click()}>
          Выбрать фото
        </button>
        <input ref={inputRef} className="visually-hidden" type="file" accept={PLACE_IMAGE_ACCEPT} multiple
          onChange={(event) => {
            const result = mergePlaceImages(files, Array.from(event.target.files ?? []))
            onChange(result.files)
            setError(result.error)
            event.target.value = ''
          }} />
      </div>

      {previews.length > 0 && (
        <div className="place-image-preview-grid">
          {previews.map(({ file, url }, index) => (
            <article className="place-image-preview" key={`${file.name}-${file.lastModified}-${index}`}>
              <img src={url} alt={file.name} />
              <span>{index === 0 ? 'Обложка' : `${index + 1}`}</span>
              <div>
                <button className="secondary media-icon" type="button" aria-label="Переместить влево"
                  disabled={disabled || index === 0} onClick={() => move(index, -1)}>←</button>
                <button className="secondary media-icon" type="button" aria-label="Переместить вправо"
                  disabled={disabled || index === files.length - 1} onClick={() => move(index, 1)}>→</button>
                <button className="danger media-icon" type="button" aria-label="Убрать фотографию"
                  disabled={disabled} onClick={() => onChange(files.filter((_, fileIndex) => fileIndex !== index))}>×</button>
              </div>
            </article>
          ))}
        </div>
      )}
      {error && <p className="message error">{error}</p>}
    </fieldset>
  )
}
