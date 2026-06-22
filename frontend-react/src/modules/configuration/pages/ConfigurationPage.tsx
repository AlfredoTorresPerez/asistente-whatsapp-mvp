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
  connectWhatsAppConfigurationRequest,
  disconnectWhatsAppConfigurationRequest,
  getWhatsAppConfigurationRequest,
  refreshWhatsAppConfigurationQrRequest,
  updateWhatsAppConfigurationPreferencesRequest,
} from '../../../services/api/configurationApi'
import type {
  WhatsAppConfigurationLinkedDeviceResponse,
  WhatsAppConfigurationPreferencesResponse,
  WhatsAppConfigurationResponse,
  WhatsAppConfigurationSessionHistoryResponse,
} from '../../../services/api/types'

type BadgeTone = 'success' | 'warning' | 'danger' | 'neutral' | 'info'
type PreferenceKey = keyof WhatsAppConfigurationPreferencesResponse

const configurationQueryKey = ['configuration', 'whatsapp'] as const

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
  if (!phoneNumber) {
    return 'Sin telefono vinculado'
  }
  if (phoneNumber.startsWith('+')) {
    return phoneNumber
  }
  return `+${phoneNumber}`
}

function formatDateTime(value: string | null) {
  if (!value) {
    return 'Sin registro'
  }
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

function toDeviceStatusLabel(status: string) {
  switch (status) {
    case 'ONLINE':
      return 'En linea'
    case 'PENDING':
      return 'Pendiente'
    case 'ERROR':
      return 'Error'
    default:
      return 'Desconectado'
  }
}

function toDeviceStatusTone(status: string): BadgeTone {
  switch (status) {
    case 'ONLINE':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'ERROR':
      return 'danger'
    default:
      return 'neutral'
  }
}

function normalizeHistoryTone(tone: string): BadgeTone {
  if (tone === 'success' || tone === 'warning' || tone === 'danger' || tone === 'info') {
    return tone
  }
  return 'neutral'
}

function toHistoryToneLabel(tone: string) {
  switch (normalizeHistoryTone(tone)) {
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

export function ConfigurationPage() {
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const configurationQuery = useQuery({
    queryKey: configurationQueryKey,
    queryFn: getWhatsAppConfigurationRequest,
    refetchInterval: 30_000,
  })
  const configuration = configurationQuery.data

  const updatePreferencesMutation = useMutation({
    mutationFn: updateWhatsAppConfigurationPreferencesRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(configurationQueryKey, response)
      showToast({
        title: 'Preferencia actualizada',
        description: 'La configuracion fue guardada en la API local.',
        tone: 'success',
      })
    },
  })

  const connectMutation = useMutation({
    mutationFn: connectWhatsAppConfigurationRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(configurationQueryKey, response)
      showToast({
        title: 'Vinculacion solicitada',
        description: 'La API local solicito iniciar la sesion WhatsApp Web.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No fue posible vincular',
        description: 'Verifica que el servicio whatsapp-web-service este levantado localmente.',
        tone: 'error',
      })
    },
  })

  const refreshQrMutation = useMutation({
    mutationFn: refreshWhatsAppConfigurationQrRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(configurationQueryKey, response)
      showToast({
        title: 'QR actualizado',
        description: 'Se solicito un nuevo codigo QR al adaptador local.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No fue posible generar QR',
        description: 'Verifica el adaptador local de WhatsApp Web.',
        tone: 'error',
      })
    },
  })

  const disconnectMutation = useMutation({
    mutationFn: disconnectWhatsAppConfigurationRequest,
    onSuccess: (response) => {
      queryClient.setQueryData(configurationQueryKey, response)
      showToast({
        title: 'Sesion desconectada',
        description: 'La API local solicito cerrar la sesion WhatsApp Web.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No fue posible desconectar',
        description: 'Verifica el estado del adaptador local.',
        tone: 'error',
      })
    },
  })

  function handlePreferenceToggle(key: PreferenceKey) {
    if (!configuration || updatePreferencesMutation.isPending) {
      return
    }

    updatePreferencesMutation.mutate({
      ...configuration.preferences,
      [key]: !configuration.preferences[key],
    })
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Button
              loading={configurationQuery.isFetching}
              onClick={() => void configurationQuery.refetch()}
              variant="secondary"
            >
              Actualizar estado
            </Button>
            <Button
              loading={refreshQrMutation.isPending}
              onClick={() => refreshQrMutation.mutate()}
            >
              Vincular dispositivo
            </Button>
          </>
        }
        description="Centro de control para gestionar conexion, dispositivos, QR y preferencias operativas de WhatsApp."
        eyebrow="Configuracion"
        title="Configuracion de WhatsApp Web"
      />

      {configurationQuery.isPending && !configuration ? (
        <LoadingState
          message="Cargando configuracion local de WhatsApp Web."
          variant="page"
        />
      ) : null}

      {configurationQuery.isError && !configuration ? (
        <ErrorState
          description="No pudimos recuperar la configuracion desde la API local. Verifica que backend-java este levantado."
          onRetry={() => void configurationQuery.refetch()}
          title="No fue posible cargar configuracion"
        />
      ) : null}

      {configuration ? (
        <div className="overflow-hidden rounded-[32px] border border-[var(--color-border)] bg-white shadow-[0_28px_90px_rgba(15,23,42,0.08)]">
          <div className="min-h-[720px] bg-[#FBFCFF]">
            <div className="min-w-0 space-y-4 p-4 lg:p-6">
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-emerald-100 bg-white px-4 py-3 shadow-[0_12px_32px_rgba(15,23,42,0.04)]">
                <div>
                  <p className="text-sm font-semibold text-slate-950">Configuracion de WhatsApp Web</p>
                  <p className="text-xs text-slate-500">
                    Conecta el telefono de la empresa y administra la sesion activa.
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge label={toStatusLabel(configuration.sessionStatus)} tone={toStatusTone(configuration.sessionStatus)} />
                  <StatusBadge
                    label={configuration.adapterReachable ? 'API local activa' : 'API local sin adaptador'}
                    tone={configuration.adapterReachable ? 'success' : 'warning'}
                  />
                </div>
              </div>

              <div className="grid gap-4 xl:grid-cols-[1.05fr_0.95fr]">
                <ConnectionStatusCard
                  configuration={configuration}
                  disconnectLoading={disconnectMutation.isPending}
                  onDisconnect={() => disconnectMutation.mutate()}
                  onReconnect={() => connectMutation.mutate()}
                  reconnectLoading={connectMutation.isPending}
                />
                <QrConnectionCard
                  configuration={configuration}
                  onRefreshQr={() => refreshQrMutation.mutate()}
                  refreshLoading={refreshQrMutation.isPending}
                />
              </div>

              <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
                <LinkedDevicesCard devices={configuration.linkedDevices} />
                <PreferencesCard
                  disabled={updatePreferencesMutation.isPending}
                  onToggle={handlePreferenceToggle}
                  preferences={configuration.preferences}
                />
              </div>

              <div className="grid gap-4 xl:grid-cols-[1fr_1fr]">
                <MainChannelCard configuration={configuration} />
                <SessionHistoryCard history={configuration.sessionHistory} />
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}

function ConnectionStatusCard({
  configuration,
  disconnectLoading,
  onDisconnect,
  onReconnect,
  reconnectLoading,
}: {
  configuration: WhatsAppConfigurationResponse
  disconnectLoading: boolean
  onDisconnect: () => void
  onReconnect: () => void
  reconnectLoading: boolean
}) {
  return (
    <Card className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-950">Estado de conexion</p>
          <p className="mt-1 text-xs text-slate-500">Sesion activa del numero empresarial.</p>
        </div>
        <StatusBadge label={toStatusLabel(configuration.sessionStatus)} tone={toStatusTone(configuration.sessionStatus)} />
      </div>

      <div className="grid gap-5 md:grid-cols-[180px_minmax(0,1fr)]">
        <div className="flex flex-col items-center justify-center rounded-[28px] border border-emerald-100 bg-emerald-50/60 p-5">
          <div className="flex h-28 w-28 items-center justify-center rounded-full border-[10px] border-emerald-200 bg-white text-emerald-600 shadow-[0_16px_40px_rgba(22,163,74,0.18)]">
            <WhatsAppIcon className="h-14 w-14" />
          </div>
          <p className="mt-4 text-center text-sm font-semibold text-slate-950">
            {formatPhone(configuration.phoneNumber)}
          </p>
          <p className="mt-1 text-center text-xs text-slate-500">{configuration.businessName}</p>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <InfoTile label="Ultima sincronizacion" value={formatDateTime(configuration.lastSynchronizationAt)} />
          <InfoTile label="Sesion activa" value={`${configuration.activeSessionHours} hora(s)`} />
          <InfoTile label="Conectado desde" value={configuration.connectedFrom} />
          <InfoTile label="Modo adaptador" value={configuration.adapterMode ?? 'Experimental'} />
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Button
          fullWidth
          loading={disconnectLoading}
          onClick={onDisconnect}
          variant="danger"
        >
          Desconectar sesion
        </Button>
        <Button
          fullWidth
          loading={reconnectLoading}
          onClick={onReconnect}
          variant="secondary"
        >
          Reiniciar sesion
        </Button>
      </div>
    </Card>
  )
}

function QrConnectionCard({
  configuration,
  onRefreshQr,
  refreshLoading,
}: {
  configuration: WhatsAppConfigurationResponse
  onRefreshQr: () => void
  refreshLoading: boolean
}) {
  return (
    <Card className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-950">Vincular nuevo dispositivo</p>
          <p className="mt-1 text-xs text-slate-500">Escanea el codigo desde WhatsApp en tu telefono.</p>
        </div>
        <Button loading={refreshLoading} onClick={onRefreshQr} size="sm">
          Generar QR
        </Button>
      </div>

      <div className="grid gap-5 md:grid-cols-[180px_minmax(0,1fr)]">
        <QrBlock qrCode={configuration.qrCode} />
        <ol className="space-y-3 text-sm text-slate-700">
          {['Abre WhatsApp en tu telefono', 'Toca Menu > Dispositivos vinculados', 'Toca Vincular un dispositivo', 'Escanea este codigo QR'].map((step, index) => (
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
        Este codigo QR es personal y temporal. No lo compartas con nadie para mantener segura la cuenta.
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

function LinkedDevicesCard({ devices }: { devices: WhatsAppConfigurationLinkedDeviceResponse[] }) {
  return (
    <Card>
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-950">Dispositivos vinculados</p>
          <p className="mt-1 text-xs text-slate-500">Datos entregados por la API local del backend.</p>
        </div>
        <StatusBadge label={`${devices.length} dispositivo(s)`} tone="info" />
      </div>

      <div className="mt-5 overflow-x-auto">
        <table className="w-full min-w-[620px] text-left text-sm">
          <thead className="text-xs uppercase tracking-[0.14em] text-slate-400">
            <tr>
              <th className="pb-3 font-semibold">Dispositivo / operador</th>
              <th className="pb-3 font-semibold">Ubicacion</th>
              <th className="pb-3 font-semibold">Estado</th>
              <th className="pb-3 font-semibold">Ultima actividad</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {devices.map((device) => (
              <tr key={device.id}>
                <td className="py-3">
                  <p className="font-semibold text-slate-950">{device.deviceName}</p>
                  <p className="text-xs text-slate-500">{device.operatorName} · {device.browser}</p>
                </td>
                <td className="py-3 text-slate-600">{device.location}</td>
                <td className="py-3">
                  <StatusBadge label={toDeviceStatusLabel(device.status)} tone={toDeviceStatusTone(device.status)} />
                </td>
                <td className="py-3 text-slate-600">{formatDateTime(device.lastActivityAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
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
        <p className="mt-1 text-xs text-slate-500">Cada cambio se guarda inmediatamente mediante API.</p>
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

function MainChannelCard({ configuration }: { configuration: WhatsAppConfigurationResponse }) {
  return (
    <Card>
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-950">Canal principal</p>
          <p className="mt-1 text-xs text-slate-500">Configuracion operativa del canal activo.</p>
        </div>
        <StatusBadge
          label={configuration.mainChannel.automaticResponsesEnabled ? 'Activo' : 'Inactivo'}
          tone={configuration.mainChannel.automaticResponsesEnabled ? 'success' : 'neutral'}
        />
      </div>
      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <InfoTile label="Nombre del canal" value={configuration.mainChannel.channelName} />
        <InfoTile label="Tipo de canal" value={configuration.mainChannel.channelType} />
        <InfoTile label="Numero asociado" value={formatPhone(configuration.mainChannel.phoneNumber)} />
        <InfoTile label="Horario de atencion" value={configuration.mainChannel.businessHours} />
      </div>
    </Card>
  )
}

function SessionHistoryCard({ history }: { history: WhatsAppConfigurationSessionHistoryResponse[] }) {
  return (
    <Card>
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-950">Historial de sesiones</p>
          <p className="mt-1 text-xs text-slate-500">Eventos recientes del canal local.</p>
        </div>
        <StatusBadge label="Ver todo" tone="info" />
      </div>
      <div className="mt-5 space-y-4">
        {history.map((item) => (
          <HistoryItem item={item} key={item.id} />
        ))}
      </div>
    </Card>
  )
}

function HistoryItem({ item }: { item: WhatsAppConfigurationSessionHistoryResponse }) {
  return (
    <div className="flex items-start gap-3">
      <span className="mt-1 h-2.5 w-2.5 rounded-full bg-emerald-500" />
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm font-semibold text-slate-900">{item.title}</p>
          <StatusBadge label={toHistoryToneLabel(item.tone)} tone={normalizeHistoryTone(item.tone)} />
        </div>
        <p className="mt-1 text-xs text-slate-500">{item.actor}</p>
        <p className="mt-1 text-xs text-slate-400">{formatDateTime(item.occurredAt)}</p>
      </div>
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
