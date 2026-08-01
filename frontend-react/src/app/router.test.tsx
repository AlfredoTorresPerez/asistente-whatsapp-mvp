import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import { SHELL_SESSION_STORAGE_KEY } from '../lib/shellSession'
import { AppProviders } from './providers/AppProviders'
import { appRoutes } from './router'

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

function resolveRequestUrl(input: Request | string | URL) {
  if (typeof input === 'string') {
    return input
  }

  if (input instanceof URL) {
    return input.toString()
  }

  return input.url
}

function buildDashboardSummaryResponse() {
  return {
    kpis: {
      openConversations: 2,
      newProspects: 1,
      openOrders: 1,
      pendingAppointments: 2,
    },
    conversationSeries: [
      { label: '2026-05-17', value: 0 },
      { label: '2026-05-18', value: 0 },
      { label: '2026-05-19', value: 0 },
      { label: '2026-05-20', value: 0 },
      { label: '2026-05-21', value: 0 },
      { label: '2026-05-22', value: 1 },
      { label: '2026-05-23', value: 1 },
    ],
    orderSeries: [
      { label: '2026-05-17', value: 0 },
      { label: '2026-05-18', value: 0 },
      { label: '2026-05-19', value: 0 },
      { label: '2026-05-20', value: 0 },
      { label: '2026-05-21', value: 0 },
      { label: '2026-05-22', value: 0 },
      { label: '2026-05-23', value: 1 },
    ],
    todayAppointments: [
      {
        id: '68000000-0000-0000-0000-000000000001',
        subject: 'Evaluacion facial inicial',
        status: 'SCHEDULED',
        customerName: 'Sofia Rojas',
        startsAt: '2026-05-23T14:00:00Z',
        durationMinutes: 45,
        location: 'Sucursal Providencia',
      },
    ],
    recentActivity: [
      {
        entityType: 'CONVERSATION',
        entityId: '64000000-0000-0000-0000-000000000001',
        title: 'Nuevo mensaje de Sofia Rojas',
        body: 'Quiero saber el precio',
        status: 'OPEN',
        occurredAt: '2026-05-23T18:10:00Z',
      },
    ],
  }
}

function setupMockFetch(role: string, userId: string, userName: string, email: string) {
  const fetchMock = vi.mocked(fetch)
  fetchMock.mockImplementation(async (input, init) => {
    const url = resolveRequestUrl(input as Request | string | URL)

    if (url.endsWith('/auth/me')) {
      return jsonResponse({
        id: userId,
        firstName: userName.split(' ')[0],
        lastName: userName.split(' ').slice(1).join(' '),
        email,
        role,
        businessId: '11111111-1111-1111-1111-111111111111',
        businessName: 'Centro Estetico Bella',
        timezone: 'America/Santiago',
      })
    }

    if (url.endsWith('/users/me')) {
      return jsonResponse({
        id: userId,
        firstName: userName.split(' ')[0],
        lastName: userName.split(' ').slice(1).join(' '),
        email,
        phone: '+56955550101',
        timezone: 'America/Santiago',
        role,
        businessName: 'Centro Estetico Bella',
      })
    }

    if (url.includes('/notifications?') && url.includes('status=UNREAD')) {
      return jsonResponse({
        items: [],
        page: 0,
        size: 1,
        totalItems: 0,
        totalPages: 0,
      })
    }

    if (url.includes('/dashboard/summary')) {
      return jsonResponse(buildDashboardSummaryResponse())
    }

    if (url.endsWith('/auth/logout') && init?.method === 'POST') {
      return jsonResponse({ status: 'LOGGED_OUT' })
    }

    throw new Error(`Unhandled fetch: ${String(init?.method ?? 'GET')} ${url}`)
  })
}

function buildSession(role: string, userId: string, userName: string, email: string) {
  return JSON.stringify({
    accessToken: 'jwt-demo-token',
    expiresAt: new Date(Date.now() + 900_000).toISOString(),
    user: {
      id: userId,
      name: userName,
      firstName: userName.split(' ')[0],
      lastName: userName.split(' ').slice(1).join(' '),
      email,
      role,
      businessId: '11111111-1111-1111-1111-111111111111',
      businessName: 'Centro Estetico Bella',
      timezone: 'America/Santiago',
      phone: '+56955550101',
      permissions: [],
    },
  })
}

