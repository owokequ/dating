import { apiRequest, idempotencyKey, jsonBody } from '../../../shared/api/http'

export type DateStatus = 'PENDING_CONFIRMATION' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED' | 'COMPLETED'
export type DateProposal = {
  id: string
  coupleId: string
  proposerId: string
  responderId: string
  scheduledAt: string
  timezone: 'Europe/Moscow'
  selectionType: 'PLACE' | 'EVENT'
  placeId: string | null
  placeName: string
  placeAddress: string
  placeCoverMediaId: string | null
  eventId: string | null
  eventOccurrenceId: string | null
  eventTitle: string | null
  eventSourceUrl: string | null
  eventPrice: string | null
  description: string | null
  status: DateStatus
  createdAt: string
  decidedAt: string | null
  cancelledAt: string | null
  version: number
}

export const listDates = () => apiRequest<DateProposal[]>('/api/v1/date-proposals')
export const getDate = (id: string) => apiRequest<DateProposal>(`/api/v1/date-proposals/${id}`)
export const createDate = (input: { scheduledAt: string; placeId: string; description?: string }) =>
  apiRequest<DateProposal>('/api/v1/date-proposals', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey() },
    body: jsonBody(input),
  })
export const createDateFromEvent = (input: { eventOccurrenceId: string; visitAt?: string; description?: string }) =>
  apiRequest<DateProposal>('/api/v1/date-proposals/from-event', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey() },
    body: jsonBody(input),
  })
export const decideDate = (id: string, action: 'accept' | 'decline' | 'cancel') =>
  apiRequest<DateProposal>(`/api/v1/date-proposals/${id}/${action}`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey() },
  })
