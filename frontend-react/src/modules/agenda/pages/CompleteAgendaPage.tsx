import dayjs from 'dayjs'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useToast } from '../../../lib/toast'
import { ApiClientError } from '../../../services/api/httpClient'
import {
  cancelAgendaBookingRequest,
  completeAgendaBookingRequest,
  confirmAgendaBookingRequest,
  getAgendaCalendarRequest,
  getAgendaFilterOptionsRequest,
  getBusinessHoursRequest,
  markAgendaBookingNoShowRequest,
  startAgendaBookingServiceRequest,
} from '../../../services/api/completeAgendaApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import {
  createBookingConfirmationLinkRequest,
  getBookingDetailRequest,
} from '../../../services/api/bookingsApi'
import type { AgendaCalendarItemResponse, BookingDetailResponse } from '../../../services/api/types'
import { getBookingStatusLabel } from '../../bookings/bookingOptions'
import { AppointmentDetailPanel } from '../components/AppointmentDetailPanel'
import { CalendarWeekNavigation } from '../components/CalendarWeekNavigation'
import { WeeklyCalendarView } from '../components/WeeklyCalendarView'
import {
  buildAppointmentWhatsAppMessage,
  buildWhatsAppUrl,
  normalizeWhatsAppPhone,
} from '../../../lib/whatsapp'
import {
  buildVisibleDays,
  calendarDays,
  getStatusLabel,
} from '../components/agendaUtils'

const today = dayjs().format('YYYY-MM-DD')

const statusOptions = [
  { label: 'Reservas activas', value: '' },
  { label: 'Confirmadas', value: 'CONFIRMADA' },
  { label: 'Pendientes de confirmacion', value: 'PENDIENTE_CONFIRMACION' },
  { label: 'En atencion', value: 'EN_ATENCION' },
  { label: 'Completadas', value: 'COMPLETADA' },
  { label: 'Canceladas', value: 'CANCELADA' },
  { label: 'Reprogramadas', value: 'REPROGRAMADA' },
  { label: 'Solicitadas', value: 'SOLICITADA' },
  { label: 'Pendientes pago', value: 'PENDIENTE_PAGO' },
  { label: 'Inasistencias', value: 'NO_ASISTE' },
  { label: 'Vencidas', value: 'EXPIRADA' },
]

function buildFilterLabel(name: string, detail?: string | null) {
  return detail ? `${name} · ${detail}` : name
}

function buildRecentActivity(detail?: BookingDetailResponse) {
  const statusEvents = (detail?.statusHistory ?? []).map((event) => ({
    id: event.id,
    title: getStatusLabel(event.newStatus),
    detail: event.reason ?? event.source,
    at: event.createdAt,
  }))

  const reminderEvents = (detail?.reminders ?? []).map((reminder) => ({
    id: reminder.id,
    title: `${getReminderTypeLabel(reminder.reminderType)} por ${getChannelLabel(reminder.channelType)}`,
    detail: getReminderStatusLabel(reminder.status),
    at: reminder.sentAt ?? reminder.scheduledAt,
  }))

  return [...statusEvents, ...reminderEvents]
    .sort((first, second) => dayjs(second.at).valueOf() - dayjs(first.at).valueOf())
    .slice(0, 4)
}

function formatCLP(value: number) {
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0,
  }).format(value)
}

function getPaymentStatusLabel(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'PAID':
    case 'PAGADO':
      return 'Pagado'
    case 'PARTIAL':
    case 'PARCIAL':
      return 'Abono parcial'
    case 'PENDING':
    case 'PENDIENTE':
      return 'Pendiente'
    case 'NOT_REQUIRED':
      return 'No requiere pago'
    default:
      return 'Sin estado de pago'
  }
}

