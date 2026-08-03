import { apiFetch } from './httpClient'
import type {
  AssignmentActiveRequest,
  AssignmentGroupResponse,
  AssignmentRequest,
  AssignmentResponse,
  AssignmentSummaryResponse,
  PagedResponse,
} from './types'

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

export type AssignmentGroupsParams = {
  page?: number
  size?: number
  search?: string
  serviceId?: string
  locationId?: string
  categoryCode?: string
  professionalId?: string
  roomId?: string
  coverage?: 'covered' | 'partial' | 'none' | ''
}

export function listAssignmentGroupsRequest(params: AssignmentGroupsParams = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 10))
  if (params.search) query.set('search', params.search)
  if (params.serviceId) query.set('serviceId', params.serviceId)
  if (params.locationId) query.set('locationId', params.locationId)
  if (params.categoryCode) query.set('categoryCode', params.categoryCode)
  if (params.professionalId) query.set('professionalId', params.professionalId)
  if (params.roomId) query.set('roomId', params.roomId)
  if (params.coverage) query.set('coverage', params.coverage)
  return apiFetch<PagedResponse<AssignmentGroupResponse>>(`/admin/assignments/groups?${query.toString()}`)
}

export function getAssignmentsSummaryRequest() {
  return apiFetch<AssignmentSummaryResponse>('/admin/assignments/summary')
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

export function setAssignmentActiveRequest(assignmentId: string, payload: AssignmentActiveRequest) {
  return apiFetch<AssignmentResponse>(`/admin/assignments/${assignmentId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function removeAssignmentRequest(assignmentId: string) {
  return apiFetch<void>(`/admin/assignments/${assignmentId}`, {
    method: 'DELETE',
  })
}
