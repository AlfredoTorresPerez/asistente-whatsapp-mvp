import { test, expect } from '@playwright/test'

const VALID_SESSION = {
  accessToken: 'qa-auto-sec-token',
  expiresAt: new Date(Date.now() + 86400000).toISOString(),
  user: {
    id: 'qa-auto-sec-user', name: 'QA Security', firstName: 'QA', lastName: 'Security',
    email: 'qa_security@demo.cl', role: 'OWNER', businessId: 'qa-auto-biz-sec',
    businessName: 'QA Security Center', timezone: 'America/Santiago', phone: null,
  },
}

test.describe('NIVEL 14 — Seguridad y control de acceso', () => {

  test('QA-14-001: Ruta protegida redirige a login sin sesion', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForURL('**/login', { timeout: 10000 })
    expect(page.url()).toContain('/login')
  })

  test('QA-14-002: Pagina publica de confirmacion funciona sin auth', async ({ page }) => {
    await page.route(/\/api\/v1\/public\/booking-confirmations\//, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        bookingId: 'qa-auto-sec-booking',
        serviceName: 'Test seguridad',
        locationName: 'QA Sucursal',
        startsAt: new Date(Date.now() + 86400000).toISOString(),
        durationMinutes: 60,
        customerName: 'QA_SEC',
        bookingStatus: 'PENDIENTE_CONFIRMACION',
        linkStatus: 'ACTIVE',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
      }) })
    })
    await page.goto('/reservas/confirmar/qa-auto-sec-token')
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Test seguridad')).toBeVisible({ timeout: 10000 })
    expect(page.url()).not.toContain('/login')
  })

  test('QA-14-003: Pagina login se renderiza correctamente', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    const passwordField = page.locator('input[type="password"]')
    await expect(passwordField).toBeVisible({ timeout: 10000 })
    const emailField = page.locator('input[type="email"]')
    await expect(emailField).toBeVisible({ timeout: 5000 })
  })

  test('QA-14-004: Token expirado redirige a login', async ({ page }) => {
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'qa-auto-expired-token',
        expiresAt: new Date(Date.now() - 3600000).toISOString(),
        user: { id: 'expired', name: 'Expired', firstName: 'Expired', lastName: 'User',
          email: 'expired@demo.cl', role: 'OWNER', businessId: 'biz-expired',
          businessName: 'Expired Biz', timezone: 'America/Santiago', phone: null },
      }))
    })
    await page.goto('/agenda')
    await page.waitForURL('**/login', { timeout: 10000 })
    expect(page.url()).toContain('/login')
  })

  test('QA-14-005: API 401 limpia sesion y redirige a login', async ({ page }) => {
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'qa-auto-invalid-token',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: { id: 'invalid', name: 'Invalid', firstName: 'Invalid', lastName: 'Token',
          email: 'invalid@demo.cl', role: 'OWNER', businessId: 'biz-invalid',
          businessName: 'Invalid Biz', timezone: 'America/Santiago', phone: null },
      }))
    })
    await page.route(/\/api\/v1\/auth\/me(\?|$)/, async (route) => {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({
        code: 'UNAUTHORIZED', message: 'Token invalido o expirado',
      }) })
    })
    await page.route(/\/api\/v1\/users\/me(\?|$)/, async (route) => {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({
        code: 'UNAUTHORIZED', message: 'Token invalido o expirado',
      }) })
    })
    await page.goto('/agenda')
    await page.waitForURL('**/login', { timeout: 10000 })
    expect(page.url()).toContain('/login')
  })

  test('QA-14-006: Input de password usa type="password"', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    const passwordInputs = page.locator('input[type="password"]')
    const count = await passwordInputs.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('QA-14-007: Sesion no se persiste en localStorage', async ({ page }) => {
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'qa-auto-check-storage',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: { id: 'storage', name: 'Storage', firstName: 'Storage', lastName: 'Check',
          email: 'storage@demo.cl', role: 'OWNER', businessId: 'biz-storage',
          businessName: 'Storage Check', timezone: 'America/Santiago', phone: null },
      }))
    })
    await page.route(/\/api\/v1\/auth\/me(\?|$)/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-sec-user', firstName: 'QA', lastName: 'Security', email: 'qa_security@demo.cl',
        role: 'OWNER', businessId: 'qa-auto-biz-sec', businessName: 'QA Security Center', timezone: 'America/Santiago',
      }) })
    })
    await page.route(/\/api\/v1\/users\/me(\?|$)/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-sec-user', firstName: 'QA', lastName: 'Security', email: 'qa_security@demo.cl',
        phone: null, timezone: 'America/Santiago', role: 'OWNER',
      }) })
    })
    await page.route(/\/api\/v1\/notifications/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route(/\/api\/v1\/dashboard\/summary/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
    })
    await page.route(/\/api\/v1\/business-locations/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
    })
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
    const hasLocalStorageKey = await page.evaluate(() => {
      for (let i = 0; i < window.localStorage.length; i++) {
        const key = window.localStorage.key(i)
        if (key?.includes('asistente') || key?.includes('session') || key?.includes('token')) {
          return true
        }
      }
      return false
    })
    expect(hasLocalStorageKey).toBeFalsy()
  })

  test('QA-14-008: Header Authorization incluye Bearer token', async ({ page }) => {
    let authHeaderValue: string | null = null
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'qa-auto-bearer-test',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: { id: 'bearer', name: 'Bearer', firstName: 'Bearer', lastName: 'Test',
          email: 'bearer@demo.cl', role: 'OWNER', businessId: 'biz-bearer',
          businessName: 'Bearer Test', timezone: 'America/Santiago', phone: null },
      }))
    })
    await page.route(/\/api\/v1\/auth\/me(\?|$)/, async (route) => {
      authHeaderValue = route.request().headers()['authorization'] ?? null
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-sec-user', firstName: 'QA', lastName: 'Security', email: 'qa_security@demo.cl',
        role: 'OWNER', businessId: 'qa-auto-biz-sec', businessName: 'QA Security Center', timezone: 'America/Santiago',
      }) })
    })
    await page.goto('/agenda')
    await page.waitForLoadState('networkidle')
    expect(authHeaderValue).not.toBeNull()
    expect(authHeaderValue).toMatch(/^Bearer\s+/)
  })

  test('QA-14-009: Auth header contiene el token correcto', async ({ page }) => {
    let authHeaderValue: string | null = null
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'qa-auto-exact-token-abc',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: { id: 'exact', name: 'Exact', firstName: 'Exact', lastName: 'Token',
          email: 'exact@demo.cl', role: 'OWNER', businessId: 'biz-exact',
          businessName: 'Exact Token', timezone: 'America/Santiago', phone: null },
      }))
    })
    await page.route(/\/api\/v1\/auth\/me(\?|$)/, async (route) => {
      authHeaderValue = route.request().headers()['authorization'] ?? null
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-sec-user', firstName: 'QA', lastName: 'Security', email: 'qa_security@demo.cl',
        role: 'OWNER', businessId: 'qa-auto-biz-sec', businessName: 'QA Security Center', timezone: 'America/Santiago',
      }) })
    })
    await page.goto('/agenda')
    await page.waitForLoadState('networkidle')
    expect(authHeaderValue).toBe('Bearer qa-auto-exact-token-abc')
  })

  test('QA-14-010: Login no expone stack traces en HTML', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    const html = await page.content()
    const stackIndicators = ['at ', '.ts:', '.tsx:', 'node_modules', 'Error:']
    for (const indicator of stackIndicators) {
      expect(html.includes(indicator)).toBeFalsy()
    }
  })
})
