import { useMutation, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { formatEstadoPago, getEstadoTone } from '../../../lib/statusFormatters'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  cancelBookingRequest,
  createBookingCancellationLinkRequest,
  createBookingConfirmationLinkRequest,
  createBookingPaymentLinkRequest,
  getBookingDetailRequest,
  refundBookingPaymentRequest,
  registerBookingManualPaymentRequest,
} from '../../../services/api/bookingsApi'
import type { CreateBookingPaymentLinkRequest } from '../../../services/api/types'
import { getBookingStatusLabel, getBookingStatusTone } from '../bookingOptions'

function formatDateTime(value: string | null) {
  return value ? dayjs(value).format('DD MMM YYYY HH:mm') : 'Sin registro'
}

export function AppointmentDetailPage() {
  const { appointmentId } = useParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false)
  const [confirmationUrl, setConfirmationUrl] = useState<string | null>(null)
  const [publicActionUrl, setPublicActionUrl] = useState<string | null>(null)
  const [paymentUrl, setPaymentUrl] = useState<string | null>(null)
  const [paymentPurpose, setPaymentPurpose] = useState<'DEPOSIT' | 'FULL' | 'MANUAL'>('DEPOSIT')
  const [manualAmount, setManualAmount] = useState('')
  const [manualReference, setManualReference] = useState('')
  const [fullAmount, setFullAmount] = useState('')

  const bookingQuery = useQuery({
    queryKey: ['bookings', 'detail', appointmentId],
    queryFn: () => getBookingDetailRequest(appointmentId ?? ''),
    enabled: Boolean(appointmentId),
    refetchInterval: isOnline ? 30_000 : false,
  })


  const confirmationLinkMutation = useMutation({
    mutationFn: async () => {
      if (!appointmentId) {
        throw new Error('No hay cita seleccionada.')
      }
      return createBookingConfirmationLinkRequest(appointmentId, { expirationMinutes: 30, sendWhatsApp: true })
    },
    onSuccess: async (response) => {
      setConfirmationUrl(response.confirmationUrl)
      await bookingQuery.refetch()
      showToast({
        title: response.status === 'SENT' ? 'Enlace enviado por WhatsApp' : 'Enlace generado',
        description: response.status === 'SENT'
          ? 'El cliente recibio el enlace de confirmacion por WhatsApp.'
          : 'El enlace quedo generado, pero no se pudo confirmar el envio automatico por WhatsApp.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo generar el enlace',
        description: 'Verifica que la cita tenga sucursal, fecha y horario disponible.',
        tone: 'error',
      })
    },
  })

  const cancelMutation = useMutation({
    mutationFn: async () => {
      if (!appointmentId) {
        throw new Error('No hay cita seleccionada.')
      }
      return cancelBookingRequest(appointmentId, {
        reason: 'Cancelada manualmente desde el detalle de agenda.',
      })
    },
    onSuccess: async () => {
      setCancelDialogOpen(false)
      await bookingQuery.refetch()
      showToast({
        title: 'Cita cancelada',
        description: 'La cita quedo marcada como cancelada en la agenda.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo cancelar la cita',
        description: 'Reintenta nuevamente en unos segundos.',
        tone: 'error',
      })
    },
  })

  const cancellationLinkMutation = useMutation({
    mutationFn: async () => {
      if (!appointmentId) {
        throw new Error('No hay cita seleccionada.')
      }
      return createBookingCancellationLinkRequest(appointmentId, {
        reason: 'Solicitud de cancelacion iniciada desde detalle de cita.',
        expirationMinutes: 720,
        sendWhatsApp: true,
        sendEmail: true,
      })
    },
    onSuccess: async (response) => {
      setPublicActionUrl(response.publicUrl)
      await bookingQuery.refetch()
      showToast({
        title: 'Enlace de cancelacion enviado',
        description: 'El cliente puede confirmar la cancelacion desde la pagina publica.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo generar el enlace',
        description: 'Verifica que la cita siga activa.',
        tone: 'error',
      })
    },
  })

  const paymentLinkMutation = useMutation({
    mutationFn: async () => {
      if (!appointmentId) {
        throw new Error('No hay cita seleccionada.')
      }
      const payload: CreateBookingPaymentLinkRequest = {
        paymentPurpose,
        expirationMinutes: 30,
        sendWhatsApp: true,
        sendEmail: true,
      }
      if (paymentPurpose === 'FULL' && fullAmount) {
        payload.amount = Number(fullAmount)
      }
      return createBookingPaymentLinkRequest(appointmentId, payload)
    },
    onSuccess: async (response) => {
      setPaymentUrl(response.checkoutUrl)
      await bookingQuery.refetch()
      showToast({
        title: 'Enlace de pago generado',
        description: 'El link quedo asociado a la reserva y listo para compartir.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo generar el pago',
        description: 'Verifica que la reserva requiera abono y siga activa.',
        tone: 'error',
      })
    },
  })

  const manualPaymentMutation = useMutation({
    mutationFn: async () => {
      if (!appointmentId) {
        throw new Error('No hay cita seleccionada.')
      }
      const amount = Number(manualAmount || bookingQuery.data?.depositAmount || 0)
      return registerBookingManualPaymentRequest(appointmentId, {
        amount,
        currency: 'CLP',
        provider: 'MANUAL',
        providerPaymentId: manualReference.trim() || undefined,
        idempotencyKey: manualReference.trim() ? `manual:${appointmentId}:${manualReference.trim()}` : undefined,
        status: 'APPROVED',
        notes: 'Pago registrado desde detalle de cita.',
      })
    },
    onSuccess: async () => {
      setManualAmount('')
      setManualReference('')
      await bookingQuery.refetch()
      showToast({
        title: 'Pago registrado',
        description: 'El abono quedo auditado en la reserva.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo registrar el pago',
        description: 'Revisa el monto y la referencia del pago.',
        tone: 'error',
      })
    },
  })

  const refundPaymentMutation = useMutation({
    mutationFn: async (paymentId: string) => {
      if (!appointmentId) {
        throw new Error('No hay cita seleccionada.')
      }
      return refundBookingPaymentRequest(appointmentId, paymentId, {
        reason: 'Reembolso registrado desde detalle de cita.',
      })
    },
    onSuccess: async () => {
      await bookingQuery.refetch()
      showToast({
        title: 'Pago reembolsado',
        description: 'El estado del pago quedo actualizado y auditado.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo reembolsar',
        description: 'Solo los pagos aprobados pueden marcarse como reembolsados.',
        tone: 'error',
      })
    },
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <div className="flex flex-wrap gap-3">
            <Button onClick={() => navigate('/appointments')} variant="secondary">
              Volver a agenda
            </Button>
            {bookingQuery.data ? (
              <>
                <Button
                  disabled={confirmationLinkMutation.isPending || bookingQuery.data.status === 'CONFIRMED'}
                  onClick={() => confirmationLinkMutation.mutate()}
                  variant="secondary"
                >
                  Enlace confirmacion
                </Button>
                <Button
                  disabled={
                    cancellationLinkMutation.isPending
                    || bookingQuery.data.status === 'CANCELADA'
                    || bookingQuery.data.status === 'CANCELLED'
                  }
                  onClick={() => cancellationLinkMutation.mutate()}
                  variant="secondary"
                >
                  Enlace cancelacion
                </Button>
                <Button onClick={() => navigate(`/appointments/${bookingQuery.data.id}/edit`)}>
                  Editar cita
                </Button>
                <Button
                  onClick={() => navigate(`/appointments/${bookingQuery.data.id}/reschedule`)}
                  variant="secondary"
                >
                  Reprogramar
                </Button>
              </>
            ) : null}
          </div>
        }
        description="Detalle operativo de la cita con enlaces al cliente, conversación o prospecto cuando existan."
        eyebrow="Detalle de cita"
        title="Cita"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Estado sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            La ficha sigue visible, pero las acciones de edición, reprogramación o cancelación quedan bloqueadas.
          </p>
        </Card>
      ) : null}

      {confirmationUrl ? (
        <Card className="border-teal-200 bg-teal-50">
          <p className="text-sm font-semibold text-teal-950">Enlace de confirmacion generado o enviado</p>
          <p className="mt-2 break-all text-sm text-teal-800">{confirmationUrl}</p>
        </Card>
      ) : null}

      {publicActionUrl ? (
        <Card className="border-sky-200 bg-sky-50">
          <p className="text-sm font-semibold text-sky-950">Enlace publico generado o enviado</p>
          <p className="mt-2 break-all text-sm text-sky-800">{publicActionUrl}</p>
        </Card>
      ) : null}

      {bookingQuery.isPending ? (
        <LoadingState message="Cargando el detalle de la cita seleccionada." variant="detail" />
      ) : bookingQuery.isError || !bookingQuery.data ? (
        <ErrorState
          description="No pudimos recuperar la cita solicitada."
          onRetry={() => void bookingQuery.refetch()}
          title="No fue posible abrir la cita"
        />
      ) : (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
          <div className="space-y-6">
            <Card className="border-emerald-100 bg-emerald-50/80">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-700">Sucursal seleccionada</p>
                  <h2 className="mt-2 text-2xl font-semibold text-emerald-950">
                    {bookingQuery.data.locationName ?? bookingQuery.data.location ?? 'Sin sucursal definida'}
                  </h2>
                  <p className="mt-1 text-sm text-emerald-800">
                    La disponibilidad y el profesional deben validarse contra esta sucursal.
                  </p>
                </div>
                <StatusBadge
                  label={bookingQuery.data.locationName ? 'Sucursal asignada' : 'Pendiente'}
                  tone={bookingQuery.data.locationName ? 'success' : 'warning'}
                />
              </div>
            </Card>

            <Card className="space-y-6">
            <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[var(--color-border)] pb-6">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
                  Cliente
                </p>
                <h2 className="mt-2 text-[30px] font-semibold text-[var(--color-text)]">
                  {bookingQuery.data.customerName}
                </h2>
                <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
                  {bookingQuery.data.customerPhone} · {bookingQuery.data.customerEmail ?? 'Sin correo'}
                </p>
              </div>

              <StatusBadge
                label={getBookingStatusLabel(bookingQuery.data.status)}
                tone={getBookingStatusTone(bookingQuery.data.status)}
              />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <InfoCard label="Asunto" value={bookingQuery.data.subject} />
              <InfoCard label="Responsable" value={bookingQuery.data.assignedUserName ?? 'Sin asignar'} />
              <InfoCard label="Inicio" value={formatDateTime(bookingQuery.data.startsAt)} />
              <InfoCard label="Duracion" value={`${bookingQuery.data.durationMinutes} minutos`} />
              <InfoCard label="Sucursal" value={bookingQuery.data.locationName ?? bookingQuery.data.location ?? 'Sin sucursal definida'} />
              <InfoCard label="Actualizada" value={formatDateTime(bookingQuery.data.updatedAt)} />
            </div>

            <Card className="space-y-3 bg-slate-50">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                Notas
              </p>
              <p className="text-sm leading-6 text-[var(--color-text-secondary)]">
                {bookingQuery.data.notes ?? 'Sin notas registradas para esta cita.'}
              </p>
            </Card>

            <div className="flex flex-wrap justify-end gap-3">
              {bookingQuery.data.status !== 'CANCELLED' && bookingQuery.data.status !== 'CANCELADA' ? (
                <Button
                  disabled={!isOnline}
                  onClick={() => setCancelDialogOpen(true)}
                  variant="danger"
                >
                  Cancelar cita
                </Button>
              ) : null}
            </div>
          </Card>

          <Card className="space-y-4">
            <h3 className="text-xl font-semibold text-[var(--color-text)]">Trazabilidad operativa</h3>
            <TraceSection
              empty="No hay enlaces publicos registrados."
              items={(bookingQuery.data.publicLinks ?? []).map((link) => ({
                id: link.id,
                title: `${link.type} · ${link.status}`,
                detail: `${formatDateTime(link.expiresAt)} · ${link.url}`,
              }))}
              title="Enlaces publicos"
            />
            <TraceSection
              empty="No hay recordatorios programados."
              items={(bookingQuery.data.reminders ?? []).map((reminder) => ({
                id: reminder.id,
                title: `${reminder.reminderType} · ${reminder.channelType} · ${reminder.status}`,
                detail: `${formatDateTime(reminder.scheduledAt)}${reminder.sentAt ? ` · enviado ${formatDateTime(reminder.sentAt)}` : ''}${reminder.failureReason ? ` · ${reminder.failureReason}` : ''}`,
              }))}
              title="Recordatorios"
            />
            <TraceSection
              empty="No hay correos registrados."
              items={(bookingQuery.data.emailLogs ?? []).map((email) => ({
                id: email.id,
                title: `${email.templateKey} · ${email.status}${email.simulation ? ' · simulacion' : ''}`,
                detail: `${email.recipientEmail} · ${email.subject} · ${formatDateTime(email.createdAt)}`,
              }))}
              title="Correos"
            />
            <TraceSection
              empty="No hay historial de estado."
              items={(bookingQuery.data.statusHistory ?? []).map((history) => ({
                id: history.id,
                title: `${history.previousStatus ?? 'Inicio'} -> ${history.newStatus}`,
                detail: `${history.source} · ${formatDateTime(history.createdAt)}${history.reason ? ` · ${history.reason}` : ''}`,
              }))}
              title="Historial"
            />
          </Card>
          </div>

          <Card className="space-y-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
                Contexto relacionado
              </p>
              <h3 className="mt-2 text-2xl font-semibold text-[var(--color-text)]">
                Acciones rápidas
              </h3>
            </div>

            {bookingQuery.data.conversationId ? (
              <Link
                className={buttonClassName({ variant: 'secondary', fullWidth: true })}
                to={`/conversations/${bookingQuery.data.conversationId}`}
              >
                Abrir conversación asociada
              </Link>
            ) : null}

            {bookingQuery.data.leadId ? (
              <Link
                className={buttonClassName({ variant: 'secondary', fullWidth: true })}
                to={`/prospects/${bookingQuery.data.leadId}`}
              >
                Abrir prospecto asociado
              </Link>
            ) : null}

            {!bookingQuery.data.conversationId && !bookingQuery.data.leadId ? (
              <EmptyState
                description="Esta cita fue creada de forma manual y no tiene contexto comercial vinculado."
                title="Sin contexto relacionado"
                variant="card"
              />
            ) : null}
          </Card>
        </div>
      )}

      <ConfirmDialog
        confirmLabel="Cancelar cita"
        confirmLoading={cancelMutation.isPending}
        description="La cita quedará marcada como cancelada y seguirá visible para trazabilidad."
        onCancel={() => setCancelDialogOpen(false)}
        onConfirm={() => void cancelMutation.mutateAsync()}
        open={cancelDialogOpen}
        title="Cancelar cita"
        tone="danger"
      />
    </section>
  )
}

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <Card className="space-y-2 bg-slate-50">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="text-sm font-medium text-[var(--color-text)]">{value}</p>
    </Card>
  )
}

function TraceSection({
  title,
  empty,
  items,
}: {
  title: string
  empty: string
  items: Array<{ id: string; title: string; detail: string; action?: { label: string; onClick: () => void; disabled?: boolean } }>
}) {
  return (
    <div className="space-y-2 rounded-2xl border border-[var(--color-border)] bg-slate-50 p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">{title}</p>
      {items.length === 0 ? (
        <p className="text-sm text-slate-600">{empty}</p>
      ) : (
        <div className="space-y-2">
          {items.slice(0, 6).map((item) => (
            <div key={item.id} className="rounded-xl bg-white px-3 py-2 text-sm">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <p className="font-semibold text-slate-900">{item.title}</p>
                {item.action ? (
                  <button
                    className="text-xs font-semibold text-blue-700 disabled:text-slate-400"
                    disabled={item.action.disabled}
                    onClick={item.action.onClick}
                    type="button"
                  >
                    {item.action.label}
                  </button>
                ) : null}
              </div>
              <p className="mt-1 break-words text-slate-600">{item.detail}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
