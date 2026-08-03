import dayjs from 'dayjs'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent, { type UserEvent } from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AppProviders } from '../../../app/providers/AppProviders'
import { CreatePublicBookingPage } from './CreatePublicBookingPage'
import type {
  AgendaAvailabilityResponse,
  AgendaSlotResponse,
  PublicCategoryResponse,
  PublicServiceBranchResponse,
  PublicServiceItemResponse,
} from '../../../services/api/types'

const API_BASES = ['http://localhost:8080/api/v1', '/api/v1']
const DAY = dayjs().add(20, 'day').format('YYYY-MM-DD')
const DAY_LABEL = dayjs(DAY).format('DD/MM/YYYY')
const DAY_2 = dayjs().add(27, 'day').format('YYYY-MM-DD')
const DAY_2_LABEL = dayjs(DAY_2).format('DD/MM/YYYY')

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function isoTime(hour: number, minute: number, date = DAY) {
  return `${date}T${pad(hour)}:${pad(minute)}:00-04:00`
}

function endIso(hour: number, minute: number, durationMinutes = 45, date = DAY) {
  const total = hour * 60 + minute + durationMinutes
  return isoTime(Math.floor(total / 60), total % 60, date)
}

function slotHour(slot: AgendaSlotResponse) {
  return Number(slot.startsAt.slice(11, 13))
}

function makeSlot(
  hour: number,
  minute: number,
  professionalId: string,
  professionalName: string,
  roomId: string | null = null,
  roomName: string | null = null,
): AgendaSlotResponse {
  return {
    startsAt: isoTime(hour, minute),
    endsAt: endIso(hour, minute),
    locationId: 'loc-1',
    locationName: 'Sucursal Centro',
    serviceId: 'svc-1',
    serviceName: 'Limpieza facial',
    durationMinutes: 45,
    professionalId,
    professionalName,
    roomId,
    roomName,
    available: true,
    reason: 'Disponible',
  }
}

function makeAvailability(slots: AgendaSlotResponse[]): AgendaAvailabilityResponse {
  return {
    locationId: 'loc-1',
    locationName: 'Sucursal Centro',
    serviceId: 'svc-1',
    serviceName: 'Limpieza facial',
    date: DAY,
    durationMinutes: 45,
    requiresRoom: false,
    requiresDeposit: false,
    slots,
  }
}

function makeCategories(): PublicCategoryResponse[] {
  return [
    {
      id: 'cat-1',
      code: 'FACIAL',
      name: 'Facial',
      description: 'Tratamientos faciales',
      active: true,
    },
  ]
}

function makeServices(): PublicServiceItemResponse[] {
  return [
    {
      id: 'svc-1',
      code: 'LIMPEZA',
      name: 'Limpieza facial',
      description: null,
      durationMinutes: 45,
      priceBase: 15000,
      categoryCode: 'FACIAL',
      categoryName: 'Facial',
      active: true,
      requiresPriorEvaluation: false,
      requiresInformedConsent: false,
    },
  ]
}

function makeBranches(): PublicServiceBranchResponse[] {
  return [
    {
      id: 'loc-1',
      code: 'CENTRO',
      name: 'Sucursal Centro',
      address: 'Avenida Siempre Viva 123',
      city: 'Santiago',
      commune: 'Santiago',
      phone: null,
    },
  ]
}

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    status,
  })
}

function defaultSlots() {
  return [
    makeSlot(9, 0, 'pro-carla', 'Carla Mendez'),
    makeSlot(9, 0, 'pro-carla', 'Carla Mendez'),
    makeSlot(9, 45, 'pro-ana', 'Ana Profesional'),
    makeSlot(9, 45, 'pro-carla', 'Carla Mendez'),
    makeSlot(11, 30, 'pro-carla', 'Carla Mendez'),
    makeSlot(12, 0, 'pro-ana', 'Ana Profesional'),
    makeSlot(14, 30, 'pro-carla', 'Carla Mendez'),
    makeSlot(18, 0, 'pro-ana', 'Ana Profesional'),
  ]
}

let availabilityStatus = 200
const availabilityByDate = new Map<string, AgendaAvailabilityResponse>()

beforeEach(() => {
  availabilityStatus = 200
  availabilityByDate.clear()
  availabilityByDate.set(DAY, makeAvailability(defaultSlots()))
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: RequestInfo | URL, init?: RequestInit) => {
      const str = typeof url === 'string' ? url : String(url)
      if (matchesApiPath(str, '/public/landing/whatsapp-entry')) {
        return jsonResponse({})
      }
      if (matchesApiPath(str, '/public/landing/categories')) {
        return jsonResponse(makeCategories())
      }
      if (matchesApiPath(str, '/public/landing/categories/FACIAL/services')) {
        return jsonResponse(makeServices())
      }
      if (matchesApiPath(str, '/public/landing/services/svc-1/branches')) {
        return jsonResponse(makeBranches())
      }
      if (matchesApiPath(str, '/public/landing/availability')) {
        if (availabilityStatus !== 200) {
          return jsonResponse({ message: 'Error interno' }, availabilityStatus)
        }
        const body = JSON.parse(String(init?.body ?? '{}')) as { date?: string }
        return jsonResponse(availabilityByDate.get(body.date ?? '') ?? makeAvailability([]))
      }
      return jsonResponse(null, 404)
    }),
  )
})

