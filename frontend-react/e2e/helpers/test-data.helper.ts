import { apiPost, apiGet, loginDemoAdmin } from './api.helper'
import type { APIResponse } from '@playwright/test'

const QA_PREFIX = 'QA_AUTO_'

let authToken: string | null = null

export function setAuthToken(token: string) {
  authToken = token
}

export async function resolveAuthToken(): Promise<string | null> {
  if (authToken) return authToken
  authToken = await loginDemoAdmin()
  return authToken
}

export function getAuthToken(): string | null {
  return authToken
}

export function qaId(name: string): string {
  return `${QA_PREFIX}${name}`
}

export function qaPhone(index: number): string {
  return `+5690000${String(index).padStart(3, '0')}`
}

export function isQaData(value: string): boolean {
  return value?.startsWith(QA_PREFIX) ?? false
}

export async function createQaClient(index = 1): Promise<{ id: string } | null> {
  try {
    const resp = await apiPost('/customers', {
      displayName: qaId(`CLIENTE_${index}`),
      phone: qaPhone(index),
      email: `qa_auto_${index}@test.cl`,
    }, authToken ?? await resolveAuthToken() ?? undefined)
    if (resp.ok) return await resp.json() as { id: string }
    return null
  } catch { return null }
}

export async function createQaBooking(overrides?: Record<string, unknown>): Promise<APIResponse> {
  const payload = {
    subject: qaId('RESERVA'),
    customerName: qaId('CLIENTE_1'),
    customerPhone: qaPhone(1),
    startsAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
    durationMinutes: 60,
    locationId: overrides?.locationId ?? null,
    status: 'PENDIENTE_CONFIRMACION',
    ...overrides,
  }
  return apiPost('/bookings', payload, authToken ?? await resolveAuthToken() ?? undefined)
}

export async function createQaBookingWithStatus(status: string): Promise<APIResponse> {
  return createQaBooking({ status })
}

export async function cleanupQaData() {
  try {
    const token = authToken ?? await resolveAuthToken()
    const resp = await apiGet('/bookings?search=QA_AUTO_', token ?? undefined)
    if (resp.ok) {
      const body = await resp.json() as { items?: Array<{ id: string }> }
      for (const item of body.items ?? []) {
        await apiPost(`/bookings/${item.id}/cancel`, { reason: 'QA cleanup' }, token ?? undefined)
      }
    }
  } catch { /* silent cleanup */ }
}
