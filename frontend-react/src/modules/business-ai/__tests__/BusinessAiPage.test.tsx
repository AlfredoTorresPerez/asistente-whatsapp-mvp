import { render, screen, waitFor, cleanup } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi } from 'vitest'
import { ToastProvider } from '../../../lib/ToastProvider'
import { BusinessAiPage } from '../pages/BusinessAiPage'
import { SHELL_SESSION_STORAGE_KEY } from '../../../lib/shellSession'

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

function setupSession() {
  window.sessionStorage.setItem(
    SHELL_SESSION_STORAGE_KEY,
    JSON.stringify({
      accessToken: 'jwt-demo-token',
      expiresAt: new Date(Date.now() + 900_000).toISOString(),
      user: {
        id: '40000000-0000-0000-0000-000000000001',
        name: 'Admin User',
        role: 'OWNER',
        businessId: '11111111-1111-1111-1111-111111111111',
        businessName: 'Centro Estetico Bella',
        timezone: 'America/Santiago',
        permissions: ['ALL'],
      },
    }),
  )
}

const defaultSettings = {
  active: true,
  mode: 'auto',
  tone: 'Cercano',
  language: 'es',
  escalationThreshold: 0.7,
  allowPrices: true,
  allowBooking: true,
  allowPromotions: false,
  requireAvailabilityCheck: true,
  allowedTopics: ['Servicios', 'Productos', 'Citas'],
  blockedTopics: ['Diagnosticos medicos', 'Temas legales'],
  updatedAt: '2026-07-30T15:00:00Z',
  activePromptVersion: 1,
}

const defaultPrompts = [
  { id: 'prompt-1', version: 1, contenido: 'Eres el asistente...', nombre: 'Prompt principal', modulo: 'AI_AGENT', tipo: 'SYSTEM_PROMPT', codigo: 'PROMPT_OPERATIVO_IA_NEGOCIO', prioridad: 1, updatedAt: '2026-07-30T15:00:00Z' },
]

let mockFetch: ReturnType<typeof vi.fn>

