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
import { Textarea } from '../../../components/ui/Textarea'
import { useToast } from '../../../lib/toast'
import { ApiClientError } from '../../../services/api/httpClient'
import {
  cancelAgendaBookingRequest,
  createTemporaryAgendaBookingRequest,
  getAgendaAvailabilityRequest,
  getAgendaCalendarRequest,
  getAgendaFilterOptionsRequest,
  getBusinessHoursRequest,
} from '../../../services/api/completeAgendaApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import {
  createBookingConfirmationLinkRequest,
  getBookingDetailRequest,
} from '../../../services/api/bookingsApi'
import type { AgendaCalendarItemResponse, BookingDetailResponse } from '../../../services/api/types'
import { AppointmentDetailPanel } from '../components/AppointmentDetailPanel'
import { CalendarWeekNavigation } from '../components/CalendarWeekNavigation'
import { WeeklyCalendarView } from '../components/WeeklyCalendarView'
import {
  buildAppointmentWhatsAppMessage,
  buildWhatsAppUrl,
  normalizeWhatsAppPhone,
  openWhatsAppUrl,
} from '../../../lib/whatsapp'
import {
  buildVisibleDays,
  calendarDays,
  formatAgendaTime,
  getStatusLabel,
} from '../components/agendaUtils'

const today = dayjs().format('YYYY-MM-DD')

const statusOptions = [
  { label: 'Reservas activas', value: '' },
  { label: 'Confirmadas', value: 'CONFIRMED' },
  { label: 'Pendientes de confirmacion', value: 'PENDIENTE_CONFIRMACION' },
  { label: 'Canceladas', value: 'CANCELLED' },
  { label: 'Reprogramadas', value: 'RESCHEDULED' },
  { label: 'Atendidas', value: 'COMPLETED' },
  { label: 'Solicitadas', value: 'REQUESTED' },
  { label: 'Pendientes pago', value: 'PENDING_PAYMENT' },
  { label: 'No asistio', value: 'NO_SHOW' },
  { label: 'Vencidas', value: 'EXPIRED' },
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
    title: `${reminder.reminderType} por ${reminder.channelType}`,
    detail: reminder.status,
    at: reminder.sentAt ?? reminder.scheduledAt,
  }))

  return [...statusEvents, ...reminderEvents]
    .sort((first, second) => dayjs(second.at).valueOf() - dayjs(first.at).valueOf())
    .slice(0, 4)
}

