import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { FilterBar } from '../../../components/ui/FilterBar'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { useShellSession } from '../../../lib/shellSession'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { getBookingsRequest } from '../../../services/api/bookingsApi'
import type { BookingSummaryResponse } from '../../../services/api/types'
import { AgendaEventCard } from '../../agenda/components/AgendaEventCard'
import { agendaTimeZone, formatAgendaTime, getAgendaDateKey, getStatusStyle } from '../../agenda/components/agendaUtils'

const fieldClassName =
  'w-full rounded-2xl border border-[var(--color-border)] bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

const filtersSchema = z.object({
  search: z.string().max(80, 'La busqueda no puede superar los 80 caracteres.'),
  status: z.string(),
  assignedUserId: z.string(),
})

type FiltersValues = z.infer<typeof filtersSchema>

const defaultFilters: FiltersValues = {
  search: '',
  status: '',
  assignedUserId: '',
}

function formatDateTime(value: string) {
  return dayjs(value).format('DD MMM YYYY HH:mm')
}

function buildCalendarDays(visibleMonth: dayjs.Dayjs) {
  const firstDay = visibleMonth.startOf('month').startOf('week')
  return Array.from({ length: 42 }, (_, index) => firstDay.add(index, 'day'))
}

function bookingToAgendaItem(booking: BookingSummaryResponse) {
  const endsAt = dayjs(booking.startsAt).add(booking.durationMinutes, 'minute').toISOString()
  const startTimeLocal = formatAgendaTime(booking.startsAt)
  const endTimeLocal = formatAgendaTime(endsAt)
  const dateLocal = getAgendaDateKey(booking.startsAt)
  return {
    bookingId: booking.id,
    subject: booking.subject,
    status: booking.status,
    startsAt: booking.startsAt,
    endsAt,
    durationMinutes: booking.durationMinutes,
    locationId: booking.locationId,
    locationName: booking.locationName ?? booking.location,
    serviceId: null,
    serviceName: booking.subject,
    professionalId: null,
    professionalName: booking.assignedUserName,
    roomId: null,
    roomName: null,
    customerName: booking.customerName,
    customerPhone: booking.customerPhone,
    sourceChannel: '',
    startsAtLocal: startTimeLocal,
    endsAtLocal: endTimeLocal,
    dateLocal,
    startTimeLocal,
    endTimeLocal,
    timezone: agendaTimeZone,
    type: 'BOOKING',
  }
}

