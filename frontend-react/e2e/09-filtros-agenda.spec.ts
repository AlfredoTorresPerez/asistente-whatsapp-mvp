import { test, expect } from '@playwright/test'
import { setupMocksForAgenda, setupMockCalendarResponse, buildMockCalendarItems, openCompleteAgenda } from './helpers/agenda.helper'

test.describe('NIVEL 4 — Filtros de agenda', () => {
  test.beforeEach(async ({ page }) => {
    await setupMocksForAgenda(page)
  })

  test('QA-04-001: Select Servicio muestra opciones del filtro', async ({ page }) => {
    await setupMockCalendarResponse(page)
    await openCompleteAgenda(page)

    const select = page.getByLabel('Servicio').first()
    const options = await select.locator('option').allTextContents()
    expect(options).toContain('Todos los servicios')
    expect(options).toContain('Limpieza facial profunda · 30 min')
    expect(options).toContain('Depilacion laser · 45 min')
  })

  test('QA-04-002: Select Profesional muestra opciones del filtro', async ({ page }) => {
    await setupMockCalendarResponse(page)
    await openCompleteAgenda(page)

    const select = page.getByLabel('Profesional')
    const options = await select.locator('option').allTextContents()
    expect(options).toContain('Todos los profesionales')
    expect(options).toContain('Profesional estetica avanzada')
    expect(options).toContain('Dra. Maria Perez')
  })

  test('QA-04-003: Select Cabina muestra opciones del filtro', async ({ page }) => {
    await setupMockCalendarResponse(page)
    await openCompleteAgenda(page)

    const select = page.getByLabel('Cabina')
    const options = await select.locator('option').allTextContents()
    expect(options).toContain('Todas las cabinas')
    expect(options).toContain('Cabina 1')
    expect(options).toContain('Cabina 2')
  })

  test('QA-04-004: Select Sucursal muestra sucursales', async ({ page }) => {
    await setupMockCalendarResponse(page)
    await openCompleteAgenda(page)

    const select = page.getByLabel('Sucursal').first()
    const options = await select.locator('option').allTextContents()
    expect(options).toContain('Todas las sucursales')
    expect(options).toContain('QA Sucursal Providencia - Providencia')
  })

  test('QA-04-005: Select Estado muestra opciones', async ({ page }) => {
    await setupMockCalendarResponse(page)
    await openCompleteAgenda(page)

    const select = page.getByLabel('Estado')
    const options = await select.locator('option').allTextContents()
    expect(options).toContain('Reservas activas')
    expect(options).toContain('Reservas confirmadas')
    expect(options).toContain('Canceladas')
  })

  test('QA-04-006: Seleccionar servicio cambia las reservas mostradas', async ({ page }) => {
    await setupMockCalendarResponse(page, buildMockCalendarItems(2))
    await openCompleteAgenda(page)

    await expect(page.getByText('QA_AUTO_CLIENTE_1').first()).toBeVisible({ timeout: 5000 })

    const filteredItems = buildMockCalendarItems(1).map((item, i) => ({
      ...item,
      customerName: `QA_AUTO_CLIENTE_FILTRADO_${i + 1}`,
    }))
    await page.route(/\/api\/v1\/agenda\/calendar/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: filteredItems, totalItems: filteredItems.length }) })
    })

    await page.getByLabel('Servicio').first().selectOption('svc-qa-001')
    await page.getByRole('button', { name: 'Actualizar' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('QA_AUTO_CLIENTE_FILTRADO_1').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-04-007: Filtro combinado profesional y estado', async ({ page }) => {
    await setupMockCalendarResponse(page, buildMockCalendarItems(3))
    await openCompleteAgenda(page)

    const filteredItems = [
      { ...buildMockCalendarItems(1)[0], customerName: 'QA_AUTO_CLIENTE_COMBINADO', status: 'CONFIRMED', professionalId: 'pro-qa-001' },
    ]
    await page.route(/\/api\/v1\/agenda\/calendar/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: filteredItems, totalItems: filteredItems.length }) })
    })

    await page.getByLabel('Profesional').first().selectOption('pro-qa-001')
    await page.getByLabel('Estado').first().selectOption('CONFIRMED')
    await page.getByRole('button', { name: 'Actualizar' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('QA_AUTO_CLIENTE_COMBINADO').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-04-008: Filtro de fecha cambia la semana visible', async ({ page }) => {
    await setupMockCalendarResponse(page, buildMockCalendarItems(2))
    await openCompleteAgenda(page)

    await expect(page.getByText('QA_AUTO_CLIENTE_1').first()).toBeVisible({ timeout: 5000 })

    const futureItems = buildMockCalendarItems(1).map((item, i) => ({
      ...item,
      startsAt: '2026-08-03T10:00:00-04:00',
      dateLocal: '2026-08-03',
      customerName: `QA_AUTO_FUTURO_${i + 1}`,
    }))
    await page.route(/\/api\/v1\/agenda\/calendar/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: futureItems, totalItems: futureItems.length }) })
    })

    const dateInput = page.locator('input[type="date"]').first()
    await dateInput.fill('2026-08-03')
    await dateInput.press('Enter')
    await page.waitForTimeout(500)

    await page.getByRole('button', { name: 'Actualizar' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('QA_AUTO_FUTURO_1').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-04-009: Boton Actualizar refetches calendario', async ({ page }) => {
    await setupMockCalendarResponse(page, buildMockCalendarItems(1))
    await openCompleteAgenda(page)

    await expect(page.getByText('QA_AUTO_CLIENTE_1').first()).toBeVisible({ timeout: 5000 })

    const updatedItems = buildMockCalendarItems(1).map((item, i) => ({
      ...item,
      customerName: `QA_AUTO_ACTUALIZADO_${i + 1}`,
    }))
    await page.route(/\/api\/v1\/agenda\/calendar/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: updatedItems, totalItems: updatedItems.length }) })
    })

    await page.getByRole('button', { name: 'Actualizar' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('QA_AUTO_ACTUALIZADO_1').first()).toBeVisible({ timeout: 5000 })
  })

  test('QA-04-010: Reset de filtros muestra vista por defecto', async ({ page }) => {
    const items = buildMockCalendarItems(2)
    await setupMockCalendarResponse(page, items)
    await openCompleteAgenda(page)

    await expect(page.getByText('QA_AUTO_CLIENTE_1').first()).toBeVisible({ timeout: 5000 })

    await page.getByLabel('Servicio').first().selectOption('svc-qa-001')
    await page.getByLabel('Estado').selectOption('CONFIRMED')

    const resetItems = items.filter((_, i) => i < 1)
    await page.route(/\/api\/v1\/agenda\/calendar/, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: resetItems, totalItems: resetItems.length }) })
    })

    await page.getByLabel('Servicio').first().selectOption('')
    await page.getByLabel('Estado').selectOption('')
    await page.getByRole('button', { name: 'Actualizar' }).click()
    await page.waitForTimeout(1000)

    await expect(page.getByText('QA_AUTO_CLIENTE_1').first()).toBeVisible({ timeout: 5000 })
  })
})
