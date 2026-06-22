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

const today = dayjs().format('YYYY-MM-DD')
const scheduleStartHour = 9
const scheduleEndHour = 21
const baseHourHeight = 96
const eventCardHeight = 56
const eventGap = 8
const rowVerticalPadding = 12
const calendarDays = 7

const weekDayLabels = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab']
const agendaTimeZone = 'America/Santiago'

function getAgendaDateKey(value: string) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: agendaTimeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

function getAgendaHourMinute(value: string) {
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

function formatAgendaTime(value: string) {
  return new Intl.DateTimeFormat('es-CL', {
    timeZone: agendaTimeZone,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

const statusOptions = [
  { label: 'Reservas activas', value: '' },
  { label: 'Reservas confirmadas', value: 'CONFIRMED' },
  { label: 'Pendientes de confirmacion', value: 'PENDIENTE_CONFIRMACION' },
  { label: 'Recordatorio enviado', value: 'RECORDATORIO_ENVIADO' },
  { label: 'Canceladas', value: 'CANCELLED' },
  { label: 'Reprogramadas', value: 'RESCHEDULED' },
  { label: 'Atendidas', value: 'COMPLETED' },
  { label: 'No asistio', value: 'NO_SHOW' },
]

const serviceColorClasses = [
  'border-emerald-200 bg-emerald-50 text-emerald-950 shadow-emerald-100',
  'border-blue-200 bg-blue-50 text-blue-950 shadow-blue-100',
  'border-violet-200 bg-violet-50 text-violet-950 shadow-violet-100',
  'border-amber-200 bg-amber-50 text-amber-950 shadow-amber-100',
  'border-orange-200 bg-orange-50 text-orange-950 shadow-orange-100',
  'border-sky-200 bg-sky-50 text-sky-950 shadow-sky-100',
]

function getWeekStart(value: string) {
  const current = dayjs(value).startOf('day')
  const daysFromMonday = (current.day() + 6) % 7
  return current.subtract(daysFromMonday, 'day')
}

function buildVisibleDays(value: string) {
  const weekStart = getWeekStart(value)
  return Array.from({ length: calendarDays }, (_, index) => weekStart.add(index, 'day'))
}

function formatCalendarDay(value: dayjs.Dayjs) {
  return {
    label: weekDayLabels[value.day()],
    dayNumber: value.format('DD'),
    month: value.format('MM'),
    key: value.format('YYYY-MM-DD'),
  }
}

function formatTimeRange(item: AgendaCalendarItemResponse) {
  const start = item.startTimeLocal ?? formatAgendaTime(item.startsAt)
  const end = item.endTimeLocal ?? formatAgendaTime(item.endsAt)
  return `${start} - ${end}`
}

function formatLongDate(value: string) {
  const dateKey = getAgendaDateKey(value)
  const dateValue = dayjs(dateKey)
  return `${weekDayLabels[dateValue.day()]}, ${dateValue.format('DD/MM/YYYY')}`
}

function getStatusLabel(status: string) {
  const normalized = status.toUpperCase()
  const labels: Record<string, string> = {
    CONFIRMED: 'Confirmada',
    CONFIRMADA: 'Confirmada',
    PENDIENTE_CONFIRMACION: 'Pendiente',
    PENDING_CONFIRMATION: 'Pendiente',
    RECORDATORIO_ENVIADO: 'Recordatorio enviado',
    REMINDER_SENT: 'Recordatorio enviado',
    CANCELLED: 'Cancelada',
    CANCELADA: 'Cancelada',
    RESCHEDULED: 'Reprogramada',
    REPROGRAMADA: 'Reprogramada',
    COMPLETED: 'Atendida',
    ATENDIDA: 'Atendida',
    NO_SHOW: 'No asistio',
    TEMPORARY: 'Temporal',
  }
  return labels[normalized] ?? status
}

function getStatusTone(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
  const normalized = status.toUpperCase()
  if (['CONFIRMED', 'CONFIRMADA', 'COMPLETED', 'ATENDIDA'].includes(normalized)) {
    return 'success'
  }
  if (['PENDIENTE_CONFIRMACION', 'PENDING_CONFIRMATION', 'TEMPORARY'].includes(normalized)) {
    return 'warning'
  }
  if (['CANCELLED', 'CANCELADA', 'NO_SHOW'].includes(normalized)) {
    return 'danger'
  }
  if (['RESCHEDULED', 'REPROGRAMADA', 'RECORDATORIO_ENVIADO', 'REMINDER_SENT'].includes(normalized)) {
    return 'info'
  }
  return 'neutral'
}

function getItemColorClass(item: AgendaCalendarItemResponse) {
  const key = item.serviceId ?? item.serviceName ?? item.subject
  const hash = key.split('').reduce((current, char) => current + char.charCodeAt(0), 0)
  return serviceColorClasses[hash % serviceColorClasses.length]
}

function getScheduleHours() {
  return Array.from({ length: scheduleEndHour - scheduleStartHour + 1 }, (_, index) => scheduleStartHour + index)
}

type AgendaHourLayout = {
  byHour: Record<number, { top: number; height: number; maxItems: number }>
  totalHeight: number
}

function getLocalHourMinute(item: AgendaCalendarItemResponse) {
  if (item.startTimeLocal) {
    return {
      hour: Number(item.startTimeLocal.slice(0, 2)),
      minute: Number(item.startTimeLocal.slice(3, 5)),
    }
  }
  return getAgendaHourMinute(item.startsAt)
}

function getItemStartHour(item: AgendaCalendarItemResponse) {
  return getLocalHourMinute(item).hour
}

function buildAgendaHourLayout(items: AgendaCalendarItemResponse[], visibleDays: dayjs.Dayjs[]): AgendaHourLayout {
  const visibleDayKeys = new Set(visibleDays.map((day) => day.format('YYYY-MM-DD')))
  const itemsByDayHour = new Map<string, number>()

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

  getScheduleHours().forEach((hour) => {
    const maxItems = Math.max(
      1,
      ...visibleDays.map((day) => itemsByDayHour.get(`${day.format('YYYY-MM-DD')}-${hour}`) ?? 0),
    )
    const height = Math.max(
      baseHourHeight,
      rowVerticalPadding * 2 + maxItems * eventCardHeight + Math.max(0, maxItems - 1) * eventGap,
    )
    byHour[hour] = { top: accumulatedTop, height, maxItems }
    accumulatedTop += height
  })

  return { byHour, totalHeight: accumulatedTop }
}

function getEventsForDayAndHour(items: AgendaCalendarItemResponse[], dayKey: string, hour: number) {
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

function getCurrentTimeIndicator(visibleDays: dayjs.Dayjs[], hourLayout: AgendaHourLayout) {
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
  const lastWhatsAppReminder = detail?.reminders
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

  const hourLayout = useMemo(() => buildAgendaHourLayout(calendarItems, visibleDays), [calendarItems, visibleDays])
  const currentTimeIndicator = useMemo(
    () => getCurrentTimeIndicator(visibleDays, hourLayout),
    [hourLayout, visibleDays],
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
    mutationFn: (startsAt: string) =>
      createTemporaryAgendaBookingRequest({
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
      }),
    onSuccess: (booking) => {
      showToast({
        title: 'Reserva temporal creada',
        description: 'Se genero el enlace de confirmacion y quedo lista para WhatsApp.',
        tone: 'success',
      })
      navigate(`/appointments/${booking.id}`)
    },
    onError: () => {
      showToast({
        title: 'No se pudo crear la reserva',
        description: 'El horario pudo quedar ocupado o faltan datos obligatorios.',
        tone: 'error',
      })
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
    onError: () => {
      showToast({
        title: 'No se pudo cancelar la reserva',
        description: 'El motivo es obligatorio o la reserva ya no permite cambios.',
        tone: 'error',
      })
    },
  })

  const canSearch = locationId && serviceId && date
  const canCreate = customerName.trim().length > 0 && customerPhone.trim().length >= 8
  const slots = availabilityMutation.data?.slots ?? []
  const activityItems = buildRecentActivity(bookingDetail)
  const selectedStatusTone = getStatusTone(selectedItem?.status ?? bookingDetail?.status ?? '')

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

      <div className="grid gap-6 2xl:grid-cols-[minmax(0,1fr)_380px]">
        <Card className="overflow-hidden p-0">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-5">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">Calendario semanal de reservas</h2>
              <p className="mt-1 text-sm text-slate-500">
                {weekStart.format('DD/MM/YYYY')} - {weekStart.add(calendarDays - 1, 'day').format('DD/MM/YYYY')} · GMT-04 / America/Santiago ·{' '}
                {calendarItems.length} reservas visibles
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button onClick={() => setDate(dayjs(date).subtract(7, 'day').format('YYYY-MM-DD'))} variant="secondary">
                Semana anterior
              </Button>
              <Button onClick={() => setDate(today)} variant="secondary">
                Hoy
              </Button>
              <Button onClick={() => setDate(dayjs(date).add(7, 'day').format('YYYY-MM-DD'))} variant="secondary">
                Semana siguiente
              </Button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <div className="min-w-[1020px]">
              <div
                className="grid border-b border-slate-100 bg-slate-50"
                style={{ gridTemplateColumns: `76px repeat(${visibleDays.length}, minmax(145px, 1fr))` }}
              >
                <div className="border-r border-slate-100 px-3 py-4 text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                  Hora
                </div>
                {visibleDays.map((day) => {
                  const formattedDay = formatCalendarDay(day)
                  const isToday = day.isSame(dayjs(), 'day')
                  return (
                    <div className="border-r border-slate-100 px-4 py-3 text-center" key={formattedDay.key}>
                      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{formattedDay.label}</p>
                      <div className="mt-1 flex items-center justify-center gap-1">
                        <span
                          className={[
                            'inline-flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold',
                            isToday ? 'bg-emerald-500 text-white' : 'text-slate-900',
                          ].join(' ')}
                        >
                          {formattedDay.dayNumber}
                        </span>
                        <span className="text-xs font-semibold text-emerald-500">/{formattedDay.month}</span>
                      </div>
                    </div>
                  )
                })}
              </div>

              <div className="relative bg-white">
                {currentTimeIndicator ? (
                  <div
                    aria-hidden="true"
                    className="pointer-events-none absolute right-0 z-30 border-t-2 border-dashed border-rose-500/80"
                    style={{ left: 76, top: currentTimeIndicator.top }}
                  >
                    <span className="absolute -left-[72px] -top-3 rounded-full bg-rose-50 px-2 py-0.5 text-[10px] font-bold text-rose-600 shadow-sm">
                      Ahora {currentTimeIndicator.label}
                    </span>
                  </div>
                ) : null}

                {getScheduleHours().map((hour) => {
                  const rowHeight = hourLayout.byHour[hour]?.height ?? baseHourHeight
                  return (
                    <div
                      className="grid border-t border-slate-100"
                      key={hour}
                      style={{ gridTemplateColumns: `76px repeat(${visibleDays.length}, minmax(145px, 1fr))`, minHeight: rowHeight }}
                    >
                      <div className="border-r border-slate-100 bg-slate-50 px-3 pt-3 text-xs font-semibold text-slate-500">
                        {String(hour).padStart(2, '0')}:00
                      </div>

                      {visibleDays.map((day) => {
                        const dayKey = day.format('YYYY-MM-DD')
                        const dayItems = calendarItems.filter((item) => (item.dateLocal ?? getAgendaDateKey(item.startsAt)) === dayKey)
                        const eventsForHour = getEventsForDayAndHour(calendarItems, dayKey, hour)
                        return (
                          <div
                            className="relative overflow-hidden border-r border-slate-100"
                            key={`${dayKey}-${hour}`}
                            style={{ minHeight: rowHeight }}
                          >
                            {dayItems.length === 0 && hour === scheduleStartHour ? (
                              <div className="mx-3 mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-3 text-center text-xs font-semibold text-slate-400">
                                Sin reservas
                              </div>
                            ) : null}

                            {eventsForHour.length > 0 ? (
                              <div className="flex h-full flex-col gap-2 px-2 py-3">
                                {eventsForHour.map((item) => {
                                  const isSelected = selectedItem?.bookingId === item.bookingId
                                  return (
                                    <button
                                      aria-label={`Ver detalle de ${item.customerName}`}
                                      className={[
                                        'relative min-h-[56px] w-full flex-none overflow-hidden rounded-2xl border p-3 text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-blue-300',
                                        getItemColorClass(item),
                                        isSelected ? 'ring-2 ring-blue-500' : '',
                                      ]
                                        .join(' ')
                                        .trim()}
                                      key={item.bookingId}
                                      onClick={() => setSelectedBookingId(item.bookingId)}
                                      onFocus={() => setSelectedBookingId(item.bookingId)}
                                      onMouseEnter={() => setSelectedBookingId(item.bookingId)}
                                      style={{ zIndex: isSelected ? 20 : 10 }}
                                      type="button"
                                    >
                                      <div className="flex items-start justify-between gap-2">
                                        <span className="text-[11px] font-bold">{formatTimeRange(item)}</span>
                                        <span className="rounded-full bg-white/70 px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-[0.12em]">
                                          {item.sourceChannel?.toUpperCase().includes('WHATSAPP') ? 'WA' : 'APP'}
                                        </span>
                                      </div>
                                      <p className="mt-1 line-clamp-1 text-xs font-bold">{item.serviceName ?? item.subject}</p>
                                      <p className="line-clamp-1 text-xs opacity-80">{item.customerName}</p>
                                      <p className="mt-1 line-clamp-1 text-[11px] opacity-70">
                                        {item.professionalName ?? 'Profesional por asignar'}
                                      </p>
                                    </button>
                                  )
                                })}
                              </div>
                            ) : null}
                          </div>
                        )
                      })}
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        </Card>

        <Card className="h-fit space-y-5 2xl:sticky 2xl:top-6">
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
                <StatusBadge label={getStatusLabel(selectedItem.status)} tone={selectedStatusTone} />
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
