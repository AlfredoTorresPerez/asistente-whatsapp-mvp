import { apiFetch } from './httpClient'
import type {
  UpdateWhatsAppConfigurationPreferencesRequest,
  WhatsAppConfigurationResponse,
} from './types'

export function getWhatsAppConfigurationRequest() {
  return apiFetch<WhatsAppConfigurationResponse>('/configuration/whatsapp')
}

export function updateWhatsAppConfigurationPreferencesRequest(
  payload: UpdateWhatsAppConfigurationPreferencesRequest,
) {
  return apiFetch<WhatsAppConfigurationResponse>('/configuration/whatsapp/preferences', {
    body: JSON.stringify(payload),
    method: 'PATCH',
  })
}

export function connectWhatsAppConfigurationRequest() {
  return apiFetch<WhatsAppConfigurationResponse>('/configuration/whatsapp/connect', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}

export function disconnectWhatsAppConfigurationRequest() {
  return apiFetch<WhatsAppConfigurationResponse>('/configuration/whatsapp/disconnect', {
    body: JSON.stringify({}),
    method: 'POST',
  })
}
