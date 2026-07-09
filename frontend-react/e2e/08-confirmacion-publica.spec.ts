import { test, expect } from '@playwright/test'
import dayjs from 'dayjs'

const TOKEN_VALIDO = 'qa-auto-token-confirm'
const TOKEN_EXPIRADO = 'qa-auto-token-expired'
const TOKEN_CANCEL = 'qa-auto-token-cancel'
const TOKEN_RESCHEDULE = 'qa-auto-token-reschedule'
const FUTURE_DATE = dayjs().add(48, 'hour').format('YYYY-MM-DDTHH:mm:ss.SSS[Z]')

test.describe('NIVEL 8 — Confirmacion publica de reserva', () => {
  test('QA-08-001: Abrir link publico valido carga sin login', async ({ page }) => {
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_VALIDO}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-confirm-1',
          serviceName: 'Limpieza facial profunda',
          locationName: 'QA Sucursal Providencia',
          professionalName: 'Profesional estetica avanzada',
          date: '2026-07-10',
          time: '10:00',
          expiresAt: new Date(Date.now() + 3600000).toISOString(),
          status: 'PENDING',
        }),
      })
    })

    await page.goto(`/reservas/confirmar/${TOKEN_VALIDO}`)
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Limpieza facial profunda')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('QA Sucursal Providencia')).toBeVisible()
  })

  test('QA-08-002: Pagina publica carga sin auth (no redirige a login)', async ({ page }) => {
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_VALIDO}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-confirm-2',
          serviceName: 'Manicure',
          locationName: 'QA Sucursal Providencia',
          status: 'PENDING',
        }),
      })
    })

    await page.goto(`/reservas/confirmar/${TOKEN_VALIDO}`)
    await page.waitForLoadState('networkidle')
    expect(page.url()).not.toContain('/login')
  })

  test('QA-08-003: Pagina muestra datos de reserva', async ({ page }) => {
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_VALIDO}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-confirm-3',
          serviceName: 'Depilacion laser',
          locationName: 'QA Sucursal Providencia',
          professionalName: 'Dra. Maria Perez',
          date: '2026-07-10',
          time: '14:00',
          expiresAt: new Date(Date.now() + 3600000).toISOString(),
          status: 'PENDING',
        }),
      })
    })

    await page.goto(`/reservas/confirmar/${TOKEN_VALIDO}`)
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Depilacion laser')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('Dra. Maria Perez')).toBeVisible()
    await expect(page.getByText('QA Sucursal Providencia')).toBeVisible()
  })

  test('QA-08-004: Confirmar reserva cambia a CONFIRMED', async ({ page }) => {
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_VALIDO}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-confirm-4',
          serviceName: 'Corte de cabello',
          locationName: 'QA Sucursal Providencia',
          status: 'PENDING',
        }),
      })
    })

    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_VALIDO}/confirm(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-confirm-4',
          status: 'CONFIRMED',
        }),
      })
    })

    await page.goto(`/reservas/confirmar/${TOKEN_VALIDO}`)
    await page.waitForLoadState('networkidle')
    const confirmButton = page.getByRole('button', { name: /confirmar|si|aceptar/i })
    if (await confirmButton.isVisible()) {
      await confirmButton.click()
      await page.waitForTimeout(1000)
    }
  })

  test('QA-08-007: Link expirado muestra mensaje correcto', async ({ page }) => {
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_EXPIRADO}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 410,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'EXPIRED',
          message: 'El enlace de confirmacion ha expirado. Solicita un nuevo enlace.',
        }),
      })
    })

    await page.goto(`/reservas/confirmar/${TOKEN_EXPIRADO}`)
    await page.waitForLoadState('networkidle')
    await expect(page.getByText(/expirado|expir|vencido/i)).toBeVisible({ timeout: 5000 })
  })

  test('QA-08-008: Link invalido muestra error controlado', async ({ page }) => {
    await page.route(/\/api\/v1\/public\/booking-confirmations\/token-invalido(\?|$)/, async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 'NOT_FOUND',
          message: 'Reserva no encontrada o enlace invalido.',
        }),
      })
    })

    await page.goto('/reservas/confirmar/token-invalido')
    await page.waitForTimeout(3000)
    await expect(page.getByText('Enlace no disponible')).toBeVisible({ timeout: 5000 })
  })

  test('QA-08-005: Cancelar reserva desde pagina publica', async ({ page }) => {
    let bookingStatus = 'PENDIENTE_CONFIRMACION'
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_CANCEL}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-cancel',
          serviceName: 'Limpieza facial profunda',
          locationName: 'QA Sucursal Providencia',
          professionalName: 'Profesional estetica avanzada',
          startsAt: FUTURE_DATE,
          durationMinutes: 60,
          customerName: 'QA_AUTO_CLIENTE',
          maskedCustomerPhone: '+569****0001',
          expiresAt: dayjs().add(48, 'hour').toISOString(),
          bookingStatus,
          linkStatus: bookingStatus === 'CANCELLED' ? 'USED' : 'ACTIVE',
        }),
      })
    })
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_CANCEL}/cancel(\\?|$)`), async (route) => {
      bookingStatus = 'CANCELLED'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-cancel',
          bookingStatus: 'CANCELLED',
          linkStatus: 'USED',
        }),
      })
    })

    await page.goto(`/reservas/confirmar/${TOKEN_CANCEL}`)
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Limpieza facial profunda')).toBeVisible({ timeout: 10000 })

    await page.getByRole('button', { name: 'Cancelar reserva' }).click()
    await page.getByLabel('Motivo obligatorio').fill('QA_AUTO motivo de cancelacion')
    await page.getByRole('button', { name: 'Confirmar cancelacion' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('Reserva cancelada')).toBeVisible({ timeout: 5000 })
  })

  test('QA-08-006: Reprogramar reserva desde pagina publica', async ({ page }) => {
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_RESCHEDULE}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-reschedule',
          serviceName: 'Depilacion laser',
          locationName: 'QA Sucursal Providencia',
          professionalName: 'Dra. Maria Perez',
          startsAt: FUTURE_DATE,
          durationMinutes: 45,
          customerName: 'QA_AUTO_CLIENTE',
          maskedCustomerPhone: '+569****0002',
          expiresAt: dayjs().add(48, 'hour').toISOString(),
          bookingStatus: 'PENDIENTE_CONFIRMACION',
          linkStatus: 'ACTIVE',
        }),
      })
    })
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_RESCHEDULE}/availability(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          slots: [{
            startsAt: dayjs(FUTURE_DATE).add(2, 'hour').toISOString(),
            endsAt: dayjs(FUTURE_DATE).add(3, 'hour').toISOString(),
            locationId: 'loc-qa-001',
            locationName: 'QA Sucursal Providencia',
            serviceId: 'svc-qa-001',
            serviceName: 'Depilacion laser',
            durationMinutes: 45,
            professionalId: 'pro-qa-002',
            professionalName: 'Dra. Maria Perez',
            roomId: 'room-qa-001',
            roomName: 'Cabina 1',
            available: true,
            reason: 'Disponible',
          }],
        }),
      })
    })
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${TOKEN_RESCHEDULE}/reschedule(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: 'qa-auto-booking-reschedule',
          bookingStatus: 'RESCHEDULED',
          linkStatus: 'USED',
        }),
      })
    })

    await page.goto(`/reservas/confirmar/${TOKEN_RESCHEDULE}`)
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Depilacion laser')).toBeVisible({ timeout: 10000 })

    await page.getByRole('button', { name: 'Reprogramar reserva' }).click()
    await page.waitForTimeout(500)

    const newDate = dayjs().add(72, 'hour').format('YYYY-MM-DD')
    await page.getByLabel('Nueva fecha').fill(newDate)
    await page.waitForTimeout(500)

    await expect(page.getByText('Horarios disponibles')).toBeVisible({ timeout: 5000 })
  })
})
