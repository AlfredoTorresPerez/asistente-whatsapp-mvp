import { test, expect, type Page } from '@playwright/test'

const DAY = (() => {
  const d = new Date(Date.now() + 20 * 24 * 60 * 60 * 1000)
  return d.toISOString().slice(0, 10)
})()

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function isoTime(hour: number, minute: number) {
  return `${DAY}T${pad(hour)}:${pad(minute)}:00-04:00`
}

function endIso(hour: number, minute: number) {
  const total = hour * 60 + minute + 45
  return isoTime(Math.floor(total / 60), total % 60)
}

function slot(hour: number, minute: number, professionalId: string, professionalName: string) {
  return {
    startsAt: isoTime(hour, minute),
    endsAt: endIso(hour, minute),
    locationId: 'loc-1',
    locationName: 'Sucursal Centro',
    serviceId: 'svc-1',
    serviceName: 'Limpieza facial',
    durationMinutes: 45,
    professionalId,
    professionalName,
    roomId: null,
    roomName: null,
    available: true,
    reason: 'Disponible',
  }
}

function availabilityPayload() {
  return {
    locationId: 'loc-1',
    locationName: 'Sucursal Centro',
    serviceId: 'svc-1',
    serviceName: 'Limpieza facial',
    date: DAY,
    durationMinutes: 45,
    requiresRoom: false,
    requiresDeposit: false,
    slots: [
      slot(9, 0, 'pro-carla', 'Carla Mendez'),
      slot(9, 45, 'pro-ana', 'Ana Profesional'),
      slot(9, 45, 'pro-carla', 'Carla Mendez'),
      slot(11, 30, 'pro-carla', 'Carla Mendez'),
      slot(12, 0, 'pro-ana', 'Ana Profesional'),
      slot(14, 30, 'pro-carla', 'Carla Mendez'),
      slot(18, 0, 'pro-ana', 'Ana Profesional'),
    ],
  }
}

async function mockApi(page: Page) {
  await page.route('**/api/v1/public/landing/whatsapp-entry', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '{}' }),
  )
  await page.route('**/api/v1/public/landing/categories', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 'cat-1',
          code: 'FACIAL',
          name: 'Facial',
          description: 'Tratamientos faciales',
          active: true,
        },
      ]),
    }),
  )
  await page.route('**/api/v1/public/landing/categories/FACIAL/services', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 'svc-1',
          code: 'LIMPEZA',
          name: 'Limpieza facial',
          description: null,
          durationMinutes: 45,
          priceBase: 15000,
          categoryCode: 'FACIAL',
          categoryName: 'Facial',
          active: true,
          requiresPriorEvaluation: false,
          requiresInformedConsent: false,
        },
      ]),
    }),
  )
  await page.route('**/api/v1/public/landing/services/svc-1/branches', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 'loc-1',
          code: 'CENTRO',
          name: 'Sucursal Centro',
          address: 'Avenida Siempre Viva 123',
          city: 'Santiago',
          commune: 'Santiago',
          phone: null,
        },
      ]),
    }),
  )
  await page.route('**/api/v1/public/landing/availability', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(availabilityPayload()),
    }),
  )
}

