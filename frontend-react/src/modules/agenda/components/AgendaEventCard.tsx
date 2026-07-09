import type { AgendaCalendarItemResponse } from '../../../services/api/types'
import { getServiceColor, getStatusStyle } from './agendaUtils'

function formatEventTime(startTime: string, endTime: string) {
  return `${startTime}-${endTime}`
}

type AgendaEventCardProps = {
  item: AgendaCalendarItemResponse
  isSelected?: boolean
  compact?: boolean
  onClick?: () => void
  showStatus?: boolean
  style?: React.CSSProperties
}

export function AgendaEventCard({ item, isSelected, compact, onClick, showStatus, style }: AgendaEventCardProps) {
  const startTime = item.startTimeLocal ?? formatAgendaTime(item.startsAt)
  const endTime = item.endTimeLocal ?? formatAgendaTime(item.endsAt)
  const isWhatsApp = item.sourceChannel?.toUpperCase().includes('WHATSAPP')
  const statusStyle = getStatusStyle(item.status)

  if (compact) {
    return (
      <button
        aria-label={`${item.serviceName ?? item.subject} ${startTime}`}
        className={[
          'group flex h-full w-full overflow-hidden rounded-md border border-slate-200 bg-white text-left shadow-xs transition hover:-translate-y-px hover:shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-300',
          isSelected ? 'ring-2 ring-blue-400' : '',
        ].join(' ')}
        onClick={onClick}
        style={{
          ...style,
          borderLeft: `3px solid ${statusStyle.hex}`,
        }}
        type="button"
      >
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-0 px-1.5 py-1">
          <div className="flex items-center gap-1">
            <span className="truncate text-[10px] font-semibold leading-tight text-slate-700">
              {startTime}
            </span>
            {isWhatsApp ? (
              <span className="shrink-0 rounded bg-green-100 px-1 py-[1px] text-[8px] font-bold leading-tight text-green-700">
                WA
              </span>
            ) : null}
          </div>
          <p className="truncate text-[10px] font-semibold leading-tight text-slate-800">
            {item.serviceName ?? item.subject}
          </p>
          <p className="truncate text-[9px] leading-tight text-slate-500">
            {item.customerName}
          </p>
        </div>
      </button>
    )
  }

  return (
    <button
      aria-label={`${item.serviceName ?? item.subject} ${startTime}`}
      className={[
        'group overflow-hidden rounded-xl border border-slate-200 bg-white text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-blue-300',
        isSelected ? 'ring-2 ring-blue-500' : '',
      ].join(' ')}
      onClick={onClick}
      style={style}
      type="button"
    >
      <div className="flex">
        <div
          className="w-1.5 shrink-0"
          style={{ backgroundColor: statusStyle.hex }}
        />
        <div className="flex min-w-0 flex-1 flex-col gap-0.5 px-3 py-2.5">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-bold leading-tight text-slate-800">
              {formatEventTime(startTime, endTime)}
            </span>
            {isWhatsApp ? (
              <span className="shrink-0 rounded-full bg-green-100 px-2 py-0.5 text-[9px] font-bold uppercase leading-tight text-green-700">
                WA
              </span>
            ) : null}
          </div>
          <p className="truncate text-xs font-semibold leading-tight text-slate-900">
            {item.serviceName ?? item.subject}
          </p>
          <p className="truncate text-[11px] leading-tight text-slate-500">
            {item.customerName}
          </p>
          {item.professionalName ? (
            <p className="truncate text-[10px] leading-tight text-slate-400">
              {item.professionalName}
            </p>
          ) : null}
          {showStatus ? (
            <p className={`mt-0.5 text-[10px] font-semibold ${statusStyle.text}`}>
              {statusStyle.label}
            </p>
          ) : null}
        </div>
      </div>
    </button>
  )
}

function formatAgendaTime(value: string) {
  return new Intl.DateTimeFormat('es-CL', {
    timeZone: 'America/Santiago',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}
