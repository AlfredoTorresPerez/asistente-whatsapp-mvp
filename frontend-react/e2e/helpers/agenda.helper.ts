import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
import dayjs from 'dayjs'
import { injectMockSession, setupDefaultMocks } from './auth.helper'

// Mock data para agenda
export const QA_MOCK_SERVICES = [
  { id: 'svc-qa-001', name: 'Limpieza facial profunda', detail: '30 min' },
  { id: 'svc-qa-002', name: 'Depilacion laser', detail: '45 min' },
  { id: 'svc-qa-003', name: 'Manicure', detail: '60 min' },
]

export const QA_MOCK_PROFESSIONALS = [
  { id: 'pro-qa-001', name: 'Profesional estetica avanzada', detail: null },
  { id: 'pro-qa-002', name: 'Dra. Maria Perez', detail: null },
]

export const QA_MOCK_ROOMS = [
  { id: 'room-qa-001', name: 'Cabina 1', detail: null },
  { id: 'room-qa-002', name: 'Cabina 2', detail: null },
]

export const QA_MOCK_LOCATIONS = [
  { id: 'loc-qa-001', code: 'qa-prov', name: 'QA Sucursal Providencia', address: 'Av. Providencia 1234', commune: 'Providencia', active: true },
]

const weekStart = dayjs().startOf('week').add(1, 'day').format('YYYY-MM-DD')

export function buildMockCalendarItems(count = 3) {
  return Array.from({ length: count }, (_, i) => {
    const hour = 10 + i
    const service = QA_MOCK_SERVICES[i % QA_MOCK_SERVICES.length]
    const professional = QA_MOCK_PROFESSIONALS[i % QA_MOCK_PROFESSIONALS.length]
    const room = QA_MOCK_ROOMS[i % QA_MOCK_ROOMS.length]
    return {
      bookingId: `qa-auto-booking-${i + 1}`,
      startsAt: `${weekStart}T${String(hour).padStart(2, '0')}:00:00-04:00`,
      endsAt: `${weekStart}T${String(hour + 1).padStart(2, '0')}:00:00-04:00`,
      dateLocal: weekStart,
      startTimeLocal: `${String(hour).padStart(2, '0')}:00`,
      endTimeLocal: `${String(hour + 1).padStart(2, '0')}:00`,
      durationMinutes: 60,
      customerName: `QA_AUTO_CLIENTE_${i + 1}`,
      customerPhone: `+5690000000${i + 1}`,
      serviceId: service.id,
      serviceName: service.name,
      professionalId: professional.id,
      professionalName: professional.name,
      roomId: room.id,
      roomName: room.name,
      locationId: 'loc-qa-001',
      locationName: 'QA Sucursal Providencia',
      status: i === 0 ? 'CONFIRMED' : i === 1 ? 'PENDIENTE_CONFIRMACION' : 'TEMPORARY',
      sourceChannel: i === 2 ? 'WHATSAPP' : 'MANUAL',
      subject: service.name,
    }
  })
}

export async function setupMocksForAgenda(page: Page) {
  await injectMockSession(page)
  await setupDefaultMocks(page)

  const mockLocations = QA_MOCK_LOCATIONS
  const mockFilterOptions = { services: QA_MOCK_SERVICES, professionals: QA_MOCK_PROFESSIONALS, rooms: QA_MOCK_ROOMS }

  await page.route(/\/api\/v1\/business-locations/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockLocations) })
  })
  await page.route(/\/api\/v1\/agenda\/filter-options/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockFilterOptions) })
  })
  await page.route(/\/api\/v1\/bookings\//, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      bookingId: 'qa-auto-booking-detail',
      customerName: 'QA_AUTO_CLIENTE',
      status: 'CONFIRMED',
      sourceChannel: 'WHATSAPP',
      reminders: [],
      statusHistory: [],
    }) })
  })
}

export async function setupMockCalendarResponse(page: Page, items = buildMockCalendarItems()) {
  const mockCalendarResponse = { items, totalItems: items.length }
  await page.route(/\/api\/v1\/agenda\/calendar/, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(mockCalendarResponse) })
  })
}

export async function openCompleteAgenda(page: Page) {
  await page.goto('/agenda')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(500)
}

export async function selectWeek(page: Page, startDate: string) {
  const dateInput = page.locator('input[type="date"]').first()
  await dateInput.fill(startDate)
  await page.keyboard.press('Enter')
  await page.waitForTimeout(300)
}

export async function expectBookingVisible(page: Page, _date: string, _startTime: string, customerOrService: string) {
  await expect(page.getByText(customerOrService).first()).toBeVisible({ timeout: 5000 })
}

export async function expectBookingNotVisible(page: Page, _date: string, _startTime: string, customerOrService: string) {
  await expect(page.getByText(customerOrService).first()).not.toBeVisible({ timeout: 5000 })
}

export async function expectCurrentTimeLineVisible(page: Page) {
  const timeline = page.locator('[data-testid="current-time-line"]')
  if (await timeline.count() > 0) {
    await expect(timeline.first()).toBeVisible({ timeout: 3000 })
  }
}

export async function expectNoOverlappingCards(page: Page) {
  const cards = page.locator('[data-testid="agenda-event-card"]')
  const count = await cards.count()
  if (count < 2) return

  const boxes: { x: number; y: number; width: number; height: number }[] = []
  for (let i = 0; i < count; i++) {
    const box = await cards.nth(i).boundingBox()
    if (box) boxes.push(box)
  }

  for (let i = 0; i < boxes.length; i++) {
    for (let j = i + 1; j < boxes.length; j++) {
      const a = boxes[i]
      const b = boxes[j]
      const overlap =
        a.x < b.x + b.width &&
        a.x + a.width > b.x &&
        a.y < b.y + b.height &&
        a.y + a.height > b.y
      if (overlap) {
        await page.screenshot({ path: 'e2e/screenshots/overlap-detected.png', fullPage: true })
        throw new Error(`Overlap detected between card ${i} and card ${j}: a(${a.x},${a.y},${a.width},${a.height}) b(${b.x},${b.y},${b.width},${b.height})`)
      }
    }
  }
}
