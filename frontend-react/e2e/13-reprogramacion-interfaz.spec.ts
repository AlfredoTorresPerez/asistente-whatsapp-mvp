import { test, expect } from '@playwright/test'
import { injectMockSession, setupDefaultMocks } from './helpers/auth.helper'
import { setupMocksForAgenda, setupMockCalendarResponse, buildMockCalendarItems, QA_MOCK_SERVICES, QA_MOCK_PROFESSIONALS } from './helpers/agenda.helper'
import { markBlocked, generateJsonReport, generateMarkdownReport } from './helpers/report.helper'
import { apiPatch } from './helpers/api.helper'

test.describe('NIVEL 7: Reprogramación desde Interfaz (TST-084 a TST-113)', () => {

  test.beforeEach(async ({ page }) => {
    await injectMockSession(page)
    await setupDefaultMocks(page)
    await setupMocksForAgenda(page)
    await setupMockCalendarResponse(page, buildMockCalendarItems(3))
  })

  test('TST-084: Reprogramar reserva que no existe - BLOCKED (requiere API)', async () => {
    markBlocked('TST-084', 'Requiere endpoint API de reprogramación con validación de existencia')
    test.skip(true, 'BLOCKED: requiere endpoint de reprogramación con manejo de 404')
  })

  test('TST-085: Estado no reprogramable - BLOCKED (requiere API)', async () => {
    markBlocked('TST-085', 'Requiere endpoint API de reprogramación con state machine')
    test.skip(true, 'BLOCKED: requiere state machine endpoint')
  })

  test('TST-092: Nueva fecha pasada - BLOCKED (requiere API)', async () => {
    markBlocked('TST-092', 'Requiere endpoint PATCH /agenda/bookings/{id}/reschedule')
    test.skip(true, 'BLOCKED')
  })

  test('TST-094: Nuevo slot ocupado - BLOCKED (requiere API)', async () => {
    markBlocked('TST-094', 'Requiere validación de disponibilidad en backend')
    test.skip(true, 'BLOCKED')
  })

  test('TST-107: Mantener original hasta confirmar nuevo - BLOCKED (requiere transacción)', async () => {
    markBlocked('TST-107', 'Requiere verificación atómica de cambio de slot')
    test.skip(true, 'BLOCKED: validación de atomicidad')
  })

  test('TST-108: Cambio atómico - BLOCKED (requiere transacción)', async () => {
    markBlocked('TST-108', 'Requiere verificación de commit atómico en backend')
    test.skip(true, 'BLOCKED')
  })

  test('TST-088: Exceso de reprogramaciones', async ({ page }) => {
    await page.goto('/appointments')
    await page.waitForLoadState('networkidle')
    // UI validation: reschedule button disabled after max count
    // Requires real booking in state machine context
    test.skip(true, 'BLOCKED: requiere reserva real con reschedule_count')
  })
})

test.afterAll(() => {
  generateJsonReport()
  generateMarkdownReport()
})
