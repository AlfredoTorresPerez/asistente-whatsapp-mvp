import { describe, it, expect } from 'vitest'
import type { AgendaCalendarItemResponse, BusinessHoursResponse } from '../../../services/api/types'
import {
  computeScheduleRange,
  buildDayAvailability,
  getScheduleHours,
  getLocalHourMinute,
  getEventsForDayAndHour,
  buildAgendaHourLayout,
  getAllEventsForDay,
  baseHourHeight,
  eventCardEstimatedHeight,
  eventGap,
  rowVerticalPadding,
} from './agendaUtils'
import dayjs from 'dayjs'
import type { DayAvailability } from './agendaUtils'

const DEFAULT_ITEM: AgendaCalendarItemResponse = {
  bookingId: 'test-booking-id',
  subject: 'Limpieza facial profunda',
  status: 'SOLICITADA',
  startsAt: '',
  endsAt: '',
  durationMinutes: 60,
  locationId: '81000000-0000-0000-0000-000000000001',
  locationName: 'Providencia',
  serviceId: '73000000-0000-0000-0000-000000000001',
  serviceName: 'Limpieza facial profunda',
  professionalId: '71000000-0000-0000-0000-000000000001',
  professionalName: 'Carla Méndez',
  roomId: null,
  roomName: null,
  customerName: 'Cliente Test',
  customerPhone: '56950954580',
  sourceChannel: 'WHATSAPP',
  dateLocal: '2026-07-15',
  startTimeLocal: '21:50',
  endTimeLocal: '22:50',
  timezone: 'America/Santiago',
  type: 'BOOKING',
}

function makeItem(
  startsAt: string,
  overrides: Partial<AgendaCalendarItemResponse> = {},
): AgendaCalendarItemResponse {
  const durationMinutes = overrides.durationMinutes ?? 60
  const startsAtDate = new Date(startsAt)
  const endsAtDate = new Date(startsAtDate.getTime() + durationMinutes * 60000)
  return {
    ...DEFAULT_ITEM,
    startsAt,
    endsAt: endsAtDate.toISOString(),
    durationMinutes,
    ...overrides,
  }
}

function makeBusinessHours(
  overrides: Partial<BusinessHoursResponse> & { dayOfWeek: number },
): BusinessHoursResponse {
  return {
    startTime: '09:00',
    endTime: '19:00',
    ...overrides,
  }
}

function makeDayAvailability(
  overrides: Partial<DayAvailability> & { dateKey: string },
): DayAvailability {
  return {
    dayOfWeek: 3,
    hasBusinessHours: true,
    startHour: 9,
    endHour: 19,
    ...overrides,
  }
}

