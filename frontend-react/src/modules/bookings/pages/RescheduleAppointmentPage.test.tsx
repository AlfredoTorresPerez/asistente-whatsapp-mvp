import dayjs from 'dayjs'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppProviders } from '../../../app/providers/AppProviders'
import { RescheduleAppointmentPage } from './RescheduleAppointmentPage'
import type {
  AgendaAvailabilityResponse,
  AgendaSlotResponse,
  BookingDetailResponse,
} from '../../../services/api/types'

const API_BASE = 'http://localhost:8080/api/v1'
const BOOKING_ID = '11111111-1111-1111-1111-111111111111'
const LOCATION_ID = 'loc-1'
const SERVICE_ID = 'svc-1'
const PROFESSIONAL_ID = 'pro-carla'

const DAY = dayjs().add(20, 'day').format('YYYY-MM-DD')
const DAY_LABEL = dayjs(DAY).format('DD/MM/YYYY')
const DAY_2 = dayjs().add(27, 'day').format('YYYY-MM-DD')

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function isoTime(hour: number, minute: number, date = DAY) {
  return `${date}T${pad(hour)}:${pad(minute)}:00-04:00`
}

function makeSlot(hour: number, minute: number, date = DAY): AgendaSlotResponse {
  return {
    startsAt: isoTime(hour, minute, date),
    endsAt: isoTime(hour, minute + 45, date),
    locationId: LOCATION_ID,
    locationName: 'Sucursal Centro',
    serviceId: SERVICE_ID,
    serviceName: 'Limpieza facial',
    durationMinutes: 45,
    professionalId: PROFESSIONAL_ID,
    professionalName: 'Carla Mendez',
    roomId: null,
    roomName: null,
    available: true,
    reason: 'Disponible',
  }
}

function makeAvailability(slots: AgendaSlotResponse[], date = DAY): AgendaAvailabilityResponse {
  return {
    locationId: LOCATION_ID,
    locationName: 'Sucursal Centro',
    serviceId: SERVICE_ID,
    serviceName: 'Limpieza facial',
    date,
    durationMinutes: 45,
    requiresRoom: false,
    requiresDeposit: false,
    slots,
  }
}

function makeBooking(): BookingDetailResponse {
  return {
    id: BOOKING_ID,
    subject: 'Limpieza facial',
    status: 'CONFIRMADA',
    startsAt: isoTime(10, 0, DAY_2),
    durationMinutes: 45,
    locationId: LOCATION_ID,
    location: 'Sucursal Centro',
    locationName: 'Sucursal Centro',
    serviceId: SERVICE_ID,
    professionalId: PROFESSIONAL_ID,
    roomId: null,
    notes: null,
    completedAt: null,
    createdAt: isoTime(9, 0),
    updatedAt: isoTime(9, 0),
    customerId: 'cust-1',
    customerName: 'Cliente Test',
    customerPhone: '56912345678',
    customerEmail: null,
    leadId: null,
    conversationId: null,
    assignedUserId: null,
    assignedUserName: null,
    requiresDeposit: false,
    depositAmount: 0,
    paymentStatus: 'NOT_REQUIRED',
    statusHistory: [],
    publicLinks: [],
    reminders: [],
    emailLogs: [],
    payments: [],
  }
}

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

let availabilityStatus = 200
let availabilityByDate = new Map<string, AgendaAvailabilityResponse>()
let rescheduleStatus = 200
let rescheduleCalls: string[] = []

