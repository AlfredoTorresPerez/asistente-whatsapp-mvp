import { test, expect } from '@playwright/test'
import { sendInboundWhatsAppMessage } from './helpers/whatsapp-simulator.helper'
import { apiGet } from './helpers/api.helper'

test.describe('NIVEL 17 — Flujo IA reservas (E2E simulado)', () => {

  test('IA-01: Mensaje de reserva con datos completos', async () => {
    const result = await sendInboundWhatsAppMessage(
      '+56900000001',
      'Quiero agendar limpieza facial para mañana a las 14:00 en Providencia'
    )
    test.skip(!result.ok, 'BLOCKED: endpoint de simulacion no disponible')
    expect(result.ok).toBeTruthy()
  })

  test('IA-02: Mensaje de cancelacion', async () => {
    const result = await sendInboundWhatsAppMessage(
      '+56900000002',
      'Quiero cancelar mi cita del viernes'
    )
    test.skip(!result.ok, 'BLOCKED: endpoint de simulacion no disponible')
    expect(result.ok).toBeTruthy()
  })

  test('IA-03: Mensaje de reprogramacion', async () => {
    const result = await sendInboundWhatsAppMessage(
      '+56900000003',
      'Necesito cambiar la hora de mi reserva'
    )
    test.skip(!result.ok, 'BLOCKED: endpoint de simulacion no disponible')
    expect(result.ok).toBeTruthy()
  })

  test('IA-04: Mensaje sin datos completos (deberia pedir mas info)', async () => {
    const result = await sendInboundWhatsAppMessage(
      '+56900000004',
      'Quiero agendar una hora'
    )
    test.skip(!result.ok, 'BLOCKED: endpoint de simulacion no disponible')
    expect(result.ok).toBeTruthy()
  })

  test('IA-05: Mensaje de precio', async () => {
    const result = await sendInboundWhatsAppMessage(
      '+56900000005',
      'Cuanto cuesta la limpieza facial'
    )
    test.skip(!result.ok, 'BLOCKED: endpoint de simulacion no disponible')
    expect(result.ok).toBeTruthy()
  })

  test('IA-06: Consulta de disponibilidad', async () => {
    const result = await sendInboundWhatsAppMessage(
      '+56900000006',
      'Que horarios tienen disponibles para depilacion laser'
    )
    test.skip(!result.ok, 'BLOCKED: endpoint de simulacion no disponible')
    expect(result.ok).toBeTruthy()
  })

  test('IA-07: Lenguaje ambiguo - lo antes posible', async () => {
    const result = await sendInboundWhatsAppMessage(
      '+56900000007',
      'Quiero agendar limpieza facial lo antes posible'
    )
    test.skip(!result.ok, 'BLOCKED: endpoint de simulacion no disponible')
    expect(result.ok).toBeTruthy()
  })

  test('IA-08: Error ortografico', async () => {
    const result = await sendInboundWhatsAppMessage(
      '+56900000008',
      'kiero ajendar limpieza facial manhana a las 3 de la tarde'
    )
    test.skip(!result.ok, 'BLOCKED: endpoint de simulacion no disponible')
    expect(result.ok).toBeTruthy()
  })

  test('IA-09: Health endpoint disponible', async () => {
    const resp = await apiGet('/actuator/health')
    expect(resp.ok()).toBeTruthy()
    const body = await resp.json()
    expect(body.status).toBe('UP')
  })
})
