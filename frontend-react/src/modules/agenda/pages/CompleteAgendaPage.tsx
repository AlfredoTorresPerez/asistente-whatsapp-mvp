import dayjs from 'dayjs'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
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
} from '../../../services/api/completeAgendaApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { createBookingConfirmationLinkRequest, getBookingDetailRequest } from '../../../services/api/bookingsApi'
import type { AgendaCalendarItemResponse, BookingDetailResponse } from '../../../services/api/types'
import { CalendarWeekNavigation } from '../components/CalendarWeekNavigation'
import { WeeklyCalendarView } from '../components/WeeklyCalendarView'
import {
  agendaTimeZone,
  buildVisibleDays,
  calendarDays,
  formatAgendaTime,
  formatLongDate,
  formatTimeRange,
  getAgendaDateKey,
  getStatusLabel,
  getStatusStyle,
  getWeekStart,
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

function getInitials(name: string) {
  const parts = name.trim().split(/\s+/).slice(0, 2)
  if (parts.length === 0 || !parts[0]) {
    return 'CL'
  }
  return parts.map((part) => part[0]?.toUpperCase()).join('')
}

function getWhatsAppStatus(item: AgendaCalendarItemResponse | null, detail?: BookingDetailResponse) {
  const lastWhatsAppReminder = (detail?.reminders ?? [])
    .filter((reminder) => reminder.channelType === 'WHATSAPP')
    .sort((first, second) => dayjs(second.sentAt ?? second.scheduledAt).valueOf() - dayjs(first.sentAt ?? first.scheduledAt).valueOf())[0]

  if (lastWhatsAppReminder?.sentAt) {
    return `WhatsApp enviado ${dayjs(lastWhatsAppReminder.sentAt).format('DD/MM HH:mm')}`
  }
  if (item?.sourceChannel?.toUpperCase().includes('WHATSAPP')) {
    return 'Canal WhatsApp asociado'
  }
  return 'Sin confirmacion WhatsApp registrada'
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
      detail: item ? `${formatTimeRange(item)} sin solapamiento visual` : 'Selecciona una cita',
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
      detail: getWhatsAppStatus(item, detail),
    },
    {
      name: 'Administracion',
      detail: 'Confirmar, reprogramar, cancelar o editar notas',
    },
  ]
}

