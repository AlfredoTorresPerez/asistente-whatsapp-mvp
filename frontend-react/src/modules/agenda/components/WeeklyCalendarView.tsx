import dayjs from 'dayjs'
import { useMemo } from 'react'
import type { AgendaCalendarItemResponse, BusinessHoursResponse } from '../../../services/api/types'
import { AgendaEventCard } from './AgendaEventCard'
import { CurrentTimeLine } from './CurrentTimeLine'
import {
  baseHourHeight,
  buildAgendaHourLayout,
  buildDayAvailability,
  calendarDays,
  computeScheduleRange,
  formatCalendarDay,
  getDaysWithEvents,
  getEventsForDayAndHour,
  getScheduleHours,
  layoutEventsInCell,
} from './agendaUtils'
import type { DayAvailability } from './agendaUtils'

type WeeklyCalendarViewProps = {
  calendarItems: AgendaCalendarItemResponse[]
  visibleDays: dayjs.Dayjs[]
  selectedBookingId: string | null
  timeColumnWidth?: number
  businessHours?: BusinessHoursResponse[]
  onSelectBooking: (bookingId: string) => void
}

export function WeeklyCalendarView({
  calendarItems,
  visibleDays,
  selectedBookingId,
  timeColumnWidth = 56,
  businessHours = [],
  onSelectBooking,
}: WeeklyCalendarViewProps) {
  const dayAvailability = useMemo(
    () => buildDayAvailability(businessHours, visibleDays),
    [businessHours, visibleDays],
  )

  const { scheduleStartHour, scheduleEndHour } = useMemo(
    () => computeScheduleRange(dayAvailability, calendarItems),
    [dayAvailability, calendarItems],
  )

  const hourLayout = useMemo(
    () => buildAgendaHourLayout(calendarItems, visibleDays, dayAvailability, scheduleStartHour, scheduleEndHour),
    [calendarItems, visibleDays, dayAvailability, scheduleStartHour, scheduleEndHour],
  )

  const daysWithEvents = useMemo(() => getDaysWithEvents(calendarItems), [calendarItems])
  const visibleDayKeys = useMemo(() => visibleDays.map((d) => d.format('YYYY-MM-DD')), [visibleDays])

  const dayAvailabilityMap = useMemo(() => {
    const map = new Map<string, DayAvailability>()
    for (const da of dayAvailability) {
      map.set(da.dateKey, da)
    }
    return map
  }, [dayAvailability])

  const scheduleHours = getScheduleHours(scheduleStartHour, scheduleEndHour)

  return (
    <div className="w-full overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
      {/* Header row */}
      <div
        className="grid border-b border-slate-200 bg-slate-50"
        style={{ gridTemplateColumns: `${timeColumnWidth}px repeat(${calendarDays}, minmax(0, 1fr))` }}
      >
        <div className="flex items-end justify-center border-r border-slate-200 pb-2 pt-3">
          <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Hora</span>
        </div>
        {visibleDays.map((day) => {
          const fd = formatCalendarDay(day)
          const isToday = day.isSame(dayjs(), 'day')
          const isEmptyDay = !daysWithEvents.has(fd.key)
          const dayAvail = dayAvailabilityMap.get(fd.key)
          const hasHours = dayAvail?.hasBusinessHours ?? false
          return (
            <div
              className="flex flex-col items-center border-r border-slate-200 px-1 pb-2 pt-3 last:border-r-0"
              key={fd.key}
            >
              <span
                className={`text-[10px] font-semibold uppercase tracking-wider ${
                  isToday ? 'text-emerald-600' : 'text-slate-500'
                }`}
              >
                {fd.label}
              </span>
              <div className="mt-1 flex items-center gap-0.5">
                <span
                  className={`inline-flex items-center justify-center text-sm font-bold leading-none ${
                    isToday
                      ? 'h-7 w-7 rounded-full bg-emerald-500 text-white'
                      : 'text-slate-900'
                  }`}
                >
                  {fd.dayNumber}
                </span>
                <span className={`text-[10px] font-semibold ${isToday ? 'text-emerald-500' : 'text-slate-400'}`}>
                  /{fd.month}
                </span>
              </div>
              {!hasHours ? (
                <span className="mt-1 text-[9px] font-semibold uppercase tracking-wider text-amber-500">
                  Sin disponibilidad
                </span>
              ) : isEmptyDay ? (
                <span className="mt-1 text-[9px] font-semibold uppercase tracking-wider text-slate-300">
                  Sin reservas
                </span>
              ) : null}
            </div>
          )
        })}
      </div>

      {/* Grid body */}
      <div className="relative">
        <CurrentTimeLine
          hourLayout={hourLayout.byHour}
          timeColumnWidth={timeColumnWidth}
          visibleDayKeys={visibleDayKeys}
          scheduleStartHour={scheduleStartHour}
          scheduleEndHour={scheduleEndHour}
        />

        {scheduleHours.length > 0 ? (
          scheduleHours.map((hour) => {
            const rowHeight = hourLayout.byHour[hour]?.height ?? baseHourHeight
            return (
              <div
                className="grid border-t border-slate-100"
                key={hour}
                style={{
                  gridTemplateColumns: `${timeColumnWidth}px repeat(${calendarDays}, minmax(0, 1fr))`,
                  height: rowHeight,
                }}
              >
                <div className="flex items-start justify-center border-r border-slate-100 pt-1.5">
                  <span className="text-[10px] font-semibold leading-none text-slate-400">
                    {String(hour).padStart(2, '0')}
                  </span>
                </div>

                {visibleDays.map((day) => {
                  const dayKey = day.format('YYYY-MM-DD')
                  const da = dayAvailabilityMap.get(dayKey)
                  const isDayActive = da?.hasBusinessHours ?? false
                  const eventsForHour = getEventsForDayAndHour(calendarItems, dayKey, hour)
                  const eventLayouts = layoutEventsInCell(eventsForHour, rowHeight)
                  return (
                    <div
                      className={`relative border-r border-slate-100 last:border-r-0 ${
                        !isDayActive ? 'bg-slate-50/50' : ''
                      }`}
                      key={`${dayKey}-${hour}`}
                      style={{ height: rowHeight }}
                    >
                      {eventLayouts.length > 0 ? (
                        eventLayouts.map(({ item, top, height, left, width }) => (
                          <div
                            key={item.bookingId}
                            className="absolute"
                            style={{
                              left,
                              width,
                              top: `${top}px`,
                              height: `${height}px`,
                              padding: '1px',
                              boxSizing: 'border-box',
                            }}
                          >
                            <AgendaEventCard
                              compact
                              isSelected={selectedBookingId === item.bookingId}
                              item={item}
                              onClick={() => onSelectBooking(item.bookingId)}
                            />
                          </div>
                        ))
                      ) : null}
                    </div>
                  )
                })}
              </div>
            )
          })
        ) : (
          <div className="flex items-center justify-center py-12 text-sm text-slate-400">
            No hay horarios de atención configurados para esta sucursal
          </div>
        )}
      </div>
    </div>
  )
}