function matchesApiPath(url: string, path: string) {
  return API_BASES.some((base) => url === `${base}${path}`)
}

afterEach(() => {
  vi.unstubAllGlobals()
})

function renderPage() {
  const router = createMemoryRouter([{ path: '/reservar', element: <CreatePublicBookingPage /> }], {
    initialEntries: ['/reservar'],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
}

async function reachStep3(user: UserEvent) {
  await screen.findByText('Facial')
  await user.click(screen.getByRole('button', { name: /Facial/ }))
  await user.click(screen.getByRole('button', { name: 'Continuar' }))
  await screen.findByText('Limpieza facial')
  await user.click(screen.getByRole('button', { name: /^Limpieza facial/ }))
  await user.click(screen.getByRole('button', { name: 'Continuar' }))
  await screen.findByText('Sucursal Centro')
  await user.click(screen.getByRole('button', { name: /^Sucursal Centro/ }))
  await user.click(screen.getByRole('button', { name: 'Continuar' }))
  await screen.findByText('Selecciona una fecha')
}

async function pickDate(user: UserEvent) {
  await user.click(screen.getByRole('button', { name: 'Seleccionar fecha' }))
  const input = document.body.querySelector('input[type="date"]') as HTMLInputElement | null
  expect(input).not.toBeNull()
  fireEvent.change(input!, { target: { value: DAY } })
}

describe('CreatePublicBookingPage - paso Fecha y hora', () => {
  it('muestra orientacion y oculta los horarios hasta elegir tramo', async () => {
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)

    expect(
      await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.'),
    ).toBeInTheDocument()
    expect(screen.queryAllByRole('button', { name: /^Hora / })).toHaveLength(0)
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' })).toHaveAttribute(
      'aria-pressed',
      'false',
    )
    expect(screen.getByRole('button', { name: 'Tarde, 3 horarios disponibles.' })).toHaveAttribute(
      'aria-pressed',
      'false',
    )
  })

  it('deshabilita el tramo sin horarios y conserva el conteo del otro', async () => {
    availabilityByDate.set(
      DAY,
      makeAvailability(defaultSlots().filter((slot) => slotHour(slot) < 12)),
    )
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)

    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')
    expect(screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' })).toBeEnabled()
    expect(
      screen.getByRole('button', { name: 'Tarde, sin horarios disponibles.' }),
    ).toBeDisabled()
  })

  it('al elegir tramo muestra solo sus horarios en orden ascendente', async () => {
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)
    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')

    await user.click(screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' }))
    expect(await screen.findByText('Horarios disponibles en la manana')).toBeInTheDocument()

    const slotButtons = screen.getAllByRole('button', { name: /^Hora / })
    const times = slotButtons.map((button) =>
      button.getAttribute('aria-label')?.match(/Hora (\d{2}:\d{2})/)?.[1],
    )
    expect(times).toEqual(['09:00', '09:45', '09:45', '11:30'])
    expect(screen.queryByRole('button', { name: /Hora 12:00/ })).toBeNull()
    expect(screen.getByRole('button', { name: 'Tarde, 3 horarios disponibles.' })).toBeInTheDocument()
  })

  it('conserva horarios de profesionales distintos a la misma hora', async () => {
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)
    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')

    await user.click(screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' }))
    const sameTime = await screen.findAllByRole('button', { name: /^Hora 09:45/ })
    expect(sameTime).toHaveLength(2)
    expect(sameTime[0]).toHaveAccessibleName(/Ana Profesional/)
    expect(sameTime[1]).toHaveAccessibleName(/Carla Mendez/)
  })

  it('seleccionar horario habilita Continuar y muestra el resumen', async () => {
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)
    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')

    await user.click(screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' }))
    const slotButton = await screen.findByRole('button', { name: /^Hora 09:00/ })
    await user.click(slotButton)

    expect(screen.getByText('Hora seleccionada: 09:00')).toBeInTheDocument()
    expect(screen.getByText('Finaliza: 09:45')).toBeInTheDocument()
    expect(screen.getByText('Profesional: Carla Mendez')).toBeInTheDocument()
    expect(screen.getByText('Cabina: No requerida')).toBeInTheDocument()
    expect(screen.getByText('Sucursal: Sucursal Centro')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled()
    expect(slotButton).toHaveAttribute('aria-pressed', 'true')
    expect(slotButton).toHaveTextContent('Seleccionado')
  })

  it('cambiar de tramo limpia el horario elegido y conserva la fecha', async () => {
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)
    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')

    await user.click(screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' }))
    await user.click(await screen.findByRole('button', { name: /^Hora 09:00/ }))
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled()

    await user.click(screen.getByRole('button', { name: 'Tarde, 3 horarios disponibles.' }))
    expect(await screen.findByText('Horarios disponibles en la tarde')).toBeInTheDocument()
    expect(screen.queryByText('Hora seleccionada: 09:00')).toBeNull()
    expect(screen.queryByRole('button', { name: /^Hora 09:00/ })).toBeNull()
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled()
    expect(screen.getByText(DAY_LABEL)).toBeInTheDocument()
  })

  it('cambiar de fecha limpia el tramo y el horario elegidos', async () => {
    availabilityByDate.set(DAY_2, makeAvailability(defaultSlots()))
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)
    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')

    await user.click(screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' }))
    await user.click(await screen.findByRole('button', { name: /^Hora 09:00/ }))
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled()

    await user.click(screen.getByRole('button', { name: 'Seleccionar fecha' }))
    const input = document.body.querySelector('input[type="date"]') as HTMLInputElement | null
    expect(input).not.toBeNull()
    fireEvent.change(input!, { target: { value: DAY_2 } })

    expect(
      await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.'),
    ).toBeInTheDocument()
    expect(screen.queryByText('Hora seleccionada: 09:00')).toBeNull()
    expect(screen.queryAllByRole('button', { name: /^Hora / })).toHaveLength(0)
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled()
    expect(screen.getByText(DAY_2_LABEL)).toBeInTheDocument()
  })

  it('Actualizar conserva el tramo e informa si el horario elegido desaparecio', async () => {
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)
    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')

    await user.click(screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' }))
    await user.click(await screen.findByRole('button', { name: /^Hora 09:00/ }))
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled()

    availabilityByDate.set(
      DAY,
      makeAvailability(defaultSlots().filter((slot) => !slot.startsAt.includes('T09:00:'))),
    )
    await user.click(screen.getByRole('button', { name: 'Actualizar' }))

    expect(
      await screen.findByText(
        'El horario seleccionado ya no esta disponible. Selecciona otro horario.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Manana, 3 horarios disponibles.' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(screen.queryByRole('button', { name: /^Hora 09:00/ })).toBeNull()
    expect(screen.getAllByRole('button', { name: /^Hora 09:45/ })).toHaveLength(2)
  })

  it('muestra el estado vacio cuando la fecha no tiene horarios', async () => {
    availabilityByDate.set(DAY, makeAvailability([]))
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)

    expect(
      await screen.findByText(
        'No encontramos horarios disponibles para esta fecha. Selecciona otro dia.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled()
  })

  it('muestra el error y permite reintentar', async () => {
    availabilityStatus = 500
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)

    expect(await screen.findByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
    expect(screen.getByText('Error interno')).toBeInTheDocument()

    availabilityStatus = 200
    availabilityByDate.set(DAY, makeAvailability(defaultSlots()))
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(
      await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.'),
    ).toBeInTheDocument()
  })

  it('soporta seleccion por teclado de tramo y horario', async () => {
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)
    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')

    const morningButton = screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' })
    morningButton.focus()
    await user.keyboard('{Enter}')
    expect(await screen.findByText('Horarios disponibles en la manana')).toBeInTheDocument()

    const firstSlot = screen.getByRole('button', { name: /^Hora 09:00/ })
    firstSlot.focus()
    await user.keyboard('{Enter}')
    expect(screen.getByText('Hora seleccionada: 09:00')).toBeInTheDocument()
  })

  it('mantiene aria-pressed y etiquetas descriptivas en tramos y horarios', async () => {
    const user = userEvent.setup()
    renderPage()
    await reachStep3(user)
    await pickDate(user)
    await screen.findByText('Selecciona manana o tarde para ver los horarios disponibles.')

    const manana = screen.getByRole('button', { name: 'Manana, 4 horarios disponibles.' })
    const tarde = screen.getByRole('button', { name: 'Tarde, 3 horarios disponibles.' })
    expect(manana).toHaveAttribute('aria-pressed', 'false')
    expect(tarde).toHaveAttribute('aria-pressed', 'false')

    await user.click(manana)
    const slotButton = await screen.findByRole('button', { name: /^Hora 09:00/ })
    expect(slotButton).toHaveAccessibleName(/hasta 09:45/)
    expect(slotButton).toHaveAccessibleName(/Carla Mendez/)
    expect(slotButton).toHaveAccessibleName(/Sucursal Centro/)

    await user.click(slotButton)
    expect(screen.getByRole('button', { name: /^Hora 09:00/ })).toHaveAccessibleName(/seleccionado/)
    expect(screen.getByRole('button', { name: /^Hora 09:00/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(manana).toHaveAttribute('aria-pressed', 'true')
    expect(tarde).toHaveAttribute('aria-pressed', 'false')
  })
})
