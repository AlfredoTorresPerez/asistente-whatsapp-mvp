import { test as base } from '@playwright/test'
import { injectMockSession, setupDefaultMocks } from './helpers/auth.helper'
import {
  setupMocksForAgenda,
  setupMockCalendarResponse,
  buildMockCalendarItems,
} from './helpers/agenda.helper'

type QAFixtures = {
  qaSession: void
  qaAgendaPage: void
}

export const test = base.extend<QAFixtures>({
  qaSession: [async ({ page }, use) => {
    await injectMockSession(page)
    await setupDefaultMocks(page)
    await use()
  }, { auto: false }],

  qaAgendaPage: [async ({ page }, use) => {
    await setupMocksForAgenda(page)
    await setupMockCalendarResponse(page, buildMockCalendarItems(3))
    await page.goto('/agenda')
    await page.waitForLoadState('networkidle')
    await use()
  }, { auto: false }],
})

export { expect } from '@playwright/test'
