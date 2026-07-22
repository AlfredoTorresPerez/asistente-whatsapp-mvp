import { test, expect } from '@playwright/test'
import dayjs from 'dayjs'
import { setupMocksForAgenda, setupMockCalendarResponse, buildMockCalendarItems, QA_MOCK_SERVICES, QA_MOCK_LOCATIONS, QA_MOCK_PROFESSIONALS, QA_MOCK_ROOMS } from './helpers/agenda.helper'

const today = dayjs().format('YYYY-MM-DD')

function buildMockAvailabilitySlots(count = 3) {
  return Array.from({ length: count }, (_, i) => ({
    startsAt: `${today}T${String(10 + i).padStart(2, '0')}:00:00-04:00`,
    endsAt: `${today}T${String(11 + i).padStart(2, '0')}:00:00-04:00`,
    locationId: QA_MOCK_LOCATIONS[0].id,
    locationName: QA_MOCK_LOCATIONS[0].name,
    serviceId: QA_MOCK_SERVICES[0].id,
    serviceName: QA_MOCK_SERVICES[0].name,
    durationMinutes: 60,
    professionalId: QA_MOCK_PROFESSIONALS[i % QA_MOCK_PROFESSIONALS.length].id,
    professionalName: QA_MOCK_PROFESSIONALS[i % QA_MOCK_PROFESSIONALS.length].name,
    roomId: QA_MOCK_ROOMS[i % QA_MOCK_ROOMS.length].id,
    roomName: QA_MOCK_ROOMS[i % QA_MOCK_ROOMS.length].name,
    available: i !== 1,
    reason: i === 1 ? 'Bloqueado por administracion' : 'Disponible',
  }))
}

async function setupAvailSection(page: import('@playwright/test').Page) {
  await setupMocksForAgenda(page)
  await setupMockCalendarResponse(page, buildMockCalendarItems(1))
  await page.goto('/agenda')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)
}

function getAvailCard(page: import('@playwright/test').Page) {
  return page.locator('article').filter({ has: page.locator('h2:has-text("Consultar disponibilidad real")') })
}

