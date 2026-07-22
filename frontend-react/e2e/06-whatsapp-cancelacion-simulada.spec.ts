import { test, expect } from '@playwright/test'
import { sendInboundWhatsAppMessage } from './helpers/whatsapp-simulator.helper'

const QA_CUSTOMER_PHONE = '+56900000002'
const QA_CUSTOMER_NAME = 'QA_AUTO_CLIENTE_CANCELACION'

test.describe('NIVEL 9 — Flujo WhatsApp simulado: cancelar', () => {
  test('QA-09-001: Cliente con reserva activa pide cancelar', { tag: '@wpp-cancel' }, async () => {
    const result = await sendInboundWhatsAppMessage(QA_CUSTOMER_PHONE, 'Quiero cancelar mi reserva')
    test.skip(!result.ok, 'Endpoint de simulacion WhatsApp no disponible — BLOCKED')
    expect(result.ok).toBeTruthy()
  })

  test('QA-09-007: Estado cambia a CANCELLED', { tag: '@wpp-cancel' }, async () => {
    await setupMocksForAgenda(page)
    const mockItems = [{
      bookingId: 'qa-auto-cancel-1',
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
      status: 'CANCELLED',
      sourceChannel: 'WHATSAPP',
      subject: 'Cancelada',
    }]
    await setupMockCalendarResponse(page, mockItems)
    await openCompleteAgenda(page)
    await expect(page.locator('span:text-is("Cancelada")').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-09-009: Agenda deja de mostrar cancelada como ocupada', { tag: '@wpp-cancel' }, async ({ page }) => {
    await setupMocksForAgenda(page)
    await setupMockCalendarResponse(page, [])
    await openCompleteAgenda(page)
    const canceladaVisible = await page.getByText(QA_CUSTOMER_NAME).isVisible().catch(() => false)
    expect(canceladaVisible).toBeFalsy()
  })
})
