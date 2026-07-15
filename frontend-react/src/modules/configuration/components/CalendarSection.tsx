import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { usePermissions } from '../../../hooks/usePermissions'
import { useToast } from '../../../lib/toast'
import {
  connectGoogleCalendarRequest,
  disconnectCalendarRequest,
  getCalendarListRequest,
  getCalendarStatusRequest,
  selectCalendarRequest,
} from '../../../services/api/calendarApi'
import type { CalendarAccountResponse } from '../../../services/api/types'

const calendarStatusQueryKey = ['calendar', 'status'] as const

function formatDateTime(value: string | null) {
  if (!value) return 'Sin registro'
  return dayjs(value).format('DD/MM/YYYY HH:mm')
}

function toStatusLabel(status: string) {
  switch (status) {
    case 'CONNECTED':
      return 'Conectado'
    case 'EXPIRED':
      return 'Expirado'
    case 'REVOKED':
      return 'Revocado'
    case 'PENDING':
      return 'Pendiente'
    default:
      return 'Desconectado'
  }
}

function toStatusTone(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
  switch (status) {
    case 'CONNECTED':
      return 'success'
    case 'PENDING':
    case 'EXPIRED':
      return 'warning'
    case 'REVOKED':
      return 'danger'
    default:
      return 'neutral'
  }
}

function InfoTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[18px] border border-slate-100 bg-white px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-950">{value}</p>
    </div>
  )
}

