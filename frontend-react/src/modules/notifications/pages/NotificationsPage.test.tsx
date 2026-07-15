import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import { appRoutes } from '../../../app/router'
import { AppProviders } from '../../../app/providers/AppProviders'
import { SHELL_SESSION_STORAGE_KEY } from '../../../lib/shellSession'

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

describe('NotificationsPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('marks an unread notification as read and refreshes the list', async () => {
    const user = userEvent.setup()
    let notificationStatus: 'UNREAD' | 'READ' = 'UNREAD'

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

    const fetchMock = vi.mocked(fetch)
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

      if (
        url.includes('/notifications?') &&
        url.includes('status=UNREAD') &&
        !url.includes('size=10')
      ) {
        return jsonResponse({
          items: [],
          page: 0,
          size: 1,
          totalItems: notificationStatus === 'UNREAD' ? 1 : 0,
          totalPages: 1,
        })
      }

      if (url.includes('/notifications?') && url.includes('size=10')) {
        return jsonResponse({
          items: [
            {
              id: '69800000-0000-0000-0000-000000000001',
              type: 'NEW_MESSAGE',
              status: notificationStatus,
              title: 'Nuevo mensaje de Sofia Rojas',
              body: 'Sofia consulto por limpieza facial y quedo una conversacion abierta.',
              relatedEntityType: 'CONVERSATION',
              relatedEntityId: '64000000-0000-0000-0000-000000000001',
              createdAt: '2026-05-23T18:16:00Z',
              readAt: notificationStatus === 'READ' ? '2026-05-23T18:25:00Z' : null,
            },
          ],
          page: 0,
          size: 10,
          totalItems: 1,
          totalPages: 1,
        })
      }

      if (
        url.endsWith('/notifications/69800000-0000-0000-0000-000000000001/read') &&
        init?.method === 'PATCH'
      ) {
        notificationStatus = 'READ'
        return jsonResponse({
          id: '69800000-0000-0000-0000-000000000001',
          status: 'READ',
          readAt: '2026-05-23T18:25:00Z',
        })
      }

      throw new Error(`Unhandled fetch: ${String(init?.method ?? 'GET')} ${url}`)
    })

    const router = createMemoryRouter(appRoutes, {
      initialEntries: ['/notifications'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(await screen.findByText('Nuevo mensaje de Sofia Rojas')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Marcar leida' }))

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Marcar leida' })).not.toBeInTheDocument()
    })

    expect(screen.getByText('Leída')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Notificaciones \(0\)/i })).toBeInTheDocument()
  })
})