async function collectPageErrors(page: Page) {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`))
  page.on('console', (message) => {
    if (message.type() === 'error') {
      errors.push(`console.error: ${message.text()}`)
    }
  })
  return errors
}

async function reachStep4(page: Page) {
  await page.goto('/reservar')
  await page.getByRole('button', { name: /Facial/ }).click()
  await page.getByRole('button', { name: 'Continuar' }).click()
  await page.getByRole('button', { name: /Limpieza facial/ }).click()
  await page.getByRole('button', { name: 'Continuar' }).click()
  await page.getByRole('button', { name: /Sucursal Centro/ }).click()
  await page.getByRole('button', { name: 'Continuar' }).click()
  await expect(page.getByText('Selecciona una fecha')).toBeVisible()
}

async function pickDate(page: Page) {
  await page.getByRole('button', { name: 'Seleccionar fecha' }).click()
  const input = page.locator('input[type="date"]')
  await expect(input).toBeAttached()
  await input.evaluate((el, value) => {
    const node = el as HTMLInputElement
    node.value = value
    node.dispatchEvent(new Event('change', { bubbles: true }))
  }, DAY)
}

test.describe('Reserva publica - paso Fecha y hora', () => {
  test('flujo completo en desktop: tramos, orden, seleccion y limpieza', async ({ page }) => {
    const pageErrors = await collectPageErrors(page)
    await mockApi(page)
    await reachStep4(page)
    await pickDate(page)

    await expect(
      page.getByText('Selecciona manana o tarde para ver los horarios disponibles.'),
    ).toBeVisible()
    const morning = page.getByRole('button', { name: 'Manana, 4 horarios disponibles.' })
    const afternoon = page.getByRole('button', { name: 'Tarde, 3 horarios disponibles.' })
    await expect(morning).toBeVisible()
    await expect(afternoon).toBeVisible()
    await expect(page.getByRole('button', { name: 'Continuar' })).toBeDisabled()
    await expect(page.getByRole('button', { name: /^Hora \d{2}:\d{2}/ })).toHaveCount(0)

    await morning.click()
    await expect(page.getByText('Horarios disponibles en la manana')).toBeVisible()
    const cards = page.getByRole('button', { name: /^Hora \d{2}:\d{2}/ })
    await expect(cards).toHaveCount(4)
    const times = await cards.evaluateAll((btns) =>
      btns.map((b) => (b.getAttribute('aria-label') ?? '').match(/Hora (\d{2}:\d{2})/)?.[1] ?? ''),
    )
    expect(times).toEqual(['09:00', '09:45', '09:45', '11:30'])
    await expect(page.getByRole('button', { name: /^Hora 12:00/ })).toHaveCount(0)

    await page.getByRole('button', { name: /^Hora 09:00/ }).click()
    await expect(page.getByText('Hora seleccionada: 09:00')).toBeVisible()
    await expect(page.getByText('Finaliza: 09:45')).toBeVisible()
    await expect(page.getByText('Profesional: Carla Mendez')).toBeVisible()
    await expect(page.getByText('Cabina: No requerida')).toBeVisible()
    await expect(page.getByText('Sucursal: Sucursal Centro')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Continuar' })).toBeEnabled()

    await afternoon.click()
    await expect(page.getByText('Horarios disponibles en la tarde')).toBeVisible()
    await expect(page.getByText('Hora seleccionada: 09:00')).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Continuar' })).toBeDisabled()
    await page.screenshot({
      path: 'e2e/screenshots/reservar-fecha-hora-desktop-tarde.png',
      fullPage: true,
    })

    await morning.click()
    await page.getByRole('button', { name: /^Hora 11:30/ }).click()
    await expect(page.getByText('Hora seleccionada: 11:30')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Continuar' })).toBeEnabled()
    await page.screenshot({
      path: 'e2e/screenshots/reservar-fecha-hora-desktop-seleccion.png',
      fullPage: true,
    })

    await page.getByRole('button', { name: 'Continuar' }).click()
    await expect(page.getByText('Nombre completo')).toBeVisible()
    await page.getByLabel('Nombre completo').fill('Maria Perez')
    await page.getByLabel('Telefono').fill('56912345678')
    await page.getByRole('button', { name: 'Continuar' }).click()
    await expect(page.getByText('Resumen de tu reserva')).toBeVisible()
    await expect(page.getByText('Limpieza facial')).toBeVisible()
    await expect(page.getByText('Sucursal Centro')).toBeVisible()
    await expect(page.getByText(/11:30 - 12:15/)).toBeVisible()
    await expect(page.getByText('Carla Mendez')).toBeVisible()
    await expect(page.getByText('Horario', { exact: true })).toBeVisible()
    expect(pageErrors).toEqual([])
  })

  test('flujo funcional en movil 390x844', async ({ page }) => {
    const pageErrors = await collectPageErrors(page)
    await page.setViewportSize({ width: 390, height: 844 })
    await mockApi(page)
    await reachStep4(page)
    await pickDate(page)

    await expect(
      page.getByText('Selecciona manana o tarde para ver los horarios disponibles.'),
    ).toBeVisible()
    await page.getByRole('button', { name: 'Manana, 4 horarios disponibles.' }).click()
    await expect(page.getByText('Horarios disponibles en la manana')).toBeVisible()
    await expect(page.getByRole('button', { name: /^Hora \d{2}:\d{2}/ })).toHaveCount(4)
    await page.screenshot({
      path: 'e2e/screenshots/reservar-fecha-hora-movil-tramo.png',
      fullPage: true,
    })

    await page.getByRole('button', { name: /^Hora 09:45/ }).first().click()
    await expect(page.getByText('Hora seleccionada: 09:45')).toBeVisible()
    await expect(page.getByText('Profesional: Ana Profesional')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Continuar' })).toBeEnabled()
    await page.screenshot({
      path: 'e2e/screenshots/reservar-fecha-hora-movil-seleccion.png',
      fullPage: true,
    })
    expect(pageErrors).toEqual([])
  })
})
