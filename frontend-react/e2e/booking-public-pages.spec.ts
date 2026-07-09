import { test, expect } from '@playwright/test'

test.describe('Paginas publicas de reservas', () => {

  test('Pago - muestra checkout pendiente', async ({ page }) => {
    const paymentId = crypto.randomUUID()
    const bookingId = crypto.randomUUID()
    await page.route(new RegExp(`/api/v1/public/booking-payments/${paymentId}/detail(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: paymentId,
          bookingId,
          amount: 10000,
          currency: 'CLP',
          status: 'PENDING',
          checkoutUrl: null,
          checkoutExpiresAt: null,
          manual: false,
          approvedAt: null,
          rejectedAt: null,
          expiredAt: null,
          refundedAt: null,
          createdAt: new Date().toISOString(),
          bookingStatus: 'PENDIENTE_CONFIRMACION',
          bookingPaymentStatus: 'PENDING',
          subject: 'Depilacion laser',
          serviceName: 'Depilacion laser',
          professionalName: 'Dr. Perez',
          roomName: 'Cabina A',
          startsAt: new Date(Date.now() + 86400000).toISOString(),
          durationMinutes: 60,
          locationName: 'Sucursal Maipu',
          customerName: 'QA Auto Cliente',
        }),
      })
    })

    await page.goto(`/reservas/pagar/${paymentId}`)
    await expect(page.getByText('Pendiente de pago').first()).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('$10.000')).toBeVisible()
  })

  test('Pago - muestra aprobado cuando el pago fue confirmado', async ({ page }) => {
    const paymentId = crypto.randomUUID()
    const bookingId = crypto.randomUUID()
    await page.route(new RegExp(`/api/v1/public/booking-payments/${paymentId}/detail(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: paymentId,
          bookingId,
          amount: 25000,
          currency: 'CLP',
          status: 'APPROVED',
          checkoutUrl: null,
          checkoutExpiresAt: null,
          manual: false,
          approvedAt: new Date().toISOString(),
          rejectedAt: null,
          expiredAt: null,
          refundedAt: null,
          createdAt: new Date(Date.now() - 3600000).toISOString(),
          bookingStatus: 'CONFIRMED',
          bookingPaymentStatus: 'PAID',
          subject: 'Manicure premium',
          serviceName: 'Manicure premium',
          professionalName: 'Maria',
          roomName: null,
          startsAt: new Date(Date.now() + 86400000).toISOString(),
          durationMinutes: 45,
          locationName: 'Sucursal Providencia',
          customerName: 'QA Cliente Aprobado',
        }),
      })
    })

    await page.goto(`/reservas/pagar/${paymentId}`)
    await expect(page.getByText('Pago aprobado').first()).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('$25.000')).toBeVisible()
  })

  test('Pago - simular aprobacion y verificar estado', async ({ page }) => {
    const paymentId = crypto.randomUUID()
    const bookingId = crypto.randomUUID()
    let currentStatus = 'PENDING'

    await page.route(new RegExp(`/api/v1/public/booking-payments/${paymentId}/detail(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: paymentId,
          bookingId,
          amount: 15000,
          currency: 'CLP',
          status: currentStatus,
          checkoutUrl: null,
          checkoutExpiresAt: null,
          manual: false,
          approvedAt: currentStatus === 'APPROVED' ? new Date().toISOString() : null,
          rejectedAt: null,
          expiredAt: null,
          refundedAt: null,
          createdAt: new Date().toISOString(),
          bookingStatus: currentStatus === 'PENDING' ? 'PENDIENTE_CONFIRMACION' : 'CONFIRMED',
          bookingPaymentStatus: currentStatus === 'PENDING' ? 'PENDING' : 'PAID',
          subject: 'Limpieza facial',
          serviceName: 'Limpieza facial',
          professionalName: 'Profesional QA',
          roomName: 'Cabina 1',
          startsAt: new Date(Date.now() + 86400000).toISOString(),
          durationMinutes: 60,
          locationName: 'QA Sucursal',
          customerName: 'QA Auto',
        }),
      })
    })

    await page.route(new RegExp(`/api/v1/public/booking-payments/${paymentId}/simulate(\\?|$)`), async (route) => {
      currentStatus = 'APPROVED'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: paymentId,
          bookingId,
          provider: 'SIMULATED',
          amount: 15000,
          currency: 'CLP',
          status: 'APPROVED',
          checkoutUrl: null,
          checkoutExpiresAt: null,
          manual: false,
          approvedAt: new Date().toISOString(),
          rejectedAt: null,
          expiredAt: null,
          refundedAt: null,
          createdAt: new Date().toISOString(),
        }),
      })
    })

    await page.goto(`/reservas/pagar/${paymentId}`)
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Pendiente de pago').first()).toBeVisible({ timeout: 10000 })

    await page.getByRole('button', { name: 'Simular pago aprobado' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('Pago aprobado').first()).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('$15.000')).toBeVisible()
  })

  test('Cancelacion - muestra datos de la reserva', async ({ page }) => {
    const token = crypto.randomUUID();
    await page.route(new RegExp(`/api/v1/public/booking-cancellations/${token}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: crypto.randomUUID(),
          serviceName: 'Corte de cabello',
          locationName: 'Sucursal Maipu',
          professionalName: 'Maria',
          date: '2026-06-30',
          time: '15:00',
          status: 'PENDING',
        }),
      });
    });

    await page.goto(`/reservas/cancelar/${token}`);
    await expect(page.getByText('Corte de cabello').first()).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('Sucursal Maipu').first()).toBeVisible();
    await expect(page.getByText('Maria').first()).toBeVisible();
  });

  test('Reprogramacion - muestra opciones de fecha', async ({ page }) => {
    const token = crypto.randomUUID();
    await page.route(new RegExp(`/api/v1/public/booking-reschedules/${token}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: crypto.randomUUID(),
          serviceName: 'Manicure',
          currentDate: '2026-06-28',
          currentTime: '10:00',
          proposedDate: '2026-06-30',
          proposedTime: '11:00',
          status: 'PENDING',
        }),
      });
    });

    await page.goto(`/reservas/reprogramar/${token}`);
    await expect(page.getByText('Manicure').first()).toBeVisible({ timeout: 5000 });
  });

  test('Confirmacion - datos y boton de confirmar', async ({ page }) => {
    const token = crypto.randomUUID();
    await page.route(new RegExp(`/api/v1/public/booking-confirmations/${token}(\\?|$)`), async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          bookingId: crypto.randomUUID(),
          serviceName: 'Depilacion laser',
          locationName: 'Sucursal Maipu',
          professionalName: 'Dr. Perez',
          date: '2026-07-01',
          time: '14:00',
          expiresAt: new Date(Date.now() + 3600000).toISOString(),
          status: 'PENDING',
        }),
      });
    });

    await page.goto(`/reservas/confirmar/${token}`);
    await expect(page.getByText('Depilacion laser').first()).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('Dr. Perez').first()).toBeVisible();
  });

});
