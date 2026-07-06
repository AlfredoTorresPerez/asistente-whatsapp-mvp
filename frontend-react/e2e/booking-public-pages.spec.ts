import { test, expect } from '@playwright/test';

test.describe('Paginas publicas de reservas', () => {

  test('Pago - muestra checkout pendiente', async ({ page }) => {
    const bookingId = crypto.randomUUID();
    await page.route(`**/api/v1/public/booking-payments/${bookingId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: bookingId,
          amount: 10000,
          currency: 'CLP',
          status: 'PENDING',
          description: 'Depilacion laser',
          checkoutUrl: null,
        }),
      });
    });

    await page.goto(`/reservas/pagar/${bookingId}`);
    await expect(page.getByRole('heading', { name: 'Pendiente de pago' })).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('$10.000')).toBeVisible();
  });

  test('Pago - muestra confirmado cuando esta aprobado', async ({ page }) => {
    const bookingId = crypto.randomUUID();
    await page.route(`**/api/v1/public/booking-payments/${bookingId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: bookingId,
          amount: 10000,
          currency: 'CLP',
          status: 'APPROVED',
          description: 'Depilacion laser',
          checkoutUrl: null,
        }),
      });
    });

    await page.goto(`/reservas/pagar/${bookingId}`);
    await expect(page.getByRole('heading', { name: 'Pago confirmado' })).toBeVisible({ timeout: 5000 });
  });

  test('Cancelacion - muestra datos de la reserva', async ({ page }) => {
    const token = crypto.randomUUID();
    await page.route(`**/api/v1/public/booking-cancellations/${token}`, async (route) => {
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
    await expect(page.getByText('Corte de cabello')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('Sucursal Maipu')).toBeVisible();
    await expect(page.getByText('Maria')).toBeVisible();
  });

  test('Reprogramacion - muestra opciones de fecha', async ({ page }) => {
    const token = crypto.randomUUID();
    await page.route(`**/api/v1/public/booking-reschedules/${token}`, async (route) => {
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
    await expect(page.getByText('Manicure')).toBeVisible({ timeout: 5000 });
  });

  test('Confirmacion - datos y boton de confirmar', async ({ page }) => {
    const token = crypto.randomUUID();
    await page.route(`**/api/v1/public/booking-confirmations/${token}`, async (route) => {
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
    await expect(page.getByText('Depilacion laser')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('Dr. Perez')).toBeVisible();
  });

});
