import dayjs from 'dayjs'
import type { AgendaCalendarItemResponse, BusinessHoursResponse } from '../../../services/api/types'

export const agendaTimeZone = 'America/Santiago'
export const baseHourHeight = 96
export const eventCardEstimatedHeight = 80
export const eventGap = 8
export const rowVerticalPadding = 12
export const calendarDays = 7
export const weekDayLabels = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab']
export const scheduleHoursStep = 1

const dayOfWeekMap: Record<number, { label: string; shortLabel: string }> = {
  1: { label: 'Lunes', shortLabel: 'Lun' },
  2: { label: 'Martes', shortLabel: 'Mar' },
  3: { label: 'Miércoles', shortLabel: 'Mie' },
  4: { label: 'Jueves', shortLabel: 'Jue' },
  5: { label: 'Viernes', shortLabel: 'Vie' },
  6: { label: 'Sábado', shortLabel: 'Sab' },
  7: { label: 'Domingo', shortLabel: 'Dom' },
}

export type EventLayout = {
  item: AgendaCalendarItemResponse
  top: number
  height: number
  left: string
  width: string
}

export type AgendaHourLayout = {
  byHour: Record<number, { top: number; height: number; maxItems: number }>
  totalHeight: number
}

export type DayAvailability = {
  dayOfWeek: number
  dateKey: string
  hasBusinessHours: boolean
  startHour: number
  endHour: number
}

export function getAgendaDateKey(value: string) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: agendaTimeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

export function getAgendaHourMinute(value: string) {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: agendaTimeZone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(new Date(value))
  const hour = Number(parts.find((part) => part.type === 'hour')?.value ?? '0')
  const minute = Number(parts.find((part) => part.type === 'minute')?.value ?? '0')
  return { hour, minute }
}

