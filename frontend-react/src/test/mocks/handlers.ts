import { http, HttpResponse } from 'msw'
import {
  mockProfile,
  mockAdminSummary,
  mockSecurityPolicy,
  mockCompanySettings,
  mockAdminUsers,
  mockRoles,
  mockWhatsAppConfig,
  mockBusinessLocations,
  mockMultisiteSummary,
} from './data/profile'

const wc = (path: string) => `*/api/v1${path}`

const defaultUser = {
  id: '40000000-0000-0000-0000-000000000001',
  firstName: 'Carla',
  lastName: 'Mendez',
  email: 'admin@demo.cl',
  role: 'OWNER',
  businessId: '11111111-1111-1111-1111-111111111111',
  businessName: 'Centro Estetico Bella',
  timezone: 'America/Santiago',
  permissions: ['ALL'],
}

export const handlers = [
  http.get(wc('/auth/me'), () => HttpResponse.json(defaultUser)),
  http.get(wc('/users/me'), () => HttpResponse.json(mockProfile)),
  http.patch(wc('/users/me'), async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ ...mockProfile, ...(body as object) })
  }),
  http.post(wc('/users/me/change-password'), () =>
    HttpResponse.json({ status: 'OK' }),
  ),
  http.get(wc('/admin/summary'), () => HttpResponse.json(mockAdminSummary)),
  http.get(wc('/admin/security'), () => HttpResponse.json(mockSecurityPolicy)),
  http.patch(wc('/admin/security'), async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ ...mockSecurityPolicy, ...(body as object) })
  }),
  http.get(wc('/admin/roles'), () => HttpResponse.json(mockRoles)),
  http.get(wc('/admin/users'), () => HttpResponse.json(mockAdminUsers)),
  http.get(wc('/admin/users/:id'), () => HttpResponse.json(mockAdminUsers.items[0])),
  http.post(wc('/admin/users'), async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ id: 'new-id', ...(body as object) }, { status: 201 })
  }),
  http.patch(wc('/admin/users/:id'), async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ ...mockAdminUsers.items[0], ...(body as object) })
  }),
  http.get(wc('/company'), () => HttpResponse.json(mockCompanySettings)),
  http.patch(wc('/company'), async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ ...mockCompanySettings, ...(body as object) })
  }),
  http.get(wc('/configuration/whatsapp'), () => HttpResponse.json(mockWhatsAppConfig)),
  http.patch(wc('/configuration/whatsapp/preferences'), async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ ...mockWhatsAppConfig.preferences, ...(body as object) })
  }),
  http.post(wc('/configuration/whatsapp/connect'), () =>
    HttpResponse.json({ status: 'CONNECTING' }),
  ),
  http.post(wc('/configuration/whatsapp/refresh-qr'), () =>
    HttpResponse.json({ qrBase64: 'data:image/png;base64,iVBORw0KGgo=' }),
  ),
  http.post(wc('/configuration/whatsapp/disconnect'), () =>
    HttpResponse.json({ status: 'DISCONNECTED' }),
  ),
  http.get(wc('/whatsapp-web/status'), () =>
    HttpResponse.json({
      status: 'DISCONNECTED',
      qrCode: null,
      phoneNumber: null,
      businessName: null,
    }),
  ),
  http.post(wc('/whatsapp-web/connect'), () =>
    HttpResponse.json({ status: 'CONNECTING' }),
  ),
  http.post(wc('/whatsapp-web/disconnect'), () =>
    HttpResponse.json({ status: 'DISCONNECTED' }),
  ),
  http.post(wc('/whatsapp-web/refresh-qr'), () =>
    HttpResponse.json({ qrBase64: 'data:image/png;base64,iVBORw0KGgo=' }),
  ),
  http.post(wc('/whatsapp-web/test-message'), () =>
    HttpResponse.json({ status: 'SENT', messageId: 'test-msg-1' }),
  ),
  http.post(wc('/test/whatsapp-inbound'), () =>
    HttpResponse.json({ status: 'PROCESSED' }),
  ),
  http.get(wc('/business-locations'), () =>
    HttpResponse.json(mockBusinessLocations),
  ),
  http.post(wc('/business-locations'), async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ id: 'new-location', ...(body as object) }, { status: 201 })
  }),
  http.put(wc('/business-locations/:id'), async ({ request }) => {
    const body = await request.json()
    return HttpResponse.json({ ...mockBusinessLocations[0], ...(body as object) })
  }),
  http.delete(wc('/business-locations/:id'), () => new HttpResponse(null, { status: 204 })),
  http.get(wc('/multisite/summary'), () => HttpResponse.json(mockMultisiteSummary)),
]
