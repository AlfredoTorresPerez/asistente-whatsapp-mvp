import { useMutation, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'
import {
  confirmPublicBookingCancellationRequest,
  getPublicBookingCancellationRequest,
} from '../../../services/api/bookingsApi'
import { ApiClientError } from '../../../services/api/httpClient'

function formatDateTime(value: string) {
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

export function BookingCancellationPage() {
  const { token } = useParams()
  const [reason, setReason] = useState('')
  const query = useQuery({
    queryKey: ['public-booking-cancellation', token],
    queryFn: () => getPublicBookingCancellationRequest(token ?? ''),
    enabled: Boolean(token),
    retry: false,
  })
  const confirmMutation = useMutation({
    mutationFn: () => confirmPublicBookingCancellationRequest(token ?? '', reason || undefined),
    onSuccess: async () => query.refetch(),
  })

  const data = query.data
  const cancelled = data?.bookingStatus === 'CANCELADA' || data?.linkStatus === 'USED'
  const expired = data?.linkStatus === 'EXPIRED' || data?.linkStatus === 'CANCELLED'

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10">
      <section className="mx-auto max-w-2xl space-y-6">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-700">Agenda centro estetico</p>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">Cancelacion de reserva</h1>
        </div>

        {query.isPending ? (
          <Card className="text-center"><p className="text-sm font-medium text-slate-700">Cargando reserva...</p></Card>
        ) : query.isError || !data ? (
          <Card className="border-rose-200 bg-rose-50 text-center">
            <h2 className="text-xl font-semibold text-rose-900">Enlace no disponible</h2>
            <p className="mt-2 text-sm text-rose-700">Solicita un nuevo enlace por WhatsApp.</p>
          </Card>
        ) : (
          <Card className="space-y-6">
            <div className="rounded-xl bg-rose-50 p-5">
              <h2 className="text-2xl font-semibold text-rose-950">
                {cancelled ? 'Reserva cancelada' : expired ? 'Enlace vencido' : 'Confirma la cancelacion'}
              </h2>
              <p className="mt-2 text-sm text-rose-800">
                {cancelled ? 'El cupo fue liberado.' : expired ? 'Este enlace ya no esta activo.' : 'Al confirmar, la cita queda cancelada.'}
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Info label="Cliente" value={`${data.customerName} (${data.maskedCustomerPhone})`} />
              <Info label="Servicio" value={data.serviceName ?? data.subject} />
              <Info label="Fecha y hora" value={formatDateTime(data.startsAt)} />
              <Info label="Sucursal" value={data.locationName ?? 'Por confirmar'} />
              <Info label="Profesional" value={data.professionalName ?? 'Por asignar'} />
              <Info label="Cabina" value={data.roomName ?? 'No requerida'} />
            </div>

            {!cancelled && !expired ? (
              <Textarea
                label="Motivo opcional"
                onChange={(event) => setReason(event.target.value)}
                rows={4}
                value={reason}
              />
            ) : null}

            <div className="flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-end">
              <Button disabled={cancelled || expired || confirmMutation.isPending} onClick={() => confirmMutation.mutate()} variant="danger">
                {confirmMutation.isPending ? 'Cancelando...' : cancelled ? 'Reserva cancelada' : 'Confirmar cancelacion'}
              </Button>
            </div>

            {confirmMutation.isError ? (
              <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {confirmMutation.error instanceof ApiClientError
                  ? confirmMutation.error.message
                  : 'No fue posible cancelar la reserva.'}
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
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-950">{value}</p>
    </div>
  )
}