function getChannelLabel(channel?: string | null) {
  switch ((channel ?? '').toUpperCase()) {
    case 'WHATSAPP':
      return 'WhatsApp'
    case 'AGENDA':
      return 'Agenda interna'
    case 'WEB':
      return 'Web'
    case 'TELEFONO':
      return 'Teléfono'
    case 'PRESENCIAL':
      return 'Presencial'
    case 'EMAIL':
      return 'Correo electrónico'
    default:
      return 'No registrado'
  }
}

function getReminderTypeLabel(type?: string | null) {
  switch ((type ?? '').toUpperCase()) {
    case 'CONFIRMATION':
      return 'Confirmación'
    case 'REMINDER':
      return 'Recordatorio'
    case 'RESCHEDULE':
      return 'Reprogramación'
    case 'CANCELLATION':
      return 'Cancelación'
    default:
      return 'Notificación'
  }
}

function getReminderStatusLabel(status?: string | null) {
  switch ((status ?? '').toUpperCase()) {
    case 'PENDING':
    case 'SCHEDULED':
      return 'Programada'
    case 'SENT':
      return 'Enviada'
    case 'DELIVERED':
      return 'Entregada'
    case 'READ':
      return 'Leída'
    case 'FAILED':
      return 'Fallida'
    case 'CANCELLED':
      return 'Cancelada'
    default:
      return 'Sin estado'
  }
}

function getNotificationSummary(detail?: BookingDetailResponse) {
  const reminders = detail?.reminders ?? []
  if (reminders.length === 0) {
    return 'Sin notificaciones programadas'
  }
  const sent = reminders.filter((reminder) => reminder.sentAt).length
  const pending = reminders.filter((reminder) =>
    ['PENDING', 'SCHEDULED'].includes((reminder.status ?? '').toUpperCase()),
  ).length
  return `${sent} enviada(s), ${pending} pendiente(s)`
}

function getOperationalRows(item: AgendaCalendarItemResponse | null, detail?: BookingDetailResponse) {
  const paidAmount = (detail?.payments ?? [])
    .filter((payment) => ['APPROVED', 'PAID'].includes((payment.status ?? '').toUpperCase()))
    .reduce((total, payment) => total + Number(payment.amount ?? 0), 0)
  const deposit = Number(detail?.depositAmount ?? 0)
  const balance = Math.max(deposit - paidAmount, 0)

  return [
    {
      name: 'Estado',
      detail: item ? getBookingStatusLabel(item.status) : 'Selecciona una cita',
    },
    {
      name: 'Pago',
      detail: `${getPaymentStatusLabel(detail?.paymentStatus)} · Abono ${formatCLP(deposit)}`,
    },
    {
      name: 'Saldo',
      detail: formatCLP(balance),
    },
    {
      name: 'Origen',
      detail: getChannelLabel(item?.sourceChannel),
    },
    {
      name: 'Notificaciones',
      detail: getNotificationSummary(detail),
    },
    {
      name: 'Historial',
      detail: `${detail?.statusHistory?.length ?? 0} cambio(s) registrados`,
    },
  ]
}

function SectionCard({
  title,
  description,
  children,
  className = '',
}: {
  title: string
  description?: string
  children: React.ReactNode
  className?: string
}) {
  return (
    <Card className={`space-y-4 ${className}`}>
      <div>
        <h2 className="text-base font-semibold text-slate-900">{title}</h2>
        {description ? <p className="mt-0.5 text-xs text-slate-500">{description}</p> : null}
      </div>
      {children}
    </Card>
  )
}

