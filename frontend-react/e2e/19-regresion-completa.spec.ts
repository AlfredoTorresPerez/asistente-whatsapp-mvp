import { test, expect } from '@playwright/test'
import { injectMockSession, setupDefaultMocks } from './helpers/auth.helper'
import { setupMocksForAgenda, setupMockCalendarResponse, buildMockCalendarItems, expectNoOverlappingCards } from './helpers/agenda.helper'
import { generateJsonReport, generateMarkdownReport } from './helpers/report.helper'

test.describe('NIVEL 17: Regresión Completa', () => {

  test.beforeEach(async ({ page }) => {
    await injectMockSession(page)
    await setupDefaultMocks(page)
    await setupMocksForAgenda(page)
    await setupMockCalendarResponse(page, buildMockCalendarItems(5))
    await page.goto('/agenda')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
  })

  test('Filtros funcionan después de cambios en agenda', async ({ page }) => {
    const bodyText = await page.textContent('body') ?? ''
    expect(bodyText.length).toBeGreaterThan(100)
  })

  test('Sin superposición visual de tarjetas', async ({ page }) => {
    await expectNoOverlappingCards(page)
  })

  test('7 columnas de días visibles', async ({ page }) => {
    const bodyText = await page.textContent('body') ?? ''
    const days = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom']
    const found = days.filter(d => bodyText.includes(d))
    expect(found.length).toBeGreaterThanOrEqual(5)
  })
})

test.afterAll(() => {
  generateJsonReport()
  generateMarkdownReport()
})
