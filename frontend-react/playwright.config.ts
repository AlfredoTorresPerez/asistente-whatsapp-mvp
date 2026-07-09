import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : 2,
  timeout: 60000,
  expect: { timeout: 10000 },
  globalSetup: './e2e/setup/globalSetup.ts',
  globalTeardown: './e2e/setup/globalTeardown.ts',
  reporter: [
    ['html', { outputFolder: 'e2e/reports/html-report', open: 'never' }],
    ['list'],
    ['json', { outputFile: 'e2e/reports/test-results.json' }],
    ['junit', { outputFile: 'e2e/reports/test-results.xml' }],
  ],
  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    cwd: '.',
    timeout: 30000,
  },
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15000,
    navigationTimeout: 20000,
  },
  projects: [
    {
      name: 'smoke',
      testMatch: '01-smoke.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'auth',
      testMatch: '02-auth.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'agenda-basica',
      testMatch: '03-agenda-basica.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'agenda-visual',
      testMatch: '04-agenda-visual.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'whatsapp-reserva',
      testMatch: '05-whatsapp-reserva-simulada.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'whatsapp-cancelacion',
      testMatch: '06-whatsapp-cancelacion-simulada.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'whatsapp-reprogramacion',
      testMatch: '07-whatsapp-reprogramacion-simulada.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'confirmacion-publica',
      testMatch: '08-confirmacion-publica.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'seguridad',
      testMatch: '12-seguridad.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'smoke-firefox',
      testMatch: '01-smoke.spec.ts',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'smoke-webkit',
      testMatch: '01-smoke.spec.ts',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'confirmacion-publica-firefox',
      testMatch: '08-confirmacion-publica.spec.ts',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'confirmacion-publica-webkit',
      testMatch: '08-confirmacion-publica.spec.ts',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'seguridad-firefox',
      testMatch: '12-seguridad.spec.ts',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'seguridad-webkit',
      testMatch: '12-seguridad.spec.ts',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'reprogramacion',
      testMatch: '13-reprogramacion-interfaz.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'cancelacion',
      testMatch: '14-cancelacion-interfaz.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'sucursales',
      testMatch: '15-sucursales.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'profesional',
      testMatch: '16-profesional.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'base-datos',
      testMatch: '17-base-datos.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'concurrencia',
      testMatch: '18-concurrencia.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'regresion',
      testMatch: '19-regresion-completa.spec.ts',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'all-chromium',
      testIgnore: ['01-smoke.spec.ts', '02-auth.spec.ts', '03-agenda-basica.spec.ts',
        '04-agenda-visual.spec.ts', '05-whatsapp-reserva-simulada.spec.ts',
        '06-whatsapp-cancelacion-simulada.spec.ts', '07-whatsapp-reprogramacion-simulada.spec.ts',
        '08-confirmacion-publica.spec.ts', '12-seguridad.spec.ts'],
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
