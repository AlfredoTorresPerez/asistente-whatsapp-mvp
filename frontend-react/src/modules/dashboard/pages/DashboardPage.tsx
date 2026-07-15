import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { DataTableShell } from '../../../components/ui/DataTableShell'
import { FilterBar } from '../../../components/ui/FilterBar'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { useShellSession } from '../../../lib/shellSession'
import { formatEstadoActividad, getEstadoTone } from '../../../lib/statusFormatters'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { getDashboardSummaryRequest } from '../../../services/api/dashboardApi'
import type {
  DashboardActivityResponse,
  DashboardSeriesPointResponse,
} from '../../../services/api/types'

const fieldClassName =
  'w-full rounded-2xl border border-[var(--color-border)] bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

const dashboardFiltersSchema = z
  .object({
    from: z.string().min(1, 'Selecciona una fecha inicial.'),
    to: z.string().min(1, 'Selecciona una fecha final.'),
  })
  .refine((values) => values.from <= values.to, {
    message: 'El rango de fechas es invalido.',
    path: ['to'],
  })

type DashboardFiltersValues = z.infer<typeof dashboardFiltersSchema>

function buildDefaultFilters(): DashboardFiltersValues {
  return {
    from: dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
    to: dayjs().format('YYYY-MM-DD'),
  }
}

function toUtcRange(values: DashboardFiltersValues) {
  return {
    from: dayjs(`${values.from}T00:00:00`).toISOString(),
    to: dayjs(`${values.to}T23:59:59.999`).toISOString(),
  }
}

function formatDateLabel(value: string) {
  return dayjs(value).format('DD MMM')
}

function formatDateTime(value: string) {
  return dayjs(value).format('DD MMM, HH:mm')
}

function resolveActivityRoute(activity: DashboardActivityResponse) {
  switch (activity.entityType) {
    case 'CONVERSATION':
      return `/conversations/${activity.entityId}`
    case 'LEAD':
      return `/prospects/${activity.entityId}`
    case 'BOOKING':
      return `/appointments/${activity.entityId}`
    case 'PRODUCT':
    case 'CATALOG_PRODUCT':
      return `/catalog/products/${activity.entityId}/edit`
    case 'SERVICE':
    case 'AESTHETIC_SERVICE':
      return `/catalog/services/${activity.entityId}/edit`
    case 'RULE':
    case 'AI_RULE':
    case 'BUSINESS_RULE':
      return `/automation-rules/${activity.entityId}/edit`
    case 'BUSINESS_AI':
    case 'KNOWLEDGE':
      return '/business-ai'
    default:
      return undefined
  }
}

function isDashboardEmpty(
  openConversations: number,
  newProspects: number,
  pendingAppointments: number,
  appointmentsCount: number,
  activityCount: number,
) {
  return (
    openConversations === 0 &&
    newProspects === 0 &&
    pendingAppointments === 0 &&
    appointmentsCount === 0 &&
    activityCount === 0
  )
}

