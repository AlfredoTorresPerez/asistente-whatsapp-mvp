import { apiFetch } from './httpClient'
import type { PublicCenterResponse } from './types'

export function getPublicCenterBySlug(slug: string) {
  return apiFetch<PublicCenterResponse>(`/public/centros/${slug}`, { auth: false })
}

export function getPublicCenterWhatsAppRedirectUrl(slug: string) {
  return `/public/centros/${slug}/whatsapp`
}

export function submitContact(slug: string, name: string, phone: string) {
  return apiFetch<{ waUrl: string }>(`/public/centros/${slug}/contacto`, {
    method: 'POST',
    body: JSON.stringify({ name, phone }),
    auth: false,
  })
}