export function CalendarSection() {
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const { hasPermission } = usePermissions()
  const [searchParams, setSearchParams] = useSearchParams()
  const [disconnectTarget, setDisconnectTarget] = useState<CalendarAccountResponse | null>(null)

  const canManage = hasPermission('CALENDAR_CONFIG_MANAGE')

  const calendarStatusQuery = useQuery({
    queryKey: calendarStatusQueryKey,
    queryFn: getCalendarStatusRequest,
    refetchInterval: 30_000,
  })

  const accounts = calendarStatusQuery.data ?? []
  const connectedAccount = accounts.find(
    (a) => a.provider === 'GOOGLE' && a.authorizationStatus === 'CONNECTED',
  )

  const calendarListQuery = useQuery({
    queryKey: ['calendar', 'list', connectedAccount?.id],
    queryFn: () => getCalendarListRequest(connectedAccount!.id),
    enabled: Boolean(connectedAccount && connectedAccount.active),
    staleTime: 60_000,
  })

  const selectCalendarMutation = useMutation({
    mutationFn: ({
      accountId,
      calendarId,
      calendarSummary,
    }: {
      accountId: string
      calendarId: string
      calendarSummary: string
    }) => selectCalendarRequest(accountId, calendarId, calendarSummary),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: calendarStatusQueryKey })
      showToast({
        title: 'Calendario seleccionado',
        description: 'El calendario de Google se vinculó correctamente.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo seleccionar el calendario',
        description: 'Verifica que el calendario esté disponible e intenta nuevamente.',
        tone: 'error',
      })
    },
  })

  const disconnectMutation = useMutation({
    mutationFn: (accountId: string) => disconnectCalendarRequest(accountId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: calendarStatusQueryKey })
      setDisconnectTarget(null)
      showToast({
        title: 'Cuenta desconectada',
        description: 'La integración con Google Calendar se eliminó.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo desconectar',
        description: 'Verifica el estado de la integración e intenta nuevamente.',
        tone: 'error',
      })
    },
  })

  useEffect(() => {
    const status = searchParams.get('calendar')
    if (!status) return

    const nextParams = new URLSearchParams(searchParams)
    nextParams.delete('calendar')
    setSearchParams(nextParams, { replace: true })

    switch (status) {
      case 'connected':
        showToast({
          title: 'Google Calendar vinculado',
          description: 'La cuenta de Google se vinculó correctamente.',
          tone: 'success',
        })
        calendarStatusQuery.refetch()
        break
      case 'error':
        showToast({
          title: 'Error al vincular',
          description: 'Ocurrió un error durante la vinculación con Google.',
          tone: 'error',
        })
        break
      case 'denied':
        showToast({
          title: 'Vinculación cancelada',
          description: 'No se otorgaron los permisos necesarios para acceder al calendario.',
          tone: 'warning',
        })
        break
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="overflow-hidden rounded-[32px] border border-[var(--color-border)] bg-white shadow-[0_28px_90px_rgba(15,23,42,0.08)]">
      <div className="bg-[#FBFCFF]">
        <div className="min-w-0 space-y-4 p-4 lg:p-6">
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-emerald-100 bg-white px-4 py-3 shadow-[0_12px_32px_rgba(15,23,42,0.04)]">
            <div>
              <p className="text-sm font-semibold text-slate-950">Integración Google Calendar</p>
              <p className="text-xs text-slate-500">
                Sincroniza tus reservas con Google Calendar automáticamente.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              {accounts.length > 0 ? (
                <StatusBadge label={`${accounts.length} cuenta(s)`} tone="info" />
              ) : null}
            </div>
          </div>

          {calendarStatusQuery.isPending ? (
            <LoadingState message="Cargando configuración de Google Calendar." variant="page" />
          ) : calendarStatusQuery.isError ? (
            <ErrorState
              description="No pudimos recuperar la configuración de Google Calendar."
              onRetry={() => void calendarStatusQuery.refetch()}
              title="No fue posible cargar configuración"
            />
          ) : accounts.length === 0 ? (
            <Card className="space-y-4">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-semibold text-slate-950">No configurado</p>
                  <p className="mt-1 text-xs text-slate-500">
                    Conecta una cuenta de Google para sincronizar tus reservas.
                  </p>
                </div>
              </div>
              {canManage ? (
                <Button onClick={() => connectGoogleCalendarRequest()}>
                  Conectar Google Calendar
                </Button>
              ) : null}
            </Card>
          ) : (
            <div className="space-y-4">
              {accounts.map((account) => (
                <AccountCard
                  account={account}
                  calendarList={calendarListQuery.data ?? []}
                  calendarListLoading={calendarListQuery.isFetching}
                  canManage={canManage}
                  disconnectLoading={
                    disconnectMutation.isPending && disconnectTarget?.id === account.id
                  }
                  selectCalendarLoading={selectCalendarMutation.isPending}
                  onConnect={() => connectGoogleCalendarRequest()}
                  onDisconnect={() => setDisconnectTarget(account)}
                  onSelectCalendar={(calendarId, calendarSummary) =>
                    selectCalendarMutation.mutate({ accountId: account.id, calendarId, calendarSummary })
                  }
                  key={account.id}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      <ConfirmDialog
        confirmLabel="Desconectar"
        confirmLoading={disconnectMutation.isPending}
        description="Se eliminará la integración y se detendrá la sincronización de reservas con Google Calendar."
        onCancel={() => setDisconnectTarget(null)}
        onConfirm={() => {
          if (disconnectTarget) {
            disconnectMutation.mutate(disconnectTarget.id)
          }
        }}
        open={disconnectTarget !== null}
        title="Desconectar Google Calendar"
        tone="danger"
      />
    </div>
  )
}

function AccountCard({
  account,
  calendarList,
  calendarListLoading,
  canManage,
  disconnectLoading,
  selectCalendarLoading,
  onConnect,
  onDisconnect,
  onSelectCalendar,
}: {
  account: CalendarAccountResponse
  calendarList: Array<{ id: string; summary: string; primary: boolean; accessRole: string }>
  calendarListLoading: boolean
  canManage: boolean
  disconnectLoading: boolean
  selectCalendarLoading: boolean
  onConnect: () => void
  onDisconnect: () => void
  onSelectCalendar: (calendarId: string, calendarSummary: string) => void
}) {
  const isConnected = account.authorizationStatus === 'CONNECTED'

  return (
    <Card className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-slate-950">
            {account.provider === 'GOOGLE' ? 'Google' : account.provider}
          </p>
          <p className="mt-1 text-xs text-slate-500">
            {account.emailMasked ?? 'Sin correo vinculado'}
          </p>
        </div>
        <StatusBadge label={toStatusLabel(account.authorizationStatus)} tone={toStatusTone(account.authorizationStatus)} />
      </div>

      {account.requiresReconnect ? (
        <div className="rounded-[18px] border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <p className="font-semibold">Requiere reconexión</p>
          <p className="mt-1 text-xs leading-5">
            La autorización con Google expiró o fue revocada. Conecta nuevamente la cuenta.
          </p>
        </div>
      ) : null}

      {!account.active && account.revokedAt ? (
        <div className="rounded-[18px] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
          <p className="font-semibold">Cuenta revocada</p>
          <p className="mt-1 text-xs leading-5">
            La integración fue revocada el {formatDateTime(account.revokedAt)}.
          </p>
        </div>
      ) : null}

      <div className="grid gap-3 sm:grid-cols-2">
        <InfoTile label="Conectada desde" value={formatDateTime(account.connectedAt)} />
        <InfoTile label="Última sincronización" value={formatDateTime(account.lastSyncAt)} />
        <InfoTile
          label="Calendario seleccionado"
          value={account.calendarSummary ?? 'Sin calendario'}
        />
      </div>

      {isConnected && account.active ? (
        <div className="space-y-3">
          <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
            Calendarios disponibles
          </p>
          {calendarListLoading ? (
            <p className="text-sm text-slate-500">Cargando calendarios...</p>
          ) : calendarList.length === 0 ? (
            <p className="text-sm text-slate-500">
              No hay calendarios disponibles para esta cuenta.
            </p>
          ) : (
            <Select
              disabled={selectCalendarLoading || !canManage}
              onChange={(e) => {
                const option = e.target.options[e.target.selectedIndex]
                onSelectCalendar(e.target.value, option.text)
              }}
              options={[
                ...(calendarList.some((c) => c.id === account.calendarId)
                  ? []
                  : [{ label: account.calendarSummary ?? 'Seleccionar calendario', value: '' }]),
                ...calendarList.map((cal) => ({
                  label: `${cal.summary}${cal.primary ? ' (principal)' : ''}`,
                  value: cal.id,
                })),
              ]}
              value={account.calendarId ?? ''}
            />
          )}
        </div>
      ) : null}

      <div className="grid gap-3 sm:grid-cols-2">
        {canManage && account.requiresReconnect ? (
          <Button fullWidth onClick={onConnect} variant="secondary">
            Reconectar
          </Button>
        ) : null}
        {canManage && isConnected ? (
          <Button
            fullWidth
            loading={disconnectLoading}
            onClick={onDisconnect}
            variant="danger"
          >
            Desconectar
          </Button>
        ) : null}
        {canManage && !isConnected && !account.requiresReconnect ? (
          <Button fullWidth onClick={onConnect}>
            Conectar Google Calendar
          </Button>
        ) : null}
      </div>
    </Card>
  )
}