export function formatAgendaTime(value: string) {
  return new Intl.DateTimeFormat('es-CL', {
    timeZone: agendaTimeZone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

export function getWeekStart(value: string) {
  const current = dayjs(value).startOf('day')
  const daysFromMonday = (current.day() + 6) % 7
  return current.subtract(daysFromMonday, 'day')
}

export function buildVisibleDays(value: string) {
  const weekStart = getWeekStart(value)
  return Array.from({ length: calendarDays }, (_, index) => weekStart.add(index, 'day'))
}

export function formatCalendarDay(value: dayjs.Dayjs) {
  return {
    label: weekDayLabels[value.day()],
    dayNumber: value.format('DD'),
    month: value.format('MM'),
    key: value.format('YYYY-MM-DD'),
  }
}

export function getLocalHourMinute(item: AgendaCalendarItemResponse) {
  if (item.startTimeLocal) {
    return {
      hour: Number(item.startTimeLocal.slice(0, 2)),
      minute: Number(item.startTimeLocal.slice(3, 5)),
    }
  }
  return getAgendaHourMinute(item.startsAt)
}

export function getItemStartHour(item: AgendaCalendarItemResponse) {
  return getLocalHourMinute(item).hour
}

export function buildDayAvailability(businessHours: BusinessHoursResponse[], visibleDays: dayjs.Dayjs[]): DayAvailability[] {
  const hoursByDay: Record<number, { startTime: number; endTime: number }> = {}
  for (const bh of businessHours) {
    const startHour = Number(bh.startTime.slice(0, 2))
    const endHour = Number(bh.endTime.slice(0, 2))
    const existing = hoursByDay[bh.dayOfWeek]
    if (!existing) {
      hoursByDay[bh.dayOfWeek] = { startTime: startHour, endTime: endHour }
    } else {
      hoursByDay[bh.dayOfWeek] = {
        startTime: Math.min(existing.startTime, startHour),
        endTime: Math.max(existing.endTime, endHour),
      }
    }
  }

return visibleDays.map((day) => {
    const jsDay = day.day()
    const sqlDay = jsDay === 0 ? 7 : jsDay

    const bh = hoursByDay[sqlDay]
    return {
      dayOfWeek: sqlDay,
      dateKey: day.format('YYYY-MM-DD'),
      hasBusinessHours: Boolean(bh),
      startHour: bh?.startTime ?? 0,
      endHour: bh?.endTime ?? 0,
    }
  })
}

export function computeScheduleRange(
  dayAvailability: DayAvailability[],
  items: AgendaCalendarItemResponse[],
): { scheduleStartHour: number; scheduleEndHour: number } {
  let minStart = 24
  let maxEnd = 0
  for (const da of dayAvailability) {
    if (da.hasBusinessHours) {
      minStart = Math.min(minStart, da.startHour)
      maxEnd = Math.max(maxEnd, da.endHour)
    }
  }
  for (const item of items) {
    const { hour: startHour, minute: startMinute } = getLocalHourMinute(item)
    if (startHour < minStart) {
      minStart = startHour
    }
    const endHour = Math.ceil((startHour * 60 + startMinute + item.durationMinutes) / 60)
    if (endHour > maxEnd) {
      maxEnd = endHour
    }
  }
  return {
    scheduleStartHour: minStart < 24 ? minStart : 9,
    scheduleEndHour: maxEnd > 0 ? maxEnd : 21,
  }
}

export function getScheduleHours(scheduleStartHour: number, scheduleEndHour: number) {
  return Array.from({ length: scheduleEndHour - scheduleStartHour + 1 }, (_, index) => scheduleStartHour + index)
}

export function getEventsForDayAndHour(items: AgendaCalendarItemResponse[], dayKey: string, hour: number) {
  return items
    .filter((item) => {
      const itemDayKey = item.dateLocal ?? getAgendaDateKey(item.startsAt)
      return itemDayKey === dayKey && getItemStartHour(item) === hour
    })
    .sort((first, second) => {
      const firstTime = first.startTimeLocal ?? formatAgendaTime(first.startsAt)
      const secondTime = second.startTimeLocal ?? formatAgendaTime(second.startsAt)
      return firstTime.localeCompare(secondTime)
    })
}

export function getAllEventsForDay(items: AgendaCalendarItemResponse[], dayKey: string) {
  return items
    .filter((item) => {
      const itemDayKey = item.dateLocal ?? getAgendaDateKey(item.startsAt)
      return itemDayKey === dayKey
    })
    .sort((first, second) => {
      const firstTime = first.startTimeLocal ?? formatAgendaTime(first.startsAt)
      const secondTime = second.startTimeLocal ?? formatAgendaTime(second.startsAt)
      return firstTime.localeCompare(secondTime)
    })
}

export function getDaysWithEvents(items: AgendaCalendarItemResponse[]) {
  const withEvents = new Set<string>()
  items.forEach((item) => {
    withEvents.add(item.dateLocal ?? getAgendaDateKey(item.startsAt))
  })
  return withEvents
}

export function calcProportionalHeight(item: AgendaCalendarItemResponse, rowHeight: number): number {
  const ratio = Math.min(item.durationMinutes / 60, 1)
  return Math.max(36, Math.floor(ratio * (rowHeight - 4)))
}

export function layoutEventsInCell(events: AgendaCalendarItemResponse[], rowHeight: number): EventLayout[] {
  if (events.length === 0) return []
  const sorted = [...events].sort((a, b) => {
    const ma = getLocalHourMinute(a).minute
    const mb = getLocalHourMinute(b).minute
    return ma - mb
  })

  if (sorted.length === 1) {
    const item = sorted[0]
    const minute = getLocalHourMinute(item).minute
    return [{
      item,
      top: (minute / 60) * (rowHeight - 2) + 1,
      height: Math.max(36, Math.min(calcProportionalHeight(item, rowHeight), rowHeight - 2)),
      left: '2px',
      width: 'calc(100% - 4px)',
    }]
  }

  const groups: AgendaCalendarItemResponse[][] = []
  for (const event of sorted) {
    const startMin = getLocalHourMinute(event).minute
    const endMin = startMin + event.durationMinutes
    let added = false
    for (const group of groups) {
      const groupOverlaps = group.some((ge) => {
        const gs = getLocalHourMinute(ge).minute
        const ge2 = gs + ge.durationMinutes
        return startMin < ge2 && endMin > gs
      })
      if (groupOverlaps) {
        group.push(event)
        added = true
        break
      }
    }
    if (!added) {
      groups.push([event])
    }
  }

  const result: EventLayout[] = []
  groups.forEach((group) => {
    const cols = group.length
    group.forEach((item, idx) => {
      const minute = getLocalHourMinute(item).minute
      result.push({
        item,
        top: (minute / 60) * (rowHeight - 2) + 1,
        height: Math.max(28, Math.min(calcProportionalHeight(item, rowHeight), rowHeight - 2)),
        left: `${(idx / cols) * 100 + 0.5}%`,
        width: `calc(${(1 / cols) * 100}% - 1px)`,
      })
    })
  })

  return result
}

export function buildAgendaHourLayout(
  items: AgendaCalendarItemResponse[],
  visibleDays: dayjs.Dayjs[],
  dayAvailability: DayAvailability[],
  scheduleStartHour: number,
  scheduleEndHour: number,
): AgendaHourLayout {
  const visibleDayKeys = new Set(visibleDays.map((day) => day.format('YYYY-MM-DD')))
  const itemsByDayHour = new Map<string, number>()

  const hasBusinessDay = new Set<string>()
  for (const da of dayAvailability) {
    if (da.hasBusinessHours) {
      hasBusinessDay.add(da.dateKey)
    }
  }

  items.forEach((item) => {
    const dateKey = item.dateLocal ?? getAgendaDateKey(item.startsAt)
    const hour = getItemStartHour(item)

    if (!visibleDayKeys.has(dateKey) || hour < scheduleStartHour || hour > scheduleEndHour) {
      return
    }

    const key = `${dateKey}-${hour}`
    itemsByDayHour.set(key, (itemsByDayHour.get(key) ?? 0) + 1)
  })

  const byHour: AgendaHourLayout['byHour'] = {}
  let accumulatedTop = 0

  getScheduleHours(scheduleStartHour, scheduleEndHour).forEach((hour) => {
    const maxItems = Math.max(
      0,
      ...visibleDays.map((day) => itemsByDayHour.get(`${day.format('YYYY-MM-DD')}-${hour}`) ?? 0),
    )
    const height = baseHourHeight
    byHour[hour] = { top: accumulatedTop, height, maxItems }
    accumulatedTop += height
  })

  return { byHour, totalHeight: accumulatedTop }
}

export function getCurrentTimeIndicator(
  visibleDays: dayjs.Dayjs[],
  hourLayout: AgendaHourLayout,
  scheduleStartHour: number,
  scheduleEndHour: number,
) {
  const now = new Date()
  const currentDateKey = getAgendaDateKey(now.toISOString())
  const isVisible = visibleDays.some((day) => day.format('YYYY-MM-DD') === currentDateKey)

  if (!isVisible) {
    return null
  }

  const { hour, minute } = getAgendaHourMinute(now.toISOString())

  if (hour < scheduleStartHour || hour > scheduleEndHour) {
    return null
  }

  const hourSlot = hourLayout.byHour[hour] ?? { top: 0, height: baseHourHeight, maxItems: 1 }
  return {
    top: hourSlot.top + (minute / 60) * hourSlot.height,
    label: formatAgendaTime(now.toISOString()),
  }
}

export function formatTimeRange(item: AgendaCalendarItemResponse) {
  const start = item.startTimeLocal ?? formatAgendaTime(item.startsAt)
  const end = item.endTimeLocal ?? formatAgendaTime(item.endsAt)
  return `${start} - ${end}`
}

export function formatLongDate(value: string) {
  const dateKey = getAgendaDateKey(value)
  const dateValue = dayjs(dateKey)
  return `${weekDayLabels[dateValue.day()]}, ${dateValue.format('DD/MM/YYYY')}`
}

const statusColorMap: Record<string, { bar: string; bg: string; text: string; hex: string; label: string }> = {
  CONFIRMED: { bar: 'bg-emerald-500', bg: 'bg-emerald-50', text: 'text-emerald-900', hex: '#10b981', label: 'Confirmada' },
  CONFIRMADA: { bar: 'bg-emerald-500', bg: 'bg-emerald-50', text: 'text-emerald-900', hex: '#10b981', label: 'Confirmada' },
  PENDIENTE_CONFIRMACION: { bar: 'bg-amber-400', bg: 'bg-amber-50', text: 'text-amber-900', hex: '#f59e0b', label: 'Pendiente' },
  PENDING_CONFIRMATION: { bar: 'bg-amber-400', bg: 'bg-amber-50', text: 'text-amber-900', hex: '#f59e0b', label: 'Pendiente' },
  TEMPORARY: { bar: 'bg-amber-400', bg: 'bg-amber-50', text: 'text-amber-900', hex: '#f59e0b', label: 'Temporal' },
  RESCHEDULED: { bar: 'bg-sky-400', bg: 'bg-sky-50', text: 'text-sky-900', hex: '#38bdf8', label: 'Reprogramada' },
  REPROGRAMADA: { bar: 'bg-sky-400', bg: 'bg-sky-50', text: 'text-sky-900', hex: '#38bdf8', label: 'Reprogramada' },
  CANCELLED: { bar: 'bg-red-400', bg: 'bg-red-50', text: 'text-red-900', hex: '#f87171', label: 'Cancelada' },
  CANCELADA: { bar: 'bg-red-400', bg: 'bg-red-50', text: 'text-red-900', hex: '#f87171', label: 'Cancelada' },
  COMPLETED: { bar: 'bg-slate-400', bg: 'bg-slate-50', text: 'text-slate-800', hex: '#94a3b8', label: 'Completada' },
  ATENDIDA: { bar: 'bg-slate-400', bg: 'bg-slate-50', text: 'text-slate-800', hex: '#94a3b8', label: 'Atendida' },
  NO_SHOW: { bar: 'bg-orange-400', bg: 'bg-orange-50', text: 'text-orange-900', hex: '#fb923c', label: 'No asistió' },
}

export function getStatusStyle(status: string) {
  const normalized = status.toUpperCase()
  return statusColorMap[normalized] ?? { bar: 'bg-slate-400', bg: 'bg-slate-50', text: 'text-slate-800', label: status }
}

export function getStatusLabel(status: string) {
  return getStatusStyle(status).label
}

const serviceColorClasses = [
  'border-l-emerald-400',
  'border-l-blue-400',
  'border-l-violet-400',
  'border-l-amber-400',
  'border-l-rose-400',
  'border-l-teal-400',
  'border-l-indigo-400',
  'border-l-cyan-400',
  'border-l-orange-400',
  'border-l-pink-400',
]

export function getServiceColor(item: AgendaCalendarItemResponse) {
  const key = item.serviceId ?? item.serviceName ?? item.subject
  const hash = key.split('').reduce((current, char) => current + char.charCodeAt(0), 0)
  return serviceColorClasses[hash % serviceColorClasses.length]
}