import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'
import {
  cancelCustomerBookingRequest,
  getCustomerBookingsRequest,
} from '../../../services/api/bookingsApi'
import type { CustomerBookingItemResponse } from '../../../services/api/types'
import { ApiClientError } from '../../../services/api/httpClient'

function formatDateTime(value: string) {
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

function isCancelable(status: string): boolean {
  const s = status.toUpperCase()
  return !['CANCELADA', 'CANCELADA_POR_CLIENTE', 'EXPIRADA', 'ATENDIDA', 'NO_ASISTE'].includes(s)
}

function statusBadge(status: string) {
  const s = status.toUpperCase()
  if (['CONFIRMED', 'CONFIRMADA'].includes(s)) return 'bg-emerald-100 text-emerald-800'
  if (['PENDING', 'PENDIENTE', 'PENDIENTE_CONFIRMACION', 'SOLICITADA', 'RESERVED', 'RESERVADA'].includes(s)) return 'bg-amber-100 text-amber-800'
  if (['CANCELADA', 'CANCELADA_POR_CLIENTE', 'CANCELLED', 'NO_SHOW', 'EXPIRADA'].includes(s)) return 'bg-red-100 text-red-800'
  if (['RESCHEDULED', 'REPROGRAMADA', 'REPROGRAMACION_PENDIENTE'].includes(s)) return 'bg-blue-100 text-blue-800'
  return 'bg-slate-100 text-slate-800'
}

export function BookingCancellationPage() {
  const { token } = useParams()
  const queryClient = useQueryClient()
  const [selectedBooking, setSelectedBooking] = useState<CustomerBookingItemResponse | null>(null)
  const [reason, setReason] = useState('')
  const [showConfirm, setShowConfirm] = useState(false)

  const bookingsQuery = useQuery({
    queryKey: ['customer-bookings', token],
    queryFn: () => getCustomerBookingsRequest(token ?? ''),
    enabled: Boolean(token),
    retry: false,
  })

  const cancelMutation = useMutation({
    mutationFn: async () => {
      if (!token || !selectedBooking) throw new Error('Selecciona una reserva.')
      return cancelCustomerBookingRequest(token, selectedBooking.bookingId, reason.trim() || undefined)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customer-bookings', token] })
      setSelectedBooking(null)
      setReason('')
      setShowConfirm(false)
    },
  })

  const bookings = bookingsQuery.data ?? []
  const isLoading = bookingsQuery.isPending
  const isError = bookingsQuery.isError

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,_#f8fafc_0%,_#fff1f0_100%)] px-4 py-8">
      <section className="mx-auto max-w-3xl space-y-5">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-red-700">Agenda centro estetico</p>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">Cancelacion de reserva</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Selecciona la reserva que deseas cancelar e ingresa el motivo.
          </p>
        </div>

        {isLoading ? (
          <Card className="text-center">
            <p className="text-sm font-medium text-slate-700">Cargando tus reservas...</p>
          </Card>
        ) : isError ? (
          <Card className="border-rose-200 bg-rose-50 text-center">
            <h2 className="text-xl font-semibold text-rose-900">No se pudieron cargar las reservas</h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              {bookingsQuery.error instanceof ApiClientError
                ? bookingsQuery.error.message
                : 'El enlace no es valido o expiro. Solicita uno nuevo.'}
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
          <div className="grid gap-4">
            {bookings.map((booking) => {
              const isSelected = selectedBooking?.bookingId === booking.bookingId
              const alreadyCancelled = !isCancelable(booking.status)
              return (
                <button
                  key={booking.bookingId}
                  className={[
                    'w-full rounded-2xl border bg-white p-5 text-left transition hover:shadow-sm',
                    isSelected
                      ? 'border-red-400 ring-4 ring-red-100'
                      : alreadyCancelled
                        ? 'border-slate-200 opacity-60'
                        : 'border-slate-200 hover:border-red-300',
                  ].join(' ')}
                  disabled={alreadyCancelled}
                  onClick={() => {
                    if (alreadyCancelled) return
                    setSelectedBooking(isSelected ? null : booking)
                    setShowConfirm(false)
                    setReason('')
                  }}
                  type="button"
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="text-base font-semibold text-slate-950">{booking.serviceName}</p>
                      <p className="mt-1 text-sm text-slate-600">{formatDateTime(booking.startsAt)}</p>
                      <p className="text-sm text-slate-500">
                        {booking.locationName ?? 'Sucursal por confirmar'}
                        {booking.professionalName ? ` · ${booking.professionalName}` : ''}
                      </p>
                    </div>
                    <span className={['rounded-full px-3 py-1 text-xs font-semibold', statusBadge(booking.status)].join(' ')}>
                      {booking.status}
                    </span>
                  </div>
                </button>
              )
            })}

            {selectedBooking && !cancelMutation.isSuccess ? (
              <Card className="space-y-5 border-red-200">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-red-700">Cancelar reserva</p>
                  <h2 className="mt-1 text-lg font-semibold text-slate-950">
                    {selectedBooking.serviceName}
                  </h2>
                  <p className="text-sm text-slate-600">{formatDateTime(selectedBooking.startsAt)}</p>
                </div>

                {!showConfirm ? (
                  <Textarea
                    label="Motivo de la cancelacion"
                    onChange={(event) => setReason(event.target.value)}
                    placeholder="Indica el motivo (obligatorio)"
                    rows={4}
                    value={reason}
                  />
                ) : null}

                {showConfirm ? (
                  <div className="rounded-2xl border border-red-200 bg-red-50 p-4">
                    <p className="text-sm font-semibold text-red-900">
                      ?Estas seguro de cancelar esta reserva?
                    </p>
                    <p className="mt-1 text-sm text-red-700">
                      {selectedBooking.serviceName} - {formatDateTime(selectedBooking.startsAt)}
                    </p>
                    {reason.trim() ? (
                      <p className="mt-2 text-sm text-red-600">Motivo: {reason}</p>
                    ) : null}
                  </div>
                ) : null}

                <div className="flex flex-wrap gap-3">
                  {!showConfirm ? (
                    <Button
                      disabled={!reason.trim()}
                      onClick={() => setShowConfirm(true)}
                    >
                      Confirmar cancelacion
                    </Button>
                  ) : (
                    <>
                      <Button
                        loading={cancelMutation.isPending}
                        onClick={() => cancelMutation.mutate()}
                        variant="danger"
                      >
                        {cancelMutation.isPending ? 'Cancelando...' : 'Si, cancelar reserva'}
                      </Button>
                      <Button
                        onClick={() => setShowConfirm(false)}
                        variant="secondary"
                      >
                        Volver
                      </Button>
                    </>
                  )}
                </div>

                {cancelMutation.isError ? (
                  <p className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                    {cancelMutation.error instanceof ApiClientError
                      ? cancelMutation.error.message
                      : 'No fue posible cancelar la reserva.'}
                  </p>
                ) : null}
              </Card>
            ) : null}

            {cancelMutation.isSuccess ? (
              <Card className="border-emerald-200 bg-emerald-50">
                <h2 className="text-xl font-semibold text-emerald-950">Reserva cancelada</h2>
                <p className="mt-2 text-sm text-emerald-800">
                  La reserva fue cancelada exitosamente. El cupo quedo liberado.
                </p>
              </Card>
            ) : null}
          </div>
        )}
      </section>
    </main>
  )
}