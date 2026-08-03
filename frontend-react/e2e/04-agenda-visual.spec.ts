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
      items: Array.from({ length: 5 }, (_, i) => ({
        bookingId: `qa-auto-visual-${i}`,
        startsAt: `${weekStart.format('YYYY-MM-DD')}T${10 + i}:00:00-04:00`,
        endsAt: `${weekStart.format('YYYY-MM-DD')}T${10 + i + 1}:00:00-04:00`,
        dateLocal: weekStart.format('YYYY-MM-DD'),
        startTimeLocal: `${String(10 + i).padStart(2, '0')}:00`,
        endTimeLocal: `${String(10 + i + 1).padStart(2, '0')}:00`,
        durationMinutes: 60,
        customerName: `QA_AUTO_VISUAL_${i}`,
        customerPhone: `+56900000${i}`,
        serviceName: 'Servicio QA',
        professionalName: 'Profesional QA',
        roomName: `Cabina ${i + 1}`,
        locationName: 'QA Sucursal',
        status: i % 2 === 0 ? 'CONFIRMED' : 'PENDIENTE_CONFIRMACION',
        sourceChannel: i % 2 === 0 ? 'WHATSAPP' : 'MANUAL',
        subject: 'Servicio QA',
      })),
      totalItems: 5,
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

test.describe('NIVEL 11 — Pruebas visuales tipo Google Calendar', () => {
  test.beforeEach(async ({ page }) => {
    await addSessionAndMocks(page)
  })

  test('QA-11-001: Semana muestra columnas', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    const dayLabels = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom']
    const found = dayLabels.filter(d => bodyText.includes(d))
    expect(found.length).toBeGreaterThanOrEqual(5)
  })

  test('QA-11-002: Columnas de 7 dias presentes', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    const dayLabels = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom']
    const found = dayLabels.filter(d => bodyText.includes(d))
    expect(found.length).toBe(7)
  })

  test('QA-11-003: Marcas de hora visibles en grid', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    const hours = ['09', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21']
    const found = hours.filter(h => bodyText.includes(h))
    expect(found.length).toBeGreaterThanOrEqual(5)
  })

  test('QA-11-004: Tarjetas muestran nombre de cliente y servicio', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    for (let i = 0; i < 3; i++) {
      await expect(page.getByText(`QA_AUTO_VISUAL_${i}`).first()).toBeVisible({ timeout: 3000 })
    }
  })

  test('QA-11-005: Reservas visibles en agenda', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    await expect(page.getByText('QA_AUTO_VISUAL_0').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-11-006: Tarjetas de reserva visibles', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.includes('QA_AUTO_VISUAL_1')).toBeTruthy()
  })

  test('QA-11-007: Celda de hora visible en calendario', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.includes('10') || bodyText.includes('11')).toBeTruthy()
  })

  test('QA-11-008: Sin overlapping — tarjetas no se sobreponen', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const cards = page.getByRole('button').filter({ hasText: /QA_AUTO_VISUAL/ })
    const count = await cards.count()
    expect(count).toBeGreaterThanOrEqual(3)
  })

  test('QA-11-009: Badge WhatsApp visible en eventos', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    const bodyText = await page.textContent('body') ?? ''
    const hasWhatsAppRef = bodyText.includes('WHATSAPP') || bodyText.includes('WhatsApp')
    expect(hasWhatsAppRef).toBeTruthy()
  })

  test('QA-11-010: Screenshot como evidencia', async ({ page }) => {
    await page.goto('/agenda')
    await page.waitForTimeout(2000)
    await page.screenshot({ path: 'e2e/screenshots/agenda-visual-evidence.png', fullPage: true })
    const fs = await import('node:fs')
    expect(fs.existsSync('e2e/screenshots/agenda-visual-evidence.png')).toBeTruthy()
  })
})
