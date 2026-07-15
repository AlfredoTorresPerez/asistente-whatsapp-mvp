import { apiFetch } from './httpClient'
import type {
  AuthUserResponse,
  ForgotPasswordResponse,
  LoginResponse,
  ResetPasswordValidationResponse,
  StatusResponse,
} from './types'

export function loginRequest(email: string, password: string) {
  return apiFetch<LoginResponse>('/auth/login', {
    auth: false,
    body: JSON.stringify({ email, password }),
    method: 'POST',
  })
}

export function meRequest() {
  return apiFetch<AuthUserResponse>('/auth/me')
}

export function logoutRequest() {
  return apiFetch<StatusResponse>('/auth/logout', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function forgotPasswordRequest(email: string) {
  return apiFetch<ForgotPasswordResponse>('/auth/forgot-password', {
    auth: false,
    body: JSON.stringify({ email }),
    method: 'POST',
  })
}

export function validateResetPasswordTokenRequest(token: string) {
  return apiFetch<ResetPasswordValidationResponse>(
    `/auth/reset-password/validate?token=${encodeURIComponent(token)}`,
    {
      auth: false,
    },
  )
}

export function resetPasswordRequest(token: string, newPassword: string, confirmPassword: string) {
  return apiFetch<StatusResponse>('/auth/reset-password', {
    auth: false,
    body: JSON.stringify({ token, newPassword, confirmPassword }),
    method: 'POST',
  })
}
