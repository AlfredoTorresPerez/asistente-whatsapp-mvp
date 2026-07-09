import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { Select } from '../../../components/ui/Select'
import { Textarea } from '../../../components/ui/Textarea'
import {
  getCustomerBookingRescheduleAvailabilityRequest,
  getCustomerBookingReschedulePreviewRequest,
  getCustomerBookingsRequest,
  getPublicBookingRescheduleRequest,
  rescheduleCustomerBookingRequest,
} from '../../../services/api/bookingsApi'
import { ApiClientError } from '../../../services/api/httpClient'
import type { AgendaSlotResponse, CustomerBookingItemResponse } from '../../../services/api/types'

type RescheduleDraft = {
  serviceId: string
  locationId: string
  date: string
  reason: string
}

function formatDateTime(value: string) {
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

function formatTime(value: string) {
  return dayjs(value).format('HH:mm')
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError ? error.message : fallback
}

function statusBadge(status: string) {
  const s = status.toUpperCase()
  if (['CONFIRMED', 'CONFIRMADA'].includes(s)) return 'bg-emerald-100 text-emerald-800'
  if (['PENDING', 'PENDIENTE', 'PENDIENTE_CONFIRMACION', 'SOLICITADA', 'RESERVED', 'RESERVADA'].includes(s)) return 'bg-amber-100 text-amber-800'
  if (['CANCELADA', 'CANCELADA_POR_CLIENTE', 'CANCELLED', 'NO_SHOW', 'EXPIRADA'].includes(s)) return 'bg-red-100 text-red-800'
  if (['RESCHEDULED', 'REPROGRAMADA', 'REPROGRAMACION_PENDIENTE'].includes(s)) return 'bg-blue-100 text-blue-800'
  return 'bg-slate-100 text-slate-800'
}

export function BookingReschedulePage() {
  const { token } = useParams()
  const queryClient = useQueryClient()
  const today = dayjs().format('YYYY-MM-DD')
  const [selectedBookingId, setSelectedBookingId] = useState<string | null>(null)
  const [selectedStep, setSelectedStep] = useState<'list' | 'preview' | 'reschedule'>('list')
  const [draft, setDraft] = useState<RescheduleDraft | null>(null)
  const [selectedSlot, setSelectedSlot] = useState<AgendaSlotResponse | null>(null)
  const [updatedBooking, setUpdatedBooking] = useState<CustomerBookingItemResponse | null>(null)

  const bookingsQuery = useQuery({
    queryKey: ['customer-bookings', token],
    queryFn: () => getCustomerBookingsRequest(token ?? ''),
    enabled: Boolean(token),
    retry: false,
  })

  const previewQuery = useQuery({
    queryKey: ['customer-booking-reschedule-preview', token, selectedBookingId],
    queryFn: () => getCustomerBookingReschedulePreviewRequest(token ?? '', selectedBookingId ?? ''),
    enabled: Boolean(token && selectedBookingId && selectedStep === 'preview'),
    retry: false,
  })

  const preview = previewQuery.data
  const selectedBooking = useMemo(
    () => bookingsQuery.data?.find((b) => b.bookingId === selectedBookingId) ?? null,
    [bookingsQuery.data, selectedBookingId],
  )

  const serviceOptions = preview?.services ?? []
  const locationOptions = preview?.locations ?? []

  useEffect(() => {
    if (!selectedBooking || selectedStep !== 'preview') return
    const currentDate = dayjs(selectedBooking.startsAt).format('YYYY-MM-DD')
    setDraft({
      serviceId: selectedBooking.serviceId ?? serviceOptions[0]?.id ?? '',
      locationId: selectedBooking.locationId ?? locationOptions[0]?.id ?? '',
      date: dayjs(currentDate).isBefore(today, 'day') ? today : currentDate,
      reason: '',
    })
    setSelectedSlot(null)
    setSelectedStep('reschedule')
  }, [locationOptions, selectedBooking, selectedStep, serviceOptions, today])

  const availabilityQuery = useQuery({
    queryKey: ['customer-booking-reschedule-availability', token, selectedBookingId, draft?.serviceId, draft?.locationId, draft?.date],
    queryFn: () =>
      getCustomerBookingRescheduleAvailabilityRequest(
        token ?? '',
        selectedBookingId ?? '',
        draft?.serviceId ?? '',
        draft?.locationId ?? '',
        draft?.date ?? today,
      ),
    enabled: Boolean(token && selectedBookingId && draft?.serviceId && draft?.locationId && draft?.date),
    retry: false,
  })

  const rescheduleMutation = useMutation({
    mutationFn: async () => {
      if (!token || !selectedBookingId || !draft || !selectedSlot) {
        throw new Error('Selecciona una reserva y un horario disponible.')
      }

      const startsAt = dayjs(selectedSlot.startsAt)
      if (!startsAt.isValid() || startsAt.isBefore(dayjs())) {
        throw new Error('La nueva fecha y hora debe ser igual o posterior a la fecha y hora actual.')
      }

      return rescheduleCustomerBookingRequest(token, selectedBookingId, {
        serviceId: selectedSlot.serviceId,
        locationId: selectedSlot.locationId,
        professionalId: selectedSlot.professionalId ?? undefined,
        roomId: selectedSlot.roomId ?? undefined,
        date: startsAt.format('YYYY-MM-DD'),
        startsAt: selectedSlot.startsAt,
        reason: (draft.reason ?? '').trim() || undefined,
      })
    },
    onSuccess: (booking) => {
      setUpdatedBooking(booking)
      setSelectedSlot(null)
      queryClient.invalidateQueries({ queryKey: ['customer-bookings', token] })
    },
  })

  const availableSlots = availabilityQuery.data?.slots.filter(
    (slot) => slot.available && !dayjs(slot.startsAt).isBefore(dayjs()),
  ) ?? []

  const bookings = bookingsQuery.data ?? []
  const isLoading = bookingsQuery.isPending
  const isError = bookingsQuery.isError

  if (updatedBooking) {
    return (
      <main className="min-h-screen bg-[linear-gradient(180deg,_#f8fafc_0%,_#eef6f8_100%)] px-4 py-8">
        <section className="mx-auto max-w-2xl">
          <Card className="border-emerald-200 bg-emerald-50">
            <h2 className="text-2xl font-semibold text-emerald-950">Reserva reprogramada</h2>
            <p className="mt-2 text-sm text-emerald-800">
              Tu reserva quedo actualizada para el {formatDateTime(updatedBooking.startsAt)} en {updatedBooking.locationName ?? 'la sucursal seleccionada'}.
            </p>
            <Button className="mt-4" onClick={() => { setUpdatedBooking(null); setSelectedBookingId(null); setSelectedStep('list'); }}>
              Volver a mis reservas
            </Button>
          </Card>
        </section>
      </main>
    )
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,_#f8fafc_0%,_#eef6f8_100%)] px-4 py-8">
      <section className="mx-auto max-w-5xl space-y-5">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-700">Asistente WhatsApp Centro Estetico</p>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">Reprogramar reserva</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Selecciona la reserva que deseas reprogramar, elige servicio, sucursal y horario.
          </p>
        </div>

        {isLoading ? (
          <Card className="text-center">
            <p className="text-sm font-medium text-slate-700">Cargando tus reservas...</p>
          </Card>
        ) : isError ? (
          <Card className="border-rose-200 bg-rose-50 text-center">
            <h2 className="text-xl font-semibold text-rose-900">Enlace no disponible</h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {getErrorMessage(bookingsQuery.error, 'El enlace no existe, ya vencio o quedo invalidado.')}
            </p>
          </Card>
        ) : bookings.length === 0 ? (
          <Card className="border-teal-200 bg-teal-50 text-center">
            <h2 className="text-xl font-semibold text-teal-900">No hay reservas activas</h2>
            <p className="mt-2 text-sm leading-6 text-teal-700">
              No encontramos reservas vigentes asociadas a este numero.
            </p>
          </Card>
        ) : (
          <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(360px,420px)]">
            <div className="space-y-4">
              <Card className="space-y-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Reservas</p>
                  <h2 className="mt-1 text-xl font-semibold text-slate-950">Elige la reserva a modificar</h2>
                </div>
                <div className="grid gap-3">
                  {bookings.map((booking) => {
                    const isSelected = selectedBookingId === booking.bookingId
                    return (
                      <button
                        key={booking.bookingId}
                        className={[
                          'w-full rounded-2xl border bg-white p-4 text-left transition hover:border-teal-300 hover:shadow-sm',
                          isSelected ? 'border-teal-400 ring-4 ring-teal-100' : 'border-slate-200',
                        ].join(' ')}
                        onClick={() => {
                          setSelectedBookingId(isSelected ? null : booking.bookingId)
                          setSelectedStep('preview')
                          setSelectedSlot(null)
                          setDraft(null)
                        }}
                        type="button"
                      >
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="text-base font-semibold text-slate-950">{booking.serviceName}</p>
                            <p className="mt-1 text-sm text-slate-600">{formatDateTime(booking.startsAt)}</p>
                            <p className="text-sm text-slate-500">{booking.locationName ?? 'Sucursal por confirmar'} · {booking.professionalName ?? 'Profesional por asignar'}</p>
                          </div>
                          <span className={['rounded-full px-3 py-1 text-xs font-semibold', statusBadge(booking.status)].join(' ')}>
                            {booking.status}
                          </span>
                        </div>
                      </button>
                    )
                  })}
                </div>
              </Card>

              {draft ? (
                <Card className="space-y-5">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.22em] text-teal-700">Nueva fecha</p>
                    <h2 className="mt-1 text-xl font-semibold text-slate-950">Selecciona servicio, sucursal y dia</h2>
                  </div>

                  <div className="grid gap-4 md:grid-cols-2">
                    <Select
                      label="Servicio"
                      onChange={(event) => {
                        setDraft((current) => current ? { ...current, serviceId: event.target.value } : current)
                        setSelectedSlot(null)
                      }}
                      options={serviceOptions.map((service) => ({
                        label: `${service.name} (${service.categoryName})`,
                        value: service.id,
                      }))}
                      value={draft.serviceId}
                    />
                    <Select
                      label="Sucursal"
                      onChange={(event) => {
                        setDraft((current) => current ? { ...current, locationId: event.target.value } : current)
                        setSelectedSlot(null)
                      }}
                      options={locationOptions.map((location) => ({
                        label: location.commune ? `${location.name} - ${location.commune}` : location.name,
                        value: location.id,
                      }))}
                      value={draft.locationId}
                    />
                  </div>

                  <div className="grid gap-4 md:grid-cols-2">
                    <Input
                      label="Dia"
                      min={today}
                      onChange={(event) => {
                        const nextDate = event.target.value
                        setDraft((current) => current ? { ...current, date: dayjs(nextDate).isBefore(today, 'day') ? today : nextDate } : current)
                        setSelectedSlot(null)
                      }}
                      type="date"
                      value={draft.date}
                    />
                    <Textarea
                      label="Motivo"
                      onChange={(event) => setDraft((current) => current ? { ...current, reason: event.target.value } : current)}
                      placeholder="Opcional"
                      rows={3}
                      value={draft.reason}
                    />
                  </div>
                </Card>
              ) : null}
            </div>

            <Card className="h-fit space-y-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Horarios</p>
                <h2 className="mt-1 text-xl font-semibold text-slate-950">Disponibles para el dia</h2>
              </div>

              {!selectedBooking || !draft ? (
                <p className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">
                  Selecciona una reserva para consultar disponibilidad.
                </p>
              ) : availabilityQuery.isPending || availabilityQuery.isFetching ? (
                <p className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-600">Cargando horarios disponibles...</p>
              ) : availabilityQuery.isError ? (
                <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                  {getErrorMessage(availabilityQuery.error, 'No fue posible consultar disponibilidad.')}
                </p>
              ) : availableSlots.length === 0 ? (
                <p className="rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                  No hay horarios disponibles para ese dia.
                </p>
              ) : (
                <div className="grid max-h-[460px] gap-3 overflow-y-auto pr-1">
                  {availableSlots.map((slot) => {
                    const selected = selectedSlot?.startsAt === slot.startsAt
                      && selectedSlot.professionalId === slot.professionalId
                      && selectedSlot.roomId === slot.roomId
                    return (
                      <button
                        key={`${slot.startsAt}-${slot.professionalId ?? 'prof'}-${slot.roomId ?? 'room'}`}
                        className={[
                          'w-full rounded-2xl border bg-white p-4 text-left transition hover:border-teal-300',
                          selected ? 'border-teal-400 ring-4 ring-teal-100' : 'border-slate-200',
                        ].join(' ')}
                        onClick={() => setSelectedSlot(slot)}
                        type="button"
                      >
                        <p className="text-base font-semibold text-slate-950">{formatTime(slot.startsAt)} - {formatTime(slot.endsAt)}</p>
                        <p className="mt-1 text-sm text-slate-600">{slot.professionalName ?? 'Profesional por asignar'}</p>
                        <p className="text-sm text-slate-500">{slot.roomName ?? 'Sin cabina requerida'}</p>
                      </button>
                    )
                  })}
                </div>
              )}

              {selectedSlot ? (
                <div className="rounded-2xl border border-teal-200 bg-teal-50 p-4">
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-teal-700">Seleccionado</p>
                  <p className="mt-2 text-sm font-semibold text-teal-950">{formatDateTime(selectedSlot.startsAt)}</p>
                </div>
              ) : null}

              <Button
                disabled={!selectedBooking || !draft || !selectedSlot || rescheduleMutation.isPending}
                loading={rescheduleMutation.isPending}
                onClick={() => rescheduleMutation.mutate()}
              >
                Confirmar reprogramacion
              </Button>

              {rescheduleMutation.isError ? (
                <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                  {getErrorMessage(rescheduleMutation.error, 'No fue posible reprogramar la reserva.')}
                </p>
              ) : null}
            </Card>
          </div>
        )}
      </section>
    </main>
  )
}