export function AppointmentsPage() {
  const navigate = useNavigate()
  const { user } = useShellSession()
  const isOnline = useOnlineStatus()
  const [visibleMonth, setVisibleMonth] = useState(dayjs().startOf('month'))
  const [selectedDate, setSelectedDate] = useState(dayjs().format('YYYY-MM-DD'))
  const [appliedFilters, setAppliedFilters] = useState<FiltersValues>(defaultFilters)
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FiltersValues>({
    resolver: zodResolver(filtersSchema),
    defaultValues: defaultFilters,
  })

  const bookingsQuery = useQuery({
    queryKey: ['bookings', 'list', visibleMonth.format('YYYY-MM'), appliedFilters],
    queryFn: () =>
      getBookingsRequest({
        page: 0,
        size: 500,
        from: visibleMonth.startOf('month').toISOString(),
        to: visibleMonth.endOf('month').toISOString(),
        search: appliedFilters.search || undefined,
        status: appliedFilters.status || undefined,
        assignedUserId: appliedFilters.assignedUserId || undefined,
      }),
    refetchInterval: isOnline ? 30_000 : false,
  })

  const bookings = useMemo(() => bookingsQuery.data?.items ?? [], [bookingsQuery.data?.items])
  const calendarDays = useMemo(() => buildCalendarDays(visibleMonth), [visibleMonth])
  const firstBookingDate = useMemo(
    () => (bookings.length > 0 ? dayjs(bookings[0].startsAt).format('YYYY-MM-DD') : null),
    [bookings],
  )
  const groupedByDate = useMemo(() => {
    const map = new Map<string, BookingSummaryResponse[]>()
    bookings.forEach((booking) => {
      const key = dayjs(booking.startsAt).format('YYYY-MM-DD')
      const items = map.get(key) ?? []
      items.push(booking)
      map.set(key, items)
    })
    return map
  }, [bookings])

  const selectedDayBookings = groupedByDate.get(selectedDate) ?? []

  useEffect(() => {
    if (!dayjs(selectedDate).isSame(visibleMonth, 'month')) {
      setSelectedDate(visibleMonth.startOf('month').format('YYYY-MM-DD'))
    }
  }, [selectedDate, visibleMonth])

  useEffect(() => {
    const monthStart = visibleMonth.startOf('month').format('YYYY-MM-DD')
    const selectedDayHasBookings = (groupedByDate.get(selectedDate) ?? []).length > 0

    if (firstBookingDate && selectedDate === monthStart && !selectedDayHasBookings) {
      setSelectedDate(firstBookingDate)
    }
  }, [firstBookingDate, groupedByDate, selectedDate, visibleMonth])

  const moveVisibleMonth = (months: number) => {
    const nextMonth = visibleMonth.add(months, 'month').startOf('month')
    setVisibleMonth(nextMonth)
    setSelectedDate(nextMonth.format('YYYY-MM-DD'))
  }

  const onSubmitFilters = handleSubmit(async (values) => {
    setAppliedFilters(values)
  })

  const clearFilters = () => {
    reset(defaultFilters)
    setAppliedFilters(defaultFilters)
  }

  const weekDayLabels = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab']

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Button onClick={() => navigate('/appointments/new')}>Crear cita</Button>
            <Link className={buttonClassName({ variant: 'secondary' })} to="/conversations">
              Ir a conversaciones
            </Link>
          </>
        }
        description="Agenda mensual simple con lista diaria, filtros y accesos al detalle para crear, reprogramar o cancelar citas."
        eyebrow="Agenda"
        title="Agenda y citas"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Estado sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            Puedes revisar la agenda cacheada, pero no crear ni sincronizar cambios hasta recuperar internet.
          </p>
        </Card>
      ) : null}

      <form onSubmit={onSubmitFilters}>
        <FilterBar
          actions={
            <>
              <Button disabled={isSubmitting} loading={isSubmitting} type="submit">
                Aplicar filtros
              </Button>
              <Button onClick={clearFilters} variant="secondary">
                Limpiar
              </Button>
            </>
          }
        >
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Busqueda</span>
            <input
              className={fieldClassName}
              placeholder="Buscar por cliente, asunto o ubicacion"
              type="search"
              {...register('search')}
            />
            {errors.search ? (
              <span className="mt-2 block text-sm text-red-700">{errors.search.message}</span>
            ) : null}
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Estado</span>
            <select className={fieldClassName} {...register('status')}>
              <option value="">Todos</option>
              <option value="REQUESTED">Solicitadas</option>
              <option value="PENDIENTE_CONFIRMACION">Pendientes de confirmacion</option>
              <option value="CONFIRMED">Confirmadas</option>
              <option value="REPROGRAMADA">Reprogramadas</option>
              <option value="CANCELADA">Canceladas</option>
              <option value="COMPLETED">Completadas</option>
              <option value="EXPIRADA">Expiradas</option>
              <option value="NO_SHOW">No asistio</option>
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Responsable</span>
            <select className={fieldClassName} {...register('assignedUserId')}>
              <option value="">Todos</option>
              <option value={user?.id ?? ''}>{user?.name ?? 'Mi usuario'}</option>
            </select>
          </label>

          <div className="rounded-[1.5rem] border border-[var(--color-border)] bg-slate-50 px-4 py-3">
            <p className="text-sm font-medium text-slate-700">Mes visible</p>
            <p className="mt-2 text-base font-semibold text-slate-950">
              {visibleMonth.format('MMMM YYYY')}
            </p>
            <p className="mt-1 text-sm text-slate-600">
              {bookingsQuery.data?.totalItems ?? 0} cita(s) encontradas.
            </p>
          </div>
        </FilterBar>
      </form>

      {bookingsQuery.isPending && !bookingsQuery.data ? (
        <LoadingState
          message="Cargando la agenda mensual y la lista diaria de citas."
          variant="page"
        />
      ) : bookingsQuery.isError && !bookingsQuery.data ? (
        <ErrorState
          description="No pudimos cargar la agenda. Reintenta para recuperar el calendario."
          onRetry={() => void bookingsQuery.refetch()}
          title="No fue posible cargar las citas"
        />
      ) : (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.5fr)_minmax(320px,0.9fr)]">
          <Card className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                  Vista mensual
                </p>
                <h2 className="mt-2 text-2xl font-semibold text-[var(--color-text)]">
                  {visibleMonth.format('MMMM YYYY')}
                </h2>
              </div>

              <div className="flex gap-2">
                <Button
                  onClick={() => moveVisibleMonth(-1)}
                  size="sm"
                  variant="secondary"
                >
                  Mes anterior
                </Button>
                <Button
                  onClick={() => moveVisibleMonth(1)}
                  size="sm"
                  variant="secondary"
                >
                  Mes siguiente
                </Button>
              </div>
            </div>

            {bookings.length === 0 ? (
              <div className="rounded-3xl border border-dashed border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900">
                La agenda del mes esta disponible aunque no existan citas. Selecciona un dia o crea una cita para ocupar un cupo.
              </div>
            ) : null}

            <div className="grid grid-cols-7 gap-3">
              {weekDayLabels.map((label) => (
                <p
                  key={label}
                  className="px-2 text-center text-xs font-semibold uppercase tracking-[0.16em] text-slate-500"
                >
                  {label}
                </p>
              ))}

              {calendarDays.map((day) => {
                  const key = day.format('YYYY-MM-DD')
                  const items = groupedByDate.get(key) ?? []
                  const active = selectedDate === key
                  const isCurrentMonth = day.month() === visibleMonth.month()
                  const isToday = day.isSame(dayjs(), 'day')

                  return (
                    <button
                      key={key}
                      className={[
                        'min-h-[118px] rounded-[22px] border p-3 text-left transition',
                        active
                          ? 'border-blue-300 bg-blue-50'
                          : 'border-[var(--color-border)] bg-white hover:border-blue-200 hover:bg-slate-50',
                        isCurrentMonth ? '' : 'opacity-45',
                        isToday && !active ? 'border-emerald-200 bg-emerald-50/40' : '',
                      ]
                        .join(' ')
                        .trim()}
                      onClick={() => setSelectedDate(key)}
                      type="button"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span
                          className={[
                            'inline-flex h-7 w-7 items-center justify-center rounded-full text-sm font-semibold',
                            isToday ? 'bg-emerald-500 text-white' : 'text-[var(--color-text)]',
                          ].join(' ')}
                        >
                          {day.format('D')}
                        </span>
                        {items.length > 0 ? (
                          <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-bold text-emerald-800">
                            {items.length}
                          </span>
                        ) : null}
                      </div>

                      <div className="mt-2 space-y-1">
                        {items.slice(0, 3).map((booking) => {
                          const ss = getStatusStyle(booking.status)
                          return (
                            <div
                              key={booking.id}
                              className={['rounded-lg px-2 py-1 text-[11px] shadow-sm', ss.bg, ss.text].join(' ')}
                              style={{ borderLeft: `2px solid ${ss.hex}` }}
                            >
                              <p className="truncate font-semibold">{booking.subject}</p>
                              <p className="truncate opacity-80">{dayjs(booking.startsAt).format('HH:mm')}</p>
                            </div>
                          )
                        })}
                        {items.length > 3 ? (
                          <p className="text-xs font-semibold text-slate-500">
                            +{items.length - 3} mas
                          </p>
                        ) : null}
                      </div>
                    </button>
                  )
              })}
            </div>
          </Card>

          <Card className="space-y-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                  Lista diaria
                </p>
                <h2 className="mt-2 text-2xl font-semibold text-[var(--color-text)]">
                  {dayjs(selectedDate).format('DD MMMM YYYY')}
                </h2>
              </div>
              <StatusBadge
                label={`${selectedDayBookings.length} cita(s)`}
                tone={selectedDayBookings.length > 0 ? 'info' : 'neutral'}
              />
            </div>

            {selectedDayBookings.length === 0 ? (
              <EmptyState
                description="La agenda de este dia esta disponible, pero aun no tiene citas registradas."
                primaryAction={{ label: 'Crear cita', to: '/appointments/new' }}
                title="Dia sin citas"
                variant="card"
              />
            ) : (
              <div className="space-y-3">
                {selectedDayBookings.map((booking) => {
                  const agendaItem = bookingToAgendaItem(booking)
                  return (
                    <Link
                      key={booking.id}
                      className="block transition hover:-translate-y-0.5"
                      to={`/appointments/${booking.id}`}
                    >
                      <AgendaEventCard
                        item={agendaItem}
                        showStatus
                      />
                    </Link>
                  )
                })}
              </div>
            )}
          </Card>
        </div>
      )}
    </section>
  )
}
