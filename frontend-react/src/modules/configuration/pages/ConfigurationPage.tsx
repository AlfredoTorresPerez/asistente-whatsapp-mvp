import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useToast } from '../../../lib/toast'
import {
  deactivateWhatsAppChannelRequest,
  getWhatsAppChannelRequest,
  updateWhatsAppChannelRequest,
  validateWhatsAppChannelRequest,
} from '../../../services/api/whatsappChannelApi'
import {
  connectWhatsAppConfigurationRequest,
  disconnectWhatsAppConfigurationRequest,
  getWhatsAppConfigurationRequest,
  refreshWhatsAppConfigurationQrRequest,
  updateWhatsAppConfigurationPreferencesRequest,
} from '../../../services/api/configurationApi'
import {
  completeOnboardingRequest,
  revalidateOnboardingRequest,
  disconnectOnboardingRequest,
} from '../../../services/api/whatsappChannelApi'
import type {
  WhatsAppChannelResponse,
  WhatsAppConfigurationPreferencesResponse,
} from '../../../services/api/types'
import { CalendarSection } from '../components/CalendarSection'
import { usePermissions } from '../../../hooks/usePermissions'
import { metaEmbeddedSignup } from '../../../services/MetaEmbeddedSignup'
import { useState } from 'react'

type BadgeTone = 'success' | 'warning' | 'danger' | 'neutral' | 'info'
type PreferenceKey = keyof WhatsAppConfigurationPreferencesResponse

const channelQueryKey = ['whatsapp', 'channel'] as const
const legacyConfigQueryKey = ['configuration', 'whatsapp'] as const

const preferenceDefinitions: Array<{
  key: PreferenceKey
  title: string
  description: string
}> = [
  {
    key: 'newMessageNotifications',
    title: 'Notificaciones de nuevos mensajes',
    description: 'Recibe notificaciones de nuevos mensajes en tiempo real.',
  },
  {
    key: 'autoReassignment',
    title: 'Reasignacion automatica',
    description: 'Reasigna conversaciones inactivas despues de 15 minutos.',
  },
  {
    key: 'agentSignature',
    title: 'Firma de agente',
    description: 'Agrega automaticamente tu firma al final de cada mensaje.',
  },
  {
    key: 'outOfHoursMessage',
    title: 'Horario fuera de servicio',
    description: 'Envia mensaje automatico cuando estes fuera de horario.',
  },
]

function formatPhone(phoneNumber: string | null) {
  if (!phoneNumber) return 'Sin telefono vinculado'
  if (phoneNumber.startsWith('+')) return phoneNumber
  return `+${phoneNumber}`
}

