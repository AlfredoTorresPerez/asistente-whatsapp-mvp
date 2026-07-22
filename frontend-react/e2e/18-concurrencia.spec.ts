import { test } from '@playwright/test'
import { markBlocked, generateJsonReport, generateMarkdownReport } from './helpers/report.helper'

test.describe('NIVEL 16: Concurrencia (PRE-RES-034 / TST-034)', () => {

  test('Dos reservas simultáneas mismo slot - BLOCKED', async () => {
    markBlocked('TST-034',
      'Prueba de concurrencia requiere: 1) Token de autenticación real, 2) Endpoint de creación de reserva, 3) Ejecución de 2 requests paralelos al mismo slot. Sin endpoint de simulación de autenticación en tests, esta prueba queda bloqueada.',
      'Crear endpoint de test que permita bypass de auth en ambiente de pruebas'
    )
    test.skip(true, 'BLOCKED: requiere auth token real y endpoint de booking')
  })

  test('Lectura sucia - BLOCKED', async () => {
    markBlocked('TST-034-2', 'Requiere validar que lectura de disponibilidad no muestre slot como libre si hay transacción concurrente')
    test.skip(true, 'BLOCKED')
  })
})

test.afterAll(() => {
  generateJsonReport()
  generateMarkdownReport()
})
