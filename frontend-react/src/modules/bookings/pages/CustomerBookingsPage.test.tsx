import dayjs from 'dayjs'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { vi } from 'vitest'
import { AppProviders } from '../../../app/providers/AppProviders'
import { CustomerBookingsPage } from './CustomerBookingsPage'

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

function futureDate(daysAhead: number, hours = 15): string {
  const d = new Date()
  d.setDate(d.getDate() + daysAhead)
  d.setHours(hours, 0, 0, 0)
  return d.toISOString().replace('Z', '-04:00')
}

const booking1Id = '11111111-1111-1111-1111-111111111111'
const booking2Id = '22222222-2222-2222-2222-222222222222'

function booking1Response() {
  return {
    bookingId: booking1Id,
    locationId: 'loc-1',
    serviceId: 'svc-1',
    professionalId: 'pro-1',
    roomId: null,
    serviceName: 'Limpieza facial',
    locationName: 'Sucursal Centro',
    professionalName: 'Ana Profesional',
    startsAt: futureDate(3),
    endsAt: futureDate(3, 16),
    durationMinutes: 60,
    status: 'CONFIRMADA',
    customerName: 'Maria Perez',
    maskedPhone: '****4580',
  }
}

function booking2Response() {
  return {
    bookingId: booking2Id,
    locationId: 'loc-2',
    serviceId: 'svc-2',
    professionalId: 'pro-2',
    roomId: null,
    serviceName: 'Masaje relajante',
    locationName: 'Sucursal Norte',
    professionalName: 'Carlos Masajista',
    startsAt: futureDate(5),
    endsAt: futureDate(5, 17),
    durationMinutes: 90,
    status: 'PENDIENTE',
    customerName: 'Maria Perez',
    maskedPhone: '****4580',
  }
}

function previewResponse(booking: ReturnType<typeof booking1Response>) {
  return {
    booking,
    services: [
      {
        id: 'svc-1',
        name: 'Limpieza facial',
        categoryName: 'Facial',
        durationMinutes: 60,
        requiresRoom: false,
      },
      {
        id: 'svc-2',
        name: 'Masaje relajante',
        categoryName: 'Masajes',
        durationMinutes: 90,
        requiresRoom: false,
      },
    ],
    locations: [
      { id: 'loc-1', name: 'Sucursal Centro', address: 'Centro 123', commune: 'Santiago' },
      { id: 'loc-2', name: 'Sucursal Norte', address: 'Norte 456', commune: 'Santiago' },
    ],
  }
}

const routes = [
  {
    path: '/reservas/mis-reservas/:token',
    element: <CustomerBookingsPage />,
  },
]

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  return render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
}

const API_BASE = 'http://localhost:8080/api/v1'

function bookingUrl(token: string) {
  return `${API_BASE}/public/customer-bookings/${token}`
}

function isCustomerBookingsIndex(str: string, token: string) {
  return (
    (str === bookingUrl(token) || str.endsWith(`/public/customer-bookings/${token}`)) &&
    !str.includes('/reschedule') &&
    !str.includes('/cancel')
  )
}

function isGetPreview(str: string, bookingId: string) {
  return str.includes('/reschedule') && str.includes(bookingId) && !str.includes('availability')
}

function isAvailability(str: string) {
  return str.includes('/reschedule/availability')
}

function methodIs(init: RequestInit | undefined, method: string) {
  const actualMethod = (init as RequestInit)?.method ?? 'GET'
  return actualMethod === method
}

