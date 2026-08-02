import { test, expect, type Browser } from '@playwright/test'

const BOOKING_ID = 'dff87343-1764-41d4-9274-6e22d21fdcfa'
const SLOT_DAY = '2026-08-06'

async function loginAndOpenReschedule(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByLabel('Correo').fill('admin@demo.cl')
  await page.getByPlaceholder('Ingresa tu contrasena').fill('Cambiar123!')
  await page.getByRole('button', { name: 'Ingresar' }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
  await page.goto(`/appointments/${BOOKING_ID}/reschedule`)
  await expect(page.getByRole('heading', { name: 'Reprogramar cita' })).toBeVisible({ timeout: 15000 })
  await expect(page.getByText('Cita actual')).toBeVisible({ timeout: 15000 })
}

async function selectSlotAndCapture(page: import('@playwright/test').Page, baseName: string) {
  await page.getByLabel('Selecciona una nueva fecha').fill(SLOT_DAY)
  const firstSlot = page.getByRole('button', { name: /Horario disponible/ }).first()
  await expect(firstSlot).toBeVisible({ timeout: 15000 })
  await page.screenshot({ path: `e2e/reports/${baseName}-fecha-sin-seleccion.png`, fullPage: true })
  await firstSlot.click()
  await expect(page.getByRole('button', { name: 'Guardar nueva fecha' })).toBeEnabled()
  await page.screenshot({ path: `e2e/reports/${baseName}-horario-seleccionado.png`, fullPage: true })
}

async function runDesktop(browser: Browser) {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  await loginAndOpenReschedule(page)
  await selectSlotAndCapture(page, 'evidencia-reprogramacion-desktop')
  await page.close()
}

async function runMobile(browser: Browser) {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } })
  await loginAndOpenReschedule(page)
  await selectSlotAndCapture(page, 'evidencia-reprogramacion-movil')
  await page.close()
}

test.describe('Evidencia visual - pantalla Reprogramar cita simplificada', () => {
  test('desktop 1440x900', async ({ browser }) => {
    await runDesktop(browser)
  })

  test('movil 390x844', async ({ browser }) => {
    await runMobile(browser)
  })
})
