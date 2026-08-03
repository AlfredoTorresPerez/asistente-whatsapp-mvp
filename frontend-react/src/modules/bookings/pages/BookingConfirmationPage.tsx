import { useMutation, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { Textarea } from '../../../components/ui/Textarea'
import {
  cancelPublicBookingFromConfirmationRequest,
  confirmPublicBookingRequest,
  getPublicBookingConfirmationAvailabilityRequest,
  getPublicBookingConfirmationRequest,
  reschedulePublicBookingFromConfirmationRequest,
} from '../../../services/api/bookingsApi'
import { ApiClientError } from '../../../services/api/httpClient'
import type { AgendaSlotResponse } from '../../../services/api/types'

function formatDateTime(value: string) {
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

function formatTime(value: string) {
  return dayjs(value).format('HH:mm')
}

function normalizeStatus(status?: string | null) {
  return (status ?? '').toUpperCase()
}

function paymentStatusLabel(status?: string | null) {
  switch (normalizeStatus(status)) {
    case 'PAID':
    case 'APPROVED':
      return 'Pagado'
    case 'PENDING':
    case 'PENDING_PAYMENT':
      return 'Pendiente de pago'
    case 'REJECTED':
    case 'FAILED':
      return 'Pago rechazado'
    case 'REFUNDED':
      return 'Reembolsado'
    case '':
      return 'Sin informacion'
    default:
      return 'Estado por revisar'
  }
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError ? error.message : fallback
}

function isFinalStatus(status: string) {
  return [
    'CANCELLED',
    'CANCELADA',
    'COMPLETED',
    'ATTENDED',
    'ATENDIDA',
    'NO_SHOW',
    'EXPIRADA',
    'EXPIRED',
    'RELEASED',
    'LIBERADA',
  ].includes(normalizeStatus(status))
}

export function BookingConfirmationPage() {
  const { token } = useParams()
  const [actionMode, setActionMode] = useState<'none' | 'reschedule' | 'cancel'>('none')
  const [cancelReason, setCancelReason] = useState('')
  const [rescheduleDate, setRescheduleDate] = useState(dayjs().add(1, 'day').format('YYYY-MM-DD'))
  const [rescheduleReason, setRescheduleReason] = useState('')

  const confirmationQuery = useQuery({
    queryKey: ['public-booking-confirmation', token],
    queryFn: () => getPublicBookingConfirmationRequest(token ?? ''),
    enabled: Boolean(token),
    retry: false,
  })

  const availabilityQuery = useQuery({
    queryKey: ['public-booking-confirmation-availability', token, rescheduleDate],
    queryFn: () => getPublicBookingConfirmationAvailabilityRequest(token ?? '', rescheduleDate, 12),
    enabled: actionMode === 'reschedule' && Boolean(token) && Boolean(rescheduleDate),
    retry: false,
  })

  const confirmMutation = useMutation({
    mutationFn: () => confirmPublicBookingRequest(token ?? ''),
    onSuccess: async () => {
      setActionMode('none')
      await confirmationQuery.refetch()
    },
  })

  const cancelMutation = useMutation({
    mutationFn: () =>
      cancelPublicBookingFromConfirmationRequest(token ?? '', { reason: cancelReason.trim() }),
    onSuccess: async () => {
      setCancelReason('')
      setActionMode('none')
      await confirmationQuery.refetch()
    },
  })

  const rescheduleMutation = useMutation({
    mutationFn: (slot: AgendaSlotResponse) =>
      reschedulePublicBookingFromConfirmationRequest(token ?? '', {
        startsAt: slot.startsAt,
        professionalId: slot.professionalId,
        roomId: slot.roomId,
        reason: (rescheduleReason ?? '').trim() || undefined,
      }),
    onSuccess: async () => {
      setRescheduleReason('')
      setActionMode('none')
      await confirmationQuery.refetch()
    },
  })

  const data = confirmationQuery.data
  const bookingStatus = normalizeStatus(data?.bookingStatus)
  const linkStatus = normalizeStatus(data?.linkStatus)
  const confirmed = bookingStatus === 'CONFIRMED' || linkStatus === 'CONFIRMED'
  const paymentPending =
    Boolean(data?.requiresDeposit) &&
    !['PAID', 'APPROVED'].includes(normalizeStatus(data?.paymentStatus))
  const cancelled = ['CANCELLED', 'CANCELADA'].includes(bookingStatus)
  const rescheduled = ['RESCHEDULED', 'REPROGRAMADA'].includes(bookingStatus)
  const expired =
    ['EXPIRADA', 'EXPIRED', 'RELEASED', 'LIBERADA'].includes(bookingStatus) ||
    ['EXPIRED', 'INVALIDATED'].includes(linkStatus)
  const closed = data ? isFinalStatus(data.bookingStatus) : false
  const changeWindowClosed = data ? !dayjs(data.startsAt).isAfter(dayjs().add(24, 'hour')) : true
  const canManage = Boolean(data) && !closed && !expired && !changeWindowClosed
  const canConfirm =
    Boolean(data) &&
    !confirmed &&
    !closed &&
    !expired &&
    !paymentPending &&
    !confirmMutation.isPending
  const availableSlots = availabilityQuery.data?.slots.filter((slot) => slot.available) ?? []

  useEffect(() => {
    if (data?.startsAt) {
      setTimeout(
        () => setRescheduleDate(dayjs(data.startsAt).add(1, 'day').format('YYYY-MM-DD')),
        0,
      )
    }
  }, [data?.startsAt])

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(45,212,191,0.22),_transparent_38%),linear-gradient(180deg,_#f8fafc_0%,_#eef6f8_100%)] px-4 py-10">
      <section className="mx-auto max-w-2xl space-y-6">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-700">
            Asistente WhatsApp Centro Estetico
          </p>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">Confirmacion de reserva</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Revisa los datos de tu cita. Desde esta pagina tambien puedes cancelar o reprogramar si
            la politica de anticipacion lo permite.
          </p>
        </div>

        {confirmationQuery.isPending ? (
          <Card className="text-center">
            <p className="text-sm font-medium text-slate-700">Cargando datos de la reserva...</p>
          </Card>
        ) : confirmationQuery.isError || !data ? (
          <Card className="border-rose-200 bg-rose-50 text-center">
            <h2 className="text-xl font-semibold text-rose-900">Enlace no disponible</h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">
              El enlace no existe, ya expiro o fue invalidado. Solicita un nuevo horario por
              WhatsApp.
            </p>
          </Card>
        ) : (
          <Card className="space-y-6">
            <div className="rounded-2xl bg-teal-50 p-5">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-teal-700">
                Estado
              </p>
              <h2 className="mt-2 text-2xl font-semibold text-teal-950">
                {cancelled
                  ? 'Reserva cancelada'
                  : rescheduled
                    ? 'Reserva reprogramada'
                    : confirmed
                      ? 'Reserva confirmada'
                      : paymentPending
                        ? 'Reserva pendiente de pago'
                        : expired
                          ? 'Reserva expirada'
                          : 'Reserva pendiente de confirmacion'}
              </h2>
              <p className="mt-2 text-sm text-teal-800">
                {cancelled
                  ? 'El cupo fue liberado y el equipo recibio la trazabilidad.'
                  : rescheduled
                    ? 'Tu nueva fecha quedo registrada correctamente.'
                    : confirmed
                      ? 'Te esperamos en la fecha y hora indicada.'
                      : paymentPending
                        ? 'El cupo se confirma cuando el abono queda aprobado.'
                        : expired
                          ? 'Este enlace ya no permite confirmar. Pide una nueva disponibilidad por WhatsApp.'
                          : 'Confirma antes del vencimiento para mantener el cupo.'}
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Info label="Servicio" value={data.serviceName ?? data.subject} />
              <Info label="Cliente" value={`${data.customerName} (${data.maskedCustomerPhone})`} />
              <Info label="Fecha y hora" value={formatDateTime(data.startsAt)} />
              <Info label="Duracion" value={`${data.durationMinutes} minutos`} />
              <Info
                label="Sucursal"
                value={data.locationName ?? data.location ?? 'Sucursal por confirmar'}
              />
              <Info label="Profesional" value={data.professionalName ?? 'Por asignar'} />
              <Info label="Cabina" value={data.roomName ?? 'No requerida'} />
              <Info
                label="Abono"
                value={
                  data.requiresDeposit
                    ? `$${Number(data.depositAmount ?? 0).toLocaleString('es-CL')} · ${paymentStatusLabel(data.paymentStatus)}`
                    : 'No requerido'
                }
              />
              <Info label="Vence" value={formatDateTime(data.expiresAt)} />
            </div>

            {changeWindowClosed && !closed ? (
              <p className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                La cancelacion y la reprogramacion desde enlace publico quedan bloqueadas con menos
                de 24 horas de anticipacion.
              </p>
            ) : null}

            <div className="flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-end">
              <Button
                disabled={!canManage || actionMode === 'cancel'}
                onClick={() => setActionMode(actionMode === 'reschedule' ? 'none' : 'reschedule')}
                variant="secondary"
              >
                Reprogramar reserva
              </Button>
              <Button
                disabled={!canManage || actionMode === 'reschedule'}
                onClick={() => setActionMode(actionMode === 'cancel' ? 'none' : 'cancel')}
                variant="danger"
              >
                Cancelar reserva
              </Button>
              <Button disabled={!canConfirm} onClick={() => confirmMutation.mutate()}>
                {confirmMutation.isPending
                  ? 'Confirmando...'
                  : confirmed
                    ? 'Reserva confirmada'
                    : paymentPending
                      ? 'Pago pendiente'
                      : 'Confirmar reserva'}
              </Button>
            </div>

            {actionMode === 'cancel' ? (
              <div className="space-y-4 rounded-2xl border border-rose-100 bg-rose-50 p-4">
                <div>
                  <h3 className="text-base font-semibold text-rose-950">Cancelar reserva</h3>
                  <p className="mt-1 text-sm text-rose-700">
                    Indica el motivo. La accion quedara registrada como realizada por el cliente.
                  </p>
                </div>
                <Textarea
                  label="Motivo obligatorio"
                  onChange={(event) => setCancelReason(event.target.value)}
                  placeholder="Ejemplo: No podre asistir a la hora reservada."
                  rows={4}
                  value={cancelReason}
                />
                <div className="flex justify-end">
                  <Button
                    disabled={cancelReason.trim().length < 5 || cancelMutation.isPending}
                    onClick={() => cancelMutation.mutate()}
                    variant="danger"
                  >
                    {cancelMutation.isPending ? 'Cancelando...' : 'Confirmar cancelacion'}
                  </Button>
                </div>
              </div>
            ) : null}

            {actionMode === 'reschedule' ? (
              <div className="space-y-4 rounded-2xl border border-blue-100 bg-blue-50 p-4">
                <div>
                  <h3 className="text-base font-semibold text-blue-950">Reprogramar reserva</h3>
                  <p className="mt-1 text-sm text-blue-700">
                    Selecciona una nueva fecha. Se mantiene el mismo servicio y la misma sucursal.
                  </p>
                </div>
                <Input
                  label="Nueva fecha"
                  onChange={(event) => setRescheduleDate(event.target.value)}
                  type="date"
                  value={rescheduleDate}
                />
                <Textarea
                  label="Comentario opcional"
                  onChange={(event) => setRescheduleReason(event.target.value)}
                  placeholder="Ejemplo: Prefiero horario de tarde."
                  rows={3}
                  value={rescheduleReason}
                />
                <div className="rounded-2xl border border-blue-100 bg-white p-4">
                  <div className="flex items-center justify-between gap-3">
                    <h4 className="text-sm font-semibold text-slate-900">Horarios disponibles</h4>
                    <Button
                      loading={availabilityQuery.isFetching}
                      onClick={() => availabilityQuery.refetch()}
                      type="button"
                      variant="secondary"
                    >
                      Actualizar
                    </Button>
                  </div>
                  <div className="mt-3 grid gap-3 sm:grid-cols-2">
                    {availabilityQuery.isPending ? (
                      <p className="text-sm text-slate-500">Cargando disponibilidad...</p>
                    ) : availabilityQuery.isError ? (
                      <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                        {getErrorMessage(
                          availabilityQuery.error,
                          'No fue posible consultar horarios disponibles.',
                        )}
                      </p>
                    ) : availableSlots.length === 0 ? (
                      <p className="text-sm text-slate-500">
                        No hay horarios disponibles para la fecha seleccionada.
                      </p>
                    ) : (
                      availableSlots.map((slot) => (
                        <button
                          className="rounded-2xl border border-slate-200 bg-white p-4 text-left shadow-sm transition hover:border-blue-300 hover:shadow-md disabled:opacity-60"
                          disabled={rescheduleMutation.isPending}
                          key={`${slot.startsAt}-${slot.professionalId ?? 'sin-profesional'}-${slot.roomId ?? 'sin-cabina'}`}
                          onClick={() => rescheduleMutation.mutate(slot)}
                          type="button"
                        >
                          <strong className="text-slate-950">
                            {formatTime(slot.startsAt)} - {formatTime(slot.endsAt)}
                          </strong>
                          <p className="mt-2 text-sm text-slate-600">
                            {slot.professionalName ?? 'Profesional por asignar'}
                          </p>
                          <p className="text-sm text-slate-500">
                            {slot.roomName ?? 'Sin cabina requerida'}
                          </p>
                        </button>
                      ))
                    )}
                  </div>
                </div>
              </div>
            ) : null}

            {confirmMutation.isError || cancelMutation.isError || rescheduleMutation.isError ? (
              <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {getErrorMessage(
                  confirmMutation.error ?? cancelMutation.error ?? rescheduleMutation.error,
                  'No fue posible procesar la solicitud. Reintenta o solicita ayuda por WhatsApp.',
                )}
              </p>
            ) : null}
          </Card>
        )}
      </section>
    </main>
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
