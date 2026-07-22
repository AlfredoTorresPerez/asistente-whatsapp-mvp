import { apiFetch } from './httpClient'
import type {
  WhatsAppChannelResponse,
  WhatsAppChannelUpdateRequest,
  WhatsAppChannelValidateResponse,
  WhatsAppChannelTestMessageRequest,
  WhatsAppChannelTestMessageResponse,
  CompleteOnboardingRequest,
  OnboardingResult,
  OnboardingStatus,
} from './types'

export function getWhatsAppChannelRequest() {
  return apiFetch<WhatsAppChannelResponse>('/whatsapp/channel')
}

export function updateWhatsAppChannelRequest(payload: WhatsAppChannelUpdateRequest) {
  return apiFetch<WhatsAppChannelResponse>('/whatsapp/channel', {
    body: JSON.stringify(payload),
    method: 'PUT',
  })
}

export function validateWhatsAppChannelRequest() {
  return apiFetch<WhatsAppChannelValidateResponse>('/whatsapp/channel/validate', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function sendWhatsAppChannelTestMessageRequest(payload: WhatsAppChannelTestMessageRequest) {
  return apiFetch<WhatsAppChannelTestMessageResponse>('/whatsapp/channel/test-message', {
    body: JSON.stringify(payload),
    method: 'POST',
  })
}

export function activateWhatsAppChannelRequest() {
  return apiFetch<WhatsAppChannelResponse>('/whatsapp/channel/activate', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function deactivateWhatsAppChannelRequest() {
  return apiFetch<WhatsAppChannelResponse>('/whatsapp/channel/deactivate', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

// ---- Meta Embedded Signup onboarding ----

export function completeOnboardingRequest(payload: CompleteOnboardingRequest) {
  return apiFetch<OnboardingResult>('/integrations/whatsapp-cloud/onboarding/complete', {
    body: JSON.stringify(payload),
    method: 'POST',
  })
}

export function getOnboardingStatusRequest() {
  return apiFetch<OnboardingStatus>('/integrations/whatsapp-cloud/status')
}

export function revalidateOnboardingRequest() {
  return apiFetch<OnboardingResult>('/integrations/whatsapp-cloud/revalidate', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function disconnectOnboardingRequest() {
  return apiFetch<void>('/integrations/whatsapp-cloud/disconnect', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}
