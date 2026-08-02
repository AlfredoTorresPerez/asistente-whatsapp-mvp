import { createRequire } from 'node:module'
import path from 'node:path'
const require = createRequire(path.join(process.cwd(), 'node_modules'))
const { chromium } = require('playwright')

const GRAFANA = 'http://localhost:3000'
const OUT = 'docs/observabilidad-capturas'
const DASHBOARDS = [
  ['asistente-resumen-general', 'Resumen General'],
  ['asistente-agenda-reservas', 'Agenda y Reservas'],
  ['asistente-inteligencia-artificial', 'Inteligencia Artificial'],
  ['asistente-whatsapp-cloud-api', 'WhatsApp Cloud API'],
  ['asistente-infraestructura', 'Infraestructura'],
  ['asistente-registros-trazas', 'Registros y Trazas'],
]

async function main() {
  const browser = await chromium.launch()
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } })
  await page.goto(`${GRAFANA}/login`, { waitUntil: 'networkidle' })
  await page.getByLabel(/user/i).first().fill('admin')
  await page.locator('input[type="password"]').fill('asistente-demo-2026')
  await page.keyboard.press('Enter')
  await page.waitForTimeout(5000)
  if (page.url().includes('/login')) throw new Error('login failed')
  await page.waitForTimeout(3000)

  for (const [uid, title] of DASHBOARDS) {
    await page.goto(`${GRAFANA}/d/${uid}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(7000)
    const frame = page.frameLocator('iframe[title*="dashboard"]').first()
    await frame.locator('body').waitFor({ timeout: 10000 }).catch(() => undefined)
    await page.screenshot({ path: `${OUT}/${uid}.png`, fullPage: false })
    console.log(`captured: ${title} (${uid})`)
  }
  await browser.close()
}

main().catch((err) => {
  console.error('CAPTURE FAILED:', err)
  process.exit(1)
})
