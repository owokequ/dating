export const MAX_PLACE_IMAGES = 5
export const MAX_PLACE_IMAGE_BYTES = 12 * 1024 * 1024
export const PLACE_IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp'

const allowedImageTypes = new Set(PLACE_IMAGE_ACCEPT.split(','))

export type MergeMediaFilesResult = {
  files: File[]
  error: string | null
}

export function mergePlaceImages(current: File[], incoming: File[]): MergeMediaFilesResult {
  const available = MAX_PLACE_IMAGES - current.length
  if (available <= 0) {
    return { files: current, error: `Можно добавить не больше ${MAX_PLACE_IMAGES} фотографий` }
  }

  const accepted: File[] = []
  let error: string | null = null
  for (const file of incoming) {
    if (!allowedImageTypes.has(file.type)) {
      error ??= `Файл «${file.name}» не является JPEG, PNG или WebP`
      continue
    }
    if (file.size > MAX_PLACE_IMAGE_BYTES) {
      error ??= `Файл «${file.name}» больше 12 МБ`
      continue
    }
    if (accepted.length >= available) {
      error ??= `Можно добавить не больше ${MAX_PLACE_IMAGES} фотографий`
      break
    }
    accepted.push(file)
  }

  return { files: [...current, ...accepted], error }
}
