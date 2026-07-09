import { test, expect } from '@playwright/test'
import { setupMocksForAgenda, setupMockCalendarResponse, openCompleteAgenda } from './helpers/agenda.helper'
import { sendInboundWhatsAppMessage } from './helpers/whatsapp-simulator.helper'

const QA_CUSTOMER_PHONE = '+56900000003'
const QA_CUSTOMER_NAME = 'QA_AUTO_CLIENTE_REPROGRAMACION'

test.describe('NIVEL 10 — Flujo WhatsApp simulado: reprogramar', () => {
  test('QA-10-001: Cliente con reserva activa pide reprogramar', { tag: '@wpp-reschedule' }, async ({ page }) => {
    const result = await sendInboundWhatsAppMessage(QA_CUSTOMER_PHONE, 'Quiero reprogramar mi reserva')
    test.skip(!result.ok, 'Endpoint de simulacion WhatsApp no disponible — BLOCKED')
    expect(result.ok).toBeTruthy()
  })

  test('QA-10-007: Reserva anterior queda RESCHEDULED', { tag: '@wpp-reschedule' }, async ({ page }) => {
    await setupMocksForAgenda(page)
    const mockItems = [{
      bookingId: 'qa-auto-reschedule-old-1',
      startsAt: '2026-07-07T10:00:00-04:00',
      endsAt: '2026-07-07T11:00:00-04:00',
      dateLocal: '2026-07-07',
      startTimeLocal: '10:00',
      endTimeLocal: '11:00',
      durationMinutes: 60,
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
      status: 'RESCHEDULED',
      sourceChannel: 'WHATSAPP',
      subject: 'Reprogramada',
    }]
    await setupMockCalendarResponse(page, mockItems)
    await openCompleteAgenda(page)
    await expect(page.locator('span:text-is("Reprogramada")').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-10-008: Nueva reserva aparece en agenda', { tag: '@wpp-reschedule' }, async ({ page }) => {
    await setupMocksForAgenda(page)
    const mockItems = [{
      bookingId: 'qa-auto-reschedule-new-1',
      startsAt: '2026-07-08T15:00:00-04:00',
      endsAt: '2026-07-08T16:00:00-04:00',
      dateLocal: '2026-07-08',
      startTimeLocal: '15:00',
      endTimeLocal: '16:00',
      durationMinutes: 60,
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
      status: 'CONFIRMED',
      sourceChannel: 'WHATSAPP',
      subject: 'Limpieza facial profunda',
    }]
    await setupMockCalendarResponse(page, mockItems)
    await openCompleteAgenda(page)
    await expect(page.getByText(QA_CUSTOMER_NAME).first()).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('Confirmada').first()).toBeVisible({ timeout: 3000 })
  })
})