function getAgentRows(item: AgendaCalendarItemResponse | null, detail?: BookingDetailResponse) {
  return [
    {
      name: 'Agenda',
      detail: item ? `${item.startTimeLocal ?? ''} sin solapamiento visual` : 'Selecciona una cita',
    },
    {
      name: 'Cliente',
      detail: detail?.customerPhone ?? item?.customerPhone ?? 'Telefono no disponible',
    },
    {
      name: 'Profesional',
      detail: item?.professionalName ?? detail?.assignedUserName ?? 'Profesional por asignar',
    },
    {
      name: 'Servicio',
      detail: item?.serviceName ?? item?.subject ?? detail?.subject ?? 'Servicio no definido',
    },
    {
      name: 'Notificaciones',
      detail: item?.sourceChannel?.toUpperCase().includes('WHATSAPP')
        ? 'Canal WhatsApp asociado'
        : 'Sin registro',
    },
    {
      name: 'Administracion',
      detail: 'Confirmar, reprogramar, cancelar o editar notas',
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
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [customerEmail, setCustomerEmail] = useState('')
  const [notes, setNotes] = useState('')
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

  const availabilityMutation = useMutation({
    mutationFn: () =>
      getAgendaAvailabilityRequest({
        locationId,
        serviceId,
        professionalId: professionalId || undefined,
        roomId: roomId || undefined,
        date,
        maxSlots: 20,
      }),
    onError: () => {
      showToast({
        title: 'No se pudo consultar disponibilidad',
        description: 'Verifica sucursal, servicio y fecha.',
        tone: 'error',
      })
    },
  })

  const createMutation = useMutation({
    mutationFn: (startsAt: string) => {
      const payload = {
        locationId,
        serviceId,
        professionalId: professionalId || undefined,
        roomId: roomId || undefined,
        startsAt,
        customerName,
        customerPhone,
        customerEmail: customerEmail || undefined,
        notes: notes || undefined,
        expirationMinutes: 30,
        generateConfirmationLink: true,
        sendWhatsApp: true,
      }
      return createTemporaryAgendaBookingRequest(payload)
    },
    onSuccess: (booking) => {
      showToast({
        title: 'Reserva temporal creada',
        description: 'Se genero el enlace de confirmacion y quedo lista para WhatsApp.',
        tone: 'success',
      })
      navigate(`/appointments/${booking.id}`)
    },
    onError: (error) => {
      const apiError = error as ApiClientError
      if (apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0) {
        const firstFieldError = Object.values(apiError.fieldErrors)[0]
        showToast({
          title: 'No se pudo crear la reserva',
          description: firstFieldError,
          tone: 'error',
        })
      } else {
        showToast({
          title: 'No se pudo crear la reserva',
          description:
            apiError?.message ?? 'El horario pudo quedar ocupado o faltan datos obligatorios.',
          tone: 'error',
        })
      }
    },
  })

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

    let popup: Window | null = null
    try {
      popup = window.open('about:blank', '_blank', 'noopener,noreferrer')
    } catch {
      /* popup blocked */
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

        try {
          const normalizedPhone = normalizeWhatsAppPhone(phone)
          const message = selectedItem
            ? buildAppointmentWhatsAppMessage(selectedItem)
            : undefined
          const url = buildWhatsAppUrl(normalizedPhone, message)

          if (popup && !popup.closed) {
            popup.location.href = url
          } else {
            openWhatsAppUrl(url)
          }
        } catch {
          /* URL building failed, confirmation was already sent */
        }
      },
      onError: (error) => {
        if (popup && !popup.closed) {
          popup.close()
        }
        const apiError = error as ApiClientError
        const code = apiError?.code
        let description = apiError?.message ?? 'Revisa la conexion del canal o intenta nuevamente.'

        if (apiError?.status === 409) {
          if (code === 'BOOKING_NOT_CONFIRMABLE') {
            description = 'La cita no puede recibir confirmacion en su estado actual. Verifica que este pendiente o solicitada.'
          } else if (code === 'BOOKING_SLOT_NOT_AVAILABLE') {
            description = 'El horario ya esta ocupado. Actualiza la agenda para ver la disponibilidad actual.'
          } else if (code === 'BOOKING_PAYMENT_REQUIRED') {
            description = 'La cita requiere un pago antes de poder confirmarse.'
          } else {
            description = apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0
              ? Object.values(apiError.fieldErrors)[0]
              : description
          }
        } else {
          description = apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0
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

  const canSearch = locationId && serviceId && date
  const canCreate = customerName.trim().length > 0 && customerPhone.trim().length >= 8
  const slots = availabilityMutation.data?.slots ?? []
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
                selectedItem={selectedItem}
                onCancel={(bookingId) => cancelMutation.mutate(bookingId)}
                onCancelReasonChange={setCancelReason}
                onConfirmWhatsApp={handleConfirmWhatsApp}
                onEditNotes={(bookingId) => navigate(`/appointments/${bookingId}/edit`)}
                onReschedule={(bookingId) => navigate(`/appointments/${bookingId}/reschedule`)}
                onViewHistory={(bookingId) => navigate(`/appointments/${bookingId}`)}
              />
            </Card>
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <SectionCard
          description="Cada cita queda conectada con agenda, cliente, profesional, servicio, notificaciones y administracion."
          title="Agentes involucrados"
        >
          <div className="grid gap-2 sm:grid-cols-2">
            {getAgentRows(selectedItem, bookingDetail).map((agent) => (
              <div
                className="rounded-lg border border-slate-100 bg-slate-50/50 p-3 transition hover:border-slate-200"
                key={agent.name}
              >
                <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-blue-600">
                  Agente {agent.name}
                </p>
                <p className="mt-1 text-xs text-slate-600">{agent.detail}</p>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard
          description="Eventos de estado, recordatorios y acciones operativas."
          title="Trazabilidad reciente"
        >
          {bookingDetailQuery.isFetching ? (
            <StatusBadge label="Actualizando" tone="info" />
          ) : null}
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

      <div className="grid gap-6 lg:grid-cols-2">
        <SectionCard
          description="Usa este bloque cuando el orquestador detecta intencion de agendar desde WhatsApp."
          title="Consultar disponibilidad real"
        >
          <div className="grid gap-3 sm:grid-cols-2">
            <Select
              label="Sucursal"
              onChange={(event) => setLocationId(event.target.value)}
              options={[
                { label: 'Selecciona sucursal', value: '' },
                ...(locationsQuery.data ?? []).map((location) => ({
                  label: location.commune
                    ? `${location.name} - ${location.commune}`
                    : location.name,
                  value: location.id,
                })),
              ]}
              value={locationId}
            />
            <Select
              disabled={filterOptionsQuery.isLoading}
              label="Servicio"
              onChange={(event) => setServiceId(event.target.value)}
              options={[
                { label: 'Selecciona servicio', value: '' },
                ...serviceOptions.filter((option) => option.value),
              ]}
              value={serviceId}
            />
            <Input
              label="Fecha"
              onChange={(event) => setDate(event.target.value)}
              type="date"
              value={date}
            />
            <Select
              label="Preferencia"
              onChange={() => undefined}
              options={[
                { label: 'Cualquier horario', value: '' },
                { label: 'Manana', value: 'MORNING' },
                { label: 'Tarde', value: 'AFTERNOON' },
              ]}
            />
            <Select
              disabled={filterOptionsQuery.isLoading}
              label="Profesional opcional"
              onChange={(event) => setProfessionalId(event.target.value)}
              options={professionalOptions}
              value={professionalId}
            />
            <Select
              disabled={filterOptionsQuery.isLoading}
              label="Cabina opcional"
              onChange={(event) => setRoomId(event.target.value)}
              options={roomOptions}
              value={roomId}
            />
          </div>

          <Button
            disabled={!canSearch}
            loading={availabilityMutation.isPending}
            onClick={() => availabilityMutation.mutate()}
            type="button"
          >
            Buscar horarios disponibles
          </Button>

          <div className="rounded-xl border border-slate-100 bg-slate-50/50 p-4">
            <h3 className="text-sm font-semibold text-slate-800">
              Horarios sugeridos por la agenda
            </h3>
            <div className="mt-3 grid gap-2 sm:grid-cols-2">
              {slots.length === 0 ? (
                <p className="text-sm text-slate-500">
                  Consulta disponibilidad para ver horarios validos.
                </p>
              ) : (
                slots.map((slot) => (
                  <button
                    className="rounded-lg border border-slate-200 bg-white p-3 text-left shadow-xs transition hover:border-blue-300 hover:shadow-sm"
                    disabled={!canCreate || createMutation.isPending}
                    key={`${slot.startsAt}-${slot.professionalId}-${slot.roomId}`}
                    onClick={() => createMutation.mutate(slot.startsAt)}
                    type="button"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <strong className="text-sm text-slate-900">{formatAgendaTime(slot.startsAt)}</strong>
                      <StatusBadge
                        label={slot.available ? 'Disponible' : 'Bloqueado'}
                        tone={slot.available ? 'success' : 'danger'}
                      />
                    </div>
                    <p className="mt-1 text-xs text-slate-600">
                      {slot.professionalName ?? 'Profesional por asignar'}
                    </p>
                    <p className="text-xs text-slate-500">
                      {slot.roomName ?? 'Sin cabina requerida'}
                    </p>
                    <p className="mt-1 text-[10px] text-slate-400">
                      Duracion: {slot.durationMinutes} minutos
                    </p>
                  </button>
                ))
              )}
            </div>
          </div>
        </SectionCard>

        <SectionCard
          description="Al elegir un horario se crea reserva temporal, enlace de confirmacion y bloqueo de cupo."
          title="Datos del cliente WhatsApp"
        >
          <Input
            label="Cliente"
            onChange={(event) => setCustomerName(event.target.value)}
            value={customerName}
          />
          <Input
            label="Telefono WhatsApp"
            onChange={(event) => setCustomerPhone(event.target.value)}
            value={customerPhone}
          />
          <Input
            label="Correo opcional"
            onChange={(event) => setCustomerEmail(event.target.value)}
            type="email"
            value={customerEmail}
          />
          <Textarea
            label="Notas de agenda"
            onChange={(event) => setNotes(event.target.value)}
            rows={4}
            value={notes}
          />
          <div className="rounded-xl bg-blue-50 p-3 text-xs text-blue-900 leading-relaxed">
            <strong className="font-semibold">Flujo coordinado:</strong> WhatsApp registra mensaje,
            orquestador detecta intencion, agenda calcula disponibilidad, recursos validan
            profesional y cabina, enlace confirma reserva y auditoria registra el evento.
          </div>
        </SectionCard>
      </div>
    </div>
  )
}