export function DashboardPage() {
  const { user } = useShellSession()
  const isOnline = useOnlineStatus()
  const [appliedFilters, setAppliedFilters] = useState<DashboardFiltersValues>(buildDefaultFilters)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DashboardFiltersValues>({
    resolver: zodResolver(dashboardFiltersSchema),
    defaultValues: appliedFilters,
  })

  const dashboardSummaryQuery = useQuery({
    queryKey: ['dashboard', 'summary', appliedFilters],
    queryFn: () => getDashboardSummaryRequest(toUtcRange(appliedFilters)),
    refetchInterval: isOnline ? 30_000 : false,
  })

  const data = dashboardSummaryQuery.data
  const kpiCards = useMemo(
    () =>
      data
        ? [
            {
              label: 'Conversaciones abiertas',
              value: String(data.kpis.openConversations),
              note: 'Abiertas o pendientes dentro del rango.',
              tone: 'info' as const,
            },
            {
              label: 'Prospectos nuevos',
              value: String(data.kpis.newProspects),
              note: 'Altas comerciales registradas en el periodo.',
              tone: 'success' as const,
            },
            {
              label: 'Citas pendientes',
              value: String(data.kpis.pendingAppointments),
              note: 'Citas pendientes o reprogramadas en el rango.',
              tone: 'neutral' as const,
            },
          ]
        : [],
    [data],
  )

  const onSubmit = handleSubmit(async (values) => {
    setAppliedFilters(values)
  })

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Link className={buttonClassName({ variant: 'primary' })} to="/conversations">
              Ver conversaciones
            </Link>
            <Link className={buttonClassName({ variant: 'secondary' })} to="/prospects">
              Ver prospectos
            </Link>
            <Link className={buttonClassName({ variant: 'secondary' })} to="/appointments">
              Ver agenda
            </Link>
          </>
        }
        description="Resumen inicial del negocio con datos reales del backend Java, agenda del dia y actividad reciente para orientar la operacion comercial."
        eyebrow="Panel principal"
        title="Panel principal"
      />

      <form className="space-y-3" onSubmit={onSubmit}>
        <FilterBar
          actions={
            <Button
              disabled={!isOnline || isSubmitting}
              loading={isSubmitting || dashboardSummaryQuery.isFetching}
              type="submit"
            >
              Actualizar
            </Button>
          }
        >
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Desde</span>
            <input className={fieldClassName} type="date" {...register('from')} />
            {errors.from ? (
              <span className="mt-2 block text-sm text-red-700">{errors.from.message}</span>
            ) : null}
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Hasta</span>
            <input className={fieldClassName} type="date" {...register('to')} />
            {errors.to ? (
              <span className="mt-2 block text-sm text-red-700">{errors.to.message}</span>
            ) : null}
          </label>

          <div className="rounded-[1.5rem] border border-[var(--color-border)] bg-slate-50 px-4 py-3">
            <p className="text-sm font-medium text-slate-700">Responsable actual</p>
            <p className="mt-2 text-base font-semibold text-slate-950">{user?.name}</p>
            <p className="mt-1 text-sm text-slate-600">
              Filtro opcional reservado para etapas posteriores.
            </p>
          </div>

          <div className="rounded-[1.5rem] border border-[var(--color-border)] bg-slate-50 px-4 py-3">
            <p className="text-sm font-medium text-slate-700">Snapshot activo</p>
            <p className="mt-2 text-base font-semibold text-slate-950">
              {formatDateLabel(appliedFilters.from)} - {formatDateLabel(appliedFilters.to)}
            </p>
            <p className="mt-1 text-sm text-slate-600">
              {dashboardSummaryQuery.dataUpdatedAt
                ? `Ultima sincronizacion ${dayjs(dashboardSummaryQuery.dataUpdatedAt).format('HH:mm:ss')}.`
                : 'Esperando la primera respuesta del backend.'}
            </p>
          </div>
        </FilterBar>
      </form>

      {dashboardSummaryQuery.isPending && !data ? (
        <LoadingState
          message="Cargando KPIs, agenda del dia y actividad reciente desde PostgreSQL."
          variant="page"
        />
      ) : null}

      {dashboardSummaryQuery.isError && !data ? (
        <ErrorState
          description="No pudimos cargar el resumen del negocio. Reintenta para consultar los datos reales."
          onRetry={() => void dashboardSummaryQuery.refetch()}
          title="No fue posible cargar el dashboard"
        />
      ) : null}

      {data &&
      isDashboardEmpty(
        data.kpis.openConversations,
        data.kpis.newProspects,
        data.kpis.pendingAppointments,
        data.todayAppointments.length,
        data.recentActivity.length,
      ) ? (
        <EmptyState
          description="Aun no hay actividad registrada para el rango seleccionado."
          primaryAction={{ label: 'Ver conversaciones', to: '/conversations' }}
          secondaryAction={{ label: 'Ver prospectos', to: '/prospects' }}
          title="Aun no hay actividad registrada"
        />
      ) : null}

      {data ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {kpiCards.map((kpi) => (
              <Card key={kpi.label} className="p-6">
                <div className="flex items-start justify-between gap-3">
                  <p className="text-sm font-medium text-slate-500">{kpi.label}</p>
                  <StatusBadge label="Activo" tone={kpi.tone} />
                </div>
                <p className="mt-4 text-4xl font-semibold text-slate-950">{kpi.value}</p>
                <p className="mt-2 text-sm text-slate-600">{kpi.note}</p>
              </Card>
            ))}
          </div>

          <div className="grid gap-4 xl:grid-cols-[1.2fr_1.2fr_0.9fr]">
            <SeriesCard
              description="Conversaciones creadas por dia en el rango seleccionado."
              series={data.conversationSeries}
              title="Actividad de conversaciones"
            />
            <Card>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                Agenda del dia
              </p>
              <h2 className="mt-3 text-2xl font-semibold text-slate-950">
                {data.todayAppointments.length} compromiso(s)
              </h2>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                Citas programadas para la jornada actual en la zona horaria del usuario.
              </p>

              <div className="mt-5 space-y-3">
                {data.todayAppointments.length === 0 ? (
                  <EmptyState
                    description="No hay citas agendadas para hoy."
                    primaryAction={{ label: 'Ver agenda', to: '/appointments' }}
                    title="Agenda sin compromisos"
                    variant="card"
                  />
                ) : (
                  data.todayAppointments.map((appointment) => (
                    <Link
                      key={appointment.id}
                      className="block rounded-[1.5rem] border border-[var(--color-border)] bg-slate-50 px-4 py-3 transition hover:border-blue-300 hover:bg-white"
                      to={`/appointments/${appointment.id}`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="text-sm font-semibold text-slate-950">
                            {appointment.subject}
                          </p>
                          <p className="mt-1 text-sm text-slate-600">{appointment.customerName}</p>
                        </div>
                        <StatusBadge
                          label={formatEstadoActividad(appointment.status)}
                          tone={getEstadoTone(appointment.status)}
                        />
                      </div>
                      <p className="mt-3 text-sm text-slate-600">
                        {formatDateTime(appointment.startsAt)} · {appointment.durationMinutes} min
                      </p>
                      <p className="mt-1 text-sm text-slate-500">
                        {appointment.location ?? 'Sin ubicacion definida'}
                      </p>
                    </Link>
                  ))
                )}
              </div>
            </Card>
          </div>

          <div className="grid gap-4 xl:grid-cols-[1.2fr_1fr]">
            <Card>
              <DataTableShell
                caption="Actividad reciente del negocio con acceso directo al detalle relacionado."
                columns={['Actividad', 'Estado', 'Momento']}
                rows={data.recentActivity.map((activity) => ({
                  id: `${activity.entityType}-${activity.entityId}`,
                  href: resolveActivityRoute(activity),
                  cells: [
                    <div key={`${activity.entityId}-title`}>
                      <p className="font-semibold text-slate-950">{activity.title}</p>
                      <p className="mt-1 text-sm leading-6 text-slate-600">{activity.body}</p>
                    </div>,
                    <div key={`${activity.entityId}-status`} className="space-y-2">
                      <StatusBadge
                        label={formatEstadoActividad(activity.status)}
                        tone={getEstadoTone(activity.status)}
                      />
                      {resolveActivityRoute(activity) ? (
                        <p className="text-xs font-medium text-blue-600">Abrir detalle</p>
                      ) : (
                        <p
                          className="text-xs font-medium text-slate-400"
                          title="No existe una pantalla de detalle implementada para este tipo de actividad."
                        >
                          Sin detalle disponible
                        </p>
                      )}
                    </div>,
                    formatDateTime(activity.occurredAt),
                  ],
                }))}
              />
            </Card>

            <Card>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                Snapshot del rango
              </p>
              <h2 className="mt-3 text-2xl font-semibold text-slate-950">
                {formatDateLabel(appliedFilters.from)} al {formatDateLabel(appliedFilters.to)}
              </h2>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                Vista resumida de la etapa comercial actual usando el dataset demo del MVP.
              </p>

              <div className="mt-5 space-y-4">
                <SummaryLine
                  label="Conversaciones con seguimiento"
                  value={data.kpis.openConversations}
                />
                <SummaryLine label="Prospectos en movimiento" value={data.kpis.newProspects} />
                <SummaryLine label="Citas pendientes" value={data.kpis.pendingAppointments} />
              </div>
            </Card>
          </div>
        </>
      ) : null}
    </section>
  )
}

