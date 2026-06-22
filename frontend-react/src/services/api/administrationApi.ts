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
  WhatsAppWebActionResponse,
  WhatsAppWebStatusResponse,
  WhatsAppWebTestMessageRequest,
  WhatsAppWebTestMessageResponse,
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

export function getWhatsAppWebStatusRequest() {
  return apiFetch<WhatsAppWebStatusResponse>('/whatsapp-web/status')
}

export function connectWhatsAppWebRequest() {
  return apiFetch<WhatsAppWebActionResponse>('/whatsapp-web/connect', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function refreshWhatsAppWebQrRequest() {
  return apiFetch<WhatsAppWebActionResponse>('/whatsapp-web/refresh-qr', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function disconnectWhatsAppWebRequest() {
  return apiFetch<WhatsAppWebActionResponse>('/whatsapp-web/disconnect', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function sendWhatsAppWebTestMessageRequest(payload: WhatsAppWebTestMessageRequest) {
  return apiFetch<WhatsAppWebTestMessageResponse>('/whatsapp-web/test-message', {
    body: JSON.stringify(payload),
    method: 'POST',
  })
}
