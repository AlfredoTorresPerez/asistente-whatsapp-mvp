import { test, expect } from '@playwright/test'
import dayjs from 'dayjs'

test.describe('NIVEL 2 — Pruebas funcionales basicas / Auth', () => {
  test('QA-02-001: Login correcto redirige a dashboard', async ({ page }) => {
    await page.route(/\/api\/v1\/auth\/login/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'qa-auto-token',
          expiresInSeconds: 86400,
          user: {
            id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto',
            email: 'qa_auto@demo.cl', role: 'OWNER', businessId: 'qa-auto-biz-001',
            businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago',
          },
        }),
      })
    })
    await page.route(/\/api\/v1\/auth\/me(\?|$)/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        role: 'OWNER', businessId: 'qa-auto-biz-001', businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago',
      }) })
    })
    await page.route(/\/api\/v1\/users\/me(\?|$)/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        phone: null, timezone: 'America/Santiago', role: 'OWNER', businessName: 'QA Auto Centro Estetico',
      }) })
    })
    await page.route(/\/api\/v1\/notifications/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route(/\/api\/v1\/dashboard\/summary/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        kpis: { openConversations: 0, newProspects: 0, openOrders: 0, pendingAppointments: 0 },
        todayAppointments: [], recentActivity: [], conversationSeries: [], orderSeries: [],
      }) })
    })

    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await page.getByLabel('Correo').fill('qa_auto@demo.cl')
    await page.getByPlaceholder('Ingresa tu contrasena').fill('qa_auto_pass')
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await page.waitForURL('**/dashboard', { timeout: 15000 })
    expect(page.url()).toContain('/dashboard')
  })

  test('QA-02-002: Login incorrecto muestra error', async ({ page }) => {
    await page.route(/\/api\/v1\/auth\/login/, async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'INVALID_CREDENTIALS', message: 'Credenciales invalidas' }),
      })
    })

    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await page.getByLabel('Correo').fill('invalido@demo.cl')
    await page.getByPlaceholder('Ingresa tu contrasena').fill('wrong_pass')
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await expect(page.getByText('Credenciales invalidas').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-02-003: Logout funciona — pagina login sin sesion', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Iniciar sesion')).toBeVisible({ timeout: 5000 })
  })

  test('QA-02-004: Recuperar contrasena — flujo completo', async ({ page }) => {
    await page.route(/\/api\/v1\/auth\/forgot-password/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        status: 'ACCEPTED',
        message: 'Si el correo esta registrado recibiras instrucciones.',
      }) })
    })

    await page.goto('/forgot-password')
    await page.waitForLoadState('networkidle')
    await page.getByLabel('Correo').fill('qa_auto@demo.cl')
    await page.getByRole('button', { name: 'Enviar enlace' }).click()
    await page.waitForURL('**/forgot-password/sent', { timeout: 10000 })
    expect(page.url()).toContain('/forgot-password/sent')
  })

  test('QA-02-005: Validar token de reset valido', async ({ page }) => {
    await page.route(/\/api\/v1\/auth\/reset-password\/validate\?token=/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        valid: true,
        expiresAt: dayjs().add(1, 'hour').toISOString(),
      }) })
    })

    await page.goto('/reset-password?token=qa-valid-token-123')
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Nueva contrasena').first()).toBeVisible({ timeout: 8000 })
  })

  test('QA-02-006: Cambiar contrasena desde token valido', async ({ page }) => {
    await page.route(/\/api\/v1\/auth\/reset-password\/validate\?token=/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        valid: true,
        expiresAt: dayjs().add(1, 'hour').toISOString(),
      }) })
    })
    await page.route(/\/api\/v1\/auth\/reset-password(\?|$)/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        status: 'ACCEPTED',
      }) })
    })

    await page.goto('/reset-password?token=qa-valid-token-456')
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Nueva contrasena').first()).toBeVisible({ timeout: 8000 })

    await page.getByLabel('Nueva contrasena').fill('NewPass123!')
    await page.getByLabel('Confirmar contrasena').fill('NewPass123!')
    await page.getByRole('button', { name: 'Guardar nueva contrasena' }).click()
    await page.waitForURL('**/login', { timeout: 10000 })
    expect(page.url()).toContain('/login')
  })

  test('QA-02-007: Ruta protegida redirige al login sin sesion', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)
    expect(page.url()).toContain('/login')
  })

  test('QA-02-008: Sesion persiste tras recargar pagina', async ({ page }) => {
    const session = {
      accessToken: 'qa-auto-persist-token',
      expiresAt: dayjs().add(24, 'hour').toISOString(),
      user: { id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl', role: 'OWNER', businessId: 'qa-auto-biz-001', businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago', phone: null },
    }
    await page.addInitScript(({ key, value }) => {
      window.sessionStorage.setItem(key, JSON.stringify(value))
    }, { key: 'asistente-whatsapp.session', value: session })

    await page.route(/\/api\/v1\/auth\/me(\?|$)/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        role: 'OWNER', businessId: 'qa-auto-biz-001', businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago',
      }) })
    })
    await page.route(/\/api\/v1\/users\/me(\?|$)/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
        phone: null, timezone: 'America/Santiago', role: 'OWNER', businessName: 'QA Auto Centro Estetico',
      }) })
    })
    await page.route(/\/api\/v1\/notifications/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
    })
    await page.route(/\/api\/v1\/dashboard\/summary/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
    })

    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)
    expect(page.url()).toContain('/dashboard')

    await page.reload()
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)
    expect(page.url()).toContain('/dashboard')
  })

  test('QA-02-009: Sesion expirada redirige al login', async ({ page }) => {
    const expiredSession = {
      accessToken: 'qa-auto-expired-token',
      expiresAt: dayjs().subtract(1, 'hour').toISOString(),
      user: { id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl', role: 'OWNER', businessId: 'qa-auto-biz-001', businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago', phone: null },
    }
    await page.addInitScript(({ key, value }) => {
      window.sessionStorage.setItem(key, JSON.stringify(value))
    }, { key: 'asistente-whatsapp.session', value: expiredSession })

    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)
    expect(page.url()).toContain('/login')
  })
})