test.describe('NIVEL 5 — Disponibilidad horaria', () => {
  test('QA-05-001: Seccion disponibilidad real visible en la pagina', async ({ page }) => {
    await setupAvailSection(page)

    const heading = page.getByRole('heading', { name: 'Consultar disponibilidad real' })
    await expect(heading).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('Buscar horarios disponibles')).toBeVisible()
  })

  test('QA-05-002: Boton buscar horarios deshabilitado sin sucursal y servicio', async ({ page }) => {
    await setupAvailSection(page)

    const searchButton = getAvailCard(page).getByRole('button', { name: 'Buscar horarios disponibles' })
    await expect(searchButton).toBeDisabled()
  })

  test('QA-05-003: Seleccionar sucursal y servicio habilita boton de busqueda', async ({ page }) => {
    await setupAvailSection(page)

    const card = getAvailCard(page)
    await card.getByLabel('Sucursal').selectOption(QA_MOCK_LOCATIONS[0].id)
    await card.getByLabel('Servicio').selectOption(QA_MOCK_SERVICES[0].id)

    await page.waitForTimeout(300)

    const searchButton = card.getByRole('button', { name: 'Buscar horarios disponibles' })
    await expect(searchButton).toBeEnabled()
  })

  test('QA-05-004: Busqueda retorna slots de disponibilidad', async ({ page }) => {
    await setupAvailSection(page)

    const mockSlots = buildMockAvailabilitySlots(3)
    await page.route(/\/api\/v1\/agenda\/availability/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        locationId: QA_MOCK_LOCATIONS[0].id,
        locationName: QA_MOCK_LOCATIONS[0].name,
        serviceId: QA_MOCK_SERVICES[0].id,
        serviceName: QA_MOCK_SERVICES[0].name,
        date: today,
        durationMinutes: 60,
        requiresRoom: true,
        requiresDeposit: false,
        slots: mockSlots,
      }) })
    })

    const card = getAvailCard(page)
    await card.getByLabel('Sucursal').selectOption(QA_MOCK_LOCATIONS[0].id)
    await card.getByLabel('Servicio').selectOption(QA_MOCK_SERVICES[0].id)
    await card.getByRole('button', { name: 'Buscar horarios disponibles' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('Horarios sugeridos por la agenda')).toBeVisible()
  })

  test('QA-05-005: Slots disponibles muestran etiqueta Disponible', async ({ page }) => {
    await setupAvailSection(page)

    const mockSlots = buildMockAvailabilitySlots(2)
    await page.route(/\/api\/v1\/agenda\/availability/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        locationId: QA_MOCK_LOCATIONS[0].id,
        locationName: QA_MOCK_LOCATIONS[0].name,
        serviceId: QA_MOCK_SERVICES[0].id,
        serviceName: QA_MOCK_SERVICES[0].name,
        date: today,
        durationMinutes: 60,
        requiresRoom: true,
        requiresDeposit: false,
        slots: mockSlots,
      }) })
    })

    const card = getAvailCard(page)
    await card.getByLabel('Sucursal').selectOption(QA_MOCK_LOCATIONS[0].id)
    await card.getByLabel('Servicio').selectOption(QA_MOCK_SERVICES[0].id)
    await card.getByRole('button', { name: 'Buscar horarios disponibles' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('Disponible').first()).toBeVisible()
  })

  test('QA-05-006: Slots no disponibles muestran etiqueta Bloqueado', async ({ page }) => {
    await setupAvailSection(page)

    const mockSlots = buildMockAvailabilitySlots(2).map((slot) => ({
      ...slot,
      available: false,
      reason: 'Bloqueado por administracion',
    }))
    await page.route(/\/api\/v1\/agenda\/availability/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        locationId: QA_MOCK_LOCATIONS[0].id,
        locationName: QA_MOCK_LOCATIONS[0].name,
        serviceId: QA_MOCK_SERVICES[0].id,
        serviceName: QA_MOCK_SERVICES[0].name,
        date: today,
        durationMinutes: 60,
        requiresRoom: true,
        requiresDeposit: false,
        slots: mockSlots,
      }) })
    })

    const card = getAvailCard(page)
    await card.getByLabel('Sucursal').selectOption(QA_MOCK_LOCATIONS[0].id)
    await card.getByLabel('Servicio').selectOption(QA_MOCK_SERVICES[0].id)
    await card.getByRole('button', { name: 'Buscar horarios disponibles' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('Bloqueado').first()).toBeVisible()
  })

  test('QA-05-007: Filtro profesional opcional en disponibilidad', async ({ page }) => {
    await setupAvailSection(page)

    const mockSlots = buildMockAvailabilitySlots(1)
    await page.route(/\/api\/v1\/agenda\/availability/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        locationId: QA_MOCK_LOCATIONS[0].id,
        locationName: QA_MOCK_LOCATIONS[0].name,
        serviceId: QA_MOCK_SERVICES[0].id,
        serviceName: QA_MOCK_SERVICES[0].name,
        date: today,
        durationMinutes: 60,
        requiresRoom: true,
        requiresDeposit: false,
        slots: mockSlots,
      }) })
    })

    const card = getAvailCard(page)
    const profOpcionalSelect = card.getByLabel('Profesional opcional')
    const options = await profOpcionalSelect.locator('option').allTextContents()
    expect(options).toContain('Todos los profesionales')
    expect(options).toContain('Profesional estetica avanzada')

    await card.getByLabel('Sucursal').selectOption(QA_MOCK_LOCATIONS[0].id)
    await card.getByLabel('Servicio').selectOption(QA_MOCK_SERVICES[0].id)
    await profOpcionalSelect.selectOption(QA_MOCK_PROFESSIONALS[0].id)
    await card.getByRole('button', { name: 'Buscar horarios disponibles' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('Horarios sugeridos por la agenda')).toBeVisible()
  })

  test('QA-05-008: Filtro cabina opcional en disponibilidad', async ({ page }) => {
    await setupAvailSection(page)

    const card = getAvailCard(page)
    const cabinaSelect = card.getByLabel('Cabina opcional')
    const options = await cabinaSelect.locator('option').allTextContents()
    expect(options).toContain('Todas las cabinas')
    expect(options).toContain('Cabina 1')
  })

  test('QA-05-009: Sin disponibilidad muestra mensaje informativo', async ({ page }) => {
    await setupAvailSection(page)

    await page.route(/\/api\/v1\/agenda\/availability/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        locationId: QA_MOCK_LOCATIONS[0].id,
        locationName: QA_MOCK_LOCATIONS[0].name,
        serviceId: QA_MOCK_SERVICES[0].id,
        serviceName: QA_MOCK_SERVICES[0].name,
        date: today,
        durationMinutes: 60,
        requiresRoom: false,
        requiresDeposit: false,
        slots: [],
      }) })
    })

    const card = getAvailCard(page)
    await card.getByLabel('Sucursal').selectOption(QA_MOCK_LOCATIONS[0].id)
    await card.getByLabel('Servicio').selectOption(QA_MOCK_SERVICES[0].id)
    await card.getByRole('button', { name: 'Buscar horarios disponibles' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('Consulta disponibilidad para ver horarios validos')).toBeVisible()
  })

  test('QA-05-010: Cambio de fecha afecta resultados de disponibilidad', async ({ page }) => {
    await setupAvailSection(page)

    const tomorrow = dayjs().add(1, 'day').format('YYYY-MM-DD')
    const mockSlots = buildMockAvailabilitySlots(1).map((slot) => ({
      ...slot,
      startsAt: `${tomorrow}T10:00:00-04:00`,
    }))
    await page.route(/\/api\/v1\/agenda\/availability/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        locationId: QA_MOCK_LOCATIONS[0].id,
        locationName: QA_MOCK_LOCATIONS[0].name,
        serviceId: QA_MOCK_SERVICES[0].id,
        serviceName: QA_MOCK_SERVICES[0].name,
        date: tomorrow,
        durationMinutes: 60,
        requiresRoom: true,
        requiresDeposit: false,
        slots: mockSlots,
      }) })
    })

    const card = getAvailCard(page)
    await card.getByLabel('Sucursal').selectOption(QA_MOCK_LOCATIONS[0].id)
    await card.getByLabel('Servicio').selectOption(QA_MOCK_SERVICES[0].id)

    const dateInput = card.locator('input[type="date"]')
    await dateInput.fill(tomorrow)
    await dateInput.press('Enter')
    await page.waitForTimeout(300)

    await card.getByRole('button', { name: 'Buscar horarios disponibles' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('Horarios sugeridos por la agenda')).toBeVisible()
  })
})