function formatDateTime(value: string | null) {
  if (!value) return 'Sin registro'
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

function toStatusLabel(status: string) {
  switch (status) {
    case 'CONNECTED':
      return 'Conectado'
    case 'QR_PENDING':
      return 'QR pendiente'
    case 'SYNCING':
      return 'Sincronizando'
    case 'ERROR':
      return 'Error'
    default:
      return 'Desconectado'
  }
}

function toStatusTone(status: string): BadgeTone {
  switch (status) {
    case 'CONNECTED':
      return 'success'
    case 'QR_PENDING':
    case 'SYNCING':
      return 'warning'
    case 'ERROR':
      return 'danger'
    default:
      return 'neutral'
  }
}

const PROVIDER_META_CLOUD_API = 'META_CLOUD_API'
const PROVIDER_WHATSAPP_WEB = 'WHATSAPP_WEB'

export function ConfigurationPage() {
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const { hasPermission } = usePermissions()
  const [showDeactivateConfirm, setShowDeactivateConfirm] = useState(false)
  const [showEditConfig, setShowEditConfig] = useState(false)

  const channelQuery = useQuery({
    queryKey: channelQueryKey,
    queryFn: getWhatsAppChannelRequest,
    refetchInterval: 30_000,
  })
  const channel = channelQuery.data

  const isMetaCloudApi = channel?.providerType === PROVIDER_META_CLOUD_API
  const isWhatsAppWeb = channel?.providerType === PROVIDER_WHATSAPP_WEB
  const hasChannel = !!channel?.providerType

  const legacyConfigQuery = useQuery({
    queryKey: legacyConfigQueryKey,
    queryFn: getWhatsAppConfigurationRequest,
    enabled: isWhatsAppWeb,
    refetchInterval: 30_000,
  })
  const legacyConfig = legacyConfigQuery.data

  const validateMutation = useMutation({
    mutationFn: validateWhatsAppChannelRequest,
    onSuccess: (response) => {
      showToast({
        title: response.valid ? 'Configuracion valida' : 'Configuracion invalida',
        description: response.message,
        tone: response.valid ? 'success' : 'warning',
      })
    },
    onError: () => {
      showToast({
        title: 'Error al validar',
        description: 'No fue posible validar la configuracion.',
        tone: 'error',
      })
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: deactivateWhatsAppChannelRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(channelQueryKey, response)
      setShowDeactivateConfirm(false)
      showToast({
        title: 'Canal desactivado',
        description: 'El canal WhatsApp se desactivo.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'Error al desactivar',
        description: 'No fue posible desactivar el canal.',
        tone: 'error',
      })
    },
  })

  const updateConfigMutation = useMutation({
    mutationFn: updateWhatsAppChannelRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(channelQueryKey, response)
      setShowEditConfig(false)
      showToast({
        title: 'Configuracion actualizada',
        description: 'Los cambios se guardaron correctamente.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'Error al actualizar',
        description: 'No fue posible guardar la configuracion.',
        tone: 'error',
      })
    },
  })

  const updatePreferencesMutation = useMutation({
    mutationFn: updateWhatsAppConfigurationPreferencesRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(legacyConfigQueryKey, response)
      showToast({
        title: 'Preferencia actualizada',
        description: 'La configuracion fue guardada.',
        tone: 'success',
      })
    },
  })

  const connectMutation = useMutation({
    mutationFn: connectWhatsAppConfigurationRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(legacyConfigQueryKey, response)
      showToast({
        title: 'Vinculacion solicitada',
        description: 'Se solicito iniciar la sesion WhatsApp Web.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No fue posible vincular',
        description: 'Verifica que el servicio este levantado.',
        tone: 'error',
      })
    },
  })

  const refreshQrMutation = useMutation({
    mutationFn: refreshWhatsAppConfigurationQrRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(legacyConfigQueryKey, response)
      showToast({
        title: 'QR actualizado',
        description: 'Se solicito un nuevo codigo QR.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No fue posible generar QR',
        description: 'Verifica el adaptador local.',
        tone: 'error',
      })
    },
  })

  const disconnectMutation = useMutation({
    mutationFn: disconnectWhatsAppConfigurationRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(legacyConfigQueryKey, response)
      showToast({
        title: 'Sesion desconectada',
        description: 'Se cerro la sesion WhatsApp Web.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No fue posible desconectar',
        description: 'Verifica el estado del adaptador.',
        tone: 'error',
      })
    },
  })

  const [onboardingState, setOnboardingState] = useState<{
    step:
      | 'idle'
      | 'opening-meta'
      | 'authorizing'
      | 'validating-waba'
      | 'validating-phone'
      | 'subscribing-webhook'
      | 'connected'
      | 'error'
    errorMessage?: string
  }>({ step: 'idle' })

  const completeOnboardingMutation = useMutation({
    mutationFn: completeOnboardingRequest,
    onSuccess: (result) => {
      setOnboardingState({ step: 'connected' })
      queryClient.invalidateQueries({ queryKey: channelQueryKey })
      showToast({
        title: 'Conexion exitosa',
        description: `Canal ${result.providerType} conectado correctamente.`,
        tone: 'success',
      })
    },
    onError: (error: Error) => {
      setOnboardingState({ step: 'error', errorMessage: error.message })
      showToast({
        title: 'Error de conexion',
        description: error.message,
        tone: 'error',
      })
    },
  })

  const revalidateMutation = useMutation({
    mutationFn: revalidateOnboardingRequest,
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: channelQueryKey })
      showToast({
        title: 'Revalidacion exitosa',
        description: `Estado: ${result.operationalStatus}`,
        tone: 'success',
      })
    },
    onError: (error: Error) => {
      showToast({
        title: 'Error de revalidacion',
        description: error.message,
        tone: 'error',
      })
    },
  })

  const disconnectCloudMutation = useMutation({
    mutationFn: disconnectOnboardingRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: channelQueryKey })
      showToast({
        title: 'Canal desconectado',
        description: 'Las credenciales fueron eliminadas.',
        tone: 'success',
      })
    },
    onError: (error: Error) => {
      showToast({
        title: 'Error al desconectar',
        description: error.message,
        tone: 'error',
      })
    },
  })

  async function handleConnectWithMeta() {
    const appId = import.meta.env.VITE_META_APP_ID as string | undefined
    const configId = import.meta.env.VITE_META_EMBEDDED_SIGNUP_CONFIG_ID as string | undefined

    if (!appId || !configId) {
      showToast({
        title: 'Meta no configurado',
        description:
          'Las variables VITE_META_APP_ID y VITE_META_EMBEDDED_SIGNUP_CONFIG_ID deben estar definidas en el entorno.',
        tone: 'error',
      })
      return
    }

    setOnboardingState({ step: 'opening-meta' })

    try {
      const result = await metaEmbeddedSignup.openSignupDialog({ appId, configId, businessId: '' })

      setOnboardingState({ step: 'authorizing' })

      if (!result.code) {
        setOnboardingState({
          step: 'error',
          errorMessage: 'Meta no devolvio un codigo de autorizacion.',
        })
        return
      }

      setOnboardingState({ step: 'validating-waba' })

      if (!result.wabaId) {
        setOnboardingState({
          step: 'error',
          errorMessage:
            'Meta no devolvio un WABA ID. Verifica que el Embedded Signup este configurado correctamente.',
        })
        return
      }

      setOnboardingState({ step: 'validating-phone' })

      if (!result.phoneNumberId) {
        setOnboardingState({
          step: 'error',
          errorMessage: 'Meta no devolvio un Phone Number ID.',
        })
        return
      }

      setOnboardingState({ step: 'subscribing-webhook' })

      completeOnboardingMutation.mutate({
        code: result.code,
        redirectUri: window.location.origin,
        wabaId: result.wabaId,
        phoneNumberId: result.phoneNumberId,
      })
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'El flujo de conexion fue cancelado o fallo.'
      setOnboardingState({ step: 'error', errorMessage: message })
    }
  }

  function handlePreferenceToggle(key: PreferenceKey) {
    if (!legacyConfig || updatePreferencesMutation.isPending) return
    updatePreferencesMutation.mutate({
      ...legacyConfig.preferences,
      [key]: !legacyConfig.preferences[key],
    })
  }

  const isLoading = channelQuery.isPending && !channel
  const isError = channelQuery.isError && !channel
  const canManage = hasPermission('WHATSAPP_CONFIG_MANAGE')

  const title = 'Configuracion del canal de WhatsApp'
  const description = isMetaCloudApi
    ? 'Administra el numero empresarial registrado en Meta, su estado operativo y la recepcion de mensajes.'
    : 'Centro de control para gestionar conexion, dispositivos, QR y preferencias operativas de WhatsApp.'

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Button
              loading={channelQuery.isFetching}
              onClick={() => void channelQuery.refetch()}
              variant="secondary"
            >
              Actualizar estado
            </Button>
            {!hasChannel && canManage ? (
              <Button onClick={() => setShowEditConfig(true)}>Configurar canal</Button>
            ) : null}
          </>
        }
        description={description}
        eyebrow="Configuracion"
        title={title}
      />

      {isLoading ? (
        <LoadingState message="Cargando configuracion del canal WhatsApp." variant="page" />
      ) : null}

      {isError ? (
        <ErrorState
          description="No pudimos recuperar la configuracion desde la API."
          onRetry={() => void channelQuery.refetch()}
          title="No fue posible cargar configuracion"
        />
      ) : null}

      {!hasChannel && !isLoading ? (
        <Card className="flex flex-col items-center gap-4 p-12 text-center">
          <div className="rounded-full bg-slate-100 p-6">
            <WhatsAppIcon className="h-12 w-12 text-slate-400" />
          </div>
          <div>
            <p className="text-lg font-semibold text-slate-900">
              El canal de Meta todavia no esta configurado.
            </p>
            <p className="mt-1 text-sm text-slate-500">
              Configura un canal de WhatsApp para comenzar a recibir mensajes.
            </p>
          </div>
          {canManage ? (
            <Button onClick={() => setShowEditConfig(true)}>Configurar canal</Button>
          ) : null}
        </Card>
      ) : null}

      {channel && hasChannel ? (
        <div className="overflow-hidden rounded-[32px] border border-[var(--color-border)] bg-white shadow-[0_28px_90px_rgba(15,23,42,0.08)]">
          <div className="min-h-[720px] bg-[#FBFCFF]">
            <div className="min-w-0 space-y-4 p-4 lg:p-6">
              {isMetaCloudApi ? (
                <CloudApiHeader channel={channel} />
              ) : (
                <WhatsAppWebHeader channel={channel} />
              )}

              <div className="grid gap-4 xl:grid-cols-[1.05fr_0.95fr]">
                <ChannelStatusCard channel={channel} />
                {isMetaCloudApi ? <MetaCloudConfigurationSummary channel={channel} /> : null}
              </div>

              {isMetaCloudApi && canManage ? (
                <div className="flex flex-wrap gap-3">
                  {onboardingState.step !== 'idle' && onboardingState.step !== 'connected' ? (
                    <OnboardingProgress
                      step={onboardingState.step}
                      errorMessage={onboardingState.errorMessage}
                    />
                  ) : null}

                  {channel.operationalStatus !== 'CONNECTED' &&
                  channel.registrationStatus !== 'REGISTERED' ? (
                    <Button
                      loading={onboardingState.step !== 'idle' && onboardingState.step !== 'error'}
                      onClick={() => void handleConnectWithMeta()}
                    >
                      Conectar con Meta
                    </Button>
                  ) : null}

                  {channel.operationalStatus === 'CONNECTED' ? (
                    <>
                      <Button
                        loading={validateMutation.isPending}
                        onClick={() => void validateMutation.mutate()}
                        variant="secondary"
                      >
                        Probar conexion
                      </Button>
                      <Button
                        loading={revalidateMutation.isPending}
                        onClick={() => void revalidateMutation.mutate()}
                        variant="secondary"
                      >
                        Revalidar
                      </Button>
                      <Button
                        loading={disconnectCloudMutation.isPending}
                        onClick={() => void disconnectCloudMutation.mutate()}
                        variant="danger"
                      >
                        Desconectar
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button
                        loading={validateMutation.isPending}
                        onClick={() => void validateMutation.mutate()}
                        variant="secondary"
                      >
                        Probar conexion
                      </Button>
                      <Button onClick={() => setShowEditConfig(true)} variant="secondary">
                        Editar configuracion
                      </Button>
                    </>
                  )}
                </div>
              ) : null}

              {isWhatsAppWeb && legacyConfig ? (
                <>
                  <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
                    <QrConnectionCard
                      qrCode={legacyConfig.qrCode}
                      onRefreshQr={() => refreshQrMutation.mutate()}
                      refreshLoading={refreshQrMutation.isPending}
                    />
                    <PreferencesCard
                      disabled={updatePreferencesMutation.isPending}
                      onToggle={handlePreferenceToggle}
                      preferences={legacyConfig.preferences}
                    />
                  </div>

                  <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
                    <MainChannelCard
                      channelName={legacyConfig.mainChannel.channelName}
                      channelType={legacyConfig.mainChannel.channelType}
                      phoneNumber={legacyConfig.mainChannel.phoneNumber}
                      businessHours={legacyConfig.mainChannel.businessHours}
                      active={legacyConfig.mainChannel.automaticResponsesEnabled}
                    />
                    <SessionHistoryCard
                      history={channel.recentEvents.map((e) => ({
                        id: e.id,
                        title: e.title,
                        actor: e.actor,
                        tone: e.tone as 'success' | 'warning' | 'danger' | 'neutral' | 'info',
                        occurredAt: e.occurredAt,
                      }))}
                    />
                  </div>

                  <div className="flex flex-wrap gap-3">
                    <Button
                      loading={connectMutation.isPending}
                      onClick={() => connectMutation.mutate()}
                    >
                      Conectar
                    </Button>
                    <Button
                      loading={refreshQrMutation.isPending}
                      onClick={() => refreshQrMutation.mutate()}
                      variant="secondary"
                    >
                      Refrescar QR
                    </Button>
                    <Button
                      loading={disconnectMutation.isPending}
                      onClick={() => disconnectMutation.mutate()}
                      variant="danger"
                    >
                      Desconectar
                    </Button>
                  </div>
                </>
              ) : null}

              {isMetaCloudApi ? (
                <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
                  <MetaConfigDetailCard channel={channel} />
                  <SessionHistoryCard
                    history={channel.recentEvents.map((e) => ({
                      id: e.id,
                      title: e.title,
                      actor: e.actor,
                      tone: e.tone as 'success' | 'warning' | 'danger' | 'neutral' | 'info',
                      occurredAt: e.occurredAt,
                    }))}
                  />
                </div>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}

      {showDeactivateConfirm ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <Card className="mx-4 w-full max-w-md space-y-4 p-6">
            <p className="text-lg font-semibold text-slate-900">Desactivar canal</p>
            <p className="text-sm text-slate-600">
              Al desactivar el canal no se recibiran mensajes entrantes. La configuracion se
              conserva.
            </p>
            <div className="flex justify-end gap-3">
              <Button onClick={() => setShowDeactivateConfirm(false)} variant="secondary">
                Cancelar
              </Button>
              <Button
                loading={deactivateMutation.isPending}
                onClick={() => deactivateMutation.mutate()}
                variant="danger"
              >
                Desactivar
              </Button>
            </div>
          </Card>
        </div>
      ) : null}

      {showEditConfig && canManage ? (
        <EditConfigDialog
          channel={channel}
          onClose={() => setShowEditConfig(false)}
          onSave={(data) => updateConfigMutation.mutate(data)}
          saving={updateConfigMutation.isPending}
        />
      ) : null}

      {isWhatsAppWeb ? <CalendarSection /> : null}
    </section>
  )
}

