import type { FullConfig } from '@playwright/test'

async function globalTeardown(_config: FullConfig) {
  console.log('')
  console.log('=== QA Auto — Global Teardown ===')
  console.log('')
  console.log('  Pruebas E2E finalizadas.')
  console.log('  Reportes disponibles en: e2e/reports/')
  console.log('  Screenshots en: e2e/screenshots/')
  console.log('  Traces en: e2e/traces/')
  console.log('')
}

export default globalTeardown
