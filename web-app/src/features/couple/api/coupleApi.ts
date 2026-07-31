import { ApiError, apiRequest } from '../../../shared/api/http'

export type CoupleMember = { userId: string; role: 'OWNER' | 'PARTNER'; joinedAt: string }
export type Couple = {
  id: string
  status: 'PENDING' | 'ACTIVE' | 'CLOSED'
  members: CoupleMember[]
  createdAt: string
  activatedAt: string | null
  version: number
}
export type Invitation = { invitationId: string; inviteUrl: string; expiresAt: string }

export async function getCurrentCouple(): Promise<Couple | null> {
  try {
    return await apiRequest<Couple>('/api/v1/couples/current')
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null
    throw error
  }
}

export const createInvitation = () => apiRequest<Invitation>('/api/v1/couple-invitations', { method: 'POST' })
export const closeCouple = () => apiRequest<void>('/api/v1/couples/current/close', { method: 'POST' })
