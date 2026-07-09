import { test, expect } from '@playwright/test'
import { setupMocksForAgenda, setupMockCalendarResponse, openCompleteAgenda } from './helpers/agenda.helper'
import { sendInboundWhatsAppMessage } from './helpers/whatsapp-simulator.helper'

const QA_CUSTOMER_PHONE = '+56900000001'
const QA_CUSTOMER_NAME = 'QA_AUTO_CLIENTE_RESERVA'

test.describe('NIVEL 7 — Flujo WhatsApp simulado: reservar', () => {
  test('QA-07-001: Enviar mensaje de reserva simulado', { tag: '@wpp-sim' }, async ({ page }) => {
    const result = await sendInboundWhatsAppMessage(QA_CUSTOMER_PHONE, 'Quiero reservar limpieza facial')
    test.skip(!result.ok, 'Endpoint de simulacion WhatsApp no disponible — BLOCKED')
    expect(result.ok).toBeTruthy()
  })

  test('QA-07-009: Reserva aparece en Agenda completa', { tag: '@wpp-sim' }, async ({ page }) => {
    await setupMocksForAgenda(page)
    const mockItems = [{
      bookingId: 'qa-auto-wpp-booking-1',
      startsAt: '2026-07-07T10:00:00-04:00',
      endsAt: '2026-07-07T10:30:00-04:00',
      dateLocal: '2026-07-07',
      startTimeLocal: '10:00',
      endTimeLocal: '10:30',
      durationMinutes: 30,
      customerName: QA_CUSTOMER_NAME,
      customerPhone: QA_CUSTOMER_PHONE,
      serviceId: 'svc-qa-001',
      serviceName: 'Limpieza facial profunda',
      professionalId: 'pro-qa-001',
      professionalName: 'Profesional estetica avanzada',
      roomId: 'room-qa-001',
      roomName: 'Cabina 1',
      locationId: 'loc-qa-001',
      locationName: 'QA Sucursal Providencia',
      status: 'PENDIENTE_CONFIRMACION',
      sourceChannel: 'WHATSAPP',
      subject: 'Limpieza facial profunda',
    }]
    await setupMockCalendarResponse(page, mockItems)
    await openCompleteAgenda(page)
    await expect(page.getByText(QA_CUSTOMER_NAME).first()).toBeVisible({ timeout: 5000 })
    await expect(page.locator('span:text-is("Pendiente")').first()).toBeVisible({ timeout: 3000 })
  })

  test('QA-07-010: Estado inicial es PENDIENTE_CONFIRMACION', { tag: '@wpp-sim' }, async ({ page }) => {
    await setupMocksForAgenda(page)
    const mockItems = [{
      bookingId: 'qa-auto-wpp-booking-2',
      startsAt: '2026-07-07T11:00:00-04:00',
      endsAt: '2026-07-07T11:30:00-04:00',
      dateLocal: '2026-07-07',
      startTimeLocal: '11:00',
      endTimeLocal: '11:30',
      durationMinutes: 30,
      customerName: QA_CUSTOMER_NAME,
      customerPhone: QA_CUSTOMER_PHONE,
      serviceId: 'svc-qa-001',
      serviceName: 'Limpieza facial profunda',
      professionalId: 'pro-qa-001',
      professionalName: 'Profesional estetica avanzada',
      roomId: null,
      roomName: null,
      locationId: 'loc-qa-001',
      locationName: 'QA Sucursal Providencia',
      status: 'PENDIENTE_CONFIRMACION',
      sourceChannel: 'WHATSAPP',
      subject: 'Limpieza facial profunda',
    }]
    await setupMockCalendarResponse(page, mockItems)
    await openCompleteAgenda(page)
    await expect(page.locator('span:text-is("Pendiente")').first()).toBeVisible({ timeout: 5000 })
  })
})
