import { apiRequest } from '../../../shared/api/http'
import type { Couple } from '../../couple/api/coupleApi'

export type InvitationPreview = { invitationId: string; expiresAt: string }
export const previewInvitation = (token: string) =>
  apiRequest<InvitationPreview>(`/api/v1/couple-invitations/${encodeURIComponent(token)}`)
export const acceptInvitation = (token: string) =>
  apiRequest<Couple>(`/api/v1/couple-invitations/${encodeURIComponent(token)}/accept`, { method: 'POST' })
