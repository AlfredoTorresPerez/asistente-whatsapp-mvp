import { apiFetch } from './httpClient'
import type {
  AssignConversationRequest,
  ConversationAiReplyResponse,
  ConversationDetailResponse,
  ConversationMessageResponse,
  ConversationMetricsResponse,
  ConversationSummaryResponse,
  CreateConversationRequest,
  CreateResponseTemplateRequest,
  PagedResponse,
  ResponseTemplateResponse,
  SendConversationMessageRequest,
  UpdateResponseTemplateRequest,
  UpdateTemplateStatusRequest,
} from './types'

type GetConversationsFilters = {
  page?: number
  size?: number
  search?: string
  status?: string
  ownerUserId?: string
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

export function getConversationMetricsRequest() {
  return apiFetch<ConversationMetricsResponse>('/conversations/metrics')
}

export function getConversationsRequest(filters: GetConversationsFilters) {
  return apiFetch<PagedResponse<ConversationSummaryResponse>>(
    `/conversations${toSearchParams(filters)}`,
  )
}

export function getConversationDetailRequest(conversationId: string) {
  return apiFetch<ConversationDetailResponse>(`/conversations/${conversationId}`)
}

export function createConversationRequest(payload: CreateConversationRequest) {
  return apiFetch<ConversationDetailResponse>('/conversations', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function sendConversationMessageRequest(
  conversationId: string,
  payload: SendConversationMessageRequest,
) {
  return apiFetch<ConversationMessageResponse>(`/conversations/${conversationId}/messages`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function assignConversationRequest(
  conversationId: string,
  payload: AssignConversationRequest,
) {
  return apiFetch<ConversationDetailResponse>(`/conversations/${conversationId}/assign`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function markConversationReadRequest(conversationId: string) {
  return apiFetch<ConversationDetailResponse>(`/conversations/${conversationId}/mark-read`, {
    method: 'POST',
  })
}

export function previewAiReplyRequest(conversationId: string) {
  return apiFetch<ConversationAiReplyResponse>(`/conversations/${conversationId}/preview-ai`, {
    method: 'POST',
  })
}

export function closeConversationRequest(conversationId: string) {
  return apiFetch<ConversationDetailResponse>(`/conversations/${conversationId}/close`, {
    method: 'POST',
  })
}

export function reopenConversationRequest(conversationId: string) {
  return apiFetch<ConversationDetailResponse>(`/conversations/${conversationId}/reopen`, {
    method: 'POST',
  })
}

export function getResponseTemplatesRequest(active?: boolean) {
  return apiFetch<ResponseTemplateResponse[]>(
    `/templates${toSearchParams({ active: active === undefined ? undefined : String(active) })}`,
  )
}

export function createResponseTemplateRequest(payload: CreateResponseTemplateRequest) {
  return apiFetch<ResponseTemplateResponse>('/templates', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateResponseTemplateRequest(
  templateId: string,
  payload: UpdateResponseTemplateRequest,
) {
  return apiFetch<ResponseTemplateResponse>(`/templates/${templateId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function updateTemplateStatusRequest(
  templateId: string,
  payload: UpdateTemplateStatusRequest,
) {
  return apiFetch<ResponseTemplateResponse>(`/templates/${templateId}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}
