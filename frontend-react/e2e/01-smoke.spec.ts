import { test, expect } from '@playwright/test'
import { setupMocksForAgenda, setupMockCalendarResponse, openCompleteAgenda } from './helpers/agenda.helper'

test.describe('NIVEL 1 — Smoke Tests basicos', () => {
  test('QA-01-001: Frontend carga en http://localhost:5173', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const title = await page.title()
    expect(title).toBeDefined()
    expect(page.url()).toContain('localhost:5173')
  })

  test('QA-01-004: Login page carga correctamente', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Iniciar sesion')).toBeVisible({ timeout: 10000 })
    await expect(page.getByLabel('Correo')).toBeVisible()
    await expect(page.getByPlaceholder('Ingresa tu contrasena')).toBeVisible()
  })

  test('QA-01-005: Sidebar con modulos visibles tras login mockeado', async ({ page }) => {
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'qa-auto-test-token',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: {
          id: 'qa-auto-user-001',
          name: 'QA Auto',
          firstName: 'QA',
          lastName: 'Auto',
          email: 'qa_auto@demo.cl',
          role: 'OWNER',
          businessId: 'qa-auto-biz-001',
          businessName: 'QA Auto Centro Estetico',
          timezone: 'America/Santiago',
          phone: null,
        },
      }))
    })
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        role: 'OWNER', businessId: 'qa-auto-biz-001', businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago',
      }) })
    })
    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        phone: null, timezone: 'America/Santiago', role: 'OWNER', businessName: 'QA Auto Centro Estetico',
      }) })
    })
    await page.route('**/api/v1/notifications*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route('**/api/v1/dashboard/summary*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        kpis: { openConversations: 0, newProspects: 0, openOrders: 0, pendingAppointments: 0 },
        todayAppointments: [], recentActivity: [], conversationSeries: [], orderSeries: [],
      }) })
    })

    await page.goto('/dashboard')
    await page.waitForTimeout(2000)

    const sidebarText = await page.textContent('nav') ?? ''
    const hasDashboard = sidebarText.includes('Dashboard')
    const hasConversaciones = sidebarText.includes('Conversaciones')
    expect(hasDashboard || hasConversaciones).toBeTruthy()
  })

  test('QA-01-006: Modulo Agenda completa se abre', async ({ page }) => {
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'qa-auto-test-token',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: {
          id: 'qa-auto-user-001', name: 'QA Auto', firstName: 'QA', lastName: 'Auto',
          email: 'qa_auto@demo.cl', role: 'OWNER', businessId: 'qa-auto-biz-001',
          businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago', phone: null,
        },
      }))
    })
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        role: 'OWNER', businessId: 'qa-auto-biz-001', businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago',
      }) })
    })
    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        phone: null, timezone: 'America/Santiago', role: 'OWNER', businessName: 'QA Auto Centro Estetico',
      }) })
    })
    await page.route('**/api/v1/notifications*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route('**/api/v1/dashboard/summary*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
    })
    await page.route('**/api/v1/business-locations*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
    })
    await page.route('**/api/v1/agenda/filter-options*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ services: [], professionals: [], rooms: [] }) })
    })
    await page.route('**/api/v1/agenda/calendar*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route('**/api/v1/agenda/availability', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ slots: [] }) })
    })

    await page.goto('/agenda')
    await page.waitForTimeout(2000)

    const bodyText = await page.textContent('body') ?? ''
    expect(
      bodyText.includes('Reservas') ||
      bodyText.includes('Agenda digital') ||
      bodyText.includes('Cargando')
    ).toBeTruthy()
  })

  test('QA-01-007: Modulo Conversaciones se abre', async ({ page }) => {
    await page.addInitScript(() => {
      window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
        accessToken: 'qa-auto-test-token',
        expiresAt: new Date(Date.now() + 86400000).toISOString(),
        user: {
          id: 'qa-auto-user-001', name: 'QA Auto', firstName: 'QA', lastName: 'Auto',
          email: 'qa_auto@demo.cl', role: 'OWNER', businessId: 'qa-auto-biz-001',
          businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago', phone: null,
        },
      }))
    })
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        role: 'OWNER', businessId: 'qa-auto-biz-001', businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago',
      }) })
    })
    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        phone: null, timezone: 'America/Santiago', role: 'OWNER', businessName: 'QA Auto Centro Estetico',
      }) })
    })
    await page.route('**/api/v1/notifications*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route('**/api/v1/conversations*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route('**/api/v1/dashboard/summary*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
    })

    await page.goto('/conversations')
    await page.waitForTimeout(2000)

    const heading = page.getByRole('heading', { name: 'Conversaciones', exact: true })
    await expect(heading).toBeVisible({ timeout: 5000 })
  })
})
