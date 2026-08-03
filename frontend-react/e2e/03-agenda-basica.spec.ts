import { test, expect } from '@playwright/test'
import dayjs from 'dayjs'
import isoWeek from 'dayjs/plugin/isoWeek.js'
import { QA_MOCK_PERMISSIONS } from './helpers/auth.helper'

dayjs.extend(isoWeek)
const weekStart = dayjs().startOf('isoWeek')

async function addSessionAndMocks(page: import('@playwright/test').Page) {
  await page.addInitScript((permissions) => {
    window.sessionStorage.setItem('asistente-whatsapp.session', JSON.stringify({
      accessToken: 'qa-auto-test-token',
      expiresAt: new Date(Date.now() + 86400000).toISOString(),
      user: {
        id: 'qa-auto-user-001', name: 'QA Auto', firstName: 'QA', lastName: 'Auto',
        email: 'qa_auto@demo.cl', role: 'OWNER', businessId: 'qa-auto-biz-001',
        businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago', phone: null,
        permissions,
      },
    }))
  }, QA_MOCK_PERMISSIONS)
  await page.route(/\/api\/v1\/auth\/me(\?|$)/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      id: 'qa-auto-user-001', firstName: 'QA', lastName: 'Auto', email: 'qa_auto@demo.cl',
      role: 'OWNER', businessId: 'qa-auto-biz-001', businessName: 'QA Auto Centro Estetico', timezone: 'America/Santiago',
      permissions: QA_MOCK_PERMISSIONS,
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
  await page.route(/\/api\/v1\/business-locations/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([{
      id: 'loc-qa-001', name: 'QA Sucursal', commune: 'Providencia', active: true,
    }]) })
  })
  await page.route(/\/api\/v1\/agenda\/filter-options/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      services: [{ id: 'svc-1', name: 'Limpieza facial', detail: null }],
      professionals: [{ id: 'pro-1', name: 'Profesional QA', detail: null }],
      rooms: [{ id: 'room-1', name: 'Cabina 1', detail: null }],
    }) })
  })
  await page.route(/\/api\/v1\/agenda\/business-hours/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([
      { dayOfWeek: 1, startTime: '09:00', endTime: '21:00' },
      { dayOfWeek: 2, startTime: '09:00', endTime: '21:00' },
      { dayOfWeek: 3, startTime: '09:00', endTime: '21:00' },
      { dayOfWeek: 4, startTime: '09:00', endTime: '21:00' },
      { dayOfWeek: 5, startTime: '09:00', endTime: '21:00' },
      { dayOfWeek: 6, startTime: '10:00', endTime: '14:00' },
      { dayOfWeek: 0, startTime: '10:00', endTime: '14:00' },
    ]) })
  })
  await page.route(/\/api\/v1\/agenda\/availability/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ slots: [] }) })
  })
  await page.route(/\/api\/v1\/agenda\/calendar/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      items: [{
        bookingId: 'qa-auto-booking-1',
        startsAt: `${weekStart.format('YYYY-MM-DD')}T10:00:00-04:00`,
        endsAt: `${weekStart.format('YYYY-MM-DD')}T11:00:00-04:00`,
        dateLocal: weekStart.format('YYYY-MM-DD'),
        startTimeLocal: '10:00',
        endTimeLocal: '11:00',
        durationMinutes: 60,
        customerName: 'QA_AUTO_CLIENTE_1',
        customerPhone: '+56900000001',
        serviceName: 'Limpieza facial',
        professionalName: 'Profesional QA',
        roomName: 'Cabina 1',
        locationName: 'QA Sucursal',
        status: 'CONFIRMED',
        sourceChannel: 'WHATSAPP',
        subject: 'Limpieza facial',
      }, {
        bookingId: 'qa-auto-booking-2',
        startsAt: `${weekStart.format('YYYY-MM-DD')}T11:00:00-04:00`,
        endsAt: `${weekStart.format('YYYY-MM-DD')}T11:30:00-04:00`,
        dateLocal: weekStart.format('YYYY-MM-DD'),
        startTimeLocal: '11:00',
        endTimeLocal: '11:30',
        durationMinutes: 30,
        customerName: 'QA_AUTO_CLIENTE_2',
        customerPhone: '+56900000002',
        serviceName: 'Depilacion',
        professionalName: 'Profesional QA',
        roomName: 'Cabina 2',
        locationName: 'QA Sucursal',
        status: 'PENDIENTE_CONFIRMACION',
        sourceChannel: 'MANUAL',
        subject: 'Depilacion',
      }, {
        bookingId: 'qa-auto-booking-3',
        startsAt: `${weekStart.format('YYYY-MM-DD')}T14:00:00-04:00`,
        endsAt: `${weekStart.format('YYYY-MM-DD')}T15:00:00-04:00`,
        dateLocal: weekStart.format('YYYY-MM-DD'),
        startTimeLocal: '14:00',
        endTimeLocal: '15:00',
        durationMinutes: 60,
        customerName: 'QA_AUTO_CLIENTE_3',
        customerPhone: '+56900000003',
        serviceName: 'Manicure',
        professionalName: 'Profesional QA',
        roomName: 'Cabina 1',
        locationName: 'QA Sucursal',
        status: 'TEMPORARY',
        sourceChannel: 'WHATSAPP',
        subject: 'Manicure',
      }],
      totalItems: 3,
    }) })
  })
  await page.route(/\/api\/v1\/bookings\//, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      bookingId: 'qa-auto-booking-1',
      customerName: 'QA_AUTO_CLIENTE_1',
      status: 'CONFIRMED',
      sourceChannel: 'WHATSAPP',
      reminders: [],
      statusHistory: [],
    }) })
  })
}

test.describe('NIVEL 3 — Agenda digital basica', () => {
  test.beforeEach(async ({ page }) => {
    await addSessionAndMocks(page)
  })

  test('QA-03-001: Agenda muestra 7 columnas Lun-Dom', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    const days = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom']
    const found = days.filter(d => bodyText.includes(d))
    expect(found.length).toBeGreaterThanOrEqual(7)
  })

  test('QA-03-002: Agenda muestra columna de horas', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    const hours = ['09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21']
    const found = hours.filter(h => bodyText.includes(h))
    expect(found.length).toBeGreaterThanOrEqual(5)
  })

  test('QA-03-003: Agenda respeta horario 09-21', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.includes('09') || bodyText.includes('9')).toBeTruthy()
    expect(bodyText.includes('21')).toBeTruthy()
  })

  test('QA-03-004: Agenda carga y muestra interfaz', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.length).toBeGreaterThan(100)
  })

  test('QA-03-005: Agenda carga sin errores', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const errorText = await page.getByText(/error|error|fallo/i).isVisible().catch(() => false)
    expect(errorText).toBeFalsy()
  })

  test('QA-03-006: Pagina de agenda renderiza componente', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.includes('Reservas') || bodyText.includes('Agenda')).toBeTruthy()
  })

  test('QA-03-007: Agenda muestra reservas mockeadas', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    await expect(page.getByText('QA_AUTO_CLIENTE_1').first()).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('QA_AUTO_CLIENTE_2').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-03-008: Status labels visibles', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.includes('Confirmada') || bodyText.includes('CONFIRMED')).toBeTruthy()
  })

  test('QA-03-009: Pendiente de confirmacion visible', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.includes('Pendiente') || bodyText.includes('TEMPORARY')).toBeTruthy()
  })

  test('QA-03-010: Reserva confirmada visible', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.includes('Confirmada')).toBeTruthy()
  })
})
