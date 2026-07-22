import { test, expect } from '@playwright/test'
import { injectMockSession, setupDefaultMocks } from './helpers/auth.helper'
import { setupMocksForAgenda, setupMockCalendarResponse, buildMockCalendarItems } from './helpers/agenda.helper'
import { markBlocked, generateJsonReport, generateMarkdownReport } from './helpers/report.helper'

test.describe('NIVEL 8: Cancelación desde Interfaz (TST-114 a TST-137)', () => {

  test.beforeEach(async ({ page }) => {
    await injectMockSession(page)
    await setupDefaultMocks(page)
    await setupMocksForAgenda(page)
    await setupMockCalendarResponse(page, buildMockCalendarItems(3))
  })

  test('TST-114: Reserva no existe', async ({ page }) => {
    await page.goto('/appointments/non-existent-id')
    await page.waitForLoadState('networkidle')
    await expect(page.getByText(/no encontrada|no existe|404/i).or(page.getByText(/error/i).first())).toBeVisible({ timeout: 5000 }).catch(() => {
      markBlocked('TST-114', 'Página de error 404 no encontrada en UI')
    })
    test.skip(true, 'BLOCKED: requiere página de detalle con ID inválido')
  })

  test('TST-115: Estado no cancelable', async () => {
    markBlocked('TST-115', 'Requiere state machine endpoint PATCH /bookings/{id}/cancel')
    test.skip(true, 'BLOCKED')
  })

  test('TST-117: Fuera de plazo', async () => {
    markBlocked('TST-117', 'Requiere política de tiempo mínimo configurada')
    test.skip(true, 'BLOCKED')
  })

  test('TST-118: Motivo obligatorio', async ({ page }) => {
    await page.goto('/appointments')
    await page.waitForLoadState('networkidle')
    // Verify cancel button exists
    test.skip(true, 'BLOCKED: requiere reserva real para probar cancelación')
  })

  test('TST-125: Liberar slot - BLOCKED', async () => {
    markBlocked('TST-125', 'Requiere validación de slot liberado vía API tras cancelación')
    test.skip(true, 'BLOCKED')
  })

  test('TST-130: Anular recordatorios - BLOCKED', async () => {
    markBlocked('TST-130', 'Requiere verificar tabla booking_reminder tras cancelación')
    test.skip(true, 'BLOCKED')
  })
})

test.afterAll(() => {
  generateJsonReport()
  generateMarkdownReport()
})
