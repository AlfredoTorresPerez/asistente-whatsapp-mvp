import type { Page } from '@playwright/test'
import dayjs from 'dayjs'

const SHELL_SESSION_KEY = 'asistente-whatsapp.session'

export const QA_MOCK_USER = {
  id: 'qa-auto-user-001',
  firstName: 'QA',
  lastName: 'Auto',
  email: 'qa_auto@demo.cl',
  role: 'OWNER',
  businessId: 'qa-auto-biz-001',
  businessName: 'QA Auto Centro Estetico',
  timezone: 'America/Santiago',
  phone: null,
}

export const QA_MOCK_AUTH_RESPONSE = {
  id: QA_MOCK_USER.id,
  firstName: QA_MOCK_USER.firstName,
  lastName: QA_MOCK_USER.lastName,
  email: QA_MOCK_USER.email,
  role: QA_MOCK_USER.role,
  businessId: QA_MOCK_USER.businessId,
  businessName: QA_MOCK_USER.businessName,
  timezone: QA_MOCK_USER.timezone,
}

export const QA_MOCK_PROFILE_RESPONSE = {
  id: QA_MOCK_USER.id,
  firstName: QA_MOCK_USER.firstName,
  lastName: QA_MOCK_USER.lastName,
  email: QA_MOCK_USER.email,
  phone: null,
  timezone: QA_MOCK_USER.timezone,
  role: QA_MOCK_USER.role,
  businessName: QA_MOCK_USER.businessName,
}

export function injectMockSession(page: Page, overrides?: Record<string, unknown>) {
  const user = { ...QA_MOCK_USER, ...overrides }
  const session = {
    accessToken: 'qa-auto-test-token',
    expiresAt: dayjs().add(24, 'hour').toISOString(),
    user,
  }
  return page.addInitScript(({ key, value }) => {
    window.sessionStorage.setItem(key, JSON.stringify(value))
  }, { key: SHELL_SESSION_KEY, value: session })
}

export async function setupDefaultMocks(page: Page) {
  await page.route(/\/api\/v1\/auth\/me(\?|$)/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(QA_MOCK_AUTH_RESPONSE) })
  })
  await page.route(/\/api\/v1\/users\/me(\?|$)/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(QA_MOCK_PROFILE_RESPONSE) })
  })
  await page.route(/\/api\/v1\/notifications/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], totalItems: 0 }) })
  })
  await page.route(/\/api\/v1\/dashboard\/summary/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
  })
}

export async function login(page: Page, email?: string, password?: string) {
  await page.goto('/login')
  await page.waitForLoadState('networkidle')
  await page.getByLabel('Correo').fill(email ?? 'qa_auto@demo.cl')
  await page.getByLabel('Contrasena').fill(password ?? 'qa_auto_pass')
  await page.getByRole('button', { name: 'Ingresar' }).click()
}

export async function logout(page: Page) {
  await page.goto('/login')
  await page.evaluate(() => window.sessionStorage.removeItem(SHELL_SESSION_KEY))
}

export async function expectAuthenticated(page: Page) {
  const hasSession = await page.evaluate(() => {
    const raw = window.sessionStorage.getItem('asistente-whatsapp.session')
    if (!raw) return false
    try {
      const parsed = JSON.parse(raw)
      return Boolean(parsed.accessToken && parsed.user)
    } catch {
      return false
    }
  })
  return hasSession
}
