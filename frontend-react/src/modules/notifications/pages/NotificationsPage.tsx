import { zodResolver } from '@hookform/resolvers/zod'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { FilterBar } from '../../../components/ui/FilterBar'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useToast } from '../../../lib/toast'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import {
  getNotificationsRequest,
  markAllNotificationsAsReadRequest,
  markNotificationAsReadRequest,
} from '../../../services/api/notificationsApi'
import type { NotificationResponse } from '../../../services/api/types'

const fieldClassName =
  'w-full rounded-2xl border border-[var(--color-border)] bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

const notificationsFiltersSchema = z.object({
  search: z.string().max(80, 'La busqueda no puede superar los 80 caracteres.'),
  status: z.string(),
  type: z.string(),
})

type NotificationsFiltersValues = z.infer<typeof notificationsFiltersSchema>

const defaultFilters: NotificationsFiltersValues = {
  search: '',
  status: '',
  type: '',
}

const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  NEW_MESSAGE: 'Nuevo mensaje',
}

const NOTIFICATION_STATUS_LABELS: Record<string, string> = {
  UNREAD: 'No leída',
  READ: 'Leída',
}

function formatNotificationType(type: string) {
  return NOTIFICATION_TYPE_LABELS[type] ?? type
}

function formatNotificationStatus(status: string) {
  return NOTIFICATION_STATUS_LABELS[status] ?? status
}

function getStatusTone(status: string) {
  switch (status) {
    case 'UNREAD':
      return 'warning'
    case 'READ':
      return 'success'
    default:
      return 'neutral'
  }
}

function resolveNotificationRoute(notification: NotificationResponse) {
  if (!notification.relatedEntityId || !notification.relatedEntityType) {
    return null
  }

  switch (notification.relatedEntityType) {
    case 'CONVERSATION':
      return `/conversations/${notification.relatedEntityId}`
    case 'LEAD':
      return `/prospects/${notification.relatedEntityId}`
    case 'BOOKING':
      return `/appointments/${notification.relatedEntityId}`
    case 'ORDER':
      return `/orders/${notification.relatedEntityId}`
    default:
      return null
  }
}