function DetailRow({ icon, label, value }: { icon: string; label: string; value: string }) {
  return (
    <div className="grid grid-cols-[72px_1fr] gap-3">
      <span className="text-xs font-bold uppercase tracking-[0.16em] text-slate-400">{icon}</span>
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{label}</p>
        <p className="mt-1 font-medium text-slate-800">{value}</p>
      </div>
    </div>
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
  const [showDetailPanel, setShowDetailPanel] = useState(false)

  const visibleDays = useMemo(() => buildVisibleDays(date), [date])
  const weekStart = visibleDays[0]
  const weekEnd = weekStart.add(calendarDays, 'day')

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'agenda-completa'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
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
    if (serviceId && !serviceOptions.some((option) => option.value === serviceId)) {
      setServiceId('')
    }
    if (professionalId && !professionalOptions.some((option) => option.value === professionalId)) {
      setProfessionalId('')
    }
    if (roomId && !roomOptions.some((option) => option.value === roomId)) {
      setRoomId('')
    }
  }, [professionalId, professionalOptions, roomId, roomOptions, serviceId, serviceOptions])

  const calendarQuery = useQuery({
    queryKey: ['agenda-calendar-week', weekStart.format('YYYY-MM-DD'), locationId, professionalId, roomId, serviceId, statusFilter],
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
    () => calendarItems.find((item) => item.bookingId === selectedBookingId) ?? calendarItems[0] ?? null,
    [calendarItems, selectedBookingId],
  )

  const bookingDetailQuery = useQuery({
    enabled: Boolean(selectedItem?.bookingId),
    queryKey: ['booking-detail-from-agenda', selectedItem?.bookingId],
    queryFn: () => getBookingDetailRequest(selectedItem?.bookingId ?? ''),
  })

  const bookingDetail = bookingDetailQuery.data

  useEffect(() => {
    if (calendarItems.length === 0) {
      setSelectedBookingId(null)
      return
    }

    if (!selectedBookingId || !calendarItems.some((item) => item.bookingId === selectedBookingId)) {
      setSelectedBookingId(calendarItems[0].bookingId)
    }
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
      console.log('[diagnostico] createTemporaryAgendaBookingRequest payload:', payload)
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
      console.log('[diagnostico] createMutation error:', { code: apiError.code, status: apiError.status, message: apiError.message, fieldErrors: apiError.fieldErrors })
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
          description: apiError?.message ?? 'El horario pudo quedar ocupado o faltan datos obligatorios.',
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
    onSuccess: () => {
      showToast({
        title: 'Confirmacion enviada',
        description: 'Se genero y envio el enlace de confirmacion por WhatsApp cuando el canal esta disponible.',
        tone: 'success',
      })
      void bookingDetailQuery.refetch()
      void calendarQuery.refetch()
    },
    onError: () => {
      showToast({
        title: 'No se pudo confirmar por WhatsApp',
        description: 'Revisa la conexion del canal o intenta nuevamente.',
        tone: 'error',
      })
    },
  })

  const cancelMutation = useMutation({
    mutationFn: (bookingId: string) => cancelAgendaBookingRequest(bookingId, { reason: cancelReason.trim() }),
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
      const description = apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length > 0
        ? Object.values(apiError.fieldErrors)[0]
        : apiError?.message ?? 'El motivo es obligatorio o la reserva ya no permite cambios.'
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
  const selectedStatusStyle = getStatusStyle(selectedItem?.status ?? bookingDetail?.status ?? '')

  function handleSelectBooking(bookingId: string) {
    setSelectedBookingId(bookingId)
    setShowDetailPanel(true)
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <div className="flex flex-wrap gap-2">
            <Button onClick={() => navigate('/appointments')} variant="secondary">
              Ver citas clasicas
            </Button>
            <Button onClick={() => navigate('/appointments/new')}>Nueva cita</Button>
          </div>
        }
        description="Vista semanal con reservas confirmadas, detalle contextual del cliente, trazabilidad y acciones de administracion."
        eyebrow="Agenda digital completa"
        title="Reservas y citas WhatsApp"
      />

      <Card className="space-y-5">
        <div className="flex flex-wrap items-end gap-3">
          <Select
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
          />
          <Select
            disabled={filterOptionsQuery.isLoading}
            label="Servicio"
            onChange={(event) => setServiceId(event.target.value)}
            options={serviceOptions}
            value={serviceId}
          />
          <Select
            disabled={filterOptionsQuery.isLoading}
            label="Profesional"
            onChange={(event) => setProfessionalId(event.target.value)}
            options={professionalOptions}
            value={professionalId}
          />
          <Select
            disabled={filterOptionsQuery.isLoading}
            label="Cabina"
            onChange={(event) => setRoomId(event.target.value)}
            options={roomOptions}
            value={roomId}
          />
          <Select
            label="Estado"
            onChange={(event) => setStatusFilter(event.target.value)}
            options={statusOptions}
            value={statusFilter}
          />
          <Input label="Semana" onChange={(event) => setDate(event.target.value)} type="date" value={date} />
          <Button onClick={() => calendarQuery.refetch()} variant="secondary">
            Actualizar
          </Button>
        </div>
      </Card>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="min-w-0">
          <CalendarWeekNavigation
            currentDate={date}
            itemsCount={calendarItems.length}
            today={today}
            onDateChange={setDate}
          />
          <WeeklyCalendarView
            calendarItems={calendarItems}
            selectedBookingId={selectedBookingId}
            visibleDays={visibleDays}
            onSelectBooking={handleSelectBooking}
          />
        </div>

        {/* Detail Panel */}
        <div className={showDetailPanel ? 'block' : 'hidden lg:block'}>
          <Card className="h-fit space-y-5 lg:sticky lg:top-6">
            {selectedItem ? (
              <>
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-br from-slate-100 to-slate-200 text-base font-bold text-slate-700">
                      {getInitials(selectedItem.customerName)}
                    </div>
                    <div>
                      <h2 className="text-lg font-bold text-slate-900">{selectedItem.customerName}</h2>
                      <p className="text-sm text-slate-500">{bookingDetail?.customerPhone ?? selectedItem.customerPhone}</p>
                    </div>
                  </div>
                  <StatusBadge label={getStatusLabel(selectedItem.status)} tone={
                    selectedStatusStyle.hex === '#10b981' ? 'success' :
                    selectedStatusStyle.hex === '#f59e0b' ? 'warning' :
                    selectedStatusStyle.hex === '#f87171' ? 'danger' : 'info'
                  } />
                </div>

                <div className="grid gap-3 rounded-3xl border border-slate-100 bg-slate-50 p-4 text-sm">
                  <DetailRow icon="Servicio" label="Servicio" value={selectedItem.serviceName ?? selectedItem.subject} />
                  <DetailRow icon="Fecha" label="Fecha y hora" value={`${formatLongDate(selectedItem.startsAt)} · ${formatTimeRange(selectedItem)}`} />
                  <DetailRow icon="Equipo" label="Profesional" value={selectedItem.professionalName ?? 'Profesional por asignar'} />
                  <DetailRow icon="Lugar" label="Ubicacion" value={selectedItem.roomName ?? selectedItem.locationName ?? 'Sin ubicacion'} />
                  <DetailRow icon="Tiempo" label="Duracion" value={`${selectedItem.durationMinutes} minutos`} />
                  <DetailRow icon="WA" label="Estado WhatsApp" value={getWhatsAppStatus(selectedItem, bookingDetail)} />
                </div>

                <div className="space-y-2">
                  <h3 className="text-sm font-semibold text-slate-800">Notas internas</h3>
                  <div className="rounded-2xl border border-slate-100 bg-white p-4 text-sm text-slate-600">
                    {bookingDetailQuery.isLoading ? 'Cargando detalle...' : bookingDetail?.notes ?? 'Sin notas registradas.'}
                  </div>
                </div>

                <div className="space-y-3">
                  <Button
                    className="bg-emerald-600 hover:bg-emerald-700"
                    fullWidth
                    loading={confirmWhatsAppMutation.isPending}
                    onClick={() => confirmWhatsAppMutation.mutate(selectedItem.bookingId)}
                  >
                    Confirmar por WhatsApp
                  </Button>
                  <Button fullWidth onClick={() => navigate(`/appointments/${selectedItem.bookingId}/reschedule`)} variant="secondary">
                    Reprogramar
                  </Button>
                  <Button fullWidth onClick={() => navigate(`/appointments/${selectedItem.bookingId}/edit`)} variant="secondary">
                    Editar notas
                  </Button>
                  <Button fullWidth onClick={() => navigate(`/appointments/${selectedItem.bookingId}`)} variant="secondary">
                    Ver historial del cliente
                  </Button>
                </div>

                <div className="space-y-2 rounded-3xl border border-red-100 bg-red-50 p-4">
                  <Textarea
                    label="Motivo de cancelacion"
                    onChange={(event) => setCancelReason(event.target.value)}
                    placeholder="Indica el motivo obligatorio antes de cancelar"
                    rows={3}
                    value={cancelReason}
                  />
                  <Button
                    disabled={cancelReason.trim().length < 5}
                    fullWidth
                    loading={cancelMutation.isPending}
                    onClick={() => cancelMutation.mutate(selectedItem.bookingId)}
                    variant="danger"
                  >
                    Cancelar cita
                  </Button>
                </div>
              </>
            ) : (
              <div className="rounded-3xl border border-dashed border-slate-200 bg-slate-50 p-6 text-center">
                <h2 className="text-base font-semibold text-slate-800">Selecciona una reserva</h2>
                <p className="mt-2 text-sm text-slate-500">
                  Al posicionarte sobre una cita se mostrara el cliente, el servicio, la trazabilidad y las acciones disponibles.
                </p>
              </div>
            )}
          </Card>
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1fr_1fr]">
        <Card className="space-y-4">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Agentes involucrados</h2>
            <p className="mt-1 text-sm text-slate-500">
              Cada cita queda conectada con agenda, cliente, profesional, servicio, notificaciones y administracion.
            </p>
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            {getAgentRows(selectedItem, bookingDetail).map((agent) => (
              <div className="rounded-2xl border border-slate-100 bg-slate-50 p-4" key={agent.name}>
                <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">Agente {agent.name}</p>
                <p className="mt-2 text-sm text-slate-700">{agent.detail}</p>
              </div>
            ))}
          </div>
        </Card>

        <Card className="space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">Trazabilidad reciente</h2>
              <p className="mt-1 text-sm text-slate-500">Eventos de estado, recordatorios y acciones operativas.</p>
            </div>
            {bookingDetailQuery.isFetching ? <StatusBadge label="Actualizando" tone="info" /> : null}
          </div>
          <div className="space-y-3">
            {activityItems.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-4 text-sm text-slate-500">
                No hay eventos recientes para la reserva seleccionada.
              </div>
            ) : (
              activityItems.map((activity) => (
                <div className="flex items-center justify-between gap-3 rounded-2xl border border-slate-100 bg-white p-4" key={activity.id}>
                  <div>
                    <p className="text-sm font-semibold text-slate-800">{activity.title}</p>
                    <p className="text-xs text-slate-500">{activity.detail}</p>
                  </div>
                  <p className="text-xs font-semibold text-slate-400">{dayjs(activity.at).format('DD/MM HH:mm')}</p>
                </div>
              ))
            )}
          </div>
        </Card>
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <Card className="space-y-5">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Consultar disponibilidad real</h2>
            <p className="mt-1 text-sm text-slate-500">
              Usa este bloque cuando el orquestador detecta intencion de agendar desde WhatsApp.
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <Select
              label="Sucursal"
              onChange={(event) => setLocationId(event.target.value)}
              options={[
                { label: 'Selecciona sucursal', value: '' },
                ...(locationsQuery.data ?? []).map((location) => ({
                  label: location.commune ? `${location.name} - ${location.commune}` : location.name,
                  value: location.id,
                })),
              ]}
              value={locationId}
            />
            <Select
              disabled={filterOptionsQuery.isLoading}
              label="Servicio"
              onChange={(event) => setServiceId(event.target.value)}
              options={[{ label: 'Selecciona servicio', value: '' }, ...serviceOptions.filter((option) => option.value)]}
              value={serviceId}
            />
            <Input label="Fecha" onChange={(event) => setDate(event.target.value)} type="date" value={date} />
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

          <div className="rounded-3xl border border-slate-100 bg-slate-50 p-4">
            <h3 className="text-sm font-semibold text-slate-800">Horarios sugeridos por la agenda</h3>
            <div className="mt-3 grid gap-3 md:grid-cols-2">
              {slots.length === 0 ? (
                <p className="text-sm text-slate-500">Consulta disponibilidad para ver horarios validos.</p>
              ) : (
                slots.map((slot) => (
                  <button
                    className="rounded-2xl border border-slate-200 bg-white p-4 text-left shadow-sm transition hover:border-blue-300 hover:shadow-md"
                    disabled={!canCreate || createMutation.isPending}
                    key={`${slot.startsAt}-${slot.professionalId}-${slot.roomId}`}
                    onClick={() => createMutation.mutate(slot.startsAt)}
                    type="button"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <strong className="text-slate-900">{formatAgendaTime(slot.startsAt)}</strong>
                      <StatusBadge label={slot.available ? 'Disponible' : 'Bloqueado'} tone={slot.available ? 'success' : 'danger'} />
                    </div>
                    <p className="mt-2 text-sm text-slate-600">{slot.professionalName ?? 'Profesional por asignar'}</p>
                    <p className="text-sm text-slate-500">{slot.roomName ?? 'Sin cabina requerida'}</p>
                    <p className="mt-2 text-xs text-slate-400">Duracion: {slot.durationMinutes} minutos</p>
                  </button>
                ))
              )}
            </div>
          </div>
        </Card>

        <Card className="space-y-5">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Datos del cliente WhatsApp</h2>
            <p className="mt-1 text-sm text-slate-500">
              Al elegir un horario se crea reserva temporal, enlace de confirmacion y bloqueo de cupo.
            </p>
          </div>
          <Input label="Cliente" onChange={(event) => setCustomerName(event.target.value)} value={customerName} />
          <Input label="Telefono WhatsApp" onChange={(event) => setCustomerPhone(event.target.value)} value={customerPhone} />
          <Input label="Correo opcional" onChange={(event) => setCustomerEmail(event.target.value)} type="email" value={customerEmail} />
          <Textarea label="Notas de agenda" onChange={(event) => setNotes(event.target.value)} rows={5} value={notes} />
          <div className="rounded-3xl bg-blue-50 p-4 text-sm text-blue-900">
            Flujo coordinado: WhatsApp registra mensaje, orquestador detecta intencion, agenda calcula disponibilidad,
            recursos validan profesional y cabina, enlace confirma reserva y auditoria registra el evento.
          </div>
        </Card>
      </div>
    </section>
  )
}