function SummaryLine({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-center justify-between rounded-[1.5rem] bg-slate-50 px-4 py-3">
      <span className="text-sm font-medium text-slate-600">{label}</span>
      <span className="text-lg font-semibold text-slate-950">{value}</span>
    </div>
  )
}

function SeriesCard({
  description,
  series,
  title,
}: {
  description: string
  series: DashboardSeriesPointResponse[]
  title: string
}) {
  const maxValue = Math.max(...series.map((point) => point.value), 1)

  return (
    <Card>
      <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">Tendencia</p>
      <h2 className="mt-3 text-2xl font-semibold text-slate-950">{title}</h2>
      <p className="mt-3 text-sm leading-6 text-slate-600">{description}</p>

      <div className="mt-6 space-y-4">
        {series.map((point) => {
          const width = Math.max((point.value / maxValue) * 100, point.value > 0 ? 12 : 4)

          return (
            <div key={`${title}-${point.label}`} className="space-y-2">
              <div className="flex items-center justify-between text-sm text-slate-600">
                <span>{formatDateLabel(point.label)}</span>
                <span className="font-semibold text-slate-950">{point.value}</span>
              </div>
              <div className="h-3 overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-[linear-gradient(90deg,_#0f3d75_0%,_#2563eb_100%)]"
                  style={{ width: `${width}%` }}
                />
              </div>
            </div>
          )
        })}
      </div>
    </Card>
  )
}
