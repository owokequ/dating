import { ApiError, apiRequest, jsonBody } from '../../../../shared/api/http'

export type MediaItem = {
  mediaId: string
  status: 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED' | 'DELETED'
  position: number
  cover: boolean
  thumbnailUrl: string | null
  cardUrl: string | null
  detailUrl: string | null
}

export type MediaCollection = {
  placeId: string
  coverMediaId: string | null
  version: number
  images: MediaItem[]
}

export const getPlaceMedia = (placeId: string) => apiRequest<MediaCollection>(
  `/api/v1/media/place-collections/${placeId}`,
)

export const uploadPlaceImage = (placeId: string, file: File) => {
  const body = new FormData()
  body.append('file', file)
  return apiRequest<{ mediaId: string; status: string }>(
    `/api/v1/admin/media/place-collections/${placeId}/assets`,
    { method: 'POST', body },
  )
}

export async function uploadPlaceImageWhenProjectionIsReady(placeId: string, file: File): Promise<void> {
  const maximumAttempts = 8
  for (let attempt = 1; attempt <= maximumAttempts; attempt++) {
    try {
      await uploadPlaceImage(placeId, file)
      return
    } catch (error) {
      const projectionIsNotReady = error instanceof ApiError && error.status === 404
      if (!projectionIsNotReady || attempt === maximumAttempts) throw error
      await new Promise((resolve) => window.setTimeout(resolve, Math.min(250 * 2 ** (attempt - 1), 1000)))
    }
  }
}

export const reorderPlaceMedia = (placeId: string, coverMediaId: string, orderedMediaIds: string[]) =>
  apiRequest<void>(`/api/v1/admin/media/place-collections/${placeId}`, {
    method: 'PATCH',
    body: jsonBody({ coverMediaId, orderedMediaIds }),
  })

export const deletePlaceImage = (placeId: string, mediaId: string) => apiRequest<void>(
  `/api/v1/admin/media/place-collections/${placeId}/assets/${mediaId}`,
  { method: 'DELETE' },
)
