import { useMutation, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { Select } from '../../../components/ui/Select'
import { Textarea } from '../../../components/ui/Textarea'
import {
  cancelCustomerBookingRequest,
  getCustomerBookingReschedulePreviewRequest,
  getCustomerBookingsRequest,
  rescheduleCustomerBookingRequest,
} from '../../../services/api/bookingsApi'
import { ApiClientError } from '../../../services/api/httpClient'
import type { CustomerBookingItemResponse } from '../../../services/api/types'
import type { UseMutationResult } from '@tanstack/react-query'

type RescheduleDraft = {
  serviceId: string
  locationId: string
  startsAt: string
  reason: string
}

function formatDateTime(value: string) {
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

function formatDateInput(value: string) {
  return dayjs(value).format('YYYY-MM-DDTHH:mm')
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError ? error.message : fallback
}

function statusTone(status: string) {
  const s = status.toUpperCase()
  if (['CONFIRMED', 'CONFIRMADA'].includes(s)) return 'bg-emerald-100 text-emerald-800'
  if (['PENDING', 'PENDIENTE', 'RESERVED', 'RESERVADA'].includes(s)) return 'bg-amber-100 text-amber-800'
  if (['CANCELADA', 'CANCELLED', 'NO_SHOW', 'EXPIRADA'].includes(s)) return 'bg-red-100 text-red-800'
  if (['RESCHEDULED', 'REPROGRAMADA'].includes(s)) return 'bg-blue-100 text-blue-800'
  return 'bg-slate-100 text-slate-800'
}

export function CustomerBookingsPage() {
  const { token } = useParams()
  const [cancellingId, setCancellingId] = useState<string | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const [selectedBookingId, setSelectedBookingId] = useState<string | null>(null)
  const [draft, setDraft] = useState<RescheduleDraft | null>(null)

  const bookingsQuery = useQuery({
    queryKey: ['customer-bookings', token],
    queryFn: () => getCustomerBookingsRequest(token ?? ''),
    enabled: Boolean(token),
    retry: false,
  })

  const selectedBooking = useMemo(
    () => bookingsQuery.data?.find((booking) => booking.bookingId === selectedBookingId) ?? null,
    [bookingsQuery.data, selectedBookingId],
  )

  const previewQuery = useQuery({
    queryKey: ['customer-booking-reschedule-preview', token, selectedBookingId],
    queryFn: () => getCustomerBookingReschedulePreviewRequest(token ?? '', selectedBookingId ?? ''),
    enabled: Boolean(token && selectedBookingId),
    retry: false,
  })

  useEffect(() => {
    const preview = previewQuery.data
    if (!preview) {
      return
    }

    const booking = preview.booking
    setDraft({
      serviceId: booking.serviceId ?? preview.services[0]?.id ?? '',
      locationId: booking.locationId ?? preview.locations[0]?.id ?? '',
      startsAt: formatDateInput(booking.startsAt),
      reason: '',
    })
  }, [previewQuery.data])

  const cancelMutation = useMutation({
    mutationFn: (bookingId: string) =>
      cancelCustomerBookingRequest(token ?? '', bookingId, (cancelReason ?? '').trim() || undefined),
    onSuccess: async () => {
      setCancellingId(null)
      setCancelReason('')
      await bookingsQuery.refetch()
    },
  })

  const rescheduleMutation = useMutation({
    mutationFn: async () => {
      if (!token || !selectedBookingId || !draft || !selectedBooking) {
        throw new Error('No hay reserva seleccionada.')
      }

      if (!draft.serviceId || !draft.locationId || !draft.startsAt) {
        throw new Error('Completa el servicio, la sucursal y la nueva fecha.')
      }

      const startsAt = dayjs(draft.startsAt)
      if (!startsAt.isValid()) {
        throw new Error('La nueva fecha y hora no es valida.')
      }

      return rescheduleCustomerBookingRequest(token, selectedBookingId, {
        serviceId: draft.serviceId,
        locationId: draft.locationId,
        startsAt: startsAt.toISOString(),
        date: startsAt.format('YYYY-MM-DD'),
        reason: (draft.reason ?? '').trim() || undefined,
      })
    },
    onSuccess: async () => {
      setSelectedBookingId(null)
      setDraft(null)
      await bookingsQuery.refetch()
    },
  })

  const bookings = bookingsQuery.data ?? []
  const preview = previewQuery.data
  const serviceOptions = preview?.services ?? []
  const locationOptions = preview?.locations ?? []

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(45,212,191,0.22),_transparent_38%),linear-gradient(180deg,_#f8fafc_0%,_#eef6f8_100%)] px-4 py-10">
      <section className="mx-auto max-w-4xl space-y-6">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-700">Asistente WhatsApp Centro Estetico</p>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">Mis reservas activas</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Aqui se muestran las reservas futuras asociadas a tu numero de celular. Puedes cancelar una reserva o abrirla para
            reprogramar su fecha, hora, sucursal o servicio.
          </p>
        </div>

        {bookingsQuery.isPending ? (
          <Card className="text-center">
            <p className="text-sm font-medium text-slate-700">Cargando tus reservas...</p>
          </Card>
        ) : bookingsQuery.isError ? (
          <Card className="border-rose-200 bg-rose-50 text-center">
            <h2 className="text-xl font-semibold text-rose-900">Enlace no disponible</h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {getErrorMessage(bookingsQuery.error, 'El enlace no es valido o expiro. Solicita uno nuevo por WhatsApp.')}
            </p>
          </Card>
        ) : bookings.length === 0 ? (
          <Card className="border-teal-200 bg-teal-50 text-center">
            <h2 className="text-xl font-semibold text-teal-900">No tienes reservas activas</h2>
            <p className="mt-2 text-sm leading-6 text-teal-700">
              No encontramos reservas futuras asociadas a tu numero de telefono. Si crees que esto es un error, contactanos
              por WhatsApp.
            </p>
          </Card>
        ) : (
          <div className="space-y-4">
            <div className="grid gap-4">
              {bookings.map((booking) => (
                <BookingCard
                  key={booking.bookingId}
                  booking={booking}
                  isSelected={selectedBookingId === booking.bookingId}
                  onCancel={() => {
                    setSelectedBookingId(null)
                    setDraft(null)
                    setCancellingId((current) => (current === booking.bookingId ? null : booking.bookingId))
                  }}
                  onSelect={() => {
                    setCancellingId(null)
                    setCancelReason('')
                    setSelectedBookingId(booking.bookingId)
                  }}
                  cancellingId={cancellingId}
                  cancelMutation={cancelMutation}
                  cancelReason={cancelReason}
                  setCancelReason={setCancelReason}
                />
              ))}
            </div>

            {selectedBooking ? (
              <Card className="space-y-5 border-teal-200 bg-teal-50/40">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-teal-700">Reprogramacion</p>
                  <h2 className="mt-1 text-2xl font-semibold text-slate-950">Editar reserva seleccionada</h2>
                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    Modifica el servicio, la sucursal o la nueva fecha y hora. El sistema validara la disponibilidad al
                    guardar.
                  </p>
                </div>

                {previewQuery.isPending ? (
                  <div className="rounded-2xl border border-slate-200 bg-white p-4 text-center">
                    <p className="text-sm font-medium text-slate-700">Cargando datos de reprogramacion...</p>
                  </div>
                ) : previewQuery.isError || !preview ? (
                  <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-center">
                    <h3 className="text-lg font-semibold text-rose-900">No fue posible abrir la reserva</h3>
                    <p className="mt-2 text-sm text-rose-700">
                      {getErrorMessage(
                        previewQuery.error,
                        'No pudimos cargar los datos necesarios para reprogramar esta reserva.',
                      )}
                    </p>
                  </div>
                ) : (
                  <div className="space-y-5">
                    <div className="grid gap-3 sm:grid-cols-2">
                      <Info label="Cliente" value={`${preview.booking.customerName} (${preview.booking.maskedPhone})`} />
                      <Info label="Servicio actual" value={preview.booking.serviceName} />
                      <Info label="Fecha actual" value={formatDateTime(preview.booking.startsAt)} />
                      <Info label="Sucursal actual" value={preview.booking.locationName ?? 'Por confirmar'} />
                      <Info label="Profesional" value={preview.booking.professionalName ?? 'Por asignar'} />
                      <Info label="Duracion" value={`${preview.booking.durationMinutes} minutos`} />
                    </div>

                    <div className="grid gap-5 md:grid-cols-2">
                      <Select
                        label="Servicio"
                        value={draft?.serviceId ?? ''}
                        onChange={(event) =>
                          setDraft((current) =>
                            current
                              ? { ...current, serviceId: event.target.value }
                              : {
                                  serviceId: event.target.value,
                                  locationId: preview.booking.locationId ?? locationOptions[0]?.id ?? '',
                                  startsAt: formatDateInput(preview.booking.startsAt),
                                  reason: '',
                                },
                          )
                        }
                        options={serviceOptions.map((service) => ({
                          label: `${service.name} (${service.categoryName})`,
                          value: service.id,
                        }))}
                      />
                      <Select
                        label="Sucursal"
                        value={draft?.locationId ?? ''}
                        onChange={(event) =>
                          setDraft((current) =>
                            current
                              ? { ...current, locationId: event.target.value }
                              : {
                                  serviceId: preview.booking.serviceId ?? serviceOptions[0]?.id ?? '',
                                  locationId: event.target.value,
                                  startsAt: formatDateInput(preview.booking.startsAt),
                                  reason: '',
                                },
                          )
                        }
                        options={locationOptions.map((location) => ({
                          label: location.commune ? `${location.name} - ${location.commune}` : location.name,
                          value: location.id,
                        }))}
                      />
                    </div>

                    <div className="grid gap-5 md:grid-cols-2">
                      <Input
                        label="Nueva fecha y hora"
                        type="datetime-local"
                        value={draft?.startsAt ?? ''}
                        onChange={(event) =>
                          setDraft((current) =>
                            current
                              ? { ...current, startsAt: event.target.value }
                              : {
                                  serviceId: preview.booking.serviceId ?? serviceOptions[0]?.id ?? '',
                                  locationId: preview.booking.locationId ?? locationOptions[0]?.id ?? '',
                                  startsAt: event.target.value,
                                  reason: '',
                                },
                          )
                        }
                      />
                      <Textarea
                        label="Motivo de reprogramacion"
                        onChange={(event) =>
                          setDraft((current) =>
                            current
                              ? { ...current, reason: event.target.value }
                              : {
                                  serviceId: preview.booking.serviceId ?? serviceOptions[0]?.id ?? '',
                                  locationId: preview.booking.locationId ?? locationOptions[0]?.id ?? '',
                                  startsAt: formatDateInput(preview.booking.startsAt),
                                  reason: event.target.value,
                                },
                          )
                        }
                        placeholder="Opcional"
                        rows={4}
                        value={draft?.reason ?? ''}
                      />
                    </div>

                    <div className="flex flex-col gap-3 border-t border-teal-200 pt-5 sm:flex-row sm:justify-end">
                      <Button
                        onClick={() => {
                          setSelectedBookingId(null)
                          setDraft(null)
                        }}
                        variant="secondary"
                      >
                        Cerrar
                      </Button>
                      <Button
                        disabled={!draft?.serviceId || !draft?.locationId || !draft?.startsAt}
                        loading={rescheduleMutation.isPending}
                        onClick={() => rescheduleMutation.mutate()}
                      >
                        Reprogramar reserva
                      </Button>
                    </div>

                    {rescheduleMutation.isError ? (
                      <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                        {getErrorMessage(
                          rescheduleMutation.error,
                          'No fue posible reprogramar la reserva. Revisa la fecha y vuelve a intentarlo.',
                        )}
                      </p>
                    ) : null}
                  </div>
                )}
              </Card>
            ) : (
              <Card className="border-teal-200 bg-teal-50">
                <p className="text-sm text-teal-800">
                  Selecciona una reserva para abrir el formulario de reprogramacion.
                </p>
              </Card>
            )}
          </div>
        )}

        {cancelMutation.isError && (
          <Card className="border-rose-200 bg-rose-50">
            <p className="text-sm text-rose-700">
              {getErrorMessage(
                cancelMutation.error,
                'No fue posible cancelar la reserva. Intenta de nuevo o contactanos por WhatsApp.',
              )}
            </p>
          </Card>
        )}
      </section>
    </main>
  )
}

function BookingCard({
  booking,
  isSelected,
  onSelect,
  onCancel,
  cancellingId,
  cancelMutation,
  cancelReason,
  setCancelReason,
}: {
  booking: CustomerBookingItemResponse
  isSelected: boolean
  onSelect: () => void
  onCancel: () => void
  cancellingId: string | null
  cancelMutation: UseMutationResult<{ status: string; bookingId: string }, Error, string, unknown>
  cancelReason: string
  setCancelReason: (value: string) => void
}) {
  const isCancelling = cancellingId === booking.bookingId

  return (
    <Card className={isSelected ? 'border-teal-300 bg-teal-50/60' : ''}>
      <div className="space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div className="space-y-2">
            <h2 className="text-xl font-semibold text-slate-950">{booking.serviceName}</h2>
            <span
              className={[
                'inline-block rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide',
                statusTone(booking.status),
              ].join(' ')}
            >
              {booking.status}
            </span>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button onClick={onSelect} variant={isSelected ? 'secondary' : 'secondary'}>
              {isSelected ? 'Editando' : 'Reprogramar'}
            </Button>
            <Button
              variant="danger"
              onClick={onCancel}
            >
              {isCancelling ? 'Cerrar cancelacion' : 'Cancelar reserva'}
            </Button>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <Info label="Cliente" value={`${booking.customerName} (${booking.maskedPhone})`} />
          <Info label="Fecha y hora" value={formatDateTime(booking.startsAt)} />
          <Info label="Duracion" value={`${booking.durationMinutes} minutos`} />
          <Info label="Sucursal" value={booking.locationName ?? 'Por confirmar'} />
          <Info label="Profesional" value={booking.professionalName ?? 'Por asignar'} />
          <Info label="Reserva" value={booking.bookingId} />
        </div>

        {isCancelling ? (
          <div className="space-y-4 rounded-2xl border border-rose-100 bg-rose-50 p-4">
            <div>
              <h3 className="text-base font-semibold text-rose-950">Cancelar reserva</h3>
              <p className="mt-1 text-sm text-rose-700">Indica el motivo (opcional).</p>
            </div>
            <Textarea
              label="Motivo"
              onChange={(event) => setCancelReason(event.target.value)}
              placeholder="Ejemplo: No podre asistir."
              rows={3}
              value={cancelReason}
            />
            <div className="flex justify-end">
              <Button
                disabled={cancelMutation.isPending}
                onClick={() => cancelMutation.mutate(booking.bookingId)}
                variant="danger"
              >
                {cancelMutation.isPending ? 'Cancelando...' : 'Confirmar cancelacion'}
              </Button>
            </div>
          </div>
        ) : null}
      </div>
    </Card>
  )
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-950">{value}</p>
    </div>
  )
}
