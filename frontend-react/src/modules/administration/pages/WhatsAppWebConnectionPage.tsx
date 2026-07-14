import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
import { Modal } from '../../../components/overlay/Modal'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { Textarea } from '../../../components/ui/Textarea'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  connectWhatsAppWebRequest,
  disconnectWhatsAppWebRequest,
  getWhatsAppWebQrRequest,
  getWhatsAppWebStatusRequest,
  refreshWhatsAppWebQrRequest,
  sendWhatsAppWebTestMessageRequest,
} from '../../../services/api/administrationApi'
import { analyzeAestheticIntent, listAestheticIntentLogs } from '../../../services/api/aestheticApi'
import type {
  AestheticIntentLogResponse,
  IntentAnalysisResponse,
  IntentEntitiesResponse,
  WhatsAppWebQrResponse,
  WhatsAppWebStatusResponse,
} from '../../../services/api/types'

const testMessageSchema = z.object({
  recipientPhone: z
    .string()
    .trim()
    .regex(/^\+?[1-9]\d{7,14}$/, 'Ingresa un telefono valido en formato internacional.'),
  body: z
    .string()
    .trim()
    .min(1, 'Ingresa un mensaje de prueba.')
    .max(1000, 'El mensaje no puede superar los 1000 caracteres.'),
})

type TestMessageValues = z.infer<typeof testMessageSchema>

function sleep(delayMs: number) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, delayMs)
  })
}

function getStatusTone(status: string) {
  switch (status) {
    case 'CONNECTED':
      return 'success'
    case 'QR_PENDING':
      return 'warning'
    case 'ERROR':
      return 'danger'
    case 'SYNCING':
      return 'info'
    default:
      return 'neutral'
  }
}

function toStatusLabel(status: string) {
  switch (status) {
    case 'CONNECTED':
      return 'Conectado'
    case 'QR_PENDING':
      return 'QR requerido'
    case 'ERROR':
      return 'Error'
    case 'SYNCING':
      return 'Sincronizando'
    default:
      return 'Desconectado'
  }
}

function toEventLabel(eventType: string) {
  switch (eventType) {
    case 'MESSAGE_RECEIVED':
      return 'Mensaje recibido'
    case 'SESSION_STATUS_CHANGED':
      return 'Estado de sesion'
    case 'QR_UPDATED':
      return 'QR actualizado'
    case 'MESSAGE_ACK_UPDATED':
      return 'Entrega actualizada'
    default:
      return eventType
  }
}

function toProcessingLabel(status: string) {
  switch (status) {
    case 'PROCESSED':
      return 'Procesado'
    case 'FAILED':
      return 'Con error'
    default:
      return 'Recibido'
  }
}


type AiDecisionSource = {
  sourceMessage: string
  intent: string
  confidence: number
  entities: IntentEntitiesResponse
  requiresDatabaseLookup: boolean
  requiresHumanHandoff: boolean
  handoffReason: string | null
  suggestedResponse: string | null
  modelName: string
  createdAt: string | null
  sourceLabel: string
}

type DecisionStatus = {
  label: string
  tone: 'success' | 'warning' | 'danger' | 'neutral' | 'info'
}

function safeParseEntities(value: string | null | undefined): IntentEntitiesResponse {
  if (!value) {
    return emptyEntities()
  }
  try {
    const parsed = JSON.parse(value) as Partial<IntentEntitiesResponse>
    return {
      servicio: parsed.servicio ?? null,
      producto: parsed.producto ?? null,
      fecha: parsed.fecha ?? null,
      hora: parsed.hora ?? null,
      profesional: parsed.profesional ?? null,
      cliente: parsed.cliente ?? null,
    }
  } catch {
    return emptyEntities()
  }
}

function emptyEntities(): IntentEntitiesResponse {
  return {
    servicio: null,
    producto: null,
    fecha: null,
    hora: null,
    profesional: null,
    cliente: null,
  }
}

function logToDecision(log: AestheticIntentLogResponse): AiDecisionSource {
  return {
    sourceMessage: log.sourceMessage,
    intent: log.intent,
    confidence: log.confidence,
    entities: safeParseEntities(log.entities),
    requiresDatabaseLookup: log.requiresDatabaseLookup,
    requiresHumanHandoff: log.requiresHumanHandoff,
    handoffReason: log.handoffReason,
    suggestedResponse: log.suggestedResponse,
    modelName: log.modelName,
    createdAt: log.createdAt,
    sourceLabel: 'Ultimo mensaje real analizado',
  }
}

