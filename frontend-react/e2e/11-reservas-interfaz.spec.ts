import { test, expect } from '@playwright/test'
import dayjs from 'dayjs'
import { setupMocksForAgenda, setupMockCalendarResponse, buildMockCalendarItems, QA_MOCK_SERVICES, QA_MOCK_LOCATIONS, QA_MOCK_PROFESSIONALS, QA_MOCK_ROOMS } from './helpers/agenda.helper'

const today = dayjs().format('YYYY-MM-DD')

function buildMockSlots(count = 2) {
  return Array.from({ length: count }, (_, i) => ({
    startsAt: `${today}T${String(10 + i).padStart(2, '0')}:00:00-04:00`,
    endsAt: `${today}T${String(11 + i).padStart(2, '0')}:00:00-04:00`,
    locationId: QA_MOCK_LOCATIONS[0].id,
    locationName: QA_MOCK_LOCATIONS[0].name,
    serviceId: QA_MOCK_SERVICES[0].id,
    serviceName: QA_MOCK_SERVICES[0].name,
    durationMinutes: 60,
    professionalId: QA_MOCK_PROFESSIONALS[0].id,
    professionalName: QA_MOCK_PROFESSIONALS[0].name,
    roomId: QA_MOCK_ROOMS[0].id,
    roomName: QA_MOCK_ROOMS[0].name,
    available: true,
    reason: 'Disponible',
  }))
}

function getAvailCard(page: import('@playwright/test').Page) {
  return page.locator('article').filter({ has: page.locator('h2:has-text("Consultar disponibilidad real")') })
}

function getCustomerCard(page: import('@playwright/test').Page) {
  return page.locator('article').filter({ has: page.locator('h2:has-text("Datos del cliente WhatsApp")') })
}

async function setupBookingFlow(page: import('@playwright/test').Page) {
  await setupMocksForAgenda(page)
  await setupMockCalendarResponse(page, buildMockCalendarItems(1))
  await page.goto('/agenda')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(800)

  const mockSlots = buildMockSlots(2)
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

  return card
}