function CloudApiHeader({ channel }: { channel: WhatsAppChannelResponse }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-emerald-100 bg-white px-4 py-3 shadow-[0_12px_32px_rgba(15,23,42,0.04)]">
      <div>
        <p className="text-sm font-semibold text-slate-950">
          Numero central de WhatsApp de la empresa
        </p>
        <p className="text-xs text-slate-500">
          Canal centralizado que atiende todas las sucursales. No requiere asignacion por sede.
        </p>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <StatusBadge
          label={channel.registrationLabel ?? 'No configurado'}
          tone={registrationTone(channel.registrationStatus)}
        />
        <StatusBadge
          label={channel.active ? 'Canal activo' : 'Canal inactivo'}
          tone={channel.active ? 'success' : 'neutral'}
        />
        <StatusBadge
          label={channel.credentialLabel ?? 'No configurado'}
          tone={credentialTone(channel.credentialStatus)}
        />
      </div>
    </div>
  )
}

function WhatsAppWebHeader({ channel }: { channel: WhatsAppChannelResponse }) {
  const sessionStatus = channel.operationalStatus === 'CONNECTED' ? 'CONNECTED' : 'DISCONNECTED'
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-emerald-100 bg-white px-4 py-3 shadow-[0_12px_32px_rgba(15,23,42,0.04)]">
      <div>
        <p className="text-sm font-semibold text-slate-950">Configuracion de WhatsApp Web</p>
        <p className="text-xs text-slate-500">
          Conecta el telefono de la empresa y administra la sesion activa.
        </p>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <StatusBadge label={toStatusLabel(sessionStatus)} tone={toStatusTone(sessionStatus)} />
        <StatusBadge
          label="API local"
          tone={channel.operationalStatus === 'CONNECTED' ? 'success' : 'warning'}
        />
      </div>
    </div>
  )
}

