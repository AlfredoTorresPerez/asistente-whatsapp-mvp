import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi } from 'vitest'
import { MultisiteOperationsPage } from './MultisiteOperationsPage'
import { SHELL_SESSION_STORAGE_KEY } from '../../../lib/shellSession'
import { ToastProvider } from '../../../lib/ToastProvider'
import { ShellSessionProvider } from '../../../lib/ShellSessionProvider'

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

function setupFetchMock(locations: unknown[]) {
  const mockFetch = vi.fn()
  vi.stubGlobal('fetch', mockFetch)

  mockFetch.mockImplementation((url: string) => {
    if (url.includes('/multisite/summary')) {
      return Promise.resolve(jsonResponse(locations))
    }
    if (url.includes('/multisite/catalog-availability')) {
      return Promise.resolve(jsonResponse([]))
    }
    if (url.includes('/multisite/professionals')) {
      return Promise.resolve(jsonResponse([]))
    }
    if (url.includes('/multisite/professional-schedules')) {
      return Promise.resolve(jsonResponse([]))
    }
    if (url.includes('/multisite/user-access')) {
      return Promise.resolve(jsonResponse([]))
    }
    if (url.includes('/multisite/channels')) {
      return Promise.resolve(jsonResponse([]))
    }
    return Promise.resolve(jsonResponse({}))
  })
  return mockFetch
}

function setupSessionStorage() {
  window.sessionStorage.setItem(
    SHELL_SESSION_STORAGE_KEY,
    JSON.stringify({
      accessToken: 'jwt-demo-token',
      expiresAt: new Date(Date.now() + 900_000).toISOString(),
      user: {
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
        permissions: ['ADMIN_MANAGE'],
      },
    }),
  )
}

const mockLocations = [
  {
    locationId: '11111111-1111-1111-1111-111111111111',
    locationCode: 'PRINCIPAL',
    locationName: 'Centro Estetico Bella - Sede Principal',
    active: true,
    conversations: 5,
    leads: 3,
    bookings: 2,
    orders: 1,
    productsWithStock: 10,
    professionals: 4,
  },
  {
    locationId: '11111111-1111-1111-1111-111111111112',
    locationCode: 'LSC',
    locationName: 'Las Condes',
    active: true,
    conversations: 0,
    leads: 0,
    bookings: 0,
    orders: 0,
    productsWithStock: 0,
    professionals: 4,
  },
  {
    locationId: '11111111-1111-1111-1111-111111111113',
    locationCode: 'MAIPU',
    locationName: 'Maipu',
    active: false,
    conversations: 2,
    leads: 1,
    bookings: 5,
    orders: 0,
    productsWithStock: 8,
    professionals: 3,
  },
]

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnWindowFocus: false,
      },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <ShellSessionProvider>
          <MemoryRouter>
            <MultisiteOperationsPage />
          </MemoryRouter>
        </ShellSessionProvider>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('MultisiteOperationsPage - Summary (Resumen)', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    setupSessionStorage()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the summary tab with a table instead of cards', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      expect(screen.getByRole('table')).toBeInTheDocument()
    })
  })

  it('shows headers in the required order', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      const headers = screen.getAllByRole('columnheader')
      const labels = headers.map((h) => h.textContent)
      expect(labels).toEqual([
        'Sede',
        'Estado',
        'Conversaciones',
        'Prospectos',
        'Citas',
        'Pedidos',
        'Productos stock',
        'Profesionales',
      ])
    })
  })

  it('renders one row per location', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      const rows = screen.getAllByRole('row')
      expect(rows.length - 1).toBe(mockLocations.length)
    })
  })

  it('shows location name and code in the Sede column', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      expect(screen.getAllByText('Centro Estetico Bella - Sede Principal').length).toBeGreaterThanOrEqual(1)
      expect(screen.getByText('PRINCIPAL')).toBeInTheDocument()
    })
  })

  it('shows active status badge for active locations', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      expect(screen.getAllByText('Activa').length).toBe(2)
    })
  })

  it('shows inactive status badge for inactive location', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Inactiva')).toBeInTheDocument()
    })
  })

  it('shows numeric values as 0 when the API returns zero', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      const table = screen.getByRole('table')
      const zeroCells = Array.from(table.querySelectorAll('td')).filter(
        (td) => td.textContent === '0',
      )
      expect(zeroCells.length).toBeGreaterThanOrEqual(5)
    })
  })

  it('filters rows when a specific location is selected', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      expect(screen.getByRole('table')).toBeInTheDocument()
    })

    const select = screen.getByRole('combobox')
    await userEvent.selectOptions(select, '11111111-1111-1111-1111-111111111112')

    await waitFor(() => {
      const rows = screen.getAllByRole('row')
      expect(rows.length - 1).toBe(1)
    })

    expect(screen.getAllByText('Las Condes').length).toBeGreaterThanOrEqual(1)
    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody?.textContent).not.toContain('Centro Estetico Bella - Sede Principal')
  })

  it('shows all rows when "Todas las sedes" is selected', async () => {
    setupFetchMock(mockLocations)
    renderPage()

    await waitFor(() => {
      expect(screen.getByRole('table')).toBeInTheDocument()
    })

    const select = screen.getByRole('combobox')
    await userEvent.selectOptions(select, '11111111-1111-1111-1111-111111111112')

    await waitFor(() => {
      const rows = screen.getAllByRole('row')
      expect(rows.length - 1).toBe(1)
    })

    await userEvent.selectOptions(select, '')

    await waitFor(() => {
      const rows = screen.getAllByRole('row')
      expect(rows.length - 1).toBe(mockLocations.length)
    })
  })

  it('shows empty state when there are no locations', async () => {
    setupFetchMock([])
    renderPage()

    await waitFor(() => {
      expect(
        screen.getByText('No existen sedes para el filtro seleccionado'),
      ).toBeInTheDocument()
    })
  })

  it('shows loading state while fetching', async () => {
    const mockFetch = vi.fn()
    vi.stubGlobal('fetch', mockFetch)

    mockFetch.mockImplementation(() => new Promise(() => {}))

    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Cargando configuracion multisede.')).toBeInTheDocument()
    })
  })

  it('shows error state when the API fails', async () => {
    const mockFetch = vi.fn()
    vi.stubGlobal('fetch', mockFetch)

    mockFetch.mockRejectedValue(new Error('API error'))

    renderPage()

    await waitFor(() => {
      expect(
        screen.getByText('No fue posible cargar operacion multisede'),
      ).toBeInTheDocument()
    })
  })
})