export function CompleteAgendaPage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [locationId, setLocationId] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [professionalId, setProfessionalId] = useState('')
  const [roomId, setRoomId] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [date, setDate] = useState(today)
  const [selectedBookingId, setSelectedBookingId] = useState<string | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const detailPanelRef = useRef<HTMLDivElement>(null)

  const visibleDays = useMemo(() => buildVisibleDays(date), [date])
  const weekStart = visibleDays[0]
  const weekEnd = weekStart.add(calendarDays, 'day')

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'agenda-completa'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
  })

  const businessHoursQuery = useQuery({
    queryKey: ['agenda-business-hours', locationId || 'all'],
    queryFn: () => getBusinessHoursRequest(locationId || undefined),
    enabled: Boolean(locationsQuery.data?.length),
  })

  const filterOptionsQuery = useQuery({
    queryKey: ['agenda-filter-options', locationId || 'all'],
    queryFn: () => getAgendaFilterOptionsRequest({ locationId: locationId || undefined }),
  })

  const serviceOptions = useMemo(
    () => [
      { label: 'Todos los servicios', value: '' },
      ...(filterOptionsQuery.data?.services ?? []).map((service) => ({
        label: buildFilterLabel(service.name, service.detail),
        value: service.id,
      })),
    ],
    [filterOptionsQuery.data?.services],
  )

  const professionalOptions = useMemo(
    () => [
      { label: 'Todos los profesionales', value: '' },
      ...(filterOptionsQuery.data?.professionals ?? []).map((professional) => ({
        label: buildFilterLabel(professional.name, professional.detail),
        value: professional.id,
      })),
    ],
    [filterOptionsQuery.data?.professionals],
  )

  const roomOptions = useMemo(
    () => [
      { label: 'Todas las cabinas', value: '' },
      ...(filterOptionsQuery.data?.rooms ?? []).map((room) => ({
        label: buildFilterLabel(room.name, room.detail),
        value: room.id,
      })),
    ],
    [filterOptionsQuery.data?.rooms],
  )

  useEffect(() => {
    const id = setTimeout(() => {
      if (serviceId && !serviceOptions.some((option) => option.value === serviceId)) {
        setServiceId('')
      }
      if (
        professionalId &&
        !professionalOptions.some((option) => option.value === professionalId)
      ) {
        setProfessionalId('')
      }
      if (roomId && !roomOptions.some((option) => option.value === roomId)) {
        setRoomId('')
      }
    }, 0)
    return () => clearTimeout(id)
  }, [professionalId, professionalOptions, roomId, roomOptions, serviceId, serviceOptions])

  const calendarQuery = useQuery({
    queryKey: [
      'agenda-calendar-week',
      weekStart.format('YYYY-MM-DD'),
      locationId,
      professionalId,
      roomId,
      serviceId,
      statusFilter,
    ],
    queryFn: () =>
      getAgendaCalendarRequest({
        from: `${weekStart.format('YYYY-MM-DD')}T00:00:00-04:00`,
        to: `${weekEnd.format('YYYY-MM-DD')}T00:00:00-04:00`,
        locationId: locationId || undefined,
        professionalId: professionalId || undefined,
        roomId: roomId || undefined,
        serviceId: serviceId || undefined,
        status: statusFilter || undefined,
      }),
  })

  const calendarItems = useMemo(
    () =>
      [...(calendarQuery.data?.items ?? [])].sort(
        (first, second) => dayjs(first.startsAt).valueOf() - dayjs(second.startsAt).valueOf(),
      ),
    [calendarQuery.data],
  )

  const selectedItem = useMemo(
    () =>
      calendarItems.find((item) => item.bookingId === selectedBookingId) ??
      calendarItems[0] ??
      null,
    [calendarItems, selectedBookingId],
  )

  const bookingDetailQuery = useQuery({
    enabled: Boolean(selectedItem?.bookingId),
    queryKey: ['booking-detail-from-agenda', selectedItem?.bookingId],
    queryFn: () => getBookingDetailRequest(selectedItem?.bookingId ?? ''),
  })

  const bookingDetail = bookingDetailQuery.data

  useEffect(() => {
    const id = setTimeout(() => {
      if (calendarItems.length === 0) {
        setSelectedBookingId(null)
        return
      }

      if (
        !selectedBookingId ||
        !calendarItems.some((item) => item.bookingId === selectedBookingId)
      ) {
        setSelectedBookingId(calendarItems[0].bookingId)
      }
    }, 0)
    return () => clearTimeout(id)
  }, [calendarItems, selectedBookingId])

  const confirmWhatsAppMutation = useMutation({
    mutationFn: (bookingId: string) =>
      createBookingConfirmationLinkRequest(bookingId, {
        expirationMinutes: 720,
        sendWhatsApp: true,
      }),
  })

  const handleConfirmWhatsApp = (bookingId: string) => {
    const phone = selectedItem?.customerPhone
    if (!phone) {
      showToast({
        title: 'No se pudo abrir WhatsApp',
        description: 'El cliente no tiene un numero de WhatsApp valido.',
        tone: 'error',
      })
      return
    }

    let url: string | null = null
    try {
      const normalizedPhone = normalizeWhatsAppPhone(phone)
      const message = selectedItem ? buildAppointmentWhatsAppMessage(selectedItem) : undefined
      url = buildWhatsAppUrl(normalizedPhone, message)
    } catch {
      /* URL building failed */
    }

    if (url) {
      const newWindow = window.open(url, '_blank', 'noopener,noreferrer')
      if (newWindow) {
        newWindow.opener = null
      }
    }

    confirmWhatsAppMutation.mutate(bookingId, {
      onSuccess: () => {
        showToast({
          title: 'Confirmacion enviada',
          description:
            'Se genero y envio el enlace de confirmacion por WhatsApp cuando el canal esta disponible.',
          tone: 'success',
        })
        void bookingDetailQuery.refetch()
        void calendarQuery.refetch()
      },
      onError: (error) => {
        const apiError = error as ApiClientError
        const code = apiError?.code
        let description = apiError?.message ?? 'Revisa la conexion del canal o intenta nuevamente.'

        if (apiError?.status === 409) {
          if (code === 'BOOKING_NOT_CONFIRMABLE') {
            description =
              'La cita no puede recibir confirmacion en su estado actual. Verifica que este pendiente o solicitada.'
          } else if (code === 'BOOKING_SLOT_NOT_AVAILABLE') {
            description =
              'El horario ya esta ocupado. Actualiza la agenda para ver la disponibilidad actual.'
          } else if (code === 'BOOKING_PAYMENT_REQUIRED') {
            description = 'La cita requiere un pago antes de poder confirmarse.'
          } else {
            description =
              apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0
                ? Object.values(apiError.fieldErrors)[0]
                : description
          }
        } else {
          description =
            apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0
              ? Object.values(apiError.fieldErrors)[0]
              : description
        }

        showToast({
          title: 'No se pudo confirmar por WhatsApp',
          description,
          tone: 'error',
        })
        void bookingDetailQuery.refetch()
      },
    })
  }

  const cancelMutation = useMutation({
    mutationFn: (bookingId: string) =>
      cancelAgendaBookingRequest(bookingId, { reason: cancelReason.trim() }),
    onSuccess: () => {
      setCancelReason('')
      showToast({
        title: 'Reserva cancelada',
        description: 'La agenda se actualizo y se registro la trazabilidad de administracion.',
        tone: 'success',
      })
      void bookingDetailQuery.refetch()
      void calendarQuery.refetch()
    },
    onError: (error) => {
      const apiError = error as ApiClientError
      const description =
        apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0
          ? Object.values(apiError.fieldErrors)[0]
          : (apiError?.message ?? 'El motivo es obligatorio o la reserva ya no permite cambios.')
      showToast({
        title: 'No se pudo cancelar la reserva',
        description,
        tone: 'error',
      })
    },
  })

  const lifecycleMutation = useMutation({
    mutationFn: ({ action, bookingId }: { action: string; bookingId: string }) => {
      if (action === 'CONFIRM') {
        return confirmAgendaBookingRequest(bookingId)
      }
      if (action === 'START') {
        return startAgendaBookingServiceRequest(bookingId)
      }
      if (action === 'COMPLETE') {
        return completeAgendaBookingRequest(bookingId)
      }
      return markAgendaBookingNoShowRequest(bookingId, { reason: 'Inasistencia registrada desde agenda' })
    },
    onSuccess: (booking) => {
      showToast({
        title: 'Cita actualizada',
        description: `Estado actual: ${getBookingStatusLabel(booking.status)}.`,
        tone: 'success',
      })
      void bookingDetailQuery.refetch()
      void calendarQuery.refetch()
    },
    onError: (error) => {
      const apiError = error as ApiClientError
      showToast({
        title: 'No se pudo actualizar la cita',
        description:
          apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0
            ? Object.values(apiError.fieldErrors)[0]
            : (apiError?.message ?? 'La cita no permite esa acción en su estado actual.'),
        tone: 'error',
      })
      void bookingDetailQuery.refetch()
    },
  })

  const activityItems = buildRecentActivity(bookingDetail)

  function handleSelectBooking(bookingId: string) {
    setSelectedBookingId(bookingId)
  }

  function handleClearFilters() {
    setLocationId('')
    setServiceId('')
    setProfessionalId('')
    setRoomId('')
    setStatusFilter('')
    setDate(today)
  }

  return (
    <div className="space-y-6">
      <PageHeader
        actions={
          <div className="flex items-center gap-2">
            <Button onClick={() => navigate('/appointments')} variant="secondary">
              Vista cl&aacute;sica
            </Button>
            <Button onClick={() => navigate('/appointments/new')}>Nueva cita</Button>
          </div>
        }
        description="Gestiona las reservas de tu centro est&eacute;tico: visualiza la semana, confirma, reprograma o cancela citas desde un solo lugar."
        eyebrow="Agenda digital completa"
        title="Reservas y citas WhatsApp"
      />

      <Card className="space-y-4">
        <div className="flex flex-wrap items-end gap-2.5">
          <Select
            id="select-location"
            label="Sucursal"
            onChange={(event) => setLocationId(event.target.value)}
            options={[
              { label: 'Todas las sucursales', value: '' },
              ...(locationsQuery.data ?? []).map((location) => ({
                label: location.commune ? `${location.name} - ${location.commune}` : location.name,
                value: location.id,
              })),
            ]}
            value={locationId}
            className="min-w-[180px]"
          />
          <Select
            id="select-service"
            disabled={filterOptionsQuery.isLoading}
            label="Servicio"
            onChange={(event) => setServiceId(event.target.value)}
            options={serviceOptions}
            value={serviceId}
            className="min-w-[160px]"
          />
          <Select
            id="select-professional"
            disabled={filterOptionsQuery.isLoading}
            label="Profesional"
            onChange={(event) => setProfessionalId(event.target.value)}
            options={professionalOptions}
            value={professionalId}
            className="min-w-[160px]"
          />
          <Select
            id="select-room"
            disabled={filterOptionsQuery.isLoading}
            label="Cabina"
            onChange={(event) => setRoomId(event.target.value)}
            options={roomOptions}
            value={roomId}
            className="min-w-[140px]"
          />
          <Select
            id="select-status"
            label="Estado"
            onChange={(event) => setStatusFilter(event.target.value)}
            options={statusOptions}
            value={statusFilter}
            className="min-w-[160px]"
          />
          <Input
            label="Semana"
            onChange={(event) => setDate(event.target.value)}
            type="date"
            value={date}
            className="min-w-[150px]"
          />
          <div className="flex gap-1.5 pb-px">
            <Button onClick={() => calendarQuery.refetch()} variant="secondary">
              Actualizar
            </Button>
            <Button onClick={handleClearFilters} variant="secondary">
              Limpiar
            </Button>
          </div>
        </div>
      </Card>

      <div className="flex gap-6">
        <div className="min-w-0 flex-1">
          <CalendarWeekNavigation
            currentDate={date}
            itemsCount={calendarItems.length}
            today={today}
            onDateChange={setDate}
          />
          <WeeklyCalendarView
            businessHours={businessHoursQuery.data ?? []}
            calendarItems={calendarItems}
            selectedBookingId={selectedBookingId}
            visibleDays={visibleDays}
            onSelectBooking={handleSelectBooking}
          />
        </div>

        <div className="hidden w-[360px] shrink-0 xl:block" ref={detailPanelRef}>
          <div className="space-y-5">
            <Card className="h-fit space-y-5 lg:sticky lg:top-6">
              <h3 className="text-xs font-bold uppercase tracking-[0.12em] text-slate-500">
                Detalles de la cita
              </h3>
              <AppointmentDetailPanel
                bookingDetail={bookingDetail}
                bookingDetailLoading={bookingDetailQuery.isLoading}
                cancelPending={cancelMutation.isPending}
                cancelReason={cancelReason}
                confirmWhatsAppPending={confirmWhatsAppMutation.isPending}
                lifecyclePending={lifecycleMutation.isPending}
                selectedItem={selectedItem}
                onCancel={(bookingId) => cancelMutation.mutate(bookingId)}
                onCancelReasonChange={setCancelReason}
                onCompleteBooking={(bookingId) =>
                  lifecycleMutation.mutate({ action: 'COMPLETE', bookingId })
                }
                onConfirmBooking={(bookingId) =>
                  lifecycleMutation.mutate({ action: 'CONFIRM', bookingId })
                }
                onConfirmWhatsApp={handleConfirmWhatsApp}
                onEditNotes={(bookingId) => navigate(`/appointments/${bookingId}/edit`)}
                onMarkNoShow={(bookingId) =>
                  lifecycleMutation.mutate({ action: 'NO_SHOW', bookingId })
                }
                onReschedule={(bookingId) => navigate(`/appointments/${bookingId}/reschedule`)}
                onStartService={(bookingId) =>
                  lifecycleMutation.mutate({ action: 'START', bookingId })
                }
                onViewHistory={(bookingId) => navigate(`/appointments/${bookingId}`)}
              />
            </Card>
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <SectionCard
          description="Información útil para decidir acciones durante la operación diaria."
          title="Resumen operativo"
        >
          <div className="grid gap-2 sm:grid-cols-2">
            {getOperationalRows(selectedItem, bookingDetail).map((item) => (
              <div
                className="rounded-lg border border-slate-100 bg-slate-50/50 p-3 transition hover:border-slate-200"
                key={item.name}
              >
                <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-blue-600">
                  {item.name}
                </p>
                <p className="mt-1 text-xs text-slate-600">{item.detail}</p>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard
          description="Eventos de estado, recordatorios y acciones operativas."
          title="Trazabilidad reciente"
        >
          {bookingDetailQuery.isFetching ? <StatusBadge label="Actualizando" tone="info" /> : null}
          <div className="space-y-2">
            {activityItems.length === 0 ? (
              <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50/50 p-3 text-xs text-slate-500">
                No hay eventos recientes para la reserva seleccionada.
              </div>
            ) : (
              activityItems.map((activity) => (
                <div
                  className="flex items-center justify-between gap-3 rounded-lg border border-slate-100 bg-white p-3 transition hover:border-slate-200"
                  key={activity.id}
                >
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-slate-800">{activity.title}</p>
                    <p className="text-xs text-slate-500 truncate">{activity.detail}</p>
                  </div>
                  <p className="shrink-0 text-xs font-semibold text-slate-400">
                    {dayjs(activity.at).format('DD/MM HH:mm')}
                  </p>
                </div>
              ))
            )}
            {selectedItem ? (
              <button
                className="text-xs font-semibold text-blue-600 hover:text-blue-700 transition-colors"
                onClick={() => navigate(`/appointments/${selectedItem.bookingId}`)}
                type="button"
              >
                Ver historial completo &rarr;
              </button>
            ) : null}
          </div>
        </SectionCard>
      </div>
    </div>
  )
}
