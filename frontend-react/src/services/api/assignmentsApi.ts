import { apiFetch } from './httpClient'
import type { AssignmentRequest, AssignmentResponse } from './types'

export function listAssignmentsRequest(params: {
  serviceId?: string
  professionalId?: string
  roomId?: string
} = {}) {
  const query = new URLSearchParams()
  if (params.serviceId) query.set('serviceId', params.serviceId)
  if (params.professionalId) query.set('professionalId', params.professionalId)
  if (params.roomId) query.set('roomId', params.roomId)
  const queryString = query.toString()
  return apiFetch<AssignmentResponse[]>(
    `/admin/assignments${queryString ? `?${queryString}` : ''}`,
  )
}

export function assignProfessionalToServiceRequest(payload: AssignmentRequest) {
  return apiFetch<AssignmentResponse>('/admin/assignments/professional-service', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function assignRoomToServiceRequest(payload: AssignmentRequest) {
  return apiFetch<AssignmentResponse>('/admin/assignments/room-service', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function removeAssignmentRequest(assignmentId: string) {
  return apiFetch<void>(`/admin/assignments/${assignmentId}`, {
    method: 'DELETE',
  })
}
