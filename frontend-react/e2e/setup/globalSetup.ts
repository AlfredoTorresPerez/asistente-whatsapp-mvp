import { request } from '@playwright/test'
import type { FullConfig } from '@playwright/test'

async function globalSetup(_config: FullConfig) {
  console.log('')
  console.log('=== QA Auto — Global Setup ===')
  console.log('')

  // Verificar conectividad basica
  const frontendOk = await checkEndpoint('http://localhost:5173', 'Frontend')
  const backendOk = await checkEndpoint('http://localhost:8080', 'Backend')

  if (!frontendOk) {
    console.warn('  [!] Frontend no disponible. Las pruebas de UI fallaran.')
    console.warn('  [!] Ejecuta: docker compose -f ../docker-compose.local.yml up -d')
  }
  if (!backendOk) {
    console.warn('  [!] Backend no disponible. Las pruebas de API fallaran.')
  }

  // Crear directorios necesarios
  const fs = await import('node:fs')
  for (const dir of ['e2e/screenshots', 'e2e/traces', 'e2e/reports']) {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true })
      console.log(`  [OK] Directorio creado: ${dir}`)
    }
  }

  console.log('')
  console.log('=== Setup completado ===')
  console.log('')
}

async function checkEndpoint(url: string, name: string): Promise<boolean> {
  try {
    const ctx = await request.newContext({ baseURL: url, timeout: 5000 })
    const response = await ctx.get('/')
    await ctx.dispose()
    console.log(`  [OK] ${name} responde (${response.status()})`)
    return true
  } catch (error) {
    console.warn(`  [!] ${name} NO disponible: ${error}`)
    return false
  }
}

export default globalSetup
