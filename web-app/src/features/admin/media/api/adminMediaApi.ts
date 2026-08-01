import { ApiError, apiRequest, jsonBody } from '../../../../shared/api/http'

export type MediaOwnerType = 'PLACE' | 'EVENT'

export type MediaItem = {
  mediaId: string
  source: 'UPLOAD' | 'REMOTE_URL'
  status: 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED' | 'DELETED'
  position: number
  cover: boolean
  thumbnailUrl: string | null
  cardUrl: string | null
  detailUrl: string | null
  sourceName: string | null
  sourceLink: string | null
}

export type MediaCollection = {
  ownerType: MediaOwnerType
  ownerId: string
  coverMediaId: string | null
  version: number
  images: MediaItem[]
}

const segment = (ownerType: MediaOwnerType) => ownerType === 'PLACE' ? 'place-collections' : 'event-collections'

export const getMedia = (ownerType: MediaOwnerType, ownerId: string) => apiRequest<MediaCollection>(
  `/api/v1/admin/media/${segment(ownerType)}/${ownerId}`,
)

export const uploadImage = (ownerType: MediaOwnerType, ownerId: string, file: File) => {
  const body = new FormData()
  body.append('file', file)
  return apiRequest<{ mediaId: string; status: string }>(
    `/api/v1/admin/media/${segment(ownerType)}/${ownerId}/assets`,
    { method: 'POST', body },
  )
}

export async function uploadImageWhenProjectionIsReady(ownerType: MediaOwnerType, ownerId: string, file: File): Promise<void> {
  const maximumAttempts = 8
  for (let attempt = 1; attempt <= maximumAttempts; attempt++) {
    try {
      await uploadImage(ownerType, ownerId, file)
      return
    } catch (error) {
      const projectionIsNotReady = error instanceof ApiError && error.status === 404
      if (!projectionIsNotReady || attempt === maximumAttempts) throw error
      await new Promise((resolve) => window.setTimeout(resolve, Math.min(250 * 2 ** (attempt - 1), 1000)))
    }
  }
}

export const reorderMedia = (ownerType: MediaOwnerType, ownerId: string, coverMediaId: string, orderedMediaIds: string[]) =>
  apiRequest<void>(`/api/v1/admin/media/${segment(ownerType)}/${ownerId}`, {
    method: 'PATCH',
    body: jsonBody({ coverMediaId, orderedMediaIds }),
  })

export const deleteImage = (ownerType: MediaOwnerType, ownerId: string, mediaId: string) => apiRequest<void>(
  `/api/v1/admin/media/${segment(ownerType)}/${ownerId}/assets/${mediaId}`,
  { method: 'DELETE' },
)

export const uploadPlaceImageWhenProjectionIsReady = (placeId: string, file: File) =>
  uploadImageWhenProjectionIsReady('PLACE', placeId, file)
