import { apiFetch } from './httpClient'
import type {
  AdminRoleResponse,
  AdminSummaryResponse,
  AdminUserRequest,
  AdminUserResponse,
  CompanySettingsRequest,
  CompanySettingsResponse,
  PagedResponse,
  SecurityPolicyRequest,
  SecurityPolicyResponse,
  StatusResponse,
  WhatsAppChannelActionResponse,
  WhatsAppChannelStatusResponse,
  WhatsAppChannelTestMessageRequest,
  WhatsAppChannelTestMessageResponse,
} from './types'

export function getAdminSummaryRequest() {
  return apiFetch<AdminSummaryResponse>('/admin/summary')
}

export type AdminUserListParams = {
  page?: number
  role?: string
  search?: string
  size?: number
  status?: string
}

export function listAdminUsersRequest(params: AdminUserListParams = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 50))
  if (params.search) query.set('search', params.search)
  if (params.role) query.set('role', params.role)
  if (params.status) query.set('status', params.status)

  return apiFetch<PagedResponse<AdminUserResponse>>(`/admin/users?${query.toString()}`)
}

export function getAdminUserRequest(userId: string) {
  return apiFetch<AdminUserResponse>(`/admin/users/${userId}`)
}

export function createAdminUserRequest(payload: AdminUserRequest) {
  return apiFetch<AdminUserResponse>('/admin/users', {
    body: JSON.stringify(payload),
    method: 'POST',
  })
}

export function updateAdminUserRequest(userId: string, payload: AdminUserRequest) {
  return apiFetch<AdminUserResponse>(`/admin/users/${userId}`, {
    body: JSON.stringify(payload),
    method: 'PATCH',
  })
}

export function deactivateAdminUserRequest(userId: string) {
  return apiFetch<StatusResponse>(`/admin/users/${userId}/deactivate`, {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function revokeAdminUserSessionsRequest(userId: string) {
  return apiFetch<StatusResponse>(`/admin/users/${userId}/revoke-sessions`, {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function resetAdminUserAccessRequest(userId: string) {
  return apiFetch<StatusResponse>(`/admin/users/${userId}/reset-access`, {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function listAdminRolesRequest() {
  return apiFetch<AdminRoleResponse[]>('/admin/roles')
}

export function getSecurityPolicyRequest() {
  return apiFetch<SecurityPolicyResponse>('/admin/security')
}

export function updateSecurityPolicyRequest(payload: SecurityPolicyRequest) {
  return apiFetch<SecurityPolicyResponse>('/admin/security', {
    body: JSON.stringify(payload),
    method: 'PATCH',
  })
}

export function getCompanySettingsRequest() {
  return apiFetch<CompanySettingsResponse>('/company')
}

export function updateCompanySettingsRequest(payload: CompanySettingsRequest) {
  return apiFetch<CompanySettingsResponse>('/company', {
    body: JSON.stringify(payload),
    method: 'PATCH',
  })
}

export function getWhatsAppChannelStatusRequest() {
  return apiFetch<WhatsAppChannelStatusResponse>('/whatsapp-channel/status')
}

export function connectWhatsAppChannelRequest() {
  return apiFetch<WhatsAppChannelActionResponse>('/whatsapp-channel/connect', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function disconnectWhatsAppChannelRequest() {
  return apiFetch<WhatsAppChannelActionResponse>('/whatsapp-channel/disconnect', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function sendWhatsAppChannelTestMessageRequest(payload: WhatsAppChannelTestMessageRequest) {
  return apiFetch<WhatsAppChannelTestMessageResponse>('/whatsapp-channel/test-message', {
    body: JSON.stringify(payload),
    method: 'POST',
  })
}

export type WhatsAppSimulationRequest = {
  from: string
  body: string
  externalMessageId: string
  sessionKey?: string
}

export function sendWhatsAppSimulationRequest(payload: WhatsAppSimulationRequest) {
  return apiFetch<StatusResponse>('/test/whatsapp-inbound', {
    body: JSON.stringify(payload),
    method: 'POST',
  })
}
