import { apiFetch } from './httpClient'
import type {
  ChangePasswordRequest,
  PagedResponse,
  StatusResponse,
  UpdateProfileRequest,
  UserProfileResponse,
  AuditLogResponse,
} from './types'

export function getCurrentProfileRequest() {
  return apiFetch<UserProfileResponse>('/users/me')
}

export function updateCurrentProfileRequest(payload: UpdateProfileRequest) {
  return apiFetch<UserProfileResponse>('/users/me', {
    body: JSON.stringify(payload),
    method: 'PATCH',
  })
}

export function changePasswordRequest(payload: ChangePasswordRequest) {
  return apiFetch<StatusResponse>('/users/me/change-password', {
    body: JSON.stringify(payload),
    method: 'POST',
  })
}

export function getAuditLogRequest(page = 0, size = 20) {
  return apiFetch<PagedResponse<AuditLogResponse>>(`/security/audit-log?page=${page}&size=${size}`)
}