export function NotificationsPage() {
  const isOnline = useOnlineStatus()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [page, setPage] = useState(0)
  const [appliedFilters, setAppliedFilters] = useState<NotificationsFiltersValues>(defaultFilters)
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<NotificationsFiltersValues>({
    resolver: zodResolver(notificationsFiltersSchema),
    defaultValues: defaultFilters,
  })

  const notificationsQuery = useQuery({
    queryKey: ['notifications', 'list', appliedFilters, page],
    queryFn: () =>
      getNotificationsRequest({
        page,
        size: 10,
        search: appliedFilters.search || undefined,
        status: appliedFilters.status || undefined,
        type: appliedFilters.type || undefined,
      }),
    placeholderData: keepPreviousData,
    refetchInterval: isOnline ? 30_000 : false,
  })

  const markAsReadMutation = useMutation({
    mutationFn: markNotificationAsReadRequest,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
      showToast({
        title: 'Notificacion actualizada',
        description: 'La notificacion se marco como leida correctamente.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No se pudo actualizar la notificacion',
        description: 'Reintenta en unos segundos para volver a marcarla como leida.',
        tone: 'error',
      })
    },
  })

  const markAllAsReadMutation = useMutation({
    mutationFn: markAllNotificationsAsReadRequest,
    onSuccess: (response) => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
      showToast({
        title: 'Centro de notificaciones actualizado',
        description:
          response.updatedCount > 0
            ? `Se marcaron ${response.updatedCount} notificacion(es) como leidas.`
            : 'No habia notificaciones pendientes por actualizar.',
        tone: 'success',
      })
    },
    onError: () => {
      showToast({
        title: 'No pudimos marcar las notificaciones',
        description: 'La operacion no se pudo completar. Intenta nuevamente.',
        tone: 'error',
      })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    setPage(0)
    setAppliedFilters(values)
  })

  const applyStatusTab = (status: '' | 'UNREAD') => {
    const nextFilters = {
      ...appliedFilters,
      status,
    }
    setValue('status', status)
    setPage(0)
    setAppliedFilters(nextFilters)
  }

  const clearFilters = () => {
    reset(defaultFilters)
    setPage(0)
    setAppliedFilters(defaultFilters)
  }

  const openNotification = async (notification: NotificationResponse) => {
    const route = resolveNotificationRoute(notification)

    if (!route) {
      showToast({
        title: 'La notificacion no tiene un destino asociado',
        description: 'Todavia no existe una entidad relacionada para abrir desde esta alerta.',
        tone: 'warning',
      })
      return
    }

    if (notification.status === 'UNREAD' && isOnline) {
      try {
        await markAsReadMutation.mutateAsync(notification.id)
      } catch {
        return
      }
    }

    navigate(route)
  }

  const currentItems = notificationsQuery.data?.items ?? []
  const pagedNotifications = notificationsQuery.data

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Button
            disabled={!isOnline || markAllAsReadMutation.isPending}
            loading={markAllAsReadMutation.isPending}
            onClick={() => void markAllAsReadMutation.mutateAsync()}
          >
            Marcar todas como leidas
          </Button>
        }
        description="Centro basico de notificaciones del negocio, con filtros, tabs y acceso directo a la entidad relacionada."
        eyebrow="Alertas"
        title="Centro de notificaciones"
      />

      <div className="flex flex-wrap gap-3">
        <button
          className={[
            'rounded-full border px-4 py-2 text-sm font-semibold transition',
            appliedFilters.status === ''
              ? 'border-blue-200 bg-blue-50 text-blue-700'
              : 'border-[var(--color-border)] bg-white text-slate-600 hover:border-blue-200',
          ]
            .join(' ')
            .trim()}
          onClick={() => applyStatusTab('')}
          type="button"
        >
          Todas
        </button>
        <button
          className={[
            'rounded-full border px-4 py-2 text-sm font-semibold transition',
            appliedFilters.status === 'UNREAD'
              ? 'border-blue-200 bg-blue-50 text-blue-700'
              : 'border-[var(--color-border)] bg-white text-slate-600 hover:border-blue-200',
          ]
            .join(' ')
            .trim()}
          onClick={() => applyStatusTab('UNREAD')}
          type="button"
        >
          No leidas
        </button>
      </div>

      <form className="space-y-3" onSubmit={onSubmit}>
        <FilterBar
          actions={
            <>
              <Button disabled={!isOnline || isSubmitting} loading={isSubmitting} type="submit">
                Aplicar filtros
              </Button>
              <Button disabled={isSubmitting} onClick={clearFilters} variant="secondary">
                Limpiar filtros
              </Button>
            </>
          }
        >
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Busqueda</span>
            <input
              className={fieldClassName}
              placeholder="Buscar por titulo o detalle"
              type="search"
              {...register('search')}
            />
            {errors.search ? (
              <span className="mt-2 block text-sm text-red-700">{errors.search.message}</span>
            ) : null}
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Estado</span>
            <select className={fieldClassName} {...register('status')}>
              <option value="">Todos</option>
              <option value="UNREAD">No leidas</option>
              <option value="READ">Leidas</option>
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Tipo</span>
            <select className={fieldClassName} {...register('type')}>
              <option value="">Todos</option>
              <option value="NEW_MESSAGE">Nuevo mensaje</option>
            </select>
          </label>

          <div className="rounded-[1.5rem] border border-[var(--color-border)] bg-slate-50 px-4 py-3">
            <p className="text-sm font-medium text-slate-700">Snapshot del listado</p>
            <p className="mt-2 text-base font-semibold text-slate-950">
              {notificationsQuery.data?.totalItems ?? 0} resultado(s)
            </p>
            <p className="mt-1 text-sm text-slate-600">
              {notificationsQuery.dataUpdatedAt
                ? `Ultima sincronizacion ${dayjs(notificationsQuery.dataUpdatedAt).format('HH:mm:ss')}.`
                : 'Esperando la primera carga del centro de notificaciones.'}
            </p>
          </div>
        </FilterBar>
      </form>

      {notificationsQuery.isPending && !notificationsQuery.data ? (
        <LoadingState
          message="Cargando notificaciones reales y preparando el centro de alertas."
          variant="table"
        />
      ) : null}

      {notificationsQuery.isError && !notificationsQuery.data ? (
        <ErrorState
          description="No pudimos recuperar las notificaciones del usuario autenticado. Reintenta para volver a consultar el centro."
          onRetry={() => void notificationsQuery.refetch()}
          title="No fue posible cargar las notificaciones"
        />
      ) : null}

      {notificationsQuery.data && currentItems.length === 0 ? (
        <EmptyState
          description="No tienes notificaciones para los filtros actuales."
          primaryAction={{ label: 'Volver al panel', to: '/dashboard' }}
          title="No tienes notificaciones"
        />
      ) : null}

      {pagedNotifications && currentItems.length > 0 ? (
        <Card className="overflow-hidden p-0">
          <div className="overflow-x-auto">
            <table className="min-w-full border-separate border-spacing-0">
              <thead>
                <tr className="bg-slate-50">
                  {['Notificacion', 'Tipo', 'Estado', 'Momento', 'Acciones'].map((column) => (
                    <th
                      key={column}
                      className="border-b border-[var(--color-border)] px-5 py-3 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-500"
                      scope="col"
                    >
                      {column}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white">
                {currentItems.map((notification) => {
                  const isCurrentRowPending =
                    markAsReadMutation.isPending && markAsReadMutation.variables === notification.id

                  return (
                    <tr key={notification.id} className="align-top transition hover:bg-slate-50">
                      <td className="border-b border-[var(--color-border)] px-5 py-4">
                        <button
                          className="text-left"
                          onClick={() => void openNotification(notification)}
                          type="button"
                        >
                          <p className="text-sm font-semibold text-slate-950">
                            {notification.title}
                          </p>
                          <p className="mt-2 max-w-xl text-sm leading-6 text-slate-600">
                            {notification.body}
                          </p>
                        </button>
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4">
                        <StatusBadge
                          label={formatNotificationType(notification.type)}
                          tone="info"
                        />
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4">
                        <StatusBadge
                          label={formatNotificationStatus(notification.status)}
                          tone={getStatusTone(notification.status)}
                        />
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4 text-sm text-slate-600">
                        <p>{dayjs(notification.createdAt).format('DD MMM YYYY')}</p>
                        <p className="mt-1 text-xs uppercase tracking-[0.16em] text-slate-500">
                          {dayjs(notification.createdAt).format('HH:mm')}
                        </p>
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4">
                        <div className="flex flex-wrap justify-end gap-2">
                          {notification.status === 'UNREAD' ? (
                            <Button
                              disabled={!isOnline || isCurrentRowPending}
                              loading={isCurrentRowPending}
                              onClick={() => void markAsReadMutation.mutateAsync(notification.id)}
                              size="sm"
                              variant="secondary"
                            >
                              Marcar leida
                            </Button>
                          ) : null}
                          <Button
                            onClick={() => void openNotification(notification)}
                            size="sm"
                            variant="primary"
                          >
                            Abrir detalle
                          </Button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <div className="flex flex-col gap-3 border-t border-[var(--color-border)] bg-slate-50 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-slate-600">
              Pagina {pagedNotifications.page + 1} de {Math.max(pagedNotifications.totalPages, 1)}
            </p>
            <div className="flex flex-wrap gap-2">
              <Button
                disabled={page === 0 || notificationsQuery.isFetching}
                onClick={() => setPage((currentPage) => Math.max(currentPage - 1, 0))}
                size="sm"
                variant="secondary"
              >
                Anterior
              </Button>
              <Button
                disabled={
                  pagedNotifications.totalPages === 0 ||
                  page >= pagedNotifications.totalPages - 1 ||
                  notificationsQuery.isFetching
                }
                onClick={() => setPage((currentPage) => currentPage + 1)}
                size="sm"
                variant="secondary"
              >
                Siguiente
              </Button>
            </div>
          </div>
        </Card>
      ) : null}
    </section>
  )
}