function ChannelStatusCard({ channel }: { channel: WhatsAppChannelResponse }) {
  return (
    <Card className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-950">Estado del canal</p>
          <p className="mt-1 text-xs text-slate-500">Informacion operativa del canal WhatsApp.</p>
        </div>
        <StatusBadge
          label={channel.operationalLabel ?? 'Inactivo'}
          tone={operationalTone(channel.operationalStatus)}
        />
      </div>

      <div className="grid gap-5 md:grid-cols-[180px_minmax(0,1fr)]">
        <div className="flex flex-col items-center justify-center rounded-[28px] border border-emerald-100 bg-emerald-50/60 p-5">
          <div className="flex h-28 w-28 items-center justify-center rounded-full border-[10px] border-emerald-200 bg-white text-emerald-600 shadow-[0_16px_40px_rgba(22,163,74,0.18)]">
            <WhatsAppIcon className="h-14 w-14" />
          </div>
          <p className="mt-4 text-center text-sm font-semibold text-slate-950">
            {formatPhone(channel.displayPhoneNumber ?? channel.normalizedPhoneNumber)}
          </p>
          <p className="mt-1 text-center text-xs text-slate-500">{channel.businessName}</p>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <InfoTile label="Proveedor" value={channel.providerLabel} />
          <InfoTile label="Numero registrado" value={formatPhone(channel.displayPhoneNumber)} />
          <InfoTile label="Estado de registro" value={channel.registrationLabel ?? '-'} />
          <InfoTile label="Notificacion" value={channel.webhookLabel ?? '-'} />
          <InfoTile label="Credencial" value={channel.credentialLabel ?? '-'} />
          <InfoTile label="Ultima validacion" value={formatDateTime(channel.lastHealthCheckAt)} />
          <InfoTile
            label="Ultimo mensaje recibido"
            value={formatDateTime(channel.lastMessageReceivedAt)}
          />
          <InfoTile
            label="Ultimo mensaje enviado"
            value={formatDateTime(channel.lastMessageSentAt)}
          />
        </div>
      </div>

      {channel.lastErrorMessage ? (
        <div className="rounded-[18px] border border-red-200 bg-red-50 px-4 py-3 text-xs leading-5 text-red-800">
          {channel.lastErrorMessage}
        </div>
      ) : null}
    </Card>
  )
}