beforeEach(() => {
  availabilityStatus = 200
  rescheduleStatus = 200
  rescheduleCalls = []
  availabilityByDate = new Map()
  availabilityByDate.set(
    DAY,
    makeAvailability([makeSlot(10, 0), makeSlot(9, 15), makeSlot(9, 0), makeSlot(11, 30)]),
  )
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: RequestInfo | URL, init?: RequestInit) => {
      const str = typeof url === 'string' ? url : String(url)
      if (str === `${API_BASE}/bookings/${BOOKING_ID}`) {
        return jsonResponse(makeBooking())
      }
      if (str === `${API_BASE}/agenda/availability` && init?.method === 'POST') {
        if (availabilityStatus !== 200) {
          return jsonResponse({ message: 'Error interno' }, availabilityStatus)
        }
        const body = JSON.parse(String(init?.body ?? '{}')) as { date?: string }
        return jsonResponse(availabilityByDate.get(body.date ?? '') ?? makeAvailability([]))
      }
      if (str === `${API_BASE}/bookings/${BOOKING_ID}/reschedule` && init?.method === 'PATCH') {
        rescheduleCalls.push(String(init?.body ?? ''))
        if (rescheduleStatus !== 200) {
          return jsonResponse({ message: 'El horario seleccionado ya no esta disponible.' }, 409)
        }
        return jsonResponse(makeBooking())
      }
      return jsonResponse(null, 404)
    }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function renderPage() {
  const router = createMemoryRouter(
    [{ path: '/appointments/:appointmentId/reschedule', element: <RescheduleAppointmentPage /> }],
    { initialEntries: [`/appointments/${BOOKING_ID}/reschedule`] },
  )
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
}

function dateInput(): HTMLInputElement {
  const input = document.body.querySelector('input[type="date"]') as HTMLInputElement | null
  expect(input).not.toBeNull()
  return input!
}

async function pickDate(value: string) {
  fireEvent.change(dateInput(), { target: { value } })
}

describe('RescheduleAppointmentPage', () => {
  it('no muestra campos editables de duracion, sucursal, ubicacion ni notas', async () => {
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    expect(document.body.querySelector('input[type="datetime-local"]')).toBeNull()
    expect(document.body.querySelector('input[type="number"]')).toBeNull()
    expect(document.body.querySelector('textarea')).toBeNull()
    expect(document.body.querySelector('select')).toBeNull()
    expect(screen.queryByLabelText(/Duracion/i)).toBeNull()
    expect(screen.queryByLabelText(/Sucursal/i)).toBeNull()
    expect(screen.queryByLabelText(/Ubicacion/i)).toBeNull()
    expect(screen.queryByLabelText(/Notas/i)).toBeNull()
    expect(screen.getByLabelText('Selecciona una nueva fecha')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Guardar nueva fecha' })).toBeInTheDocument()
  })

  it('no muestra horarios antes de seleccionar una fecha', async () => {
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    expect(
      screen.getByText('Selecciona una fecha para ver los horarios disponibles.'),
    ).toBeInTheDocument()
    expect(screen.queryAllByRole('button', { name: /^Horario disponible/ })).toHaveLength(0)
  })

  it('muestra los horarios ordenados ascendentemente al elegir fecha', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    await pickDate(DAY)
    const slots = await screen.findAllByRole('button', { name: /^Horario disponible/ })

    const times = slots.map((slot) => slot.getAttribute('aria-label'))
    expect(times).toEqual([
      'Horario disponible a las 09:00, finaliza a las 09:45.',
      'Horario disponible a las 09:15, finaliza a las 10:00.',
      'Horario disponible a las 10:00, finaliza a las 10:45.',
      'Horario disponible a las 11:30, finaliza a las 12:15.',
    ])
    expect(screen.queryAllByRole('button', { name: /^Horario disponible/ })).toHaveLength(4)
  })

  it('una fecha sin disponibilidad muestra el mensaje y permite cambiar de fecha', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    availabilityByDate.set(DAY, makeAvailability([]))
    await pickDate(DAY)
    expect(
      await screen.findByText(
        'No encontramos horarios disponibles para esta fecha. Selecciona otro día.',
      ),
    ).toBeInTheDocument()

    availabilityByDate.set(DAY_2, makeAvailability([makeSlot(9, 0, DAY_2)]))
    await pickDate(DAY_2)
    expect(await screen.findByRole('button', { name: /^Horario disponible/ })).toBeInTheDocument()
  })

  it('un error de consulta muestra el mensaje y permite reintentar', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    availabilityStatus = 500
    await pickDate(DAY)
    expect(
      await screen.findByText(
        'No fue posible consultar los horarios disponibles. Intenta nuevamente.',
      ),
    ).toBeInTheDocument()

    availabilityStatus = 200
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    await waitFor(() =>
      expect(screen.getAllByRole('button', { name: /^Horario disponible/ })).not.toHaveLength(0),
    )
  })

  it('mantiene deshabilitado Guardar hasta elegir un horario y habilita el resumen', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    const save = () => screen.getByRole('button', { name: 'Guardar nueva fecha' })
    expect(save()).toBeDisabled()

    await pickDate(DAY)
    expect(save()).toBeDisabled()

    const slot09 = await screen.findByRole('button', {
      name: 'Horario disponible a las 09:00, finaliza a las 09:45.',
    })
    expect(slot09).toHaveAttribute('aria-pressed', 'false')
    await user.click(slot09)
    expect(slot09).toHaveAttribute('aria-pressed', 'true')
    expect(save()).toBeEnabled()

    const summary = screen.getByText(
      (_content, node) =>
        node?.tagName === 'P' && (node.textContent?.includes('Nueva fecha:') ?? false),
    )
    expect(summary.textContent).toMatch(new RegExp(`Nueva fecha: ${DAY_LABEL}`))
    expect(summary.textContent).toMatch(/Hora seleccionada: 09:00/)
    expect(summary.textContent).toMatch(/Finaliza: 09:45/)
  })

  it('cambiar la fecha limpia el horario seleccionado y deshabilita Guardar', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    await pickDate(DAY)
    const slot09 = await screen.findByRole('button', {
      name: 'Horario disponible a las 09:00, finaliza a las 09:45.',
    })
    await user.click(slot09)
    expect(screen.getByRole('button', { name: 'Guardar nueva fecha' })).toBeEnabled()

    availabilityByDate.set(DAY_2, makeAvailability([makeSlot(9, 0, DAY_2)], DAY_2))
    await pickDate(DAY_2)

    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Guardar nueva fecha' })).toBeDisabled(),
    )
    expect(screen.queryByText('Hora seleccionada: 09:00')).toBeNull()
  })

  it('una seleccion de horario muestra resumen con hora de termino segun duracion original', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    await pickDate(DAY)
    const slot = await screen.findByRole('button', {
      name: 'Horario disponible a las 11:30, finaliza a las 12:15.',
    })
    await user.click(slot)

    const summary = screen.getByText(
      (_content, node) =>
        node?.tagName === 'P' && (node.textContent?.includes('Nueva fecha:') ?? false),
    )
    expect(summary.textContent).toMatch(/Hora seleccionada: 11:30/)
    expect(summary.textContent).toMatch(/Finaliza: 12:15/)
  })

  it('envia solo startsAt al guardar y navega al detalle', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    await pickDate(DAY)
    const slot = await screen.findByRole('button', {
      name: 'Horario disponible a las 09:00, finaliza a las 09:45.',
    })
    await user.click(slot)
    await user.click(screen.getByRole('button', { name: 'Guardar nueva fecha' }))

    await waitFor(() => expect(rescheduleCalls).toHaveLength(1))
    const sent = JSON.parse(rescheduleCalls[0]) as Record<string, unknown>
    expect(Object.keys(sent)).toEqual(['startsAt'])
    expect(sent.startsAt).toBe(new Date(isoTime(9, 0)).toISOString())
  })

  it('cuando el horario deja de estar disponible muestra el mensaje', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByLabelText('Selecciona una nueva fecha')

    await pickDate(DAY)
    const slot = await screen.findByRole('button', {
      name: 'Horario disponible a las 09:00, finaliza a las 09:45.',
    })
    await user.click(slot)

    rescheduleStatus = 409
    await user.click(screen.getByRole('button', { name: 'Guardar nueva fecha' }))

    await waitFor(() =>
      expect(
        screen.getByText('El horario seleccionado acaba de dejar de estar disponible. Selecciona otro horario.'),
      ).toBeInTheDocument(),
    )
  })
})
