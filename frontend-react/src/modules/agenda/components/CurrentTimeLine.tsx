import { useEffect, useState } from 'react'
import { agendaTimeZone, baseHourHeight } from './agendaUtils'

type CurrentTimeLineProps = {
  visibleDayKeys: string[]
  hourLayout: Record<number, { top: number; height: number; maxItems: number }>
  timeColumnWidth: number
  scheduleStartHour: number
  scheduleEndHour: number
}

export function CurrentTimeLine({
  visibleDayKeys,
  hourLayout,
  timeColumnWidth,
  scheduleStartHour,
  scheduleEndHour,
}: CurrentTimeLineProps) {
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const tick = () => setNow(new Date())
    const nextMinute = new Date()
    nextMinute.setSeconds(0, 0)
    nextMinute.setMinutes(nextMinute.getMinutes() + 1)
    const msUntilNextMinute = nextMinute.getTime() - Date.now()

    const timeout = setTimeout(() => {
      tick()
      const interval = setInterval(tick, 60000)
      return () => clearInterval(interval)
    }, msUntilNextMinute)

    return () => clearTimeout(timeout)
  }, [])

  const dateKey = new Intl.DateTimeFormat('en-CA', {
    timeZone: agendaTimeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now)

  const isVisible = visibleDayKeys.includes(dateKey)
  if (!isVisible) return null

  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: agendaTimeZone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(now)

  const hour = Number(parts.find((p) => p.type === 'hour')?.value ?? 0)
  const minute = Number(parts.find((p) => p.type === 'minute')?.value ?? 0)

  if (hour < scheduleStartHour || hour > scheduleEndHour) return null

  const slot = hourLayout[hour] ?? { top: 0, height: baseHourHeight, maxItems: 1 }
  const top = slot.top + (minute / 60) * slot.height
  const label = new Intl.DateTimeFormat('es-CL', {
    timeZone: agendaTimeZone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(now)

  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute z-30"
      style={{ left: 0, right: 0, top: 0 }}
    >
      <div
        className="border-t-2 border-dashed border-rose-400/80"
        style={{ position: 'absolute', top, left: `${timeColumnWidth + 4}px`, right: 0 }}
      >
        <span className="absolute -top-3 rounded-full bg-rose-50 px-2 py-0.5 text-[10px] font-bold text-rose-600 shadow-sm"
          style={{ left: '4px' }}>
          Ahora {label}
        </span>
      </div>
    </div>
  )
}