function MetaCloudConfigurationSummary({ channel }: { channel: WhatsAppChannelResponse }) {
  const cloud = channel.metaCloudConfig
  return (
    <Card className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-950">Resumen de Meta</p>
          <p className="mt-1 text-xs text-slate-500">Configuracion de la cuenta empresarial.</p>
        </div>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <InfoTile label="Proveedor" value="Meta WhatsApp Cloud API" />
        <InfoTile label="Empresa" value={channel.businessName} />
        <InfoTile label="Numero central" value={formatPhone(channel.displayPhoneNumber)} />
        <InfoTile label="Cobertura" value="Todas las sucursales" />
        <InfoTile label="Estado de registro" value={channel.registrationLabel ?? '-'} />
        <InfoTile label="Canal activo" value={channel.active ? 'Si' : 'No'} />
        {cloud ? (
          <>
            <InfoTile label="ID del numero" value={cloud.phoneNumberId ?? '-'} />
            <InfoTile label="ID cuenta empresarial" value={cloud.businessAccountId ?? '-'} />
            <InfoTile label="Version API" value={cloud.graphApiVersion ?? '-'} />
            <InfoTile label="Webhook" value={cloud.webhookLabel ?? '-'} />
            <InfoTile
              label="Credencial configurada"
              value={cloud.credentialStatus === 'CONFIGURED' ? 'Si' : 'No'}
            />
          </>
        ) : null}
      </div>
    </Card>
  )
}

function MetaConfigDetailCard({ channel }: { channel: WhatsAppChannelResponse }) {
  const cloud = channel.metaCloudConfig
  return (
    <Card>
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-950">Configuracion de Meta</p>
          <p className="mt-1 text-xs text-slate-500">Detalles tecnicos del canal Cloud API.</p>
        </div>
      </div>
      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <InfoTile label="Numero central visible" value={formatPhone(channel.displayPhoneNumber)} />
        <InfoTile label="Numero normalizado" value={channel.normalizedPhoneNumber ?? '-'} />
        <InfoTile label="Modo de operacion" value="Centralizado" />
        <InfoTile label="Sucursales atendidas" value="Todas" />
        {cloud ? (
          <>
            <InfoTile label="ID tecnico del numero" value={cloud.phoneNumberId ?? '-'} />
            <InfoTile label="ID cuenta empresarial" value={cloud.businessAccountId ?? '-'} />
            <InfoTile label="Version API Graph" value={cloud.graphApiVersion ?? '-'} />
            <InfoTile label="URL webhook" value={cloud.webhookCallbackUrl ?? '-'} />
            <InfoTile label="Estado webhook" value={cloud.webhookLabel ?? '-'} />
            <InfoTile
              label="Credencial"
              value={
                cloud.credentialStatus === 'CONFIGURED' ? 'Configurada: Si' : 'Configurada: No'
              }
            />
            {cloud.tokenExpiresAt ? (
              <InfoTile label="Vencimiento token" value={formatDateTime(cloud.tokenExpiresAt)} />
            ) : null}
            <InfoTile label="Canal activo" value={cloud.active ? 'Si' : 'No'} />
          </>
        ) : null}
      </div>
    </Card>
  )
}

