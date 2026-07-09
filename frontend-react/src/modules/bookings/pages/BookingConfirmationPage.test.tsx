import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { vi } from 'vitest'
import { AppProviders } from '../../../app/providers/AppProviders'
import { BookingConfirmationPage } from './BookingConfirmationPage'

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

function confirmationResponse(overrides: Record<string, unknown> = {}) {
  return {
    bookingId: '22222222-2222-2222-2222-222222222222',
    bookingStatus: 'PENDIENTE_CONFIRMACION',
    linkStatus: 'OPENED',
    subject: 'Limpieza facial',
    serviceName: 'Limpieza facial profunda',
    professionalName: 'Ana Profesional',
    roomName: 'Sala Estetica 1',
    startsAt: '2026-07-15T15:00:00-04:00',
    durationMinutes: 60,
    locationId: '33333333-3333-3333-3333-333333333333',
    location: 'Sucursal Providencia',
    locationName: 'Sucursal Providencia',
    customerName: 'Maria Perez',
    maskedCustomerPhone: '****4580',
    requiresDeposit: false,
    depositAmount: 0,
    paymentStatus: 'NOT_REQUIRED',
    expiresAt: '2026-07-10T15:00:00-04:00',
    confirmedAt: null,
    ...overrides,
  }
}

const routes = [
  {
    path: '/reservas/confirmar/:token',
    element: <BookingConfirmationPage />,
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

describe('BookingConfirmationPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows loading state while fetching booking data', async () => {
    vi.mocked(fetch).mockImplementation(() => new Promise(() => {}))
    renderAt('/reservas/confirmar/valid-token')
    expect(await screen.findByText('Cargando datos de la reserva...')).toBeInTheDocument()
  })

  it('shows error card when token is invalid', async () => {
    vi.mocked(fetch).mockRejectedValue(new Error('Not found'))
    renderAt('/reservas/confirmar/invalid-token')

    expect(await screen.findByText('Enlace no disponible')).toBeInTheDocument()
  })

  it('renders booking details with phone 56950954580', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(confirmationResponse()))

    renderAt('/reservas/confirmar/valid-token')

    expect(await screen.findByText('Maria Perez (****4580)')).toBeInTheDocument()
    expect(screen.getByText('Limpieza facial profunda')).toBeInTheDocument()
    expect(screen.getByText('Sucursal Providencia')).toBeInTheDocument()
    expect(screen.getByText('15/07/2026 15:00')).toBeInTheDocument()
  })

  it('shows confirm button when booking is pending confirmation', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(confirmationResponse()))

    renderAt('/reservas/confirmar/valid-token')

    expect(await screen.findByRole('button', { name: 'Confirmar reserva' })).toBeInTheDocument()
  })

  it('shows "Reserva confirmada" and disables confirm when already confirmed', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(confirmationResponse({ bookingStatus: 'CONFIRMADA', linkStatus: 'CONFIRMED' })))

    renderAt('/reservas/confirmar/valid-token')

    expect(await screen.findByRole('button', { name: 'Reserva confirmada' })).toBeDisabled()
  })

  it('shows cancel and reschedule buttons when allowed', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(confirmationResponse()))

    renderAt('/reservas/confirmar/valid-token')

    expect(await screen.findByRole('button', { name: 'Reprogramar reserva' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar reserva' })).toBeInTheDocument()
  })

  it('shows cancellation form when cancel button is clicked', async () => {
    const user = userEvent.setup()
    vi.mocked(fetch).mockResolvedValue(jsonResponse(confirmationResponse()))

    renderAt('/reservas/confirmar/valid-token')

    const cancelBtn = await screen.findByRole('button', { name: 'Cancelar reserva' })
    await user.click(cancelBtn)

    expect(screen.getByRole('heading', { name: 'Cancelar reserva' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Ejemplo: No podre asistir a la hora reservada.')).toBeInTheDocument()
  })

  it('calls cancel API with reason and phone 56950954580 data', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(jsonResponse(confirmationResponse()))

    renderAt('/reservas/confirmar/valid-token')

    const cancelBtn = await screen.findByRole('button', { name: 'Cancelar reserva' })
    await user.click(cancelBtn)

    const textarea = screen.getByPlaceholderText('Ejemplo: No podre asistir a la hora reservada.')
    await user.type(textarea, 'No podre asistir a la hora reservada.')

    fetchMock.mockResolvedValue(jsonResponse(confirmationResponse({ bookingStatus: 'CANCELADA', linkStatus: 'USED' })))

    const confirmCancelBtn = screen.getByRole('button', { name: 'Confirmar cancelacion' })
    await user.click(confirmCancelBtn)

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/public/booking-confirmations/valid-token/cancel'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ reason: 'No podre asistir a la hora reservada.' }),
        }),
      )
    })
  })

  it('shows "Reserva cancelada" after successful cancellation', async () => {
    const user = userEvent.setup()
    const pendingData = confirmationResponse()
    const cancelledData = confirmationResponse({ bookingStatus: 'CANCELADA', linkStatus: 'USED' })
    let cancelled = false

    const fetchMock = vi.mocked(fetch)
    fetchMock.mockImplementation(async (url) => {
      const str = typeof url === 'string' ? url : url instanceof URL ? url.toString() : url.url
      if (str.includes('/cancel')) {
        cancelled = true
        return jsonResponse(cancelledData)
      }
      return jsonResponse(cancelled ? cancelledData : pendingData)
    })

    renderAt('/reservas/confirmar/valid-token')

    const cancelBtn = await screen.findByRole('button', { name: 'Cancelar reserva' })
    await user.click(cancelBtn)

    const textarea = screen.getByPlaceholderText('Ejemplo: No podre asistir a la hora reservada.')
    await user.type(textarea, 'No podre asistir')

    const confirmCancelBtn = screen.getByRole('button', { name: 'Confirmar cancelacion' })
    await user.click(confirmCancelBtn)

    expect(await screen.findByText('Reserva cancelada')).toBeInTheDocument()
  })
})
