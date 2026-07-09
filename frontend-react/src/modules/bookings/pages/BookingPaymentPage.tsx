import { useMutation, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useParams } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import {
  getPublicBookingPaymentDetailRequest,
  simulateBookingPaymentRequest,
} from '../../../services/api/bookingsApi'
import { ApiClientError } from '../../../services/api/httpClient'

function formatDateTime(value: string) {
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

function formatCurrency(amount: number, currency: string) {
  return new Intl.NumberFormat('es-CL', { style: 'currency', currency }).format(amount)
}

const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendiente de pago',
  APPROVED: 'Pago aprobado',
  REJECTED: 'Pago rechazado',
  EXPIRED: 'Pago expirado',
  REFUNDED: 'Pago reembolsado',
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-50 text-amber-900 border-amber-200',
  APPROVED: 'bg-teal-50 text-teal-900 border-teal-200',
  REJECTED: 'bg-rose-50 text-rose-900 border-rose-200',
  EXPIRED: 'bg-slate-50 text-slate-700 border-slate-200',
  REFUNDED: 'bg-blue-50 text-blue-900 border-blue-200',
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError ? error.message : fallback
}

export function BookingPaymentPage() {
  const { paymentId } = useParams()

  const paymentQuery = useQuery({
    queryKey: ['public-booking-payment', paymentId],
    queryFn: () => getPublicBookingPaymentDetailRequest(paymentId ?? ''),
    enabled: Boolean(paymentId),
    retry: false,
  })

  const simulateMutation = useMutation({
    mutationFn: (action: 'APPROVED' | 'REJECTED') =>
      simulateBookingPaymentRequest(paymentId ?? '', action),
    onSuccess: async () => {
      await paymentQuery.refetch()
    },
  })

  const data = paymentQuery.data
  const status = data?.status ?? ''
  const statusLabel = STATUS_LABELS[status] ?? status
  const statusColor = STATUS_COLORS[status] ?? 'bg-slate-50 text-slate-900 border-slate-200'
  const isPending = status === 'PENDING'
  const isTerminal = ['APPROVED', 'REJECTED', 'EXPIRED', 'REFUNDED'].includes(status)

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(45,212,191,0.22),_transparent_38%),linear-gradient(180deg,_#f8fafc_0%,_#eef6f8_100%)] px-4 py-10">
      <section className="mx-auto max-w-2xl space-y-6">
        <div className="text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-teal-700">Asistente WhatsApp Centro Estetico</p>
          <h1 className="mt-3 text-3xl font-semibold text-slate-950">Pago de reserva</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Revisa los datos del pago. Si la reserva requiere abono, puedes pagar desde esta pagina.
          </p>
        </div>

        {paymentQuery.isPending ? (
          <Card className="text-center"><p className="text-sm font-medium text-slate-700">Cargando datos del pago...</p></Card>
        ) : paymentQuery.isError || !data ? (
          <Card className="border-rose-200 bg-rose-50 text-center">
            <h2 className="text-xl font-semibold text-rose-900">Pago no encontrado</h2>
            <p className="mt-2 text-sm leading-6 text-rose-700">El enlace de pago no existe o fue invalidado. Solicita ayuda por WhatsApp.</p>
          </Card>
        ) : (
          <Card className="space-y-6">
            <div className={`rounded-2xl p-5 ${statusColor}`}>
              <p className="text-xs font-semibold uppercase tracking-[0.24em]">Estado</p>
              <h2 className="mt-2 text-2xl font-semibold">{statusLabel}</h2>
              {isPending && (
                <p className="mt-2 text-sm">El pago esta pendiente. Usa los botones de simulacion para probar el flujo.</p>
              )}
              {status === 'APPROVED' && (
                <p className="mt-2 text-sm">El pago fue aprobado correctamente. La reserva quedo confirmada.</p>
              )}
              {status === 'REJECTED' && (
                <p className="mt-2 text-sm">El pago fue rechazado. La reserva no sera confirmada hasta recibir el pago.</p>
              )}
              {status === 'EXPIRED' && (
                <p className="mt-2 text-sm">El enlace de pago expiro. Solicita un nuevo enlace por WhatsApp.</p>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Info label="Servicio" value={data.serviceName ?? data.subject} />
              <Info label="Cliente" value={data.customerName} />
              <Info label="Fecha y hora" value={formatDateTime(data.startsAt)} />
              <Info label="Duracion" value={`${data.durationMinutes} minutos`} />
              <Info label="Sucursal" value={data.locationName ?? 'Sucursal por confirmar'} />
              <Info label="Profesional" value={data.professionalName ?? 'Por asignar'} />
              <Info label="Monto" value={formatCurrency(data.amount, data.currency)} />
              <Info label="Estado del pago" value={statusLabel} />
              <Info label="Moneda" value={data.currency} />
              {data.checkoutExpiresAt && <Info label="Vence" value={formatDateTime(data.checkoutExpiresAt)} />}
            </div>

            {isPending && (
              <div className="flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-center">
                <Button
                  disabled={simulateMutation.isPending}
                  onClick={() => simulateMutation.mutate('APPROVED')}
                >
                  {simulateMutation.isPending ? 'Procesando...' : 'Simular pago aprobado'}
                </Button>
                <Button
                  disabled={simulateMutation.isPending}
                  onClick={() => simulateMutation.mutate('REJECTED')}
                  variant="danger"
                >
                  {simulateMutation.isPending ? 'Procesando...' : 'Simular pago rechazado'}
                </Button>
              </div>
            )}

            {isTerminal && (
              <div className="rounded-2xl bg-slate-50 p-4 text-center">
                <p className="text-sm text-slate-600">
                  {status === 'APPROVED'
                    ? 'El pago ya fue procesado. No se requieren acciones adicionales.'
                    : 'Este pago ya fue procesado. No se pueden realizar mas acciones.'}
                </p>
              </div>
            )}

            {simulateMutation.isError && (
              <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {getErrorMessage(simulateMutation.error, 'No fue posible procesar la simulacion del pago.')}
              </p>
            )}
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
