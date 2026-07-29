import dayjs from 'dayjs'
import { useMemo } from 'react'
import type { AgendaCalendarItemResponse, BusinessHoursResponse } from '../../../services/api/types'
import { AgendaEventCard } from './AgendaEventCard'
import { CalendarLegend } from './CalendarLegend'
import { CurrentTimeLine } from './CurrentTimeLine'
import {
  baseHourHeight,
  buildAgendaHourLayout,
  buildDayAvailability,
  calendarDays,
  computeScheduleRange,
  eventGap,
  formatCalendarDay,
  getDaysWithEvents,
  getEventsForDayAndHour,
  getScheduleHours,
  getAllEventsForDay,
  rowVerticalPadding,
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

function DayHeaderCell({
  day,
  daysWithEvents,
  dayAvailabilityMap,
  allEventsForDay,
}: {
  day: dayjs.Dayjs
  daysWithEvents: Set<string>
  dayAvailabilityMap: Map<string, DayAvailability>
  allEventsForDay: Map<string, AgendaCalendarItemResponse[]>
}) {
  const fd = formatCalendarDay(day)
  const isToday = day.isSame(dayjs(), 'day')
  const isEmptyDay = !daysWithEvents.has(fd.key)
  const dayAvail = dayAvailabilityMap.get(fd.key)
  const hasHours = dayAvail?.hasBusinessHours ?? false
  const dayEvents = allEventsForDay.get(fd.key) ?? []
  const totalSlots = hasHours ? (dayAvail!.endHour - dayAvail!.startHour) * 2 : 0
  const usedSlots = dayEvents.length
  const occupancyPct = totalSlots > 0 ? Math.round((usedSlots / totalSlots) * 100) : 0

  return (
    <div
      className={`flex flex-col items-center border-r border-slate-200 px-1 pb-2 pt-2.5 last:border-r-0 ${
        isToday ? 'bg-blue-50/40' : ''
      }`}
      key={fd.key}
    >
      <span
        className={`text-[10px] font-bold uppercase tracking-wider ${
          isToday ? 'text-blue-600' : 'text-slate-500'
        }`}
      >
        {fd.label}
      </span>
      <div className="mt-0.5 flex items-center gap-0.5">
        <span
          className={`inline-flex items-center justify-center text-sm font-bold leading-none ${
            isToday ? 'h-7 w-7 rounded-full bg-blue-600 text-white shadow-sm' : 'text-slate-900'
          }`}
        >
          {fd.dayNumber}
        </span>
        <span
          className={`text-[10px] font-semibold ${isToday ? 'text-blue-500' : 'text-slate-400'}`}
        >
          /{fd.month}
        </span>
      </div>
      {hasHours ? (
        <div className="mt-1 flex items-center gap-1">
          <span className="text-[10px] font-semibold text-slate-700">{usedSlots}</span>
          <span className="text-[9px] text-slate-400">/ {totalSlots}</span>
          <div className="ml-1 h-1.5 w-10 overflow-hidden rounded-full bg-slate-200">
            <div
              className="h-full rounded-full transition-all duration-300"
              style={{
                width: `${Math.min(occupancyPct, 100)}%`,
                backgroundColor:
                  occupancyPct >= 80 ? '#ef4444' : occupancyPct >= 50 ? '#f59e0b' : '#10b981',
              }}
            />
          </div>
        </div>
      ) : isEmptyDay ? (
        <span className="mt-1 text-[9px] font-semibold uppercase tracking-wider text-slate-300">
          Sin reservas
        </span>
      ) : (
        <span className="mt-1 text-[9px] font-semibold uppercase tracking-wider text-amber-500">
          Sin atencion
        </span>
      )}
    </div>
  )
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
    () =>
      buildAgendaHourLayout(
        calendarItems,
        visibleDays,
        dayAvailability,
        scheduleStartHour,
        scheduleEndHour,
      ),
    [calendarItems, visibleDays, dayAvailability, scheduleStartHour, scheduleEndHour],
  )

  const daysWithEvents = useMemo(() => getDaysWithEvents(calendarItems), [calendarItems])
  const visibleDayKeys = useMemo(
    () => visibleDays.map((d) => d.format('YYYY-MM-DD')),
    [visibleDays],
  )

  const dayAvailabilityMap = useMemo(() => {
    const map = new Map<string, DayAvailability>()
    for (const da of dayAvailability) {
      map.set(da.dateKey, da)
    }
    return map
  }, [dayAvailability])

  const allEventsForDay = useMemo(() => {
    const map = new Map<string, AgendaCalendarItemResponse[]>()
    for (const key of visibleDayKeys) {
      map.set(key, getAllEventsForDay(calendarItems, key))
    }
    return map
  }, [calendarItems, visibleDayKeys])

  const scheduleHours = getScheduleHours(scheduleStartHour, scheduleEndHour)

  return (
    <div className="w-full overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs">
      <div
        className="grid border-b border-slate-200 bg-slate-50"
        style={{
          gridTemplateColumns: `${timeColumnWidth}px repeat(${calendarDays}, minmax(0, 1fr))`,
        }}
      >
        <div className="flex items-end justify-center border-r border-slate-200 pb-2 pt-2.5">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
            Hora
          </span>
        </div>
        {visibleDays.map((day) => (
          <DayHeaderCell
            allEventsForDay={allEventsForDay}
            day={day}
            dayAvailabilityMap={dayAvailabilityMap}
            daysWithEvents={daysWithEvents}
            key={day.format('YYYY-MM-DD')}
          />
        ))}
      </div>

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
                    {String(hour).padStart(2, '0')}:00
                  </span>
                </div>

                {visibleDays.map((day) => {
                  const dayKey = day.format('YYYY-MM-DD')
                  const da = dayAvailabilityMap.get(dayKey)
                  const isDayActive = da?.hasBusinessHours ?? false
                  const eventsForHour = getEventsForDayAndHour(calendarItems, dayKey, hour)
                  return (
                    <div
                      className={`flex flex-col border-r border-slate-100 last:border-r-0 ${
                        !isDayActive ? 'bg-slate-50/50' : ''
                      }`}
                      key={`${dayKey}-${hour}`}
                      style={{ gap: `${eventGap}px`, padding: `${rowVerticalPadding}px` }}
                    >
                      {eventsForHour.map((item) => (
                        <AgendaEventCard
                          compact
                          key={item.bookingId}
                          isSelected={selectedBookingId === item.bookingId}
                          item={item}
                          onClick={() => onSelectBooking(item.bookingId)}
                        />
                      ))}
                    </div>
                  )
                })}
              </div>
            )
          })
        ) : (
          <div className="flex items-center justify-center py-12 text-sm text-slate-400">
            No hay horarios de atenci&oacute;n configurados para esta sucursal
          </div>
        )}
      </div>

      <div className="border-t border-slate-100 px-4 py-2.5">
        <CalendarLegend />
      </div>
    </div>
  )
}
