import { test, expect } from '@playwright/test'

test.describe('LOCAL MATURITY — Validacion del entorno local', () => {
  test('Frontend carga en localhost:5173', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    expect(page.url()).toContain('localhost:5173')
  })

  test('Login page con formulario', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await expect(page.getByText('Iniciar sesion')).toBeVisible({ timeout: 10000 })
    await expect(page.getByLabel('Correo')).toBeVisible()
  })

  test.describe('API Backend', () => {
    let accessToken: string

    test.beforeAll(async ({ request }) => {
      const resp = await request.post('http://localhost:8080/api/v1/auth/login', {
        data: { email: 'admin@demo.cl', password: 'Cambiar123!' },
      })
      expect(resp.status()).toBe(200)
      const body = await resp.json()
      accessToken = body.accessToken
      expect(typeof accessToken).toBe('string')
    })

    test('Health endpoint UP', async ({ request }) => {
      const resp = await request.get('http://localhost:8080/actuator/health')
      expect(resp.status()).toBe(200)
      const body = await resp.json()
      expect(body.status).toBe('UP')
    })

    test('Login con credenciales demo', async ({ request }) => {
      const resp = await request.post('http://localhost:8080/api/v1/auth/login', {
        data: { email: 'admin@demo.cl', password: 'Cambiar123!' },
      })
      expect(resp.status()).toBe(200)
      const body = await resp.json()
      expect(body.accessToken).toBeDefined()
      expect(typeof body.accessToken).toBe('string')
    })

    test('Company endpoint autenticado', async ({ request }) => {
      const resp = await request.get('http://localhost:8080/api/v1/company', {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      expect(resp.status()).toBe(200)
      const body = await resp.json()
      expect(body.id).toBeDefined()
    })

    test('WhatsApp Web status endpoint responde', async ({ request }) => {
      const resp = await request.get('http://localhost:8080/api/v1/whatsapp-web/status', {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      const text = await resp.text()
      const parsed = JSON.parse(text)
      expect(parsed).toBeDefined()
    })

    test('Conversaciones lista autenticada', async ({ request }) => {
      const resp = await request.get('http://localhost:8080/api/v1/conversations?page=0&size=5', {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      expect(resp.ok()).toBeTruthy()
    })
  })

  test.describe('WhatsApp Public URL', () => {
    test('Pagina publica de centros carga con contenido', async ({ page }) => {
      await page.goto('/centro-estetica-bella')
      await page.waitForLoadState('networkidle')
      await expect(page.getByRole('heading', { name: /Realza tu belleza/i })).toBeVisible({ timeout: 10000 })
      await expect(page.getByRole('button', { name: /Agendar por WhatsApp/i })).toBeVisible()
    })
  })
})