function previewToDecision(response: IntentAnalysisResponse, sourceMessage: string): AiDecisionSource {
  return {
    sourceMessage,
    intent: response.intencion,
    confidence: response.confianza,
    entities: response.entidades ?? emptyEntities(),
    requiresDatabaseLookup: response.requiereConsultaBaseDatos,
    requiresHumanHandoff: response.requiereDerivacionHumana,
    handoffReason: response.motivoDerivacion,
    suggestedResponse: response.respuestaSugerida,
    modelName: response.modelo,
    createdAt: new Date().toISOString(),
    sourceLabel: 'Mensaje de prueba analizado',
  }
}

function getDecisionStatus(decision: AiDecisionSource | null): DecisionStatus {
  if (!decision) {
    return { label: 'Sin analisis', tone: 'neutral' }
  }
  if (decision.requiresHumanHandoff) {
    return { label: 'Derivar a humano', tone: 'danger' }
  }
  if (decision.confidence < 0.6) {
    return { label: 'Informacion insuficiente', tone: 'warning' }
  }
  if (decision.requiresDatabaseLookup) {
    return { label: 'Requiere consulta interna', tone: 'warning' }
  }
  return { label: 'Respuesta automatica', tone: 'success' }
}

function getAppliedRules(decision: AiDecisionSource): string[] {
  const rules = new Set<string>()

  if (decision.requiresDatabaseLookup) {
    rules.add('Consultar base de datos antes de responder datos operativos')
  }
  if (decision.requiresHumanHandoff) {
    rules.add('Derivar a atencion humana antes de confirmar o recomendar')
  }
  if (decision.handoffReason) {
    rules.add('Aplicar bloqueo de seguridad estetica por condicion sensible')
  }
  if (decision.confidence < 0.6) {
    rules.add('Pedir aclaracion por baja confianza de interpretacion')
  }
  if (decision.intent.includes('reservar') || decision.intent.includes('disponibilidad') || decision.intent.includes('reprogramar')) {
    rules.add('Validar disponibilidad y profesional antes de confirmar agenda')
  }
  if (decision.intent.includes('precio') || decision.intent.includes('producto') || decision.intent.includes('duracion') || decision.intent.includes('promocion')) {
    rules.add('Usar catalogo vigente; no inventar precio, stock, duracion ni promociones')
  }
  if (decision.intent.includes('contraindicacion') || decision.intent.includes('recomendacion') || decision.intent.includes('evaluacion')) {
    rules.add('No emitir diagnosticos ni prometer resultados garantizados')
  }
  if (decision.suggestedResponse) {
    rules.add('Mostrar respuesta sugerida filtrada por reglas del centro')
  }

  return Array.from(rules).slice(0, 6)
}

function toIntentLabel(intent: string) {
  return intent
    .split('_')
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ')
}

const QR_STORAGE_KEY = 'whatsapp-web-last-qr'

