import { test, expect } from '@playwright/test'

const MOCK_AUTH_USER = {
  id: 'user-001',
  firstName: 'Admin',
  lastName: 'Demo',
  email: 'admin@demo.cl',
  role: 'OWNER',
  businessId: 'biz-001',
  businessName: 'Centro Estetica Demo',
  timezone: 'America/Santiago',
}

const MOCK_PROFILE_USER = {
  id: 'user-001',
  firstName: 'Admin',
  lastName: 'Demo',
  email: 'admin@demo.cl',
  phone: null,
  timezone: 'America/Santiago',
  role: 'OWNER',
  businessName: 'Centro Estetica Demo',
}

const MOCK_LOCATIONS = [
  {
    id: 'loc-001',
    code: 'principal',
    name: 'Sucursal Central Maipu',
    address: 'Av. Los Pajaritos 1234, Local 5, Maipu',
    city: 'Santiago',
    commune: 'Maipu',
    phone: '+56 9 1234 5678',
    whatsappNumber: '+56 9 1234 5678',
    timezone: 'America/Santiago',
    active: true,
  },
  {
    id: 'loc-002',
    code: 'suc-norte',
    name: 'Sucursal Norte Huechuraba',
    address: 'Av. Del Valle 850, Local 12',
    city: 'Santiago',
    commune: 'Huechuraba',
    phone: '+56 9 2345 6789',
    whatsappNumber: '+56 9 2345 6789',
    timezone: 'America/Santiago',
    active: true,
  },
  {
    id: 'loc-003',
    code: 'suc-providencia',
    name: 'Sucursal Providencia',
    address: 'Av. Providencia 2345, Piso 3, Of. 304',
    city: 'Santiago',
    commune: 'Providencia',
    phone: '+56 9 3456 7890',
    whatsappNumber: '+56 9 3456 7890',
    timezone: 'America/Santiago',
    active: false,
  },
]

test.describe('Admin Locations - Visual Layout', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'test-token',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: {
          id: 'user-001',
          name: 'Admin Demo',
          firstName: 'Admin',
          lastName: 'Demo',
          email: 'admin@demo.cl',
          role: 'OWNER',
          businessId: 'biz-001',
          businessName: 'Centro Estetica Demo',
          timezone: 'America/Santiago',
          phone: null,
        },
      }))
    })
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_AUTH_USER) })
    })
    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_PROFILE_USER) })
    })
    await page.route('**/api/v1/notifications*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route('**/api/v1/dashboard/summary*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
    })
    await page.route('**/api/v1/business-locations', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_LOCATIONS) })
    })
  })

  test('tabla de sedes en 1920x1080', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
    await page.goto('/admin/locations')
    await expect(page.getByText('Sucursales registradas')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('Sucursal Central Maipu')).toBeVisible()
    await page.screenshot({ path: 'e2e/screenshots/admin-locations-1920.png', fullPage: true })
  })

  test('tabla de sedes en 1366x768', async ({ page }) => {
    await page.setViewportSize({ width: 1366, height: 768 })
    await page.goto('/admin/locations')
    await expect(page.getByText('Sucursales registradas')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('Sucursal Central Maipu')).toBeVisible()
    await page.screenshot({ path: 'e2e/screenshots/admin-locations-1366.png', fullPage: true })
  })

  test('tabla de sedes en 390x844 (mobile)', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto('/admin/locations')
    await expect(page.getByText('Sucursales registradas')).toBeVisible({ timeout: 10000 })
    await page.screenshot({ path: 'e2e/screenshots/admin-locations-390.png', fullPage: true })
  })
})