function QrConnectionCard({
  qrCode,
  onRefreshQr,
  refreshLoading,
}: {
  qrCode: string | null
  onRefreshQr: () => void
  refreshLoading: boolean
}) {
  return (
    <Card className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-950">Vincular nuevo dispositivo</p>
          <p className="mt-1 text-xs text-slate-500">
            Escanea el codigo desde WhatsApp en tu telefono.
          </p>
        </div>
        <Button loading={refreshLoading} onClick={onRefreshQr} size="sm">
          Generar QR
        </Button>
      </div>
      <div className="grid gap-5 md:grid-cols-[180px_minmax(0,1fr)]">
        <QrBlock qrCode={qrCode} />
        <ol className="space-y-3 text-sm text-slate-700">
          {[
            'Abre WhatsApp en tu telefono',
            'Toca Menu > Dispositivos vinculados',
            'Toca Vincular un dispositivo',
            'Escanea este codigo QR',
          ].map((step, index) => (
            <li className="flex gap-3" key={step}>
              <span className="flex h-7 w-7 items-center justify-center rounded-full bg-emerald-50 text-xs font-bold text-emerald-700">
                {index + 1}
              </span>
              <span className="pt-1">{step}</span>
            </li>
          ))}
        </ol>
      </div>
      <div className="rounded-[18px] border border-emerald-100 bg-emerald-50 px-4 py-3 text-xs leading-5 text-emerald-800">
        Este codigo QR es personal y temporal. No lo compartas con nadie.
      </div>
    </Card>
  )
}

function QrBlock({ qrCode }: { qrCode: string | null }) {
  if (qrCode?.startsWith('data:image')) {
    return (
      <div className="rounded-[24px] border border-slate-200 bg-white p-3">
        <img alt="QR WhatsApp Web" className="h-auto w-full rounded-[16px]" src={qrCode} />
      </div>
    )
  }
  if (qrCode) {
    return (
      <div className="rounded-[24px] border border-slate-200 bg-white p-4">
        <p className="max-h-[160px] overflow-auto break-all font-mono text-xs leading-5 text-slate-700">
          {qrCode}
        </p>
      </div>
    )
  }
  return (
    <div className="grid h-[180px] grid-cols-7 gap-1 rounded-[24px] border border-dashed border-slate-300 bg-white p-4">
      {Array.from({ length: 49 }).map((_, index) => (
        <span
          className={[
            'rounded-[4px]',
            index % 3 === 0 || index % 7 === 1 || index === 12 || index === 36
              ? 'bg-slate-900'
              : 'bg-slate-100',
          ].join(' ')}
          key={index}
        />
      ))}
    </div>
  )
}

function PreferencesCard({
  disabled,
  onToggle,
  preferences,
}: {
  disabled: boolean
  onToggle: (key: PreferenceKey) => void
  preferences: WhatsAppConfigurationPreferencesResponse
}) {
  return (
    <Card>
      <div>
        <p className="text-sm font-semibold text-slate-950">Preferencias generales</p>
        <p className="mt-1 text-xs text-slate-500">
          Cada cambio se guarda inmediatamente mediante API.
        </p>
      </div>
      <div className="mt-5 space-y-4">
        {preferenceDefinitions.map((preference) => (
          <div className="flex items-center justify-between gap-4" key={preference.key}>
            <div>
              <p className="text-sm font-semibold text-slate-900">{preference.title}</p>
              <p className="mt-1 text-xs leading-5 text-slate-500">{preference.description}</p>
            </div>
            <Switch
              checked={preferences[preference.key]}
              disabled={disabled}
              onClick={() => onToggle(preference.key)}
            />
          </div>
        ))}
      </div>
    </Card>
  )
}

function MainChannelCard({
  channelName,
  channelType,
  phoneNumber,
  businessHours,
  active,
}: {
  channelName: string
  channelType: string
  phoneNumber: string | null
  businessHours: string
  active: boolean
}) {
  return (
    <Card>
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-950">Canal principal</p>
          <p className="mt-1 text-xs text-slate-500">Configuracion operativa del canal activo.</p>
        </div>
        <StatusBadge label={active ? 'Activo' : 'Inactivo'} tone={active ? 'success' : 'neutral'} />
      </div>
      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <InfoTile label="Nombre del canal" value={channelName} />
        <InfoTile label="Tipo de canal" value={channelType} />
        <InfoTile label="Numero asociado" value={formatPhone(phoneNumber)} />
        <InfoTile label="Horario de atencion" value={businessHours} />
      </div>
    </Card>
  )
}