describe('app shell router', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('Caso 6: redirects unauthenticated users to login', async () => {
    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/admin'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByRole('heading', { name: 'Iniciar sesion' })).toBeInTheDocument()
  })

  it('Caso 1: OWNER can see admin menu items and navigate to them', async () => {
    window.sessionStorage.setItem(
      SHELL_SESSION_STORAGE_KEY,
      buildSession('OWNER', 'u1', 'Dueno Test', 'owner@demo.cl'),
    )
    setupMockFetch('OWNER', 'u1', 'Dueno Test', 'owner@demo.cl')

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/admin'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByText('Conversaciones abiertas')).toBeInTheDocument()
    expect(screen.getByText('Administración')).toBeInTheDocument()
    expect(screen.getByText('Empresa')).toBeInTheDocument()
    expect(screen.getByText('Sucursales')).toBeInTheDocument()
    expect(screen.getByText('Cabinas')).toBeInTheDocument()
    expect(screen.getByText('Servicios')).toBeInTheDocument()
    expect(screen.getByText('Asignaciones')).toBeInTheDocument()
    expect(screen.getByText('MultiSede')).toBeInTheDocument()
    expect(screen.getByText('Canal de WhatsApp')).toBeInTheDocument()
    expect(screen.getByText('Usuarios y Roles')).toBeInTheDocument()
    expect(screen.getByText('Seguridad')).toBeInTheDocument()
  })

  it('Caso 2: ADMIN can see admin menu items', async () => {
    window.sessionStorage.setItem(
      SHELL_SESSION_STORAGE_KEY,
      buildSession('ADMIN', 'u2', 'Admin Test', 'admin@demo.cl'),
    )
    setupMockFetch('ADMIN', 'u2', 'Admin Test', 'admin@demo.cl')

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/admin'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByText('Conversaciones abiertas')).toBeInTheDocument()
    expect(screen.getByText('Administración')).toBeInTheDocument()
    expect(screen.getByText('Empresa')).toBeInTheDocument()
    expect(screen.getByText('Sucursales')).toBeInTheDocument()
  })

  it('Caso 3: AGENT cannot see Administracion and is redirected from admin routes', async () => {
    window.sessionStorage.setItem(
      SHELL_SESSION_STORAGE_KEY,
      buildSession('AGENT', 'u3', 'Agente Demo', 'agent@demo.cl'),
    )
    setupMockFetch('AGENT', 'u3', 'Agente Demo', 'agent@demo.cl')

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/admin'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByText('Conversaciones abiertas')).toBeInTheDocument()
    expect(screen.queryByText('Administración')).not.toBeInTheDocument()
    expect(screen.queryByText('Empresa')).not.toBeInTheDocument()
  })

  it('Caso 4: SALES cannot see Administracion and is redirected from admin routes', async () => {
    window.sessionStorage.setItem(
      SHELL_SESSION_STORAGE_KEY,
      buildSession('SALES', 'u4', 'Ventas Test', 'sales@demo.cl'),
    )
    setupMockFetch('SALES', 'u4', 'Ventas Test', 'sales@demo.cl')

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/admin'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByText('Conversaciones abiertas')).toBeInTheDocument()
    expect(screen.queryByText('Administración')).not.toBeInTheDocument()
  })

  it('Caso 5: SUPERVISOR cannot see Administracion', async () => {
    window.sessionStorage.setItem(
      SHELL_SESSION_STORAGE_KEY,
      buildSession('SUPERVISOR', 'u5', 'Supervisor Test', 'supervisor@demo.cl'),
    )
    setupMockFetch('SUPERVISOR', 'u5', 'Supervisor Test', 'supervisor@demo.cl')

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/dashboard'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByText('Conversaciones abiertas')).toBeInTheDocument()
    expect(screen.queryByText('Administración')).not.toBeInTheDocument()
  })

  it('Caso 7: admin route stays expanded on reload for authorized users', async () => {
    window.sessionStorage.setItem(
      SHELL_SESSION_STORAGE_KEY,
      buildSession('OWNER', 'u1', 'Dueno Test', 'owner@demo.cl'),
    )
    setupMockFetch('OWNER', 'u1', 'Dueno Test', 'owner@demo.cl')

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/admin/company'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByText('Configuración de empresa')).toBeInTheDocument()
    expect(screen.getByText('Administración')).toBeInTheDocument()
    expect(screen.getByText('Empresa')).toBeInTheDocument()
  })

  it('navigates from login to dashboard with a real backend-backed shell session', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockImplementation(async (input, init) => {
      const url = resolveRequestUrl(input as Request | string | URL)

      if (url.endsWith('/auth/login') && init?.method === 'POST') {
        return jsonResponse({
          accessToken: 'jwt-demo-token',
          tokenType: 'Bearer',
          expiresInSeconds: 900,
          user: {
            id: '40000000-0000-0000-0000-000000000001',
            firstName: 'Carla',
            lastName: 'Mendez',
            email: 'admin@demo.cl',
            role: 'OWNER',
            businessId: '11111111-1111-1111-1111-111111111111',
            businessName: 'Centro Estetico Bella',
            timezone: 'America/Santiago',
          },
        })
      }

      if (url.endsWith('/users/me') && init?.method === 'PATCH') {
        return jsonResponse({})
      }

      if (url.endsWith('/users/me')) {
        return jsonResponse({
          id: '40000000-0000-0000-0000-000000000001',
          firstName: 'Carla',
          lastName: 'Mendez',
          email: 'admin@demo.cl',
          phone: '+56955550101',
          timezone: 'America/Santiago',
          role: 'OWNER',
          businessName: 'Centro Estetico Bella',
        })
      }

      if (url.includes('/notifications?') && url.includes('status=UNREAD')) {
        return jsonResponse({
          items: [],
          page: 0,
          size: 1,
          totalItems: 1,
          totalPages: 1,
        })
      }

      if (url.includes('/dashboard/summary')) {
        return jsonResponse(buildDashboardSummaryResponse())
      }

      throw new Error(`Unhandled fetch: ${String(init?.method ?? 'GET')} ${url}`)
    })

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/login'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    await user.type(screen.getByLabelText('Correo'), 'admin@demo.cl')
    await user.type(screen.getByLabelText('Contrasena'), 'Cambiar123!')
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Conversaciones abiertas')).toBeInTheDocument()
    expect(screen.getAllByText('Centro Estetico Bella')).toHaveLength(2)
    expect(screen.getByRole('link', { name: /Notificaciones \(1\)/i })).toBeInTheDocument()
  })

  it('logs out and redirects back to login from a protected route', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)
    window.sessionStorage.setItem(
      SHELL_SESSION_STORAGE_KEY,
      JSON.stringify({
        accessToken: 'jwt-demo-token',
        expiresAt: new Date(Date.now() + 900_000).toISOString(),
        user: {
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
        },
      }),
    )

    fetchMock.mockImplementation(async (input, init) => {
      const url = resolveRequestUrl(input as Request | string | URL)

      if (url.endsWith('/auth/me')) {
        return jsonResponse({
          id: '40000000-0000-0000-0000-000000000001',
          firstName: 'Carla',
          lastName: 'Mendez',
          email: 'admin@demo.cl',
          role: 'OWNER',
          businessId: '11111111-1111-1111-1111-111111111111',
          businessName: 'Centro Estetico Bella',
          timezone: 'America/Santiago',
        })
      }

      if (url.endsWith('/users/me')) {
        return jsonResponse({
          id: '40000000-0000-0000-0000-000000000001',
          firstName: 'Carla',
          lastName: 'Mendez',
          email: 'admin@demo.cl',
          phone: '+56955550101',
          timezone: 'America/Santiago',
          role: 'OWNER',
          businessName: 'Centro Estetico Bella',
        })
      }

      if (url.includes('/notifications?') && url.includes('status=UNREAD')) {
        return jsonResponse({
          items: [],
          page: 0,
          size: 1,
          totalItems: 1,
          totalPages: 1,
        })
      }

      if (url.includes('/dashboard/summary')) {
        return jsonResponse(buildDashboardSummaryResponse())
      }

      if (url.endsWith('/auth/logout') && init?.method === 'POST') {
        return jsonResponse({
          status: 'LOGGED_OUT',
        })
      }

      throw new Error(`Unhandled fetch: ${String(init?.method ?? 'GET')} ${url}`)
    })

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/dashboard'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    await screen.findByText('Conversaciones abiertas')

    await user.click(screen.getByRole('button', { name: /Carla Mendez/i }))
    await user.click(screen.getByRole('button', { name: 'Cerrar sesion' }))
    const confirmDialog = await screen.findByRole('dialog')
    await user.click(within(confirmDialog).getByRole('button', { name: 'Cerrar sesion' }))

    expect(await screen.findByRole('heading', { name: 'Iniciar sesion' })).toBeInTheDocument()
  })
})