describe('computeScheduleRange', () => {
  it('usa horario laboral cuando no hay items', () => {
    const availability: DayAvailability[] = [
      makeDayAvailability({ dateKey: '2026-07-15', dayOfWeek: 3, startHour: 9, endHour: 19 }),
    ]
    const result = computeScheduleRange(availability, [])
    expect(result.scheduleStartHour).toBe(9)
    expect(result.scheduleEndHour).toBe(19)
  })

  it('extiende el rango hasta incluir una cita fuera del horario laboral (21:50-22:50)', () => {
    const availability: DayAvailability[] = [
      makeDayAvailability({ dateKey: '2026-07-15', dayOfWeek: 3, startHour: 9, endHour: 19 }),
    ]
    const items = [
      makeItem('2026-07-16T01:50:00.000Z', {
        startTimeLocal: '21:50',
        endTimeLocal: '22:50',
        dateLocal: '2026-07-15',
        durationMinutes: 60,
      }),
    ]
    const result = computeScheduleRange(availability, items)
    expect(result.scheduleStartHour).toBe(9)
    expect(result.scheduleEndHour).toBeGreaterThanOrEqual(22)
  })

  it('no extiende el rango cuando la cita esta dentro del horario laboral', () => {
    const availability: DayAvailability[] = [
      makeDayAvailability({ dateKey: '2026-07-15', dayOfWeek: 3, startHour: 9, endHour: 19 }),
    ]
    const items = [
      makeItem('2026-07-15T12:00:00-04:00', {
        startTimeLocal: '12:00',
        endTimeLocal: '13:00',
        dateLocal: '2026-07-15',
        durationMinutes: 60,
      }),
    ]
    const result = computeScheduleRange(availability, items)
    expect(result.scheduleStartHour).toBe(9)
    expect(result.scheduleEndHour).toBe(19)
  })

  it('usa defaults 9-21 cuando no hay disponibilidad ni items', () => {
    const result = computeScheduleRange([], [])
    expect(result.scheduleStartHour).toBe(9)
    expect(result.scheduleEndHour).toBe(21)
  })

  it('extiende el rango cuando no hay disponibilidad pero hay items', () => {
    const items = [
      makeItem('2026-07-16T01:50:00.000Z', {
        startTimeLocal: '21:50',
        endTimeLocal: '22:50',
        dateLocal: '2026-07-15',
        durationMinutes: 60,
      }),
    ]
    const result = computeScheduleRange([], items)
    expect(result.scheduleStartHour).toBeLessThanOrEqual(21)
    expect(result.scheduleEndHour).toBeGreaterThanOrEqual(22)
  })

  it('respeta el inicio mas temprano entre horario laboral y citas', () => {
    const availability: DayAvailability[] = [
      makeDayAvailability({ dateKey: '2026-07-15', dayOfWeek: 3, startHour: 9, endHour: 19 }),
    ]
    const items = [
      makeItem('2026-07-15T08:00:00-04:00', {
        startTimeLocal: '08:00',
        endTimeLocal: '08:30',
        dateLocal: '2026-07-15',
        durationMinutes: 30,
      }),
    ]
    const result = computeScheduleRange(availability, items)
    expect(result.scheduleStartHour).toBe(8)
    expect(result.scheduleEndHour).toBe(19)
  })

  it('considera la hora de termino basada en inicio + duracion', () => {
    const availability: DayAvailability[] = [
      makeDayAvailability({ dateKey: '2026-07-15', dayOfWeek: 3, startHour: 9, endHour: 19 }),
    ]
    const items = [
      makeItem('2026-07-15T18:00:00-04:00', {
        startTimeLocal: '18:00',
        endTimeLocal: '20:00',
        dateLocal: '2026-07-15',
        durationMinutes: 120,
      }),
    ]
    const result = computeScheduleRange(availability, items)
    expect(result.scheduleEndHour).toBeGreaterThanOrEqual(20)
  })

  it('extiende el rango minimo necesario para la ultima cita (22:50)', () => {
    const availability: DayAvailability[] = [
      makeDayAvailability({ dateKey: '2026-07-15', dayOfWeek: 3, startHour: 9, endHour: 19 }),
    ]
    const items = [
      makeItem('2026-07-16T01:50:00.000Z', {
        startTimeLocal: '21:50',
        endTimeLocal: '22:50',
        dateLocal: '2026-07-15',
        durationMinutes: 60,
      }),
      makeItem('2026-07-15T10:00:00-04:00', {
        startTimeLocal: '10:00',
        endTimeLocal: '10:30',
        dateLocal: '2026-07-15',
        durationMinutes: 30,
        bookingId: 'other',
      }),
    ]
    const result = computeScheduleRange(availability, items)
    expect(result.scheduleStartHour).toBe(9)
    expect(result.scheduleEndHour).toBe(23)
  })
})

describe('buildDayAvailability', () => {
  const visibleDays = [
    dayjs('2026-07-13'),
    dayjs('2026-07-14'),
    dayjs('2026-07-15'),
    dayjs('2026-07-16'),
    dayjs('2026-07-17'),
    dayjs('2026-07-18'),
    dayjs('2026-07-19'),
  ]

  it('convierte business hours a DayAvailability correctamente', () => {
    const hours: BusinessHoursResponse[] = [
      makeBusinessHours({ dayOfWeek: 1, startTime: '09:00', endTime: '19:00' }),
      makeBusinessHours({ dayOfWeek: 3, startTime: '09:00', endTime: '19:00' }),
    ]
    const result = buildDayAvailability(hours, visibleDays)
    expect(result).toHaveLength(7)
    expect(result.find((d) => d.dayOfWeek === 1)?.hasBusinessHours).toBe(true)
    expect(result.find((d) => d.dayOfWeek === 1)?.startHour).toBe(9)
    expect(result.find((d) => d.dayOfWeek === 3)?.hasBusinessHours).toBe(true)
    expect(result.find((d) => d.dayOfWeek === 7)?.hasBusinessHours).toBe(false)
  })

  it('marca sin disponibilidad cuando no hay business hours', () => {
    const result = buildDayAvailability([], visibleDays)
    result.forEach((day) => {
      expect(day.hasBusinessHours).toBe(false)
    })
  })
})

describe('getScheduleHours', () => {
  it('genera horas desde start hasta end inclusive', () => {
    const result = getScheduleHours(9, 19)
    expect(result).toEqual([9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19])
  })

  it('genera rango extendido (9-22)', () => {
    const result = getScheduleHours(9, 22)
    expect(result).toEqual([9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22])
  })
})

