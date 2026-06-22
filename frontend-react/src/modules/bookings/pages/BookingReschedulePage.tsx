import { useMutation, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ApiClientError } from '../../../services/api/httpClient'
import {
  confirmPublicBookingRescheduleRequest,
  getPublicBookingRescheduleRequest,
  rejectPublicBookingRescheduleRequest,
} from '../../../services/api/bookingsApi'

function formatDateTime(value: string) {
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

export function BookingReschedulePage() {
  const { token } = useParams()
  const query = useQuery({
    queryKey: ['public-booking-reschedule', token],
    queryFn: () => getPublicBookingRescheduleRequest(token ?? ''),
    enabled: Boolean(token),
    retry: false,
  })
  const confirmMutation = useMutation({
    mutationFn: () => confirmPublicBookingRescheduleRequest(token ?? ''),
    onSuccess: async () => query.refetch(),
  })
  const rejectMutation = useMutation({
    mutationFn: () => rejectPublicBookingRescheduleRequest(token ?? ''),
    onSuccess: async () => query.refetch(),
  })

  const data = query.data
  const used = data?.linkStatus === 'USED' || data?.bookingStatus === 'REPROGRAMADA'
  const expired = data?.linkStatus === 'EXPIRED' || data?.linkStatus === 'CANCELLED'
  const rejected = data?.linkStatus === 'REJECTED'

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10">
      <section className="mx-auto max-w-2xl space-y-6">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-700">Agenda centro estetico</p>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">Reprogramacion de reserva</h1>
        </div>

        {query.isPending ? (
          <Card className="text-center"><p className="text-sm font-medium text-slate-700">Cargando propuesta...</p></Card>
        ) : query.isError || !data ? (
          <Card className="border-rose-200 bg-rose-50 text-center">
            <h2 className="text-xl font-semibold text-rose-900">Enlace no disponible</h2>
            <p className="mt-2 text-sm text-rose-700">Solicita una nueva propuesta por WhatsApp.</p>
          </Card>
        ) : (
          <Card className="space-y-6">
            <div className="rounded-xl bg-teal-50 p-5">
              <h2 className="text-2xl font-semibold text-teal-950">
                {used ? 'Reserva reprogramada' : expired ? 'Enlace vencido' : rejected ? 'Cambio rechazado' : 'Confirma la nueva fecha'}
              </h2>
              <p className="mt-2 text-sm text-teal-800">
                {used ? 'Tu agenda ya fue actualizada.' : expired ? 'Este enlace ya no esta activo.' : rejected ? 'Mantendremos la fecha anterior.' : 'Revisa la fecha actual y la nueva propuesta.'}
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Info label="Cliente" value={`${data.customerName} (${data.maskedCustomerPhone})`} />
              <Info label="Servicio" value={data.serviceName ?? data.subject} />
              <Info label="Fecha actual" value={formatDateTime(data.currentStartsAt)} />
              <Info label="Nueva fecha" value={formatDateTime(data.proposedStartsAt)} />
              <Info label="Sucursal actual" value={data.currentLocationName ?? 'Por confirmar'} />
              <Info label="Nueva sucursal" value={data.proposedLocationName ?? data.currentLocationName ?? 'Por confirmar'} />
              <Info label="Profesional" value={data.proposedProfessionalName ?? data.currentProfessionalName ?? 'Por asignar'} />
              <Info label="Cabina" value={data.proposedRoomName ?? data.currentRoomName ?? 'No requerida'} />
            </div>

            {data.reason ? <p className="rounded-xl bg-slate-100 p-4 text-sm text-slate-700">{data.reason}</p> : null}

            <div className="flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-end">
              <Button disabled={used || expired || rejected || rejectMutation.isPending} onClick={() => rejectMutation.mutate()} variant="secondary">
                Rechazar cambio
              </Button>
              <Button disabled={used || expired || rejected || confirmMutation.isPending} onClick={() => confirmMutation.mutate()}>
                {confirmMutation.isPending ? 'Confirmando...' : 'Confirmar reprogramacion'}
              </Button>
            </div>

            {confirmMutation.isError || rejectMutation.isError ? (
              <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {(confirmMutation.error ?? rejectMutation.error) instanceof ApiClientError
                  ? (confirmMutation.error ?? rejectMutation.error)?.message
                  : 'No fue posible procesar la solicitud.'}
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
