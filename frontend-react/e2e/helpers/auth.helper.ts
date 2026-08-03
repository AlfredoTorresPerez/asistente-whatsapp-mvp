import type { Page } from '@playwright/test'
import dayjs from 'dayjs'

const SHELL_SESSION_KEY = 'asistente-whatsapp.session'

export const QA_MOCK_PERMISSIONS = [
  'DASHBOARD_VIEW', 'CONVERSATIONS_VIEW', 'CONVERSATIONS_REPLY', 'CONVERSATIONS_ASSIGN',
  'AGENDA_VIEW', 'BOOKINGS_CREATE', 'BOOKINGS_UPDATE', 'BOOKINGS_CANCEL', 'BOOKINGS_RESCHEDULE',
  'CATALOG_VIEW', 'CATALOG_MANAGE', 'LOCATIONS_VIEW', 'LOCATIONS_MANAGE', 'USERS_VIEW', 'USERS_MANAGE',
  'SECURITY_AUDIT_VIEW', 'WHATSAPP_CONFIG_VIEW', 'WHATSAPP_CONFIG_MANAGE', 'REPORTS_VIEW',
  'CALENDAR_CONFIG_VIEW', 'CALENDAR_CONFIG_MANAGE', 'NOTIFICATIONS_VIEW', 'TEMPLATE_MANAGE',
  'LEAD_MANAGE', 'ORDER_MANAGE', 'AUTOMATION_MANAGE', 'ADMIN_MANAGE', 'SECURITY_MANAGE',
  'CHANNEL_MANAGE', 'CONTENT_VIEW', 'CONTENT_MANAGE', 'PROFESSIONAL_VIEW', 'PROFESSIONAL_MANAGE',
  'ROOM_VIEW', 'ROOM_MANAGE', 'ASSIGNMENT_VIEW', 'ASSIGNMENT_MANAGE',
  'BUSINESS_AI_VIEW', 'BUSINESS_AI_MANAGE', 'BUSINESS_AI_TEST', 'BUSINESS_AI_AUDIT_VIEW',
  'BUSINESS_AI_REVIEW', 'BUSINESS_AI_SEND',
]

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
  permissions: QA_MOCK_PERMISSIONS,
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
  permissions: QA_MOCK_PERMISSIONS,
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
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      kpis: { openConversations: 0, newProspects: 0, openOrders: 0, pendingAppointments: 0 },
      todayAppointments: [], recentActivity: [], conversationSeries: [], orderSeries: [],
    }) })
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
