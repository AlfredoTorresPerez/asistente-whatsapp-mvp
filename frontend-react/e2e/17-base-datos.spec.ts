import { test, expect } from '@playwright/test'
import { markBlocked, generateJsonReport, generateMarkdownReport } from './helpers/report.helper'
import { connectTestDatabase, isDbAvailable } from './helpers/db.helper'

test.describe('NIVEL 15: Base de Datos', () => {

  test('POST-RES-001: Reserva guardada correctamente - BLOCKED', async () => {
    markBlocked('POST-RES-001', 'No hay conexión segura a base de datos de pruebas. Se requiere configuración de testcontainers o DB de test con credenciales read-only.')
    test.skip(!isDbAvailable(), 'BLOCKED: sin conexión DB segura')
  })

  test('POST-RES-003: No duplicidad - BLOCKED', async () => {
    markBlocked('POST-RES-003', 'Requiere verificar índice único parcial uq_booking_customer_professional_active en DB')
    test.skip(true, 'BLOCKED: sin conexión DB')
  })

  test('POST-RES-004: Slot ocupado después de reservar - BLOCKED', async () => {
    markBlocked('POST-RES-004', 'Requiere verificar exclusión constraint ex_booking_professional_no_overlap_active')
    test.skip(true, 'BLOCKED: sin conexión DB')
  })

  test('TST-034: Reserva simultánea - prevención por constraint - BLOCKED', async () => {
    markBlocked('TST-034', 'Requiere verificación de exclusión constraint + manejo de error concurrente en API')
    test.skip(true, 'BLOCKED')
  })

  test('Historial de estado registrado - BLOCKED', async () => {
    markBlocked('TST-112', 'Requiere verificar tabla booking_status_history tras cambio de estado')
    test.skip(true, 'BLOCKED')
  })
})

test.afterAll(() => {
  generateJsonReport()
  generateMarkdownReport()
})