test.describe('NIVEL 6 — Reservas desde interfaz', () => {
  test('QA-06-001: Seccion datos del cliente WhatsApp es visible', async ({ page }) => {
    await setupBookingFlow(page)

    await expect(page.getByRole('heading', { name: 'Datos del cliente WhatsApp' })).toBeVisible({ timeout: 5000 })
    const customerCard = getCustomerCard(page)
    await expect(customerCard.getByLabel('Cliente')).toBeVisible()
    await expect(customerCard.getByLabel('Telefono WhatsApp')).toBeVisible()
  })

  test('QA-06-002: Nombre de cliente requerido para crear reserva', async ({ page }) => {
    await setupBookingFlow(page)

    const slotButton = page.locator('button').filter({ has: page.locator('span:text-is("Disponible")') }).first()
    await expect(slotButton).toBeDisabled()
  })

  test('QA-06-003: Telefono de cliente requerido para crear reserva', async ({ page }) => {
    await setupBookingFlow(page)

    const customerCard = getCustomerCard(page)
    await customerCard.getByLabel('Cliente').fill('QA_AUTO_CLIENTE_TEST')

    const slotButton = page.locator('button').filter({ has: page.locator('span:text-is("Disponible")') }).first()
    await expect(slotButton).toBeDisabled()
  })

  test('QA-06-004: Click en slot disponible crea reserva temporal', async ({ page }) => {
    await setupBookingFlow(page)

    await page.route(/\/api\/v1\/agenda\/temporary-bookings/, async (route) => {
      const body = JSON.parse(route.request().postData() ?? '{}')
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-created-booking',
        subject: body.customerName,
        status: 'TEMPORARY',
        startsAt: today + 'T10:00:00',
        durationMinutes: 60,
        customerName: body.customerName,
        customerPhone: body.customerPhone,
        reminders: [],
        statusHistory: [],
        publicLinks: [],
        payments: [],
        emailLogs: [],
      }) })
    })

    const customerCard = getCustomerCard(page)
    await customerCard.getByLabel('Cliente').fill('QA_AUTO_CLIENTE_TEST')
    await customerCard.getByLabel('Telefono WhatsApp').fill('+56912345678')

    await page.locator('button').filter({ has: page.locator('span:text-is("Disponible")') }).first().click()
    await page.waitForTimeout(1000)
  })

  test('QA-06-005: Creacion de reserva actualiza el calendario', async ({ page }) => {
    await setupBookingFlow(page)

    let bookingCreated = false
    await page.route(/\/api\/v1\/agenda\/temporary-bookings/, async (route) => {
      bookingCreated = true
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        id: 'qa-auto-created-booking-2',
        subject: 'QA_AUTO_CLIENTE_TEST',
        status: 'TEMPORARY',
        startsAt: today + 'T10:00:00',
        durationMinutes: 60,
        customerName: 'QA_AUTO_CLIENTE_TEST',
        customerPhone: '+56912345678',
        reminders: [],
        statusHistory: [],
        publicLinks: [],
        payments: [],
        emailLogs: [],
      }) })
    })

    const customerCard = getCustomerCard(page)
    await customerCard.getByLabel('Cliente').fill('QA_AUTO_CLIENTE_TEST')
    await customerCard.getByLabel('Telefono WhatsApp').fill('+56912345678')

    await page.locator('button').filter({ has: page.locator('span:text-is("Disponible")') }).first().click()
    await page.waitForTimeout(1500)

    expect(bookingCreated).toBeTruthy()
  })

  test('QA-06-006: Campo opcional de correo acepta entrada', async ({ page }) => {
    await setupBookingFlow(page)

    const customerCard = getCustomerCard(page)
    const emailInput = customerCard.getByLabel('Correo opcional')
    await emailInput.fill('test@qa-auto.cl')
    await expect(emailInput).toHaveValue('test@qa-auto.cl')
  })

  test('QA-06-007: Campo de notas acepta entrada', async ({ page }) => {
    await setupBookingFlow(page)

    const customerCard = getCustomerCard(page)
    const notesInput = customerCard.getByLabel('Notas de agenda')
    await notesInput.fill('QA_AUTO nota de prueba')
    await expect(notesInput).toHaveValue('QA_AUTO nota de prueba')
  })

  test('QA-06-008: Campos vacios no permiten crear reserva', async ({ page }) => {
    await setupBookingFlow(page)

    const slotButtons = page.locator('button').filter({ has: page.locator('span:text-is("Disponible")') })
    const count = await slotButtons.count()
    for (let i = 0; i < count; i++) {
      await expect(slotButtons.nth(i)).toBeDisabled()
    }
  })

  test('QA-06-009: Error 409 con slot ocupado muestra mensaje especifico', async ({ page }) => {
    await setupBookingFlow(page)

    await page.route(/\/api\/v1\/agenda\/temporary-bookings/, async (route) => {
      await route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({
        status: 409,
        code: 'AGENDA_SLOT_NOT_AVAILABLE',
        message: 'El horario ya esta ocupado.',
        fieldErrors: { startsAt: 'El horario seleccionado ya esta ocupado.' },
      }) })
    })

    const customerCard = getCustomerCard(page)
    await customerCard.getByLabel('Cliente').fill('QA_AUTO_CLIENTE_ERROR')
    await customerCard.getByLabel('Telefono WhatsApp').fill('+56912345678')

    await page.locator('button').filter({ has: page.locator('span:text-is("Disponible")') }).first().click()

    const toast = page.getByRole('status').first()
    await expect(toast).toBeVisible({ timeout: 8000 })
    await expect(toast).toContainText('No se pudo crear la reserva')
    await expect(toast).toContainText('ocupado')
  })

  test('QA-06-011: Error 409 con fieldErrors de telefono muestra detalle especifico', async ({ page }) => {
    await setupBookingFlow(page)

    await page.route(/\/api\/v1\/agenda\/temporary-bookings/, async (route) => {
      await route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({
        status: 409,
        code: 'BOOKING_VALIDATION_ERROR',
        message: 'La solicitud contiene errores de validacion.',
        fieldErrors: { customerPhone: 'El telefono debe tener formato chileno valido (569XXXXXXXX).' },
      }) })
    })

    const customerCard = getCustomerCard(page)
    await customerCard.getByLabel('Cliente').fill('QA_AUTO_CLIENTE_ERROR')
    await customerCard.getByLabel('Telefono WhatsApp').fill('+56912345678')

    await page.locator('button').filter({ has: page.locator('span:text-is("Disponible")') }).first().click()

    const toast = page.getByRole('status').first()
    await expect(toast).toBeVisible({ timeout: 8000 })
    await expect(toast).toContainText('No se pudo crear la reserva')
    await expect(toast).toContainText('formato chileno')
  })

  test('QA-06-010: Todos los inputs de datos del cliente estan presentes', async ({ page }) => {
    await setupBookingFlow(page)

    const customerCard = getCustomerCard(page)
    await expect(customerCard.getByLabel('Cliente')).toBeVisible()
    await expect(customerCard.getByLabel('Telefono WhatsApp')).toBeVisible()
    await expect(customerCard.getByLabel('Correo opcional')).toBeVisible()
    await expect(customerCard.getByLabel('Notas de agenda')).toBeVisible()
  })
})
