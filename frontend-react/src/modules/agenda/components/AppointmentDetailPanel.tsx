import dayjs from 'dayjs'
import { Button } from '../../../components/ui/Button'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { Textarea } from '../../../components/ui/Textarea'
import type {
  AgendaCalendarItemResponse,
  BookingDetailResponse,
} from '../../../services/api/types'
import {
  formatLongDate,
  formatTimeRange,
  getStatusLabel,
  getStatusStyle,
} from './agendaUtils'

function getInitials(name: string) {
  const parts = name.trim().split(/\s+/).slice(0, 2)
  if (parts.length === 0 || !parts[0]) {
    return 'CL'
  }
  return parts.map((part) => part[0]?.toUpperCase()).join('')
}

function getWhatsAppStatus(
  item: AgendaCalendarItemResponse | null,
  detail?: BookingDetailResponse,
) {
  const lastWhatsAppReminder = (detail?.reminders ?? [])
    .filter((reminder) => reminder.channelType === 'WHATSAPP')
    .sort(
      (first, second) =>
        dayjs(second.sentAt ?? second.scheduledAt).valueOf() -
        dayjs(first.sentAt ?? first.scheduledAt).valueOf(),
    )[0]

  if (lastWhatsAppReminder?.sentAt) {
    return `WhatsApp enviado ${dayjs(lastWhatsAppReminder.sentAt).format('DD/MM HH:mm')}`
  }
  if (item?.sourceChannel?.toUpperCase().includes('WHATSAPP')) {
    return 'Canal WhatsApp asociado'
  }
  return 'Sin confirmacion WhatsApp registrada'
}

function DetailRow({ icon, label, value }: { icon: string; label: string; value: string }) {
  return (
    <div className="grid grid-cols-[72px_1fr] gap-2">
      <span className="text-[11px] font-bold uppercase tracking-[0.12em] text-slate-400">{icon}</span>
      <div>
        <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-slate-400">{label}</p>
        <p className="mt-0.5 text-sm font-medium text-slate-800">{value}</p>
      </div>
    </div>
  )
}

type AppointmentDetailPanelProps = {
  selectedItem: AgendaCalendarItemResponse | null
  bookingDetail: BookingDetailResponse | undefined
  bookingDetailLoading: boolean
  cancelReason: string
  onCancelReasonChange: (value: string) => void
  onConfirmWhatsApp: (bookingId: string) => void
  onCancel: (bookingId: string) => void
  onReschedule: (bookingId: string) => void
  onViewHistory: (bookingId: string) => void
  onEditNotes: (bookingId: string) => void
  confirmWhatsAppPending: boolean
  cancelPending: boolean
}

export function AppointmentDetailPanel({
  selectedItem,
  bookingDetail,
  bookingDetailLoading,
  cancelReason,
  onCancelReasonChange,
  onConfirmWhatsApp,
  onCancel,
  onReschedule,
  onViewHistory,
  onEditNotes,
  confirmWhatsAppPending,
  cancelPending,
}: AppointmentDetailPanelProps) {
  const selectedStatusStyle = getStatusStyle(selectedItem?.status ?? bookingDetail?.status ?? '')

  if (!selectedItem) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/50 p-8 text-center">
        <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-slate-100">
          <svg className="h-6 w-6 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </div>
        <h3 className="text-sm font-semibold text-slate-800">Selecciona una reserva</h3>
        <p className="mt-1 text-xs text-slate-500">
          Al posicionarte sobre una cita se mostrar&aacute; el detalle, la trazabilidad y las acciones disponibles.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-blue-50 to-blue-100 text-sm font-bold text-blue-700">
            {getInitials(selectedItem.customerName)}
          </div>
          <div className="min-w-0">
            <h2 className="text-base font-bold text-slate-900 truncate">
              {selectedItem.customerName}
            </h2>
            <p className="text-xs text-slate-500 truncate">
              {bookingDetail?.customerPhone ?? selectedItem.customerPhone}
            </p>
          </div>
        </div>
        <StatusBadge
          label={getStatusLabel(selectedItem.status)}
          tone={
            selectedStatusStyle.hex === '#10b981'
              ? 'success'
              : selectedStatusStyle.hex === '#f59e0b'
                ? 'warning'
                : selectedStatusStyle.hex === '#f87171'
                  ? 'danger'
                  : 'info'
          }
        />
      </div>

      <div className="divide-y divide-slate-100 rounded-xl border border-slate-100 bg-slate-50/50">
        <div className="space-y-3 p-4">
          <DetailRow
            icon="Servicio"
            label="Servicio"
            value={selectedItem.serviceName ?? selectedItem.subject}
          />
          <DetailRow
            icon="Fecha"
            label="Fecha y hora"
            value={`${formatLongDate(selectedItem.startsAt)} · ${formatTimeRange(selectedItem)}`}
          />
          <DetailRow
            icon="Equipo"
            label="Profesional"
            value={selectedItem.professionalName ?? 'Profesional por asignar'}
          />
          <DetailRow
            icon="Lugar"
            label="Ubicacion"
            value={selectedItem.roomName ?? selectedItem.locationName ?? 'Sin ubicacion'}
          />
          <DetailRow
            icon="Tiempo"
            label="Duracion"
            value={`${selectedItem.durationMinutes} minutos`}
          />
          <DetailRow
            icon="WA"
            label="Estado WhatsApp"
            value={getWhatsAppStatus(selectedItem, bookingDetail)}
          />
        </div>
      </div>

      <div>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-[0.1em] text-slate-500">
          Notas internas
        </h3>
        <div className="rounded-xl border border-slate-100 bg-white p-3 text-sm text-slate-600">
          {bookingDetailLoading
            ? 'Cargando detalle...'
            : (bookingDetail?.notes ?? 'Sin notas registradas.')}
        </div>
      </div>

      <div className="space-y-2.5">
        <Button
          className="bg-emerald-600 hover:bg-emerald-700"
          fullWidth
          loading={confirmWhatsAppPending}
          onClick={() => onConfirmWhatsApp(selectedItem.bookingId)}
        >
          Confirmar por WhatsApp
        </Button>
        <div className="grid grid-cols-2 gap-2">
          <Button
            fullWidth
            onClick={() => onReschedule(selectedItem.bookingId)}
            variant="secondary"
          >
            Reprogramar
          </Button>
          <Button
            fullWidth
            onClick={() => onEditNotes(selectedItem.bookingId)}
            variant="secondary"
          >
            Editar notas
          </Button>
        </div>
        <Button
          fullWidth
          onClick={() => onViewHistory(selectedItem.bookingId)}
          variant="secondary"
        >
          Ver historial del cliente
        </Button>
      </div>

      <div className="rounded-xl border border-red-100 bg-red-50/50 p-4">
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-[0.1em] text-red-600">
          Zona de peligro
        </h3>
        <Textarea
          label="Motivo de cancelacion"
          onChange={(event) => onCancelReasonChange(event.target.value)}
          placeholder="Indica el motivo obligatorio antes de cancelar"
          rows={2}
          value={cancelReason}
        />
        <Button
          disabled={cancelReason.trim().length < 5}
          fullWidth
          loading={cancelPending}
          onClick={() => onCancel(selectedItem.bookingId)}
          variant="danger"
          className="mt-2"
        >
          Cancelar cita
        </Button>
      </div>
    </div>
  )
}
