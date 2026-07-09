import { apiPost } from './api.helper'

let SIMULATOR_ENABLED = true

export function setSimulatorEnabled(enabled: boolean) {
  SIMULATOR_ENABLED = enabled
}

export async function sendInboundWhatsAppMessage(customerPhone: string, message: string, sessionKey?: string): Promise<{ ok: boolean; error?: string }> {
  try {
    const body: Record<string, unknown> = {
      from: customerPhone,
      body: message,
    }
    if (sessionKey) body.sessionKey = sessionKey
    const resp = await apiPost('/test/whatsapp-inbound', body)
    return { ok: resp.ok, error: resp.ok ? undefined : await resp.text() }
  } catch (error) {
    return { ok: false, error: String(error) }
  }
}

export function isWhatsAppSimulatorAvailable(): boolean {
  return SIMULATOR_ENABLED
}
