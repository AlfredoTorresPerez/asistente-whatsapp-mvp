import { render, screen, waitFor, cleanup } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi } from 'vitest'
import { ToastProvider } from '../../../lib/ToastProvider'
import { BusinessAiPage } from '../pages/BusinessAiPage'
import { ShellSessionProvider } from '../../../lib/ShellSessionProvider'
import { SHELL_SESSION_STORAGE_KEY } from '../../../lib/shellSession'
import type { AiPreviewResponse } from '../../../services/api/types'

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

const sessionUser = {
  id: '40000000-0000-0000-0000-000000000001',
  name: 'Admin User',
  firstName: 'Admin',
  lastName: 'User',
  email: 'admin@demo.cl',
  role: 'OWNER',
  businessId: '11111111-1111-1111-1111-111111111111',
  businessName: 'Centro Estetico Bella',
  timezone: 'America/Santiago',
  phone: '+56955550101',
  permissions: ['ALL'],
}

function setupSession(permissions: string[] = ['ALL']) {
  window.sessionStorage.setItem(
    SHELL_SESSION_STORAGE_KEY,
    JSON.stringify({
      accessToken: 'jwt-demo-token',
      expiresAt: new Date(Date.now() + 900_000).toISOString(),
      user: { ...sessionUser, permissions },
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

const defaultPreviewResponse: AiPreviewResponse = {
  status: 'OK',
  message: 'Preview generado sin persistir',
  result: {
    businessId: '11111111-1111-1111-1111-111111111111',
    conversationId: '00000000-0000-0000-0000-000000000000',
    customerId: '00000000-0000-0000-0000-000000000000',
    primaryIntent: 'SERVICE_INFORMATION',
    secondaryIntent: null,
    agentType: 'SALES',
    extractedData: { servicio: 'Depilacion Laser' },
    missingData: ['horario_preferido'],
    urgency: 'normal',
    requiresHuman: false,
    handoffReason: null,
    responseToCustomer: 'Ofrecemos depilacion laser y cera.',
    confidence: 0.85,
    summaryForHuman: null,
  },
}

const defaultService = {
  id: 'srv-1', name: 'Depilacion Laser', active: true, categoryCode: 'DEPILACION',
  categoryName: 'Depilacion', description: 'Depilacion laser definitiva', durationMinutes: 30,
  priceBase: 15000, professionalRequired: 'Profesional estética', supplies: null,
  contraindications: null, availabilityRules: null, bookingRules: null, cancellationRules: null,
  aftercareRecommendations: null, requiresPriorEvaluation: false, requiresInformedConsent: false,
  createdAt: '2026-07-30T15:00:00Z', updatedAt: '2026-07-30T15:00:00Z',
  professionalIds: [], roomIds: [],
}

const defaultLocation = {
  id: 'loc-1', code: 'LOC001', name: 'Sucursal Centro', address: 'Av. Siempre Viva 123',
  city: 'Santiago', commune: 'Santiago', phone: '+56911111111', whatsappNumber: null,
  timezone: 'America/Santiago', active: true, createdAt: '2026-07-30T15:00:00Z', updatedAt: '2026-07-30T15:00:00Z',
}

let mockFetch: ReturnType<typeof vi.fn>

function baseMock(services?: any[], products?: any[], rules?: any[], locations?: any[], professionals?: any[], rooms?: any[], schedules?: any[], assignments?: any[]) {
  mockFetch.mockImplementation((url: string) => {
    if (url.includes('/business-ai/settings')) return Promise.resolve(jsonResponse(defaultSettings))
    if (url.includes('/business-ai/prompts')) return Promise.resolve(jsonResponse(defaultPrompts))
    if (url.includes('/esthetic/intent/logs')) return Promise.resolve(jsonResponse({ items: [] }))
    if (url.includes('/esthetic/services')) return Promise.resolve(jsonResponse({ items: services ?? [defaultService] }))
    if (url.includes('/esthetic/products')) return Promise.resolve(jsonResponse({ items: products ?? [] }))
    if (url.includes('/esthetic/rules')) return Promise.resolve(jsonResponse({ items: rules ?? [] }))
    if (url.includes('/esthetic/service-categories')) return Promise.resolve(jsonResponse({ items: [] }))
    if (url.includes('/esthetic/product-categories')) return Promise.resolve(jsonResponse({ items: [] }))
    if (url.includes('/business-locations')) return Promise.resolve(jsonResponse(locations ?? [defaultLocation]))
    if (url.includes('/admin/professionals')) return Promise.resolve(jsonResponse({ items: professionals ?? [] }))
    if (url.includes('/admin/rooms')) return Promise.resolve(jsonResponse({ items: rooms ?? [] }))
    if (url.includes('/multisite/professional-schedules')) return Promise.resolve(jsonResponse(schedules ?? []))
    if (url.includes('/admin/assignments')) return Promise.resolve(jsonResponse(assignments ?? []))
    if (url.includes('/conversations')) return Promise.resolve(jsonResponse({ items: [] }))
    if (url.includes('/api/v1/ai/preview')) return Promise.resolve(jsonResponse(defaultPreviewResponse))
    if (url.includes('/auth/me')) return Promise.resolve(jsonResponse(sessionUser))
    if (url.includes('/users/me')) return Promise.resolve(jsonResponse({ ...sessionUser, phone: '+56955550101' }))
    return Promise.resolve(jsonResponse({}))
  })
}

function setupMock() {
  mockFetch = vi.fn()
  vi.stubGlobal('fetch', mockFetch)
  baseMock()
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <ShellSessionProvider>
            <BusinessAiPage />
          </ShellSessionProvider>
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
    expect(screen.getByText('Mensaje del cliente')).toBeTruthy()
  })

  it('shows Modo de prueba indicator in test section', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    expect(screen.getByText('Modo de prueba')).toBeTruthy()
    expect(screen.getByText(/No se enviarán mensajes/)).toBeTruthy()
  })

  it('calls preview endpoint when test is run', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => {
      const previewCalls = mockFetch.mock.calls.filter((call) => {
        const [url, opts] = call as [string, RequestInit]
        return url.includes('/api/v1/ai/preview') && opts?.method === 'POST'
      })
      expect(previewCalls.length).toBeGreaterThan(0)
    })
  })

  it('displays routing result after preview', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => {
      expect(screen.getByText('Puede responder automáticamente')).toBeTruthy()
    })
    expect(screen.getByText(/Motivo de consulta/)).toBeTruthy()
    expect(screen.getByText(/Seguridad:/)).toBeTruthy()
  })

  it('shows info faltante when present', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => {
      expect(screen.getByText('Información faltante:')).toBeTruthy()
    })
  })

  it('requires conversation selection for send button', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Selecciona una conversación/ })).toBeTruthy()
    })
  })

  it('blocks send when user lacks BUSINESS_AI_SEND permission', async () => {
    cleanup()
    vi.restoreAllMocks()
    window.sessionStorage.clear()
    setupSession(['BUSINESS_AI_VIEW', 'BUSINESS_AI_TEST', 'BUSINESS_AI_AUDIT_VIEW', 'BUSINESS_AI_MANAGE'])

    const restrictedUser = { ...sessionUser, permissions: ['BUSINESS_AI_VIEW', 'BUSINESS_AI_TEST', 'BUSINESS_AI_AUDIT_VIEW', 'BUSINESS_AI_MANAGE'] }
    mockFetch = vi.fn()
    vi.stubGlobal('fetch', mockFetch)
    mockFetch.mockImplementation((url: string) => {
      if (url.includes('/auth/me')) return Promise.resolve(jsonResponse(restrictedUser))
      if (url.includes('/users/me')) return Promise.resolve(jsonResponse({ ...restrictedUser, phone: '+56955550101' }))
      if (url.includes('/business-ai/settings')) return Promise.resolve(jsonResponse(defaultSettings))
      if (url.includes('/business-ai/prompts')) return Promise.resolve(jsonResponse(defaultPrompts))
      if (url.includes('/conversations')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/api/v1/ai/preview')) return Promise.resolve(jsonResponse(defaultPreviewResponse))
      if (url.includes('/business-locations')) return Promise.resolve(jsonResponse([]))
      if (url.includes('/admin/professionals')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/admin/rooms')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/multisite/professional-schedules')) return Promise.resolve(jsonResponse([]))
      if (url.includes('/admin/assignments')) return Promise.resolve(jsonResponse([]))
      if (url.includes('/esthetic/services')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/products')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/rules')) return Promise.resolve(jsonResponse({ items: [] }))
      return Promise.resolve(jsonResponse({}))
    })
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => {
      expect(screen.getByText(/Sin permiso para enviar/)).toBeTruthy()
    })
  })

  it('shows human handoff when requiresHuman is true', async () => {
    const handoffPreview: AiPreviewResponse = {
      status: 'OK',
      message: 'Preview generado sin persistir',
      result: {
        businessId: '11111111-1111-1111-1111-111111111111',
        conversationId: '00000000-0000-0000-0000-000000000000',
        customerId: '00000000-0000-0000-0000-000000000000',
        primaryIntent: 'COMPLAINT',
        secondaryIntent: null,
        agentType: 'HUMAN_HANDOFF',
        extractedData: {},
        missingData: [],
        urgency: 'high',
        requiresHuman: true,
        handoffReason: 'Cliente solicita hablar con gerente',
        responseToCustomer: 'Le transferiremos con un asesor.',
        confidence: 0.95,
        summaryForHuman: null,
      },
    }

    mockFetch.mockImplementation((url: string) => {
      if (url.includes('/business-ai/settings')) return Promise.resolve(jsonResponse(defaultSettings))
      if (url.includes('/business-ai/prompts')) return Promise.resolve(jsonResponse(defaultPrompts))
      if (url.includes('/api/v1/ai/preview')) return Promise.resolve(jsonResponse(handoffPreview))
      if (url.includes('/auth/me')) return Promise.resolve(jsonResponse(sessionUser))
      if (url.includes('/users/me')) return Promise.resolve(jsonResponse({ ...sessionUser, phone: '+56955550101' }))
      if (url.includes('/esthetic/services')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/products')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/rules')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/business-locations')) return Promise.resolve(jsonResponse([]))
      if (url.includes('/admin/professionals')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/admin/rooms')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/multisite/professional-schedules')) return Promise.resolve(jsonResponse([]))
      if (url.includes('/admin/assignments')) return Promise.resolve(jsonResponse([]))
      return Promise.resolve(jsonResponse({}))
    })

    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => {
      expect(screen.getByText(/Debe derivarse/)).toBeTruthy()
    })
  })

  it('shows send confirmation dialog', async () => {
    const convResponse = {
      items: [{ id: 'conv-1', customerName: 'Cliente Test', customerPhone: '+56912345678' }],
    }
    mockFetch.mockImplementation((url: string) => {
      if (url.includes('/business-ai/settings')) return Promise.resolve(jsonResponse(defaultSettings))
      if (url.includes('/business-ai/prompts')) return Promise.resolve(jsonResponse(defaultPrompts))
      if (url.includes('/conversations')) return Promise.resolve(jsonResponse(convResponse))
      if (url.includes('/api/v1/ai/preview')) return Promise.resolve(jsonResponse(defaultPreviewResponse))
      if (url.includes('/auth/me')) return Promise.resolve(jsonResponse(sessionUser))
      if (url.includes('/users/me')) return Promise.resolve(jsonResponse({ ...sessionUser, phone: '+56955550101' }))
      if (url.includes('/esthetic/services')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/products')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/esthetic/rules')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/business-locations')) return Promise.resolve(jsonResponse([]))
      if (url.includes('/admin/professionals')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/admin/rooms')) return Promise.resolve(jsonResponse({ items: [] }))
      if (url.includes('/multisite/professional-schedules')) return Promise.resolve(jsonResponse([]))
      if (url.includes('/admin/assignments')) return Promise.resolve(jsonResponse([]))
      return Promise.resolve(jsonResponse({}))
    })

    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => expect(screen.getByText('Puede responder automáticamente')).toBeTruthy())

    await waitFor(() => {
      expect(screen.getByText(/Cliente Test/)).toBeTruthy()
    })

    const select = screen.getByRole('combobox') as HTMLSelectElement
    expect(select).toBeTruthy()
    expect(select.querySelector('option[value=""]')).toBeTruthy()
    expect(select.querySelector('option[value="conv-1"]')).toBeTruthy()

    const button = screen.getByRole('button', { name: /Selecciona una conversación/ })
    expect(button).toBeTruthy()
    expect(button).toBeDisabled()
  })

  it('does not create booking or send message during preview', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Probar asistente' }))
    await userEvent.click(screen.getByText('Probar'))
    await waitFor(() => {
      expect(screen.getByText('Puede responder automáticamente')).toBeTruthy()
    })
    const bookingCalls = mockFetch.mock.calls.filter((call) => {
      const [url] = call as [string]
      return url.includes('/bookings') || url.includes('/appointments')
    })
    expect(bookingCalls.length).toBe(0)
  })

  it('shows summary cards in knowledge section', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getByText('Servicios activos')).toBeTruthy()
      expect(screen.getByText('Sucursales activas')).toBeTruthy()
      expect(screen.getByText('Horarios configurados')).toBeTruthy()
    })
    expect(screen.getAllByText('1/1').length).toBeGreaterThanOrEqual(1)
  })

  it('shows admin links in summary cards', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getByText(/Administrar servicios/)).toBeTruthy()
      expect(screen.getByText(/Administrar sucursales/)).toBeTruthy()
      expect(screen.getByText(/Administrar profesionales/)).toBeTruthy()
    })
    const link = screen.getByText(/Administrar servicios/).closest('a')
    expect(link).toHaveAttribute('href', '/admin/services')
  })

  it('shows readiness section with checks', async () => {
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getByText('Preparación del asistente')).toBeTruthy()
    })
    expect(screen.getByText(/de 10 verificaciones correctas/)).toBeTruthy()
    expect(screen.getByText('Servicios sin precio')).toBeTruthy()
    expect(screen.getByText('Sucursales sin horario')).toBeTruthy()
  })

  it('shows warnings for services without price', async () => {
    cleanup()
    vi.restoreAllMocks()
    window.sessionStorage.clear()
    setupSession()
    mockFetch = vi.fn()
    vi.stubGlobal('fetch', mockFetch)
    baseMock(
      [{ ...defaultService, priceBase: 0 }],
      [], [], [defaultLocation], [], [], [], [],
    )
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getAllByText(/sin precio/).length).toBeGreaterThanOrEqual(1)
    })
  })

  it('shows readiness failure for services without price', async () => {
    cleanup()
    vi.restoreAllMocks()
    window.sessionStorage.clear()
    setupSession()
    mockFetch = vi.fn()
    vi.stubGlobal('fetch', mockFetch)
    baseMock(
      [{ ...defaultService, priceBase: 0 }],
      [], [], [defaultLocation], [], [], [], [],
    )
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getByText('Preparación del asistente')).toBeTruthy()
    })
    const failedIcons = screen.getAllByText('✗')
    expect(failedIcons.length).toBeGreaterThan(0)
  })

  it('shows readiness warning for location without address', async () => {
    cleanup()
    vi.restoreAllMocks()
    window.sessionStorage.clear()
    setupSession()
    mockFetch = vi.fn()
    vi.stubGlobal('fetch', mockFetch)
    baseMock(
      [defaultService],
      [], [], [{ ...defaultLocation, address: null }], [], [], [], [],
    )
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getAllByText(/sin dirección/).length).toBeGreaterThanOrEqual(1)
    })
  })

  it('shows empty counts when no data', async () => {
    cleanup()
    vi.restoreAllMocks()
    window.sessionStorage.clear()
    setupSession()
    mockFetch = vi.fn()
    vi.stubGlobal('fetch', mockFetch)
    baseMock([], [], [], [], [], [], [], [])
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getByText('Servicios activos')).toBeTruthy()
    })
    const zeroCounts = screen.getAllByText('0/0')
    expect(zeroCounts.length).toBeGreaterThanOrEqual(2)
  })

  it('shows readiness pass for location with schedule', async () => {
    cleanup()
    vi.restoreAllMocks()
    window.sessionStorage.clear()
    setupSession()
    mockFetch = vi.fn()
    vi.stubGlobal('fetch', mockFetch)
    baseMock(
      [defaultService],
      [], [], [defaultLocation], [], [], [
        { id: 'sch-1', professionalId: 'prof-1', professionalName: 'Prof 1', locationId: 'loc-1', locationName: 'Sucursal Centro', dayOfWeek: 1, startTime: '09:00', endTime: '18:00', active: true },
      ], [],
    )
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Información del negocio' }))
    await waitFor(() => {
      expect(screen.getByText('Preparación del asistente')).toBeTruthy()
    })
    const passedChecks = screen.getAllByText('✓')
    expect(passedChecks.length).toBeGreaterThan(0)
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

  it('shows save button when settings are modified', async () => {
    renderPage()
    const checkboxes = await screen.findAllByRole('checkbox')
    await userEvent.click(checkboxes[0])
    await waitFor(() => {
      expect(screen.getByText('Guardar configuración')).toBeTruthy()
    })
  })
})
