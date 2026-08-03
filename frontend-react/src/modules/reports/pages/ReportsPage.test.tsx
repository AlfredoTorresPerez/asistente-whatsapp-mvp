import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import { ShellSessionProvider } from '../../../lib/ShellSessionProvider'
import { SHELL_SESSION_STORAGE_KEY } from '../../../lib/shellSession'
import { ReportsPage } from './ReportsPage'

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

function resolveUrl(input: Request | string | URL) {
  if (typeof input === 'string') return input
  if (input instanceof URL) return input.toString()
  return input.url
}

const user = {
  id: '40000000-0000-0000-0000-000000000001',
  name: 'Carla Mendez',
  firstName: 'Carla',
  lastName: 'Mendez',
  email: 'admin@demo.cl',
  role: 'OWNER',
  businessId: '11111111-1111-1111-1111-111111111111',
  businessName: 'Centro Estetico Bella',
  timezone: 'America/Santiago',
  phone: '+56955550101',
  permissions: ['REPORTS_VIEW', 'REPORTS_EXPORT'],
}

const reportsSummary = {
  period: {
    from: '2026-07-05',
    to: '2026-08-03',
    previousFrom: '2026-06-05',
    previousTo: '2026-07-04',
    timezone: 'America/Santiago',
  },
  kpis: [
    {
      label: 'Conversion a cita',
      currentValue: 45,
      previousValue: 0,
      variationPercent: null,
      valueType: 'PERCENT',
      lowerIsBetter: false,
      help: 'Porcentaje de prospectos que generaron cita.',
    },
  ],
  operationalKpis: [
    {
      label: 'Ingresos estimados',
      currentValue: 35000,
      previousValue: 0,
      variationPercent: null,
      valueType: 'CURRENCY',
      lowerIsBetter: false,
      help: 'Suma del precio base vigente del servicio.',
    },
  ],
  occupancyByProfessional: [
    {
      id: 'prof-1',
      name: 'Marcela Fuentes',
      availableMinutes: 480,
      reservedMinutes: 240,
      occupancyPercent: 50,
    },
  ],
  occupancyByRoom: [],
  occupancyByLocation: [],
  topServices: [
    {
      serviceId: 'svc-1',
      serviceName: 'Limpieza facial',
      bookings: 3,
      estimatedRevenue: 35000,
    },
  ],
  channelDistribution: [],
  conversationPerformance: [],
  appointmentDistribution: [],
  appointmentPerformance: [],
  conversionFunnel: [],
  prospects: {
    items: [],
    total: 0,
    page: 0,
    size: 15,
  },
}

function setupSession(overrides: Partial<typeof user> = {}) {
  const sessionUser = { ...user, ...overrides }
  window.sessionStorage.setItem(
    SHELL_SESSION_STORAGE_KEY,
    JSON.stringify({
      accessToken: 'jwt-demo-token',
      expiresAt: new Date(Date.now() + 900_000).toISOString(),
      user: sessionUser,
    }),
  )
  return sessionUser
}

function renderReportsPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <ShellSessionProvider>
        <MemoryRouter>
          <ReportsPage />
        </MemoryRouter>
      </ShellSessionProvider>
    </QueryClientProvider>,
  )
}

describe('ReportsPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows implemented indicators and hides future actions', async () => {
    const sessionUser = setupSession()
    vi.mocked(fetch).mockImplementation(async (input) => {
      const url = resolveUrl(input as Request | string | URL)
      if (url.endsWith('/auth/me')) return jsonResponse(sessionUser)
      if (url.endsWith('/users/me')) return jsonResponse(sessionUser)
      if (url.includes('/business-locations')) return jsonResponse([])
      if (url.includes('/agenda/filter-options')) {
        return jsonResponse({ professionals: [], services: [], rooms: [] })
      }
      if (url.includes('/reports/summary?')) return jsonResponse(reportsSummary)
      throw new Error(`Solicitud no manejada: ${url}`)
    })

    renderReportsPage()

    expect(await screen.findByText('Indicadores operativos')).toBeInTheDocument()
    expect(screen.getAllByText('$35.000').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Sin periodo anterior').length).toBeGreaterThan(0)
    expect(screen.getByText('Marcela Fuentes')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Exportar CSV' })).toBeInTheDocument()
    expect(screen.queryByText('Programar envio')).not.toBeInTheDocument()
  })

  it('does not expose export when the user lacks export permission', async () => {
    const sessionUser = setupSession({ permissions: ['REPORTS_VIEW'], role: 'SALES' })
    vi.mocked(fetch).mockImplementation(async (input) => {
      const url = resolveUrl(input as Request | string | URL)
      if (url.endsWith('/auth/me')) return jsonResponse(sessionUser)
      if (url.endsWith('/users/me')) return jsonResponse(sessionUser)
      if (url.includes('/business-locations')) return jsonResponse([])
      if (url.includes('/agenda/filter-options')) {
        return jsonResponse({ professionals: [], services: [], rooms: [] })
      }
      if (url.includes('/reports/summary?')) return jsonResponse(reportsSummary)
      throw new Error(`Solicitud no manejada: ${url}`)
    })

    renderReportsPage()

    expect(await screen.findByText('Indicadores operativos')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Exportar CSV' })).not.toBeInTheDocument()
  })
})