function setupMock(options?: { settings?: any; prompts?: any[]; logs?: any[]; services?: any[]; products?: any[]; rules?: any[] }) {
  mockFetch = vi.fn()
  vi.stubGlobal('fetch', mockFetch)

  const { settings, prompts, logs, services, products, rules } = options ?? {}

  mockFetch.mockImplementation((url: string) => {
    if (url.includes('/business-ai/settings')) return Promise.resolve(jsonResponse(settings ?? defaultSettings))
    if (url.includes('/business-ai/prompts')) return Promise.resolve(jsonResponse(prompts ?? defaultPrompts))
    if (url.includes('/esthetic/intent/logs')) return Promise.resolve(jsonResponse({ items: logs ?? [] }))
    if (url.includes('/esthetic/services')) return Promise.resolve(jsonResponse({ items: services ?? [] }))
    if (url.includes('/esthetic/products')) return Promise.resolve(jsonResponse({ items: products ?? [] }))
    if (url.includes('/esthetic/rules')) return Promise.resolve(jsonResponse({ items: rules ?? [] }))
    if (url.includes('/esthetic/service-categories')) return Promise.resolve(jsonResponse({ items: [] }))
    if (url.includes('/esthetic/product-categories')) return Promise.resolve(jsonResponse({ items: [] }))
    if (url.includes('/conversations')) return Promise.resolve(jsonResponse({ items: [] }))
    if (url.includes('/esthetic/intent/analyze')) return Promise.resolve(jsonResponse({
      intencion: 'consulta_servicios', confianza: 0.85, respuestaSugerida: 'Ofrecemos depilacion laser y cera.', mensajeUsuario: 'Hola, quiero informacion',
    }))
    return Promise.resolve(jsonResponse({}))
  })
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <BusinessAiPage />
        </ToastProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

describe('BusinessAiPage', () => {
  beforeEach(() => {
    setupSession()
    setupMock()
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    window.sessionStorage.clear()
  })

  it('shows loading state while fetching', () => {
    mockFetch.mockImplementationOnce(() => new Promise(() => {}))
    renderPage()
    expect(screen.getByText('Cargando...')).toBeTruthy()
  })

  it('loads settings and shows metrics', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('IA activa')).toBeTruthy()
      expect(screen.getByText('Conversaciones resueltas')).toBeTruthy()
    })
  })

  it('navigates to test section', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    expect(screen.getByText('Mensaje de prueba')).toBeTruthy()
  })

  it('navigates to knowledge section', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    expect(screen.getByText('Agregar')).toBeTruthy()
  })

  it('navigates to advanced section', async () => {
    renderPage()
    const buttons = await screen.findAllByRole('button', { name: 'Configuración avanzada' })
    await userEvent.click(buttons[0])
    const expandButtons = await screen.findAllByRole('button', { name: /Configuración avanzada/ })
    if (expandButtons.length > 1) {
      await userEvent.click(expandButtons[1])
    }
    await waitFor(() => {
      expect(screen.getByText('Instrucciones internas del asistente')).toBeTruthy()
    })
  })

  it('advanced section is collapsed until expanded', async () => {
    renderPage()
    expect(screen.queryByText('Instrucciones internas del asistente')).toBeNull()
    const buttons = await screen.findAllByRole('button', { name: 'Configuración avanzada' })
    await userEvent.click(buttons[0])
    expect(screen.queryByText('Instrucciones internas del asistente')).toBeNull()
    const expandButtons = await screen.findAllByRole('button', { name: /Configuración avanzada/ })
    if (expandButtons.length > 1) {
      await userEvent.click(expandButtons[1])
    }
    await waitFor(() => {
      expect(screen.getByText('Instrucciones internas del asistente')).toBeTruthy()
    })
  })

  it('shows empty state for audit section when no logs', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Consultas por revisar' }))
    await waitFor(() => {
      expect(screen.getByText(/No hay respuestas/)).toBeTruthy()
    })
  })

  it('calls analyze endpoint when test is run', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await waitFor(() => expect(screen.getByText('Mensaje de prueba')).toBeTruthy())
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => {
      const analyzeCalls = mockFetch.mock.calls.filter(
        ([url, opts]: [string, RequestInit]) =>
          url.includes('/esthetic/intent/analyze') && opts?.method === 'POST',
      )
      expect(analyzeCalls.length).toBeGreaterThan(0)
    })
  })

  it('shows save button when settings are modified', async () => {
    renderPage()
    const checkboxes = await screen.findAllByRole('checkbox')
    await userEvent.click(checkboxes[0])
    await waitFor(() => {
      expect(screen.getByText('Guardar configuración')).toBeTruthy()
    })
  })

  it('shows empty state when no knowledge data', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getByText('No hay información disponible en esta sección.')).toBeTruthy()
    })
  })

  it('shows service list in knowledge section', async () => {
    cleanup()
    vi.restoreAllMocks()
    window.sessionStorage.clear()

    setupSession()

    const srvMock = vi.fn()
    vi.stubGlobal('fetch', srvMock)
    srvMock.mockImplementation((url: string) => {
      if (url.includes('/business-ai/settings')) return Promise.resolve(jsonResponse(defaultSettings))
      if (url.includes('/business-ai/prompts')) return Promise.resolve(jsonResponse(defaultPrompts))
      if (url.includes('/esthetic/intent/logs')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/services')) return Promise.resolve(jsonResponse({
        items: [{
          id: 'srv-1', name: 'Depilacion Laser', active: true, categoryCode: 'DEPILACION',
          categoryName: 'Depilacion', description: 'Depilacion laser definitiva', durationMinutes: 30,
          priceBase: 15000, updatedAt: '2026-07-30T15:00:00Z',
        }],
      }))
      if (url.includes('/esthetic/products')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/rules')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/service-categories')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/product-categories')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/conversations')) return Promise.resolve(jsonResponse({ items: [] }))
      return Promise.resolve(jsonResponse({}))
    })

    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getByText('Depilacion Laser')).toBeTruthy()
    })
  })
})