describe('CustomerBookingsPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows form below the selected booking card', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)

    fetchMock.mockImplementation(async (url, init) => {
      const str = typeof url === 'string' ? url : url.toString()
      if (isCustomerBookingsIndex(str, 'validtoken') && methodIs(init, 'GET')) {
        return jsonResponse([booking1Response(), booking2Response()])
      }
      if (methodIs(init, 'GET') && isGetPreview(str, booking1Id)) {
        return jsonResponse(previewResponse(booking1Response()))
      }
      return jsonResponse(null)
    })

    renderAt('/reservas/mis-reservas/validtoken')

    expect(await screen.findByText('Limpieza facial')).toBeInTheDocument()
    expect(screen.getByText('Masaje relajante')).toBeInTheDocument()

    const reprogramarButtons = screen.getAllByRole('button', { name: 'Reprogramar' })
    await user.click(reprogramarButtons[0])

    expect(await screen.findByText('Editar reserva seleccionada')).toBeInTheDocument()
  })

  it('opens form below the correct card when selecting different reservations', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)

    fetchMock.mockImplementation(async (url, init) => {
      const str = typeof url === 'string' ? url : url.toString()
      if (isCustomerBookingsIndex(str, 'validtoken') && methodIs(init, 'GET')) {
        return jsonResponse([booking1Response(), booking2Response()])
      }
      if (methodIs(init, 'GET') && isGetPreview(str, booking1Id)) {
        return jsonResponse(previewResponse(booking1Response()))
      }
      if (methodIs(init, 'GET') && isGetPreview(str, booking2Id)) {
        return jsonResponse(previewResponse(booking2Response()))
      }
      return jsonResponse(null)
    })

    renderAt('/reservas/mis-reservas/validtoken')

    expect(await screen.findByText('Limpieza facial')).toBeInTheDocument()

    const reprogramarButtons = screen.getAllByRole('button', { name: 'Reprogramar' })
    await user.click(reprogramarButtons[0])
    expect(await screen.findByText('Editar reserva seleccionada')).toBeInTheDocument()

    await user.click(reprogramarButtons[1])
    expect(await screen.findByText('Editar reserva seleccionada')).toBeInTheDocument()

    const formHeaders = screen.getAllByText('Reprogramacion')
    expect(formHeaders).toHaveLength(1)
  })

  it('closes the form when Cerrar is clicked without saving', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)

    fetchMock.mockImplementation(async (url, init) => {
      const str = typeof url === 'string' ? url : url.toString()
      if (isCustomerBookingsIndex(str, 'validtoken') && methodIs(init, 'GET')) {
        return jsonResponse([booking1Response(), booking2Response()])
      }
      if (methodIs(init, 'GET') && isGetPreview(str, booking1Id)) {
        return jsonResponse(previewResponse(booking1Response()))
      }
      return jsonResponse(null)
    })

    renderAt('/reservas/mis-reservas/validtoken')

    expect(await screen.findByText('Limpieza facial')).toBeInTheDocument()

    const reprogramarButtons = screen.getAllByRole('button', { name: 'Reprogramar' })
    await user.click(reprogramarButtons[0])

    expect(await screen.findByText('Editar reserva seleccionada')).toBeInTheDocument()

    const cerrarButtons = screen.getAllByRole('button', { name: 'Cerrar' })
    await user.click(cerrarButtons[0])

    await waitFor(() => {
      expect(screen.queryByText('Editar reserva seleccionada')).not.toBeInTheDocument()
    })
  })

  it('preloads booking data in the form', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)
    const b1 = booking1Response()

    fetchMock.mockImplementation(async (url, init) => {
      const str = typeof url === 'string' ? url : url.toString()
      if (isCustomerBookingsIndex(str, 'validtoken') && methodIs(init, 'GET')) {
        return jsonResponse([b1, booking2Response()])
      }
      if (methodIs(init, 'GET') && isGetPreview(str, booking1Id)) {
        return jsonResponse(previewResponse(b1))
      }
      return jsonResponse(null)
    })

    renderAt('/reservas/mis-reservas/validtoken')

    expect(await screen.findByText('Limpieza facial')).toBeInTheDocument()

    const reprogramarButtons = screen.getAllByRole('button', { name: 'Reprogramar' })
    await user.click(reprogramarButtons[0])

    await waitFor(() => {
      expect(screen.getAllByText('Maria Perez (****4580)').length).toBeGreaterThanOrEqual(2)
    })
    await waitFor(() => {
      expect(screen.getAllByText('Limpieza facial').length).toBeGreaterThanOrEqual(2)
    })
    const dateText = dayjs(b1.startsAt).format('DD/MM/YYYY HH:mm')
    await waitFor(() => {
      expect(screen.getAllByText(dateText).length).toBeGreaterThanOrEqual(2)
    })
    await waitFor(() => {
      expect(screen.getAllByText('Sucursal Centro').length).toBeGreaterThanOrEqual(2)
    })
    await waitFor(() => {
      expect(screen.getAllByText('Ana Profesional').length).toBeGreaterThanOrEqual(2)
    })
    await waitFor(() => {
      expect(screen.getAllByText('60 minutos').length).toBeGreaterThanOrEqual(2)
    })
  })

  it('performs successful reschedule and closes the form', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)
    const b1 = booking1Response()
    const laterDate = futureDate(5)

    fetchMock.mockImplementation(async (url, init) => {
      const str = typeof url === 'string' ? url : url.toString()
      const method = (init as RequestInit)?.method ?? 'GET'
      if (isCustomerBookingsIndex(str, 'validtoken') && method === 'GET') {
        return jsonResponse([b1, booking2Response()])
      }
      if (method === 'GET' && isGetPreview(str, booking1Id)) {
        return jsonResponse(previewResponse(b1))
      }
      if (method === 'POST' && isGetPreview(str, booking1Id)) {
        return jsonResponse({ ...b1, startsAt: laterDate, status: 'REPROGRAMADA' })
      }
      if (method === 'GET' && isAvailability(str)) {
        return jsonResponse({
          locationId: 'loc-1',
          locationName: 'Sucursal Centro',
          serviceId: 'svc-1',
          serviceName: 'Limpieza facial',
          date: dayjs(laterDate).format('YYYY-MM-DD'),
          durationMinutes: 60,
          requiresRoom: false,
          requiresDeposit: false,
          slots: [
            {
              startsAt: laterDate,
              endsAt: futureDate(5, 16),
              locationId: 'loc-1',
              locationName: 'Sucursal Centro',
              serviceId: 'svc-1',
              serviceName: 'Limpieza facial',
              durationMinutes: 60,
              professionalId: 'pro-1',
              professionalName: 'Ana Profesional',
              roomId: null,
              roomName: null,
              available: true,
              reason: '',
            },
          ],
        })
      }
      return jsonResponse(null)
    })

    renderAt('/reservas/mis-reservas/validtoken')

    expect(await screen.findByText('Limpieza facial')).toBeInTheDocument()

    const reprogramarButtons = screen.getAllByRole('button', { name: 'Reprogramar' })
    await user.click(reprogramarButtons[0])

    expect(await screen.findByText('Editar reserva seleccionada')).toBeInTheDocument()

    const reprogramarReservaBtn = screen.getByRole('button', { name: 'Reprogramar reserva' })
    await user.click(reprogramarReservaBtn)

    await waitFor(() => {
      expect(screen.queryByText('Editar reserva seleccionada')).not.toBeInTheDocument()
    })
  })

  it('keeps the form open on availability error and shows error message', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)
    const b1 = booking1Response()

    fetchMock.mockImplementation(async (url, init) => {
      const str = typeof url === 'string' ? url : url.toString()
      const method = (init as RequestInit)?.method ?? 'GET'
      if (isCustomerBookingsIndex(str, 'validtoken') && method === 'GET') {
        return jsonResponse([b1, booking2Response()])
      }
      if (method === 'GET' && isGetPreview(str, booking1Id)) {
        return jsonResponse(previewResponse(b1))
      }
      if (method === 'POST' && isGetPreview(str, booking1Id)) {
        return jsonResponse({ message: 'No hay disponibilidad para la fecha seleccionada.' }, 409)
      }
      return jsonResponse(null)
    })

    renderAt('/reservas/mis-reservas/validtoken')

    expect(await screen.findByText('Limpieza facial')).toBeInTheDocument()

    const reprogramarButtons = screen.getAllByRole('button', { name: 'Reprogramar' })
    await user.click(reprogramarButtons[0])

    expect(await screen.findByText('Editar reserva seleccionada')).toBeInTheDocument()

    const reprogramarReservaBtn = screen.getByRole('button', { name: 'Reprogramar reserva' })
    await user.click(reprogramarReservaBtn)

    expect(
      await screen.findByText('No hay disponibilidad para la fecha seleccionada.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Editar reserva seleccionada')).toBeInTheDocument()
  })

  it('only shows one reschedule form at a time', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)

    fetchMock.mockImplementation(async (url, init) => {
      const str = typeof url === 'string' ? url : url.toString()
      if (isCustomerBookingsIndex(str, 'validtoken') && methodIs(init, 'GET')) {
        return jsonResponse([booking1Response(), booking2Response()])
      }
      if (methodIs(init, 'GET') && isGetPreview(str, booking1Id)) {
        return jsonResponse(previewResponse(booking1Response()))
      }
      if (methodIs(init, 'GET') && isGetPreview(str, booking2Id)) {
        return jsonResponse(previewResponse(booking2Response()))
      }
      return jsonResponse(null)
    })

    renderAt('/reservas/mis-reservas/validtoken')

    expect(await screen.findByText('Limpieza facial')).toBeInTheDocument()

    const reprogramarButtons = screen.getAllByRole('button', { name: 'Reprogramar' })

    await user.click(reprogramarButtons[0])
    expect(await screen.findByText('Editar reserva seleccionada')).toBeInTheDocument()

    await user.click(reprogramarButtons[1])
    expect(await screen.findByText('Editar reserva seleccionada')).toBeInTheDocument()

    const reprogramacionHeaders = screen.getAllByText('Reprogramacion')
    expect(reprogramacionHeaders).toHaveLength(1)
  })
})