describe('getLocalHourMinute', () => {
  it('usa startTimeLocal cuando existe', () => {
    const item = makeItem('2026-07-16T01:50:00.000Z', {
      startTimeLocal: '21:50',
      endTimeLocal: '22:50',
      dateLocal: '2026-07-15',
    })
    const result = getLocalHourMinute(item)
    expect(result.hour).toBe(21)
    expect(result.minute).toBe(50)
  })

  it('fallback a la conversion desde startsAt cuando no hay startTimeLocal', () => {
    const item = makeItem('2026-07-16T01:50:00.000Z', { dateLocal: '2026-07-15' })
    delete (item as Record<string, unknown>).startTimeLocal
    const result = getLocalHourMinute(item)
    expect(result.hour).toBe(21)
    expect(result.minute).toBe(50)
  })
})

describe('getEventsForDayAndHour', () => {
  it('filtra items por dia y hora exacta', () => {
    const items = [
      makeItem('2026-07-16T01:50:00.000Z', { startTimeLocal: '21:50', dateLocal: '2026-07-15' }),
      makeItem('2026-07-15T10:00:00-04:00', {
        startTimeLocal: '10:00',
        dateLocal: '2026-07-15',
        bookingId: 'other',
      }),
    ]
    const result = getEventsForDayAndHour(items, '2026-07-15', 21)
    expect(result).toHaveLength(1)
    expect(result[0].bookingId).toBe('test-booking-id')
  })

  it('retorna vacio cuando no hay eventos para ese dia/hora', () => {
    const items = [
      makeItem('2026-07-15T10:00:00-04:00', { startTimeLocal: '10:00', dateLocal: '2026-07-15' }),
    ]
    const result = getEventsForDayAndHour(items, '2026-07-15', 21)
    expect(result).toHaveLength(0)
  })
})

describe('getAllEventsForDay', () => {
  it('retorna todos los items de un dia', () => {
    const items = [
      makeItem('2026-07-16T01:50:00.000Z', { startTimeLocal: '21:50', dateLocal: '2026-07-15' }),
      makeItem('2026-07-15T10:00:00-04:00', {
        startTimeLocal: '10:00',
        dateLocal: '2026-07-15',
        bookingId: 'other',
      }),
      makeItem('2026-07-16T10:00:00-04:00', {
        startTimeLocal: '10:00',
        dateLocal: '2026-07-16',
        bookingId: 'next-day',
      }),
    ]
    const result = getAllEventsForDay(items, '2026-07-15')
    expect(result).toHaveLength(2)
  })
})

describe('buildAgendaHourLayout', () => {
  const visibleDays = [
    dayjs('2026-07-13'),
    dayjs('2026-07-14'),
    dayjs('2026-07-15'),
    dayjs('2026-07-16'),
    dayjs('2026-07-17'),
    dayjs('2026-07-18'),
    dayjs('2026-07-19'),
  ]

  it('incluye items aunque esten fuera del horario laboral si el schedule range lo permite', () => {
    const items = [
      makeItem('2026-07-16T01:50:00.000Z', {
        startTimeLocal: '21:50',
        dateLocal: '2026-07-15',
        durationMinutes: 60,
      }),
    ]
    const dayAvailability = [
      makeDayAvailability({
        dateKey: '2026-07-15',
        dayOfWeek: 3,
        startHour: 9,
        endHour: 19,
        hasBusinessHours: true,
      }),
    ]
    const result = buildAgendaHourLayout(items, visibleDays, dayAvailability, 9, 23)
    expect(result.byHour[21]).toBeDefined()
    expect(result.byHour[21].maxItems).toBe(1)
    expect(result.totalHeight).toBeGreaterThan(0)
  })

  it('asigna maxItems 0 a horas sin eventos', () => {
    const result = buildAgendaHourLayout([], visibleDays, [], 9, 19)
    expect(result.byHour[9].maxItems).toBe(0)
    expect(result.byHour[15].maxItems).toBe(0)
  })

  it('calcula altura dinamica segun maxItems en la fila', () => {
    const items = [
      makeItem('2026-07-15T10:00:00-04:00', {
        startTimeLocal: '10:00',
        dateLocal: '2026-07-15',
        bookingId: 'a',
      }),
      makeItem('2026-07-15T10:30:00-04:00', {
        startTimeLocal: '10:30',
        dateLocal: '2026-07-15',
        bookingId: 'b',
      }),
      makeItem('2026-07-15T11:00:00-04:00', {
        startTimeLocal: '11:00',
        dateLocal: '2026-07-15',
        bookingId: 'c',
      }),
    ]
    const dayAvailability = [
      makeDayAvailability({ dateKey: '2026-07-15', dayOfWeek: 3, startHour: 9, endHour: 19 }),
    ]
    const result = buildAgendaHourLayout(items, visibleDays, dayAvailability, 9, 19)
    expect(result.byHour[10].maxItems).toBe(2)
    expect(result.byHour[10].height).toBeGreaterThan(baseHourHeight)
    expect(result.byHour[11].maxItems).toBe(1)
    expect(result.byHour[11].height).toBe(baseHourHeight)
  })
})
