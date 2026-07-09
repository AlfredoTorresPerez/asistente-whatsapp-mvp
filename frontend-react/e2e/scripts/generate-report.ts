import * as fs from 'fs'
import * as path from 'path'

const WORKSPACE_ROOT = path.resolve(__dirname, '..', '..')
const E2E_ROOT = path.resolve(WORKSPACE_ROOT, 'e2e')
const REPORT_DIR = path.resolve(E2E_ROOT, 'reports')
const RESULTS_JSON = path.resolve(REPORT_DIR, 'test-results.json')
const RESULTS_MD = path.resolve(REPORT_DIR, 'test-status-report.md')

interface PlaywrightTestCase {
  title: string
  status: string
  duration: number
  errors?: Array<{ message?: string }>
  file?: string
  line?: number
}

interface PlaywrightSuite {
  title: string
  file?: string
  status?: string
  suites?: PlaywrightSuite[]
  specs?: Array<{
    title: string
    ok: boolean
    status: string
    tests: Array<PlaywrightTestCase>
    file?: string
    line?: number
  }>
}

function flattenResults(suite: PlaywrightSuite, results: Array<{ id: string; estado: string; error?: string; duracionMs?: number }> = []) {
  for (const spec of suite.specs ?? []) {
    const specName = spec.title
    for (const testCase of spec.tests ?? []) {
      const state = testCase.status === 'expected' ? 'PASSED' : testCase.status === 'unexpected' ? 'FAILED' : testCase.status === 'skipped' ? 'SKIPPED' : 'BLOCKED'
      results.push({
        id: specName,
        estado: state,
        error: testCase.errors?.[0]?.message,
        duracionMs: testCase.duration,
      })
    }
  }
  for (const subSuite of suite.suites ?? []) {
    flattenResults(subSuite, results)
  }
  return results
}

function generateReport() {
  if (!fs.existsSync(RESULTS_JSON)) {
    console.log(`No se encontró ${RESULTS_JSON}. Ejecute primero la suite de pruebas.`)
    return
  }

  const raw = JSON.parse(fs.readFileSync(RESULTS_JSON, 'utf-8'))
  const flatResults = flattenResults(raw)
  const passed = flatResults.filter(r => r.estado === 'PASSED').length
  const failed = flatResults.filter(r => r.estado === 'FAILED').length
  const skipped = flatResults.filter(r => r.estado === 'SKIPPED').length
  const blocked = flatResults.filter(r => r.estado === 'BLOCKED').length
  const critical = flatResults.filter(r => r.estado === 'FAILED')

  const md = `# Status de Pruebas Automatizadas — Asistente de Negocios WhatsApp

**Fecha de ejecución:** ${new Date().toISOString()}
**Fuente de casuísticas:** agenda_digital_whatsapp_casuisticas.xlsx

## Resumen ejecutivo

| Total | Passed | Failed | Skipped | Blocked |
|-------|-------:|-------:|--------:|--------:|
| ${flatResults.length} | ${passed} | ${failed} | ${skipped} | ${blocked} |

## Detalle de casos

| ID | Estado | Error |
|----|--------|-------|
${flatResults.map(r => `| ${r.id} | ${r.estado} | ${r.error ?? '-'} |`).join('\n')}

${critical.length > 0 ? `## Fallos críticos\n${critical.map(f => `- **${f.id}**: ${f.error}`).join('\n')}\n` : ''}

## Recomendaciones
- **Alta**: Implementar endpoint de simulación WhatsApp para desbloquear pruebas TST-001 a TST-056
- **Alta**: Crear seed QA_AUTO_ en backend para datos de prueba aislados
- **Media**: Configurar testcontainers para pruebas de base de datos
- **Media**: Agregar endpoint de concurrencia controlada para prueba TST-034
- **Baja**: Agregar más sucursales (3-8) en demo data para pruebas multi-sucursal
`

  fs.writeFileSync(RESULTS_MD, md, 'utf-8')
  console.log(`Reporte generado: ${RESULTS_MD}`)
  console.log(`Resumen: ${passed} passed, ${failed} failed, ${skipped} skipped, ${blocked} blocked de ${flatResults.length} total`)
}

generateReport()