function SessionHistoryCard({
  history,
}: {
  history: Array<{
    id: string
    title: string
    actor: string
    tone: string
    occurredAt: string | null
  }>
}) {
  function normalizeTone(tone: string): BadgeTone {
    if (tone === 'success' || tone === 'warning' || tone === 'danger' || tone === 'info')
      return tone
    return 'neutral'
  }
  function toneLabel(tone: string) {
    switch (normalizeTone(tone)) {
      case 'success':
        return 'Correcto'
      case 'warning':
        return 'Atencion'
      case 'danger':
        return 'Error'
      case 'info':
        return 'Informativo'
      default:
        return 'Registro'
    }
  }
  return (
    <Card>
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-950">Historial del canal</p>
          <p className="mt-1 text-xs text-slate-500">Eventos recientes del canal.</p>
        </div>
        <StatusBadge label="Ver todo" tone="info" />
      </div>
      <div className="mt-5 space-y-4">
        {history.length === 0 ? (
          <p className="text-sm text-slate-400">Sin eventos registrados.</p>
        ) : (
          history.map((item) => (
            <div className="flex items-start gap-3" key={item.id}>
              <span className="mt-1 h-2.5 w-2.5 rounded-full bg-emerald-500" />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                  <StatusBadge label={toneLabel(item.tone)} tone={normalizeTone(item.tone)} />
                </div>
                <p className="mt-1 text-xs text-slate-500">{item.actor}</p>
                <p className="mt-1 text-xs text-slate-400">{formatDateTime(item.occurredAt)}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </Card>
  )
}

function EditConfigDialog({
  channel,
  onClose,
  onSave,
  saving,
}: {
  channel: WhatsAppChannelResponse | undefined
  onClose: () => void
  onSave: (data: Record<string, string | null>) => void
  saving: boolean
}) {
  const cloud = channel?.metaCloudConfig
  const [form, setForm] = useState({
    displayPhoneNumber: channel?.displayPhoneNumber ?? '',
    normalizedPhoneNumber: channel?.normalizedPhoneNumber ?? '',
    phoneNumberId: cloud?.phoneNumberId ?? '',
    businessAccountId: cloud?.businessAccountId ?? '',
    graphApiVersion: cloud?.graphApiVersion ?? 'v23.0',
    webhookCallbackUrl: cloud?.webhookCallbackUrl ?? '',
  })

  function handleChange(field: string, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const data: Record<string, string | null> = {}
    Object.entries(form).forEach(([key, value]) => {
      data[key] = value.trim() || null
    })
    onSave(data)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <Card className="mx-4 w-full max-w-lg space-y-4 p-6">
        <p className="text-lg font-semibold text-slate-900">Editar configuracion del canal</p>
        <form className="space-y-4" onSubmit={handleSubmit}>
          <div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Numero visible
            </label>
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm"
              value={form.displayPhoneNumber}
              onChange={(e) => handleChange('displayPhoneNumber', e.target.value)}
              placeholder="+56 9 XXXX XXXX"
            />
          </div>
          <div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Numero normalizado
            </label>
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm"
              value={form.normalizedPhoneNumber}
              onChange={(e) => handleChange('normalizedPhoneNumber', e.target.value)}
              placeholder="569XXXXXXX"
            />
          </div>
          <div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              ID tecnico del numero (Phone Number ID)
            </label>
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm"
              value={form.phoneNumberId}
              onChange={(e) => handleChange('phoneNumberId', e.target.value)}
            />
          </div>
          <div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              ID cuenta empresarial (WABA ID)
            </label>
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm"
              value={form.businessAccountId}
              onChange={(e) => handleChange('businessAccountId', e.target.value)}
            />
          </div>
          <div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Version API Graph
            </label>
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm"
              value={form.graphApiVersion}
              onChange={(e) => handleChange('graphApiVersion', e.target.value)}
            />
          </div>
          <div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              URL del webhook
            </label>
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm"
              value={form.webhookCallbackUrl}
              onChange={(e) => handleChange('webhookCallbackUrl', e.target.value)}
              placeholder="https://tudominio.com/api/v1/integrations/whatsapp-cloud/webhook"
            />
          </div>
          <div className="rounded-[14px] border border-amber-200 bg-amber-50 px-4 py-3 text-xs leading-5 text-amber-800">
            Las credenciales de acceso, secreto de aplicacion y token de verificacion se configuran
            mediante variables de entorno por seguridad.
          </div>
          <div className="flex justify-end gap-3">
            <Button disabled={saving} onClick={onClose} type="button" variant="secondary">
              Cancelar
            </Button>
            <Button loading={saving} type="submit">
              Guardar
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}

function InfoTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[18px] border border-slate-100 bg-white px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-950">{value}</p>
    </div>
  )
}

function Switch({
  checked,
  disabled,
  onClick,
}: {
  checked: boolean
  disabled: boolean
  onClick: () => void
}) {
  return (
    <button
      aria-checked={checked}
      className={[
        'relative h-7 w-12 rounded-full transition focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-emerald-100 disabled:cursor-not-allowed disabled:opacity-60',
        checked ? 'bg-emerald-500' : 'bg-slate-200',
      ].join(' ')}
      disabled={disabled}
      onClick={onClick}
      role="switch"
      type="button"
    >
      <span
        className={[
          'absolute top-1 h-5 w-5 rounded-full bg-white shadow transition',
          checked ? 'left-6' : 'left-1',
        ].join(' ')}
      />
    </button>
  )
}

function WhatsAppIcon({ className }: { className: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M5.5 27L7.2 21.6A10.8 10.8 0 1 1 11 25.1L5.5 27Z"
        fill="currentColor"
        opacity="0.18"
      />
      <path
        d="M7.3 24.7L8.7 20.3A9.1 9.1 0 1 1 12 23.4L7.3 24.7Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="2.2"
      />
      <path
        d="M12.7 11.5C12.3 10.6 12 10.5 11.5 10.5H11C10.4 10.5 9.8 11.1 9.8 12.1C9.8 13.1 10.4 15.1 12.2 16.9C14.1 18.8 16.3 19.5 17.4 19.5C18.4 19.5 19.3 18.9 19.5 18.2L19.8 17.3C19.9 17 19.8 16.7 19.5 16.5L17.5 15.6C17.2 15.5 16.9 15.5 16.7 15.8L15.9 16.8C15.7 17 15.5 17 15.2 16.9C14.6 16.6 13.8 16.2 13.2 15.5C12.6 14.9 12.2 14.2 12 13.7C11.9 13.4 11.9 13.2 12.1 13L12.8 12.2C13 12 12.9 11.7 12.7 11.5Z"
        fill="currentColor"
      />
    </svg>
  )
}

function registrationTone(status: string | null): BadgeTone {
  switch (status) {
    case 'REGISTERED':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'ERROR':
      return 'danger'
    default:
      return 'neutral'
  }
}

function operationalTone(status: string | null): BadgeTone {
  switch (status) {
    case 'CONNECTED':
      return 'success'
    case 'CONFIGURING':
    case 'DEGRADED':
      return 'warning'
    case 'DISCONNECTED':
    case 'ERROR':
      return 'danger'
    default:
      return 'neutral'
  }
}

function credentialTone(status: string | null): BadgeTone {
  switch (status) {
    case 'CONFIGURED':
      return 'success'
    case 'EXPIRING':
      return 'warning'
    case 'EXPIRED':
    case 'INVALID':
      return 'danger'
    default:
      return 'neutral'
  }
}

const ONBOARDING_STEPS: Array<{ step: string; label: string }> = [
  { step: 'opening-meta', label: 'Abriendo Meta' },
  { step: 'authorizing', label: 'Autorizacion recibida' },
  { step: 'validating-waba', label: 'Validando WABA' },
  { step: 'validating-phone', label: 'Validando numero' },
  { step: 'subscribing-webhook', label: 'Suscribiendo webhook' },
  { step: 'connected', label: 'Conectado' },
]

function OnboardingProgress({ step, errorMessage }: { step: string; errorMessage?: string }) {
  const currentIndex = ONBOARDING_STEPS.findIndex((s) => s.step === step)
  const isError = step === 'error'
  return (
    <div className="w-full space-y-3 rounded-[18px] border border-emerald-100 bg-emerald-50 px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700">
        {isError ? 'Error de conexion' : 'Conectando con Meta...'}
      </p>
      <div className="space-y-2">
        {ONBOARDING_STEPS.map((s, index) => {
          const isActive = index === currentIndex && !isError
          const isDone = index < currentIndex && !isError
          const isFailed = isError && index === currentIndex
          return (
            <div className="flex items-center gap-2 text-xs" key={s.step}>
              {isDone ? (
                <span className="flex h-4 w-4 items-center justify-center rounded-full bg-emerald-500 text-[10px] text-white">
                  &#10003;
                </span>
              ) : isActive ? (
                <span className="flex h-4 w-4 items-center justify-center rounded-full border-2 border-emerald-500" />
              ) : isFailed ? (
                <span className="flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] text-white">
                  &#10007;
                </span>
              ) : (
                <span className="flex h-4 w-4 items-center justify-center rounded-full border border-slate-300" />
              )}
              <span
                className={
                  isDone
                    ? 'text-emerald-700'
                    : isActive
                      ? 'font-semibold text-emerald-900'
                      : isFailed
                        ? 'text-red-700'
                        : 'text-slate-400'
                }
              >
                {s.label}
              </span>
            </div>
          )
        })}
      </div>
      {errorMessage ? <p className="text-xs leading-5 text-red-700">{errorMessage}</p> : null}
    </div>
  )
}