function loadStoredQr(): WhatsAppWebQrResponse | null {
  try {
    const raw = localStorage.getItem(QR_STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as WhatsAppWebQrResponse
  } catch {
    return null
  }
}

function storeQr(qr: WhatsAppWebQrResponse) {
  try {
    localStorage.setItem(QR_STORAGE_KEY, JSON.stringify(qr))
  } catch {
  }
}

function clearStoredQr() {
  try {
    localStorage.removeItem(QR_STORAGE_KEY)
  } catch {
  }
}

export function WhatsAppWebConnectionPage() {
  const isOnline = useOnlineStatus()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [isQrModalOpen, setIsQrModalOpen] = useState(false)
  const [isDisconnectDialogOpen, setIsDisconnectDialogOpen] = useState(false)
  const [isQrLoading, setIsQrLoading] = useState(false)
  const [qrExpiresIn, setQrExpiresIn] = useState<number | null>(null)
  const [previousStatus, setPreviousStatus] = useState<string | null>(null)
  const qrRefreshTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const countdownTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<TestMessageValues>({
    resolver: zodResolver(testMessageSchema),
    defaultValues: {
      recipientPhone: '56950954580',
      body: 'Hola, este es un mensaje de prueba enviado desde el adaptador experimental WhatsApp Web.',
    },
  })

  const watchedTestMessage = useWatch({ control, name: 'body' })

  const isQrPending = (status: string | undefined) => status === 'QR_PENDING'

  const whatsAppWebStatusQuery = useQuery({
    queryKey: ['administration', 'whatsapp-web', 'status'],
    queryFn: getWhatsAppWebStatusRequest,
    refetchInterval: isOnline ? 20_000 : false,
  })

  const whatsAppWebQrQuery = useQuery({
    queryKey: ['administration', 'whatsapp-web', 'qr'],
    queryFn: getWhatsAppWebQrRequest,
    refetchInterval: isOnline && isQrPending(whatsAppWebStatusQuery.data?.sessionStatus) ? 5_000 : false,
  })

  const intentLogsQuery = useQuery({
    queryKey: ['esthetic', 'intent', 'latest-log'],
    queryFn: () => listAestheticIntentLogs({ page: 0, size: 1 }),
    refetchInterval: isOnline ? 20_000 : false,
  })

  const invalidateStatus = async () => {
    await queryClient.invalidateQueries({ queryKey: ['administration', 'summary'] })
    await queryClient.invalidateQueries({ queryKey: ['administration', 'whatsapp-web', 'status'] })
    await queryClient.invalidateQueries({ queryKey: ['administration', 'whatsapp-web', 'qr'] })
    await queryClient.invalidateQueries({ queryKey: ['esthetic', 'intent', 'latest-log'] })
  }

  const fetchLatestStatus = async () =>
    queryClient.fetchQuery({
      queryKey: ['administration', 'whatsapp-web', 'status'],
      queryFn: getWhatsAppWebStatusRequest,
      staleTime: 0,
    })

  const waitForQrAvailability = async () => {
    let latestStatus: WhatsAppWebStatusResponse | undefined

    for (let attempt = 0; attempt < 8; attempt += 1) {
      latestStatus = await fetchLatestStatus()

      if (latestStatus.qrCode || latestStatus.sessionStatus === 'ERROR') {
        return latestStatus
      }

      await sleep(1500)
    }

    return latestStatus
  }

  const openQrModalWithRefresh = async () => {
    setIsQrModalOpen(true)

    if (whatsAppWebStatusQuery.data?.qrCode) {
      return
    }

    setIsQrLoading(true)
    try {
      const latestStatus = await waitForQrAvailability()
      if (!latestStatus?.qrCode) {
        showToast({
          title: 'QR aun no disponible',
          description: 'El adaptador sigue preparando el QR. Reintenta en unos segundos si no aparece.',
          tone: 'warning',
        })
      }
    } catch {
      showToast({
        title: 'No pudimos refrescar el QR',
        description: 'El estado del adaptador no pudo sincronizarse en este momento.',
        tone: 'error',
      })
    } finally {
      setIsQrLoading(false)
    }
  }

  const connectMutation = useMutation({
    mutationFn: connectWhatsAppWebRequest,
    onSuccess: async () => {
      await invalidateStatus()
      await openQrModalWithRefresh()
      showToast({
        title: 'Conexion solicitada',
        description: 'El adaptador genero un nuevo QR para la sesion experimental.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No pudimos reconectar WhatsApp Web',
        description: 'Reintenta en unos segundos para volver a solicitar la sesion experimental.',
        tone: 'error',
      })
    },
  })

  const refreshQrMutation = useMutation({
    mutationFn: refreshWhatsAppWebQrRequest,
    onSuccess: async () => {
      await invalidateStatus()
      await openQrModalWithRefresh()
      showToast({
        title: 'QR actualizado',
        description: 'Se genero un nuevo QR para la sesion experimental WhatsApp Web.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No pudimos actualizar el QR',
        description: 'El adaptador no respondio correctamente. Reintenta en breve.',
        tone: 'error',
      })
    },
  })

  const disconnectMutation = useMutation({
    mutationFn: disconnectWhatsAppWebRequest,
    onSuccess: async () => {
      await invalidateStatus()
      clearStoredQr()
      showToast({
        title: 'Canal desconectado',
        description: 'La sesion experimental WhatsApp Web quedo cerrada correctamente.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No pudimos desconectar WhatsApp Web',
        description: 'La solicitud no pudo completarse. Reintenta en unos segundos.',
        tone: 'error',
      })
    },
  })

  const testMessageMutation = useMutation({
    mutationFn: sendWhatsAppWebTestMessageRequest,
    onSuccess: async (response) => {
      await invalidateStatus()
      reset(undefined, { keepValues: true })
      showToast({
        title: 'Mensaje enviado al adaptador',
        description: `Identificador externo: ${response.externalMessageId}.`,
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No pudimos enviar el mensaje',
        description: 'Revisa el telefono de destino o el estado del adaptador experimental.',
        tone: 'error',
      })
    },
  })

  const intentPreviewMutation = useMutation({
    mutationFn: (message: string) => analyzeAestheticIntent({ message }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['esthetic', 'intent', 'latest-log'] })
      showToast({
        title: 'Analisis IA actualizado',
        description: 'Se interpretó el mensaje de prueba con el motor de reglas del centro estetico.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No pudimos analizar el mensaje',
        description: 'Revisa que el backend este disponible y que el modulo estetico este migrado.',
        tone: 'error',
      })
    },
  })

  const status = whatsAppWebStatusQuery.data
  const qrData = whatsAppWebQrQuery.data
  const isSyncing =
    connectMutation.isPending ||
    refreshQrMutation.isPending ||
    disconnectMutation.isPending ||
    testMessageMutation.isPending

  const latestIntentLog = intentLogsQuery.data?.items[0] ?? null
  const lastAnalyzedMessage = intentPreviewMutation.variables ?? watchedTestMessage ?? ''
  const previewDecision = intentPreviewMutation.data
    ? previewToDecision(intentPreviewMutation.data, lastAnalyzedMessage)
    : null
  const latestDecision = previewDecision ?? (latestIntentLog ? logToDecision(latestIntentLog) : null)

  const onSubmit = handleSubmit(async (values) => {
    await testMessageMutation.mutateAsync(values)
  })

  const analyzeCurrentTestMessage = handleSubmit(async (values) => {
    await intentPreviewMutation.mutateAsync(values.body.trim())
  })

  const handleManualRefreshQr = useCallback(() => {
    void refreshQrMutation.mutateAsync()
  }, [refreshQrMutation])

  const handleConnect = useCallback(() => {
    void connectMutation.mutateAsync()
  }, [connectMutation])

  useEffect(() => {
    if (whatsAppWebQrQuery.data) {
      storeQr(whatsAppWebQrQuery.data)
    }
  }, [whatsAppWebQrQuery.data])

  useEffect(() => {
    if (previousStatus !== 'CONNECTED' && status?.sessionStatus === 'CONNECTED') {
      showToast({
        title: 'QR escaneado, conectando...',
        description: 'El telefono se vinculo correctamente al adaptador WhatsApp Web.',
        tone: 'success',
      })
    }
    setPreviousStatus(status?.sessionStatus ?? null)
  }, [status?.sessionStatus, previousStatus, showToast])

  useEffect(() => {
    if (countdownTimerRef.current) {
      clearInterval(countdownTimerRef.current)
      countdownTimerRef.current = null
    }

    if (!qrData?.expiresAt || !qrData.qrCode) {
      setQrExpiresIn(null)
      return
    }

    const updateCountdown = () => {
      const expiresAt = dayjs(qrData.expiresAt)
      const now = dayjs()
      const diff = expiresAt.diff(now, 'seconds')
      if (diff <= 0) {
        setQrExpiresIn(0)
        void queryClient.invalidateQueries({ queryKey: ['administration', 'whatsapp-web', 'qr'] })
        return
      }
      setQrExpiresIn(diff)
    }

    updateCountdown()
    countdownTimerRef.current = setInterval(updateCountdown, 1000)

    return () => {
      if (countdownTimerRef.current) {
        clearInterval(countdownTimerRef.current)
        countdownTimerRef.current = null
      }
    }
  }, [qrData?.expiresAt, qrData?.qrCode, queryClient])

  const displayQrCode = qrData?.qrCode ?? status?.qrCode ?? loadStoredQr()?.qrCode ?? null
  const displayQrExpiresIn = qrExpiresIn
  const isQrExpired = displayQrExpiresIn !== null && displayQrExpiresIn <= 10

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Button
              disabled={!isOnline || isSyncing}
              loading={connectMutation.isPending}
              onClick={handleConnect}
            >
              {status?.sessionStatus === 'QR_PENDING' ? 'Conectar' : 'Iniciar conexion'}
            </Button>
            <Button
              disabled={!isOnline || isSyncing}
              onClick={() => void openQrModalWithRefresh()}
              variant="secondary"
            >
              Ver QR
            </Button>
            <Button
              disabled={!isOnline || isSyncing}
              loading={refreshQrMutation.isPending}
              onClick={handleManualRefreshQr}
              variant="secondary"
            >
              Refrescar QR
            </Button>
            <Button
              disabled={!isOnline || isSyncing}
              onClick={() => setIsDisconnectDialogOpen(true)}
              variant="danger"
            >
              Desconectar
            </Button>
          </>
        }
        description="Controla el estado del adaptador desacoplado, revisa el QR de sesion y ejecuta un envio de prueba sin acoplar el backend al runtime Node."
        eyebrow="Administracion"
        title="Conexion WhatsApp Web"
      />

      <Card className="border-amber-200 bg-amber-50">
        <div className="flex flex-wrap items-center gap-3">
          <StatusBadge label="Experimental" tone="warning" />
          <StatusBadge
            label={toStatusLabel(isSyncing ? 'SYNCING' : status?.sessionStatus ?? 'DISCONNECTED')}
            tone={isSyncing ? 'info' : getStatusTone(status?.sessionStatus ?? 'DISCONNECTED')}
          />
        </div>
        <p className="mt-4 text-sm leading-7 text-slate-700">
          Canal experimental WhatsApp Web: usado solo para demos, validacion temprana y pilotos controlados. Puede requerir reconexion manual mediante QR y no garantiza disponibilidad continua.
        </p>
      </Card>

      {whatsAppWebStatusQuery.isPending && !status ? (
        <LoadingState
          message="Consultando el estado del adaptador WhatsApp Web y los ultimos eventos registrados."
          variant="page"
        />
      ) : null}

      {whatsAppWebStatusQuery.isError && !status ? (
        <ErrorState
          description="No pudimos consultar el estado del canal experimental. Reintenta para volver a sincronizarlo."
          onRetry={() => void whatsAppWebStatusQuery.refetch()}
          title="No fue posible cargar WhatsApp Web"
        />
      ) : null}

      {status ? (
        <>
          <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
            <Card className="space-y-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                    Estado del canal
                  </p>
                  <h2 className="mt-2 text-2xl font-semibold text-slate-950">
                    Sesion experimental
                  </h2>
                </div>
                <StatusBadge
                  label={toStatusLabel(isSyncing ? 'SYNCING' : status.sessionStatus)}
                  tone={isSyncing ? 'info' : getStatusTone(status.sessionStatus)}
                />
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <SnapshotItem
                  label="Modo"
                  value={status.adapterMode === 'EXPERIMENTAL' ? 'Experimental' : status.adapterMode}
                />
                <SnapshotItem
                  label="Adaptador disponible"
                  value={status.adapterReachable ? 'Disponible' : 'No disponible'}
                />
                <SnapshotItem
                  label="Telefono vinculado"
                  value={status.phoneNumber ?? 'Sin numero vinculado'}
                />
                <SnapshotItem
                  label="Ultimo evento"
                  value={
                    status.lastEventAt
                      ? dayjs(status.lastEventAt).format('DD/MM/YYYY HH:mm')
                      : 'Sin eventos recientes'
                  }
                />
              </div>

              <div className="flex flex-wrap gap-3 border-t border-[var(--color-border)] pt-5">
                <Button
                  disabled={!isOnline || isSyncing}
                  loading={connectMutation.isPending}
                  onClick={handleConnect}
                >
                  {status.sessionStatus === 'QR_PENDING' ? 'Conectar' : 'Iniciar conexion'}
                </Button>
                <Button
                  disabled={!isOnline || isSyncing}
                  loading={refreshQrMutation.isPending}
                  onClick={handleManualRefreshQr}
                  variant="secondary"
                >
                  Refrescar QR
                </Button>
                <Button
                  disabled={!isOnline || isSyncing}
                  onClick={() => setIsDisconnectDialogOpen(true)}
                  variant="danger"
                >
                  Desconectar
                </Button>
              </div>
            </Card>

            <Card className="space-y-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                  Eventos recientes
                </p>
                <h2 className="mt-2 text-2xl font-semibold text-slate-950">
                  Historial del adaptador
                </h2>
              </div>

              {status.recentEvents.length === 0 ? (
                <EmptyState
                  description="Todavia no hay webhooks ni cambios de sesion registrados para este negocio."
                  primaryAction={{ label: 'Solicitar QR', to: '/admin/whatsapp-web' }}
                  title="Sin eventos recientes"
                  variant="card"
                />
              ) : (
                <div className="space-y-3">
                  {status.recentEvents.map((event) => (
                    <div
                      key={event.deliveryId}
                      className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 px-4 py-4"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <p className="text-sm font-semibold text-slate-950">
                          {toEventLabel(event.eventType)}
                        </p>
                        <StatusBadge
                          label={toProcessingLabel(event.processingStatus)}
                          tone={event.processingStatus === 'FAILED' ? 'danger' : event.processingStatus === 'PROCESSED' ? 'success' : 'info'}
                        />
                      </div>
                      <p className="mt-2 text-sm text-slate-600">
                        ID de entrega: <span className="font-mono text-slate-700">{event.deliveryId}</span>
                      </p>
                      <p className="mt-1 text-sm text-slate-600">
                        Recibido {dayjs(event.receivedAt).format('DD/MM/YYYY HH:mm:ss')}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>

          <AiRuleResponseCard
            decision={latestDecision}
            error={intentLogsQuery.isError}
            loading={intentLogsQuery.isPending && !latestDecision}
            onAnalyze={() => void analyzeCurrentTestMessage()}
            analyzing={intentPreviewMutation.isPending}
            analyzeDisabled={!isOnline || intentPreviewMutation.isPending || !watchedTestMessage?.trim()}
            showingPreview={Boolean(previewDecision)}
          />

          <Card className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                  Probar envio
                </p>
                <h2 className="mt-2 text-2xl font-semibold text-slate-950">
                  Mensaje de prueba real
                </h2>
              </div>
              <StatusBadge label="Experimental" tone="warning" />
            </div>

            <form className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]" onSubmit={onSubmit}>
              <Input
                error={errors.recipientPhone?.message}
                hint="Formato esperado: 56950954580"
                label="Telefono destino"
                placeholder="56950954580"
                {...register('recipientPhone')}
              />
              <Textarea
                error={errors.body?.message}
                hint="Solo texto en Fase 1. No se envian archivos ni multimedia."
                label="Mensaje de prueba"
                placeholder="Escribe un mensaje corto para validar el adaptador experimental."
                {...register('body')}
              />

              <div className="xl:col-span-2 flex flex-wrap justify-between gap-3 border-t border-[var(--color-border)] pt-5">
                <Link to="/admin">
                  <Button variant="secondary">Volver a administracion</Button>
                </Link>
                <Button
                  disabled={!isOnline || isSyncing}
                  loading={testMessageMutation.isPending || isSubmitting}
                  type="submit"
                >
                  Probar envio
                </Button>
              </div>
            </form>
          </Card>
        </>
      ) : null}

      <Modal
        maxWidthClassName="max-w-[560px]"
        onClose={() => {
          setIsQrModalOpen(false)
          setIsQrLoading(false)
          if (qrRefreshTimerRef.current) {
            clearInterval(qrRefreshTimerRef.current)
            qrRefreshTimerRef.current = null
          }
        }}
        open={isQrModalOpen}
      >
        <div className="space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                Conexion experimental
              </p>
              <h2 className="mt-2 text-2xl font-semibold text-slate-950">
                QR de conexion WhatsApp Web
              </h2>
            </div>
            <div className="flex items-center gap-2">
              {status?.sessionStatus === 'CONNECTED' ? (
                <StatusBadge label="Escaneado ✓" tone="success" />
              ) : displayQrExpiresIn !== null ? (
                <StatusBadge
                  label={`Expira en ${displayQrExpiresIn}s`}
                  tone={isQrExpired ? 'danger' : 'warning'}
                />
              ) : null}
              <StatusBadge label="Experimental" tone="warning" />
            </div>
          </div>

          {isQrLoading ? (
            <LoadingState
              message="Solicitando el QR vigente desde el adaptador WhatsApp Web."
              variant="card"
            />
          ) : displayQrCode && status?.sessionStatus !== 'CONNECTED' ? (
            <div className="rounded-[24px] border border-[var(--color-border)] bg-slate-50 px-5 py-5">
              <p className="text-sm font-semibold text-slate-900">
                Escanea el QR con WhatsApp
                {displayQrExpiresIn !== null && displayQrExpiresIn <= 30 ? (
                  <span className="ml-2 text-red-600">
                    ({displayQrExpiresIn}s restantes)
                  </span>
                ) : null}
              </p>
              {displayQrCode.startsWith('data:image') ? (
                <div className="mt-3 flex justify-center rounded-[18px] bg-white px-4 py-4">
                  <img
                    alt="QR WhatsApp Web"
                    className="h-auto w-full max-w-[320px] rounded-[12px]"
                    src={displayQrCode}
                  />
                </div>
              ) : (
                <p className="mt-3 break-all rounded-[18px] bg-white px-4 py-4 font-mono text-sm text-slate-700">
                  {displayQrCode}
                </p>
              )}
              {isQrExpired ? (
                <div className="mt-3 flex justify-center">
                  <Button
                    disabled={!isOnline || isSyncing}
                    loading={refreshQrMutation.isPending}
                    onClick={handleManualRefreshQr}
                    size="sm"
                  >
                    Refrescar QR
                  </Button>
                </div>
              ) : null}
            </div>
          ) : status?.sessionStatus === 'CONNECTED' ? (
            <div className="rounded-[24px] border border-green-200 bg-green-50 px-5 py-5 text-center">
              <p className="text-lg font-semibold text-green-800">Telefono conectado ✓</p>
              <p className="mt-2 text-sm text-green-700">
                El adaptador WhatsApp Web esta vinculado y listo para usar.
              </p>
            </div>
          ) : (
            <EmptyState
              description="Todavia no existe un QR vigente. Solicita reconexion para generar uno nuevo."
              title="Sin QR disponible"
              variant="card"
            />
          )}

          <div className="flex justify-center gap-3">
            <Button
              disabled={!isOnline || isSyncing || status?.sessionStatus === 'CONNECTED'}
              loading={refreshQrMutation.isPending}
              onClick={handleManualRefreshQr}
              variant="secondary"
              size="sm"
            >
              Refrescar QR
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        confirmLabel="Desconectar"
        confirmLoading={disconnectMutation.isPending}
        description="Esta accion cerrara la sesion experimental actual del adaptador WhatsApp Web. Podras reconectarla mas tarde mediante un nuevo QR."
        onCancel={() => setIsDisconnectDialogOpen(false)}
        onConfirm={() => {
          setIsDisconnectDialogOpen(false)
          void disconnectMutation.mutateAsync()
        }}
        open={isDisconnectDialogOpen}
        title="¿Desconectar WhatsApp Web?"
        tone="danger"
      />
    </section>
  )
}

function AiRuleResponseCard({
  decision,
  loading,
  error,
  onAnalyze,
  analyzing,
  analyzeDisabled,
  showingPreview,
}: {
  decision: AiDecisionSource | null
  loading: boolean
  error: boolean
  onAnalyze: () => void
  analyzing: boolean
  analyzeDisabled: boolean
  showingPreview: boolean
}) {
  const status = getDecisionStatus(decision)
  const rules = decision ? getAppliedRules(decision) : []

  return (
    <Card className="space-y-5 border-blue-100 bg-blue-50/40">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
            Motor inteligente
          </p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-950">
            Respuesta IA y reglas aplicadas
          </h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Muestra la interpretacion del ultimo mensaje procesado y la decision del motor de reglas antes de responder al cliente.
          </p>
        </div>
        <div className="flex flex-wrap justify-end gap-2">
          {showingPreview ? <StatusBadge label="Vista previa" tone="info" /> : null}
          <StatusBadge label={status.label} tone={status.tone} />
        </div>
      </div>

      {loading ? (
        <LoadingState message="Consultando el ultimo analisis de IA y reglas del centro estetico." variant="card" />
      ) : error ? (
        <ErrorState
          description="No fue posible recuperar el ultimo analisis registrado. Puedes intentar analizar el mensaje de prueba manualmente."
          title="Sin analisis disponible"
        />
      ) : decision ? (
        <div className="space-y-5">
          <div className="grid gap-4 lg:grid-cols-4">
            <DecisionMetric label="Intencion" value={toIntentLabel(decision.intent)} />
            <DecisionMetric label="Confianza" value={`${Math.round(decision.confidence * 100)}%`} />
            <DecisionMetric label="Consulta BD" value={decision.requiresDatabaseLookup ? 'Si' : 'No'} />
            <DecisionMetric label="Derivacion" value={decision.requiresHumanHandoff ? 'Si' : 'No'} />
          </div>

          <div className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
            <div className="rounded-[22px] border border-blue-100 bg-white px-5 py-5">
              <p className="text-sm font-semibold text-slate-950">Entidades detectadas</p>
              <dl className="mt-4 grid gap-3 sm:grid-cols-2">
                <EntityItem label="Servicio" value={decision.entities.servicio} />
                <EntityItem label="Producto" value={decision.entities.producto} />
                <EntityItem label="Fecha" value={decision.entities.fecha} />
                <EntityItem label="Hora" value={decision.entities.hora} />
                <EntityItem label="Profesional" value={decision.entities.profesional} />
                <EntityItem label="Cliente" value={decision.entities.cliente} />
              </dl>
            </div>

            <div className="rounded-[22px] border border-blue-100 bg-white px-5 py-5">
              <p className="text-sm font-semibold text-slate-950">Reglas aplicadas</p>
              {rules.length > 0 ? (
                <ul className="mt-4 space-y-2 text-sm leading-6 text-slate-700">
                  {rules.map((rule) => (
                    <li key={rule} className="flex gap-2">
                      <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-500" />
                      <span>{rule}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="mt-4 text-sm leading-6 text-slate-600">
                  No hay reglas adicionales registradas para esta interpretacion.
                </p>
              )}
            </div>
          </div>

          <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
            <div className="rounded-[22px] border border-blue-100 bg-white px-5 py-5">
              <p className="text-sm font-semibold text-slate-950">Respuesta sugerida</p>
              <p className="mt-3 whitespace-pre-line text-sm leading-7 text-slate-700">
                {decision.suggestedResponse ?? 'Sin respuesta sugerida registrada.'}
              </p>
            </div>

            <div className="rounded-[22px] border border-blue-100 bg-white px-5 py-5">
              <p className="text-sm font-semibold text-slate-950">Auditoria</p>
              <div className="mt-4 space-y-3 text-sm text-slate-600">
                <p>
                  Origen: <span className="font-semibold text-slate-800">{decision.sourceLabel}</span>
                </p>
                <p>
                  Modelo: <span className="font-mono text-slate-800">{decision.modelName}</span>
                </p>
                <p>
                  Fecha: <span className="font-semibold text-slate-800">{decision.createdAt ? dayjs(decision.createdAt).format('DD/MM/YYYY HH:mm:ss') : 'No registrada'}</span>
                </p>
                {decision.handoffReason ? (
                  <p className="rounded-[16px] border border-red-100 bg-red-50 px-3 py-3 text-red-700">
                    Motivo derivacion: {decision.handoffReason}
                  </p>
                ) : null}
              </div>
            </div>
          </div>

          <div className="rounded-[22px] border border-blue-100 bg-white px-5 py-5">
            <p className="text-sm font-semibold text-slate-950">Mensaje analizado</p>
            <p className="mt-3 text-sm leading-7 text-slate-700">{decision.sourceMessage}</p>
          </div>
        </div>
      ) : (
        <EmptyState
          description="Aun no existen mensajes analizados. Ingresa un mensaje de prueba y presiona Analizar con IA para validar reglas sin enviarlo al cliente."
          title="Sin respuesta IA registrada"
          variant="card"
        />
      )}

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-blue-100 pt-5">
        <p className="max-w-2xl text-sm leading-6 text-slate-600">
          Este recuadro es de auditoria operativa: permite validar que la IA interpreta la intencion y que las reglas impiden inventar precios, horarios, stock o indicaciones sensibles.
        </p>
        <Button disabled={analyzeDisabled} loading={analyzing} onClick={onAnalyze} variant="secondary">
          Analizar mensaje de prueba
        </Button>
      </div>
    </Card>
  )
}

function DecisionMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[20px] border border-blue-100 bg-white px-4 py-4">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-2 text-base font-semibold text-slate-950">{value}</p>
    </div>
  )
}

function EntityItem({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">{label}</dt>
      <dd className="mt-1 text-sm font-semibold text-slate-800">{value && value.trim() ? value : 'No detectado'}</dd>
    </div>
  )
}

function SnapshotItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 px-4 py-4">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-2 text-base font-semibold text-slate-950">{value}</p>
    </div>
  )
}
