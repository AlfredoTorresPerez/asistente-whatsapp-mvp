import * as fs from 'fs'
import * as path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export interface TestResult {
  id: string
  nivel: string
  modulo: string
  caso: string
  estado: 'PASSED' | 'FAILED' | 'SKIPPED' | 'BLOCKED'
  evidencia?: string
  error?: string
  recomendacion?: string
  duracionMs?: number
}

const results: TestResult[] = []
const REPORT_DIR = path.resolve(__dirname, '..', 'reports')

function ensureReportDir() {
  if (!fs.existsSync(REPORT_DIR)) fs.mkdirSync(REPORT_DIR, { recursive: true })
}

export function registerTestCase(result: TestResult) {
  results.push(result)
}

export function markPassed(id: string, evidencia?: string) {
  registerTestCase({ id, nivel: '', modulo: '', caso: '', estado: 'PASSED', evidencia })
}

export function markFailed(id: string, error: string, recomendacion?: string) {
  registerTestCase({ id, nivel: '', modulo: '', caso: '', estado: 'FAILED', error, recomendacion })
}

export function markSkipped(id: string, reason: string) {
  registerTestCase({ id, nivel: '', modulo: '', caso: '', estado: 'SKIPPED', error: reason })
}

export function markBlocked(id: string, reason: string, recomendacion?: string) {
  registerTestCase({ id, nivel: '', modulo: '', caso: '', estado: 'BLOCKED', error: reason, recomendacion })
}

export function getResults(): TestResult[] {
  return [...results]
}

export function generateJsonReport(): string {
  ensureReportDir()
  const report = {
    fecha: new Date().toISOString(),
    ambiente: process.env.NODE_ENV ?? 'local',
    frontendUrl: process.env.BASE_URL ?? 'http://localhost:5173',
    backendUrl: process.env.API_URL ?? 'http://localhost:8080',
    total: results.length,
    passed: results.filter(r => r.estado === 'PASSED').length,
    failed: results.filter(r => r.estado === 'FAILED').length,
    skipped: results.filter(r => r.estado === 'SKIPPED').length,
    blocked: results.filter(r => r.estado === 'BLOCKED').length,
    results,
  }
  const filePath = path.join(REPORT_DIR, 'test-status-report.json')
  fs.writeFileSync(filePath, JSON.stringify(report, null, 2), 'utf-8')
  return filePath
}

export function generateMarkdownReport(): string {
  ensureReportDir()
  const passed = results.filter(r => r.estado === 'PASSED').length
  const failed = results.filter(r => r.estado === 'FAILED').length
  const skipped = results.filter(r => r.estado === 'SKIPPED').length
  const blocked = results.filter(r => r.estado === 'BLOCKED').length

  const criticalFailures = results.filter(r => r.estado === 'FAILED')
  const minorFailures: TestResult[] = []

  let md = `# Status de Pruebas Automatizadas — Asistente de Negocios WhatsApp

**Fecha de ejecución:** ${new Date().toISOString()}
**Frontend URL:** ${process.env.BASE_URL ?? 'http://localhost:5173'}
**Backend URL:** ${process.env.API_URL ?? 'http://localhost:8080'}
**Fuente de casuísticas:** agenda_digital_whatsapp_casuisticas.xlsx

## Resumen ejecutivo

| Total | Passed | Failed | Skipped | Blocked |
|-------|-------:|-------:|--------:|--------:|
| ${results.length} | ${passed} | ${failed} | ${skipped} | ${blocked} |

## Detalle de casos

| ID | Estado | Evidencia | Error | Recomendación |
|----|--------|----------|-------|---------------|
`

  for (const r of results) {
    md += `| ${r.id} | ${r.estado} | ${r.evidencia ?? '-'} | ${r.error ?? '-'} | ${r.recomendacion ?? '-'} |\n`
  }

  if (criticalFailures.length > 0) {
    md += `\n## Hallazgos críticos\n`
    for (const f of criticalFailures) {
      md += `- **${f.id}**: ${f.error}\n`
    }
  }

  md += `\n## Recomendaciones\n`
  md += `- **Alta**: Implementar endpoint de simulación WhatsApp para desbloquear pruebas TST-001 a TST-056\n`
  md += `- **Alta**: Crear seed QA_AUTO_ en backend para datos de prueba aislados\n`
  md += `- **Media**: Configurar testcontainers para pruebas de base de datos\n`
  md += `- **Media**: Agregar endpoint de concurrencia controlada para prueba TST-034\n`

  const filePath = path.join(REPORT_DIR, 'test-status-report.md')
  fs.writeFileSync(filePath, md, 'utf-8')
  return filePath
}

export function clearResults() {
  results.length = 0
}
