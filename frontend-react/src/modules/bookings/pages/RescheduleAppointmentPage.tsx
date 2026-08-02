import dayjs from 'dayjs'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { getBookingDetailRequest, rescheduleBookingRequest } from '../../../services/api/bookingsApi'
import { getAgendaAvailabilityRequest } from '../../../services/api/completeAgendaApi'
import type { AgendaSlotResponse } from '../../../services/api/types'

export function RescheduleAppointmentPage() {
  const { appointmentId } = useParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const [selectedDate, setSelectedDate] = useState('')
  const [selectedSlot, setSelectedSlot] = useState<AgendaSlotResponse | null>(null)

  const bookingQuery = useQuery({
    queryKey: ['bookings', 'detail', appointmentId],
    queryFn: () => getBookingDetailRequest(appointmentId ?? ''),
    enabled: Boolean(appointmentId),
  })

  const booking = bookingQuery.data

  const availabilityQuery = useQuery({
    queryKey: ['booking-reschedule-availability', appointmentId, selectedDate],
    queryFn: () =>
      getAgendaAvailabilityRequest({
        locationId: booking!.locationId!,
        serviceId: booking!.serviceId!,
        professionalId: booking!.professionalId ?? undefined,
        date: selectedDate,
        maxSlots: 40,
      }),
    enabled: Boolean(selectedDate && booking?.locationId && booking?.serviceId),
    retry: false,
  })

  const sortedSlots = useMemo(() => {
    const slots = (availabilityQuery.data?.slots ?? []).filter((slot) => slot.available)
    return [...slots].sort(
      (a, b) => dayjs(a.startsAt).valueOf() - dayjs(b.startsAt).valueOf(),
    )
  }, [availabilityQuery.data])

  useEffect(() => {
    setSelectedSlot(null)
  }, [selectedDate])

  const rescheduleMutation = useMutation({
    mutationFn: async () => {
      if (!appointmentId || !selectedSlot) {
        throw new Error('No hay cita u horario seleccionado.')
      }
      return rescheduleBookingRequest(appointmentId, {
        startsAt: dayjs(selectedSlot.startsAt).toISOString(),
      })
    },
    onSuccess: () => {
      showToast({
        title: 'Cita reprogramada',
        description: 'La cita fue reprogramada correctamente.',
        tone: 'success',
      })
      navigate(appointmentId ? `/appointments/${appointmentId}` : '/appointments')
    },
    onError: (error) => {
      showToast({
        title: 'No se pudo reprogramar la cita',
        description:
          'El horario seleccionado acaba de dejar de estar disponible. Selecciona otro horario.',
        tone: 'error',
      })
    },
  })

  const handleDateChange = (value: string) => {
    setSelectedDate(value)
  }

  const handleSlotSelect = (slot: AgendaSlotResponse) => {
    setSelectedSlot((current) =>
      current?.startsAt === slot.startsAt ? null : slot,
    )
  }

  const selectedSlotEndsAt = useMemo(() => {
    if (!selectedSlot || !booking) {
      return null
    }
    return dayjs(selectedSlot.startsAt).add(booking.durationMinutes, 'minute')
  }, [selectedSlot, booking])

  const goBack = () =>
    navigate(appointmentId ? `/appointments/${appointmentId}` : '/appointments')

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button onClick={goBack} variant="secondary">
            Volver al detalle
          </Button>
        }
        description="Selecciona una nueva fecha y elige un horario disponible para tu cita."
        eyebrow="Reprogramacion"
        title="Reprogramar cita"
      />

      {bookingQuery.isPending ? (
        <LoadingState message="Cargando la cita para reprogramarla." variant="detail" />
      ) : bookingQuery.isError || !booking ? (
        <ErrorState
          description="No pudimos cargar la cita seleccionada."
          onRetry={() => void bookingQuery.refetch()}
          title="No fue posible abrir la cita"
        />
      ) : (
        <Card>
          <div className="space-y-6">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-teal-700">
                Cita actual
              </p>
              <h2 className="mt-1 text-xl font-semibold text-slate-950">
                {booking.subject}
              </h2>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                {dayjs(booking.startsAt).format('DD/MM/YYYY HH:mm')}
                {' · '}
                {booking.locationName ?? booking.location}
                {' · '}
                {booking.durationMinutes} minutos
              </p>
            </div>

            <div className="space-y-3">
              <Input
                label="Selecciona una nueva fecha"
                type="date"
                value={selectedDate}
                min={dayjs().format('YYYY-MM-DD')}
                onChange={(event) => handleDateChange(event.target.value)}
              />
            </div>

            <div className="space-y-3" aria-live="polite">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                Horarios disponibles
              </p>

              {!selectedDate ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-4 text-center">
                  <p className="text-sm text-slate-500">
                    Selecciona una fecha para ver los horarios disponibles.
                  </p>
                </div>
              ) : availabilityQuery.isPending ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-4 text-center">
                  <p className="text-sm font-medium text-slate-700">Cargando horarios...</p>
                </div>
              ) : availabilityQuery.isError ? (
                <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-center">
                  <p className="text-sm text-rose-700">
                    No fue posible consultar los horarios disponibles. Intenta nuevamente.
                  </p>
                  <Button
                    className="mt-3"
                    onClick={() => void availabilityQuery.refetch()}
                    variant="secondary"
                  >
                    Reintentar
                  </Button>
                </div>
              ) : sortedSlots.length === 0 ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-4 text-center">
                  <p className="text-sm text-slate-500">
                    No encontramos horarios disponibles para esta fecha. Selecciona otro día.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-4">
                  {sortedSlots.map((slot) => {
                    const selected = selectedSlot?.startsAt === slot.startsAt
                    const slotEnd = dayjs(slot.startsAt)
                      .add(booking.durationMinutes, 'minute')
                      .format('HH:mm')
                    const label = `Horario disponible a las ${dayjs(slot.startsAt).format('HH:mm')}, finaliza a las ${slotEnd}.`
                    return (
                      <button
                        key={`${slot.startsAt}-${slot.professionalId ?? 'any'}-${slot.roomId ?? 'any'}`}
                        type="button"
                        aria-label={label}
                        aria-pressed={selected}
                        onClick={() => handleSlotSelect(slot)}
                        className={[
                          'flex flex-col items-center rounded-xl border px-3 py-2 text-sm font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-teal-500',
                          selected
                            ? 'border-teal-500 bg-teal-50 text-teal-800 ring-2 ring-teal-500'
                            : 'border-slate-200 bg-white text-slate-700 hover:border-teal-300 hover:bg-teal-50/50',
                        ].join(' ')}
                      >
                        <span>{dayjs(slot.startsAt).format('HH:mm')}</span>
                        <span className="text-xs font-normal text-slate-400">
                          Hasta {slotEnd}
                        </span>
                        {selected ? (
                          <span className="mt-0.5 text-teal-600" aria-hidden="true">
                            ✓
                          </span>
                        ) : null}
                      </button>
                    )
                  })}
                </div>
              )}
            </div>

            {selectedDate && selectedSlot && selectedSlotEndsAt ? (
              <div className="rounded-2xl border border-teal-200 bg-teal-50 p-4">
                <p className="text-sm leading-6 text-teal-900">
                  <span className="font-semibold">Nueva fecha:</span>{' '}
                  {dayjs(selectedSlot.startsAt).format('DD/MM/YYYY')}
                  <br />
                  <span className="font-semibold">Hora seleccionada:</span>{' '}
                  {dayjs(selectedSlot.startsAt).format('HH:mm')}
                  <br />
                  <span className="font-semibold">Finaliza:</span>{' '}
                  {selectedSlotEndsAt.format('HH:mm')}
                </p>
              </div>
            ) : null}

            <div className="flex flex-wrap justify-end gap-3">
              <Button onClick={goBack} variant="secondary">
                Cancelar
              </Button>
              <Button
                disabled={!isOnline || !selectedDate || !selectedSlot}
                loading={rescheduleMutation.isPending}
                onClick={() => rescheduleMutation.mutate()}
              >
                Guardar nueva fecha
              </Button>
            </div>
          </div>
        </Card>
      )}
    </section>
  )
}
