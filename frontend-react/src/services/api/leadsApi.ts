import { apiFetch } from './httpClient'
import type {
  AddLeadNoteRequest,
  CreateLeadFromConversationRequest,
  CreateLeadRequest,
  LeadDetailResponse,
  LeadNoteResponse,
  LeadSummaryResponse,
  PagedResponse,
  UpdateLeadRequest,
  UpdateLeadStageRequest,
} from './types'

type GetLeadsFilters = {
  page?: number
  size?: number
  search?: string
  stage?: string
  origin?: string
  assignedUserId?: string
}

function toSearchParams(filters: Record<string, string | number | undefined | null>) {
  const searchParams = new URLSearchParams()

  Object.entries(filters).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }

    searchParams.set(key, String(value))
  })

  const queryString = searchParams.toString()
  return queryString ? `?${queryString}` : ''
}

export function getLeadsRequest(filters: GetLeadsFilters) {
  return apiFetch<PagedResponse<LeadSummaryResponse>>(`/leads${toSearchParams(filters)}`)
}

export function createLeadRequest(payload: CreateLeadRequest) {
  return apiFetch<LeadDetailResponse>('/leads', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getLeadDetailRequest(leadId: string) {
  return apiFetch<LeadDetailResponse>(`/leads/${leadId}`)
}

export function updateLeadRequest(leadId: string, payload: UpdateLeadRequest) {
  return apiFetch<LeadDetailResponse>(`/leads/${leadId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function addLeadNoteRequest(leadId: string, payload: AddLeadNoteRequest) {
  return apiFetch<LeadNoteResponse>(`/leads/${leadId}/notes`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateLeadStageRequest(leadId: string, payload: UpdateLeadStageRequest) {
  return apiFetch<LeadDetailResponse>(`/leads/${leadId}/stage`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function createLeadFromConversationRequest(
  conversationId: string,
  payload: CreateLeadFromConversationRequest,
) {
  return apiFetch<LeadDetailResponse>(`/conversations/${conversationId}/prospects`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
