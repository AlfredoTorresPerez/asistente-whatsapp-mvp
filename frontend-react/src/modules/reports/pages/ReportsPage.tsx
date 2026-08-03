import { useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import {
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { DataTableShell } from '../../../components/ui/DataTableShell'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import {
  formatChileanCurrency,
  formatChileanDate,
  formatChileanNumber,
  formatChileanPercent,
  formatChileanShortDate,
  formatMaskedPhone,
  formatMinutesAsHours,
} from '../../../lib/formatters'
import { useShellSession } from '../../../lib/shellSession'
import { formatEstado } from '../../../lib/statusFormatters'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { usePermissions } from '../../../hooks/usePermissions'
import { getAgendaFilterOptionsRequest } from '../../../services/api/completeAgendaApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { downloadReportsCsvRequest, getReportsSummaryRequest } from '../../../services/api/reportsApi'
import type {
  ReportsAppointmentDistributionPoint,
  ReportsFunnelStageResponse,
  ReportsKpiItem,
  ReportsOccupancyResponse,
  ReportsProspectRowResponse,
  ReportsSummaryResponse,
} from '../../../services/api/types'

const STAGE_LABELS: Record<string, string> = {
  NEW: 'Nuevo',
  CONTACTED: 'Contactado',
  INTERESTED: 'Interesado',
  SCHEDULED: 'Agendado',
  WON: 'Ganado',
  LOST: 'Perdido',
}

const STAGE_TONES: Record<string, 'info' | 'success' | 'warning' | 'danger' | 'neutral'> = {
  NEW: 'info',
  CONTACTED: 'neutral',
  INTERESTED: 'warning',
  SCHEDULED: 'warning',
  WON: 'success',
  LOST: 'danger',
}

const ATTENTION_TONES: Record<string, 'info' | 'success' | 'warning' | 'danger' | 'neutral'> = {
  Pendiente: 'info',
  'Con cita': 'warning',
  Atendido: 'success',
  Cerrado: 'neutral',
  Perdido: 'danger',
}

const BOOKING_STATUS_OPTIONS = [
  { value: '', label: 'Todos los estados' },
  { value: 'SOLICITADA', label: 'Solicitada' },
  { value: 'PENDIENTE_CONFIRMACION', label: 'Pendiente confirmacion' },
  { value: 'PENDIENTE_PAGO', label: 'Pendiente pago' },
  { value: 'CONFIRMADA', label: 'Confirmada' },
  { value: 'REPROGRAMACION_PENDIENTE', label: 'Reprogramacion pendiente' },
  { value: 'REPROGRAMADA', label: 'Reprogramada' },
  { value: 'CANCELADA', label: 'Cancelada' },
  { value: 'CANCELADA_POR_CLIENTE', label: 'Cancelada por cliente' },
  { value: 'EXPIRADA', label: 'Expirada' },
  { value: 'COMPLETADA', label: 'Completada' },
  { value: 'NO_ASISTE', label: 'Inasistencia' },
]

const PIE_COLORS: Record<string, string> = {
  SOLICITADA: '#94a3b8',
  PENDIENTE_CONFIRMACION: '#fbbf24',
  PENDIENTE_PAGO: '#f97316',
  CONFIRMADA: '#22c55e',
  REPROGRAMACION_PENDIENTE: '#a855f7',
  REPROGRAMADA: '#8b5cf6',
  CANCELADA: '#ef4444',
  CANCELADA_POR_CLIENTE: '#dc2626',
  EXPIRADA: '#6b7280',
  ATENDIDA: '#3b82f6',
  COMPLETADA: '#3b82f6',
  NO_ASISTE: '#f59e0b',
}

const CHANNEL_LABELS: Record<string, string> = {
  WHATSAPP: 'WhatsApp',
  WEB: 'Sitio publico',
  MANUAL: 'Ingreso manual',
  EMAIL: 'Correo electronico',
}

function buildDefaultFilters() {
  return {
    from: dayjs().subtract(29, 'day').format('YYYY-MM-DD'),
    to: dayjs().format('YYYY-MM-DD'),
    locationId: '',
    professionalId: '',
    serviceId: '',
    bookingStatus: '',
  }
}

function formatChannel(channel: string) {
  return CHANNEL_LABELS[channel] ?? formatEstado(channel)
}

function formatKpiValue(kpi: Pick<ReportsKpiItem, 'currentValue' | 'valueType'>) {
  if (kpi.valueType === 'PERCENT') return `${formatChileanNumber(kpi.currentValue)}%`
  if (kpi.valueType === 'CURRENCY') return formatChileanCurrency(kpi.currentValue)
  if (kpi.valueType === 'HOURS') return `${formatChileanNumber(kpi.currentValue)} h`
  if (kpi.valueType === 'MINUTES') return `${formatChileanNumber(kpi.currentValue)} min`
  return formatChileanNumber(kpi.currentValue)
}

function KpiVariation({ value, lowerIsBetter }: { value: number | null; lowerIsBetter: boolean }) {
  if (value === null) {
    return <span className="text-xs font-medium text-slate-500">Sin periodo anterior</span>
  }
  if (value === 0) return <span className="text-xs text-slate-400">0%</span>
  const isPositive = lowerIsBetter ? value < 0 : value > 0
  return (
    <span className={`text-xs font-semibold ${isPositive ? 'text-emerald-600' : 'text-red-600'}`}>
      {value > 0 ? '+' : ''}
      {formatChileanPercent(value)}
    </span>
  )
}

function KpiCard({
  label,
  currentValue,
  variationPercent,
  help,
  lowerIsBetter,
  valueType,
}: {
  label: string
  currentValue: number
  variationPercent: number | null
  help: string
  lowerIsBetter: boolean
  valueType: ReportsKpiItem['valueType']
}) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
            {label}
          </p>
          <p className="mt-3 text-2xl font-semibold text-slate-950" title={help}>
            {formatKpiValue({ currentValue, valueType })}
          </p>
          <p className="mt-1 text-xs text-slate-400">
            vs. periodo anterior:{' '}
            <KpiVariation lowerIsBetter={lowerIsBetter} value={variationPercent} />
          </p>
        </div>
        <div className="group relative shrink-0">
          <span className="inline-flex h-6 w-6 cursor-help items-center justify-center rounded-full border border-slate-200 text-xs font-semibold text-slate-400">
            ?
          </span>
          <div className="absolute right-0 top-full z-10 mt-1 hidden w-64 rounded-xl border border-slate-200 bg-white p-3 text-xs leading-5 text-slate-600 shadow-lg group-hover:block">
            {help}
          </div>
        </div>
      </div>
    </Card>
  )
}

export function ReportsPage() {
  const isOnline = useOnlineStatus()
  const { user } = useShellSession()
  const { hasAnyPermission } = usePermissions()
  const [filters, setFilters] = useState(buildDefaultFilters)
  const [prospectPage, setProspectPage] = useState(0)
  const [exporting, setExporting] = useState(false)
  const [exportError, setExportError] = useState<string | null>(null)
  const pageSize = 15
  const canExportReports =
    hasAnyPermission('REPORTS_EXPORT') || user?.role === 'OWNER' || user?.role === 'ADMIN'

  const locationsQuery = useQuery({
    queryKey: ['business-locations', true],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: true }),
    staleTime: 5 * 60 * 1000,
  })

  const agendaFilterQuery = useQuery({
    queryKey: ['agenda-filter-options', filters.locationId || undefined],
    queryFn: () => getAgendaFilterOptionsRequest({ locationId: filters.locationId || undefined }),
    staleTime: 5 * 60 * 1000,
  })

  const reportsQuery = useQuery({
    queryKey: ['reports', 'summary', filters, prospectPage],
    queryFn: () =>
      getReportsSummaryRequest({
        from: filters.from,
        to: filters.to,
        locationId: filters.locationId || undefined,
        professionalId: filters.professionalId || undefined,
        serviceId: filters.serviceId || undefined,
        bookingStatus: filters.bookingStatus || undefined,
        page: prospectPage,
        size: pageSize,
      }),
    refetchInterval: isOnline ? 60_000 : false,
  })

  const data = reportsQuery.data
  const professionals = agendaFilterQuery.data?.professionals ?? []
  const services = agendaFilterQuery.data?.services ?? []
  const locations = locationsQuery.data ?? []

  const filteredProfessionals = filters.locationId
    ? professionals.filter((p) => !p.locationId || p.locationId === filters.locationId)
    : professionals

  function handleFilterChange(key: string, value: string) {
    setFilters((prev) => {
      const next = { ...prev, [key]: value }
      if (key === 'locationId') {
        next.professionalId = ''
        next.serviceId = ''
      }
      return next
    })
    setProspectPage(0)
  }

  function clearFilters() {
    setFilters(buildDefaultFilters())
    setProspectPage(0)
  }

  async function exportCsv() {
    if (!data || !canExportReports) return
    setExporting(true)
    setExportError(null)
    try {
      const blob = await downloadReportsCsvRequest({
        from: filters.from,
        to: filters.to,
        locationId: filters.locationId || undefined,
        professionalId: filters.professionalId || undefined,
        serviceId: filters.serviceId || undefined,
        bookingStatus: filters.bookingStatus || undefined,
        page: 0,
        size: 500,
      })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `reporte_${filters.from}_${filters.to}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setExportError('No fue posible exportar el reporte con los filtros actuales.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <div className="flex flex-wrap gap-2">
            <Button
              disabled={!isOnline || reportsQuery.isFetching}
              onClick={clearFilters}
              variant="secondary"
            >
              Limpiar filtros
            </Button>
            {canExportReports ? (
              <Button disabled={!data || !isOnline || exporting} onClick={exportCsv} variant="secondary">
                {exporting ? 'Exportando' : 'Exportar CSV'}
              </Button>
            ) : null}
          </div>
        }
        description="Indicadores de conversaciones, prospectos, citas y rendimiento del centro estetico."
        eyebrow="Reportes"
        title="Reportes"
      />

      {data ? (
        <p className="text-sm text-slate-500">
          Periodo: {formatChileanDate(data.period.from)} al {formatChileanDate(data.period.to)}.
          Zona horaria: {data.period.timezone}.
        </p>
      ) : null}

      {exportError ? (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {exportError}
        </div>
      ) : null}

      <div className="flex flex-wrap gap-4 items-end">
        <label className="block">
          <span className="mb-1 block text-xs font-semibold text-slate-600">Desde</span>
          <input
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
            max={filters.to}
            type="date"
            value={filters.from}
            onChange={(e) => handleFilterChange('from', e.target.value)}
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold text-slate-600">Hasta</span>
          <input
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
            min={filters.from}
            type="date"
            value={filters.to}
            onChange={(e) => handleFilterChange('to', e.target.value)}
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold text-slate-600">Sucursal</span>
          <select
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm min-w-[160px]"
            value={filters.locationId}
            onChange={(e) => handleFilterChange('locationId', e.target.value)}
          >
            <option value="">Todas las sucursales</option>
            {locations.map((l) => (
              <option key={l.id} value={l.id}>
                {l.name}
              </option>
            ))}
          </select>
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold text-slate-600">Profesional</span>
          <select
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm min-w-[160px]"
            value={filters.professionalId}
            onChange={(e) => handleFilterChange('professionalId', e.target.value)}
          >
            <option value="">Todos los profesionales</option>
            {filteredProfessionals.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold text-slate-600">Servicio</span>
          <select
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm min-w-[160px]"
            value={filters.serviceId}
            onChange={(e) => handleFilterChange('serviceId', e.target.value)}
          >
            <option value="">Todos los servicios</option>
            {services.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold text-slate-600">Estado de cita</span>
          <select
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm min-w-[180px]"
            value={filters.bookingStatus}
            onChange={(e) => handleFilterChange('bookingStatus', e.target.value)}
          >
            {BOOKING_STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {reportsQuery.isPending && !data ? (
        <LoadingState message="Cargando indicadores del negocio." variant="page" />
      ) : null}

      {reportsQuery.isError && !data ? (
        <ErrorState
          description="No pudimos cargar los reportes. Reintenta para consultar los datos."
          onRetry={() => void reportsQuery.refetch()}
          title="No fue posible cargar los reportes"
        />
      ) : null}

      {data ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
            {data.kpis.map((kpi) => (
              <KpiCard key={kpi.label} {...kpi} />
            ))}
          </div>

          <section className="space-y-3">
            <h2 className="text-lg font-semibold text-slate-900">Indicadores operativos</h2>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5">
              {data.operationalKpis.map((kpi) => (
                <KpiCard key={kpi.label} {...kpi} />
              ))}
            </div>
          </section>

          <div className="grid gap-4 xl:grid-cols-3">
            <OccupancyPanel title="Ocupacion por profesional" rows={data.occupancyByProfessional} />
            <OccupancyPanel title="Ocupacion por cabina" rows={data.occupancyByRoom} />
            <OccupancyPanel title="Ocupacion por sucursal" rows={data.occupancyByLocation} />
          </div>

          <Card className="p-5">
            <p className="mb-4 text-sm font-semibold text-slate-900">Servicios mas solicitados</p>
            {data.topServices.length === 0 ? (
              <EmptyState
                description="No hay servicios con citas en el periodo seleccionado."
                title="Sin datos"
                variant="card"
              />
            ) : (
              <DataTableShell
                caption="Servicios ordenados por cantidad de citas e ingresos estimados."
                columns={['Servicio', 'Citas', 'Ingresos estimados']}
                rows={data.topServices.map((service) => ({
                  id: service.serviceId,
                  cells: [
                    <span key={`${service.serviceId}-name`} className="font-medium text-slate-900">
                      {service.serviceName}
                    </span>,
                    <span key={`${service.serviceId}-bookings`}>
                      {formatChileanNumber(service.bookings)}
                    </span>,
                    <span key={`${service.serviceId}-revenue`}>
                      {formatChileanCurrency(service.estimatedRevenue)}
                    </span>,
                  ],
                }))}
              />
            )}
          </Card>

          <div className="grid gap-4 lg:grid-cols-[280px_1fr]">
            <Card className="space-y-4 p-5">
              <p className="text-sm font-semibold text-slate-900">Conversaciones por canal</p>
              {data.channelDistribution.length === 0 ||
              data.channelDistribution.every((c) => c.count === 0) ? (
                <EmptyState
                  description="No hay conversaciones en el periodo seleccionado."
                  title="Sin datos"
                  variant="card"
                />
              ) : (
                data.channelDistribution.map((ch) => (
                  <div key={ch.channel}>
                    <div className="flex items-center justify-between text-sm">
                      <span className="font-medium text-slate-700">{formatChannel(ch.channel)}</span>
                      <span className="font-semibold text-slate-900">{ch.count}</span>
                    </div>
                    <div className="mt-2 flex items-center gap-2">
                      <div className="h-3 flex-1 overflow-hidden rounded-full bg-slate-100">
                        <div
                          className="h-full rounded-full bg-blue-500"
                          style={{ width: `${Math.max(ch.percentage, 2)}%` }}
                        />
                      </div>
                      <span className="text-xs text-slate-500">{ch.percentage}%</span>
                    </div>
                  </div>
                ))
              )}
            </Card>

            <Card className="p-5">
              <p className="mb-4 text-sm font-semibold text-slate-900">
                Rendimiento de conversaciones
              </p>
              {data.conversationPerformance.length === 0 ? (
                <EmptyState
                  description="No hay datos de conversaciones en el periodo."
                  title="Sin datos"
                  variant="card"
                />
              ) : (
                <ResponsiveContainer width="100%" height={280}>
                  <LineChart data={data.conversationPerformance}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis
                      dataKey="date"
                      tick={{ fontSize: 11 }}
                      tickFormatter={(v) => dayjs(v).format('DD MMM')}
                    />
                    <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                    <Tooltip labelFormatter={(v) => formatChileanDate(v as string)} />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey="received"
                      stroke="#3b82f6"
                      strokeWidth={2}
                      name="Recibidas"
                      dot={false}
                    />
                    <Line
                      type="monotone"
                      dataKey="aiAnswered"
                      stroke="#14b8a6"
                      strokeWidth={2}
                      name="Respondidas por IA"
                      dot={false}
                    />
                    <Line
                      type="monotone"
                      dataKey="humanAnswered"
                      stroke="#8b5cf6"
                      strokeWidth={2}
                      name="Atencion humana"
                      dot={false}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </Card>
          </div>

          <div className="grid gap-4 lg:grid-cols-[380px_1fr]">
            <Card className="p-5">
              <p className="mb-4 text-sm font-semibold text-slate-900">Estado de citas</p>
              {data.appointmentDistribution.length === 0 ? (
                <EmptyState
                  description="No hay citas registradas en el periodo."
                  title="Sin datos"
                  variant="card"
                />
              ) : (
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie
                      data={data.appointmentDistribution}
                      dataKey="count"
                      nameKey="label"
                      cx="50%"
                      cy="50%"
                      outerRadius={80}
                    >
                      {data.appointmentDistribution.map((entry) => (
                        <Cell key={entry.status} fill={PIE_COLORS[entry.status] ?? '#94a3b8'} />
                      ))}
                    </Pie>
                    <Tooltip
                      content={({ active, payload }) => {
                        if (!active || !payload?.length) return null
                        const d = payload[0]?.payload as
                          ReportsAppointmentDistributionPoint | undefined
                        return (
                          <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs shadow-lg">
                            <p className="font-semibold text-slate-900">
                              {d?.label ?? payload[0]?.name}
                            </p>
                            <p className="text-slate-600">
                              {d?.count ?? payload[0]?.value} ({d?.percentage ?? 0}%)
                            </p>
                          </div>
                        )
                      }}
                    />
                    <Legend
                      verticalAlign="bottom"
                      align="center"
                      iconType="circle"
                      iconSize={10}
                      formatter={(value: string) => {
                        const entry = data.appointmentDistribution.find((e) => e.label === value)
                        return `${value} (${entry?.count ?? 0})`
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </Card>

            <Card className="p-5">
              <p className="mb-4 text-sm font-semibold text-slate-900">Rendimiento de citas</p>
              {data.appointmentPerformance.length === 0 ||
              data.appointmentPerformance.every(
                (p) => p.solicitada + p.confirmada + p.completada + p.cancelada + p.ausencia === 0,
              ) ? (
                <EmptyState
                  description="No hay citas registradas en el periodo."
                  title="Sin datos"
                  variant="card"
                />
              ) : (
                <div className="overflow-x-auto max-h-[280px] overflow-y-auto">
                  <table className="w-full text-xs">
                    <thead className="sticky top-0 bg-white">
                      <tr className="text-left text-slate-500">
                        <th className="pb-2 pr-3 font-semibold">Fecha</th>
                        <th className="pb-2 pr-3 font-semibold">Solicitada</th>
                        <th className="pb-2 pr-3 font-semibold">Confirmada</th>
                        <th className="pb-2 pr-3 font-semibold">Completada</th>
                        <th className="pb-2 pr-3 font-semibold">Cancelada</th>
                        <th className="pb-2 font-semibold">No asiste</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.appointmentPerformance.map((point) => (
                        <tr key={point.date} className="border-t border-slate-100">
                          <td className="py-2 pr-3 text-slate-700">
                            {formatChileanShortDate(point.date)}
                          </td>
                          <td className="py-2 pr-3 font-medium text-blue-600">
                            {point.solicitada}
                          </td>
                          <td className="py-2 pr-3 font-medium text-emerald-600">
                            {point.confirmada}
                          </td>
                          <td className="py-2 pr-3 font-medium text-slate-900">
                            {point.completada}
                          </td>
                          <td className="py-2 pr-3 font-medium text-red-500">{point.cancelada}</td>
                          <td className="py-2 font-medium text-amber-600">{point.ausencia}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>
          </div>

          <Card className="p-5">
            <p className="mb-4 text-sm font-semibold text-slate-900">
              Embudo de conversion comercial
            </p>
            {data.conversionFunnel.length === 0 ||
            data.conversionFunnel.every((f) => f.count === 0) ? (
              <EmptyState
                description="No hay datos suficientes para el embudo de conversion."
                title="Sin datos"
                variant="card"
              />
            ) : (
              <FunnelChart funnel={data.conversionFunnel} />
            )}
          </Card>

          <ProspectTable
            page={prospectPage}
            pageSize={pageSize}
            prospects={data.prospects}
            onPageChange={setProspectPage}
          />
        </>
      ) : null}
    </section>
  )
}

function FunnelChart({ funnel }: { funnel: ReportsFunnelStageResponse[] }) {
  const maxCount = Math.max(...funnel.map((f) => f.count), 1)

  return (
    <div className="space-y-3">
      {funnel.map((stage) => {
        const widthPct = maxCount > 0 ? (stage.count / maxCount) * 100 : 0
        return (
          <div key={stage.name} className="flex items-center gap-4">
            <span className="w-44 text-right text-sm font-medium text-slate-700 shrink-0">
              {stage.name}
            </span>
            <div className="flex-1 flex items-center gap-3">
              <div
                className="h-8 rounded-lg bg-gradient-to-r from-blue-500 to-blue-400 transition-all"
                style={{ width: `${Math.max(widthPct, 2)}%` }}
              />
              <span className="text-sm font-semibold text-slate-900 whitespace-nowrap">
                {stage.count}
              </span>
            </div>
            <div className="w-28 text-right shrink-0">
              {stage.conversionFromPrevious != null ? (
                <span className="text-xs text-slate-500">
                  {stage.conversionFromPrevious}% vs. anterior
                </span>
              ) : (
                <span className="text-xs text-slate-400">---</span>
              )}
            </div>
            <div className="w-28 text-right shrink-0">
              {stage.conversionFromFirst != null ? (
                <span className="text-xs text-emerald-600 font-medium">
                  {stage.conversionFromFirst}% acumulado
                </span>
              ) : (
                <span className="text-xs text-slate-400">100%</span>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}

function OccupancyPanel({ rows, title }: { rows: ReportsOccupancyResponse[]; title: string }) {
  return (
    <Card className="p-5">
      <p className="mb-4 text-sm font-semibold text-slate-900">{title}</p>
      {rows.length === 0 || rows.every((row) => row.availableMinutes + row.reservedMinutes === 0) ? (
        <EmptyState
          description="No hay capacidad configurada para el periodo seleccionado."
          title="Sin datos"
          variant="card"
        />
      ) : (
        <div className="space-y-4">
          {rows.map((row) => {
            const percent = row.occupancyPercent ?? 0
            return (
              <div key={row.id} className="space-y-2">
                <div className="flex items-start justify-between gap-3">
                  <span className="min-w-0 font-medium text-slate-800">{row.name}</span>
                  <span className="shrink-0 text-sm font-semibold text-slate-950">
                    {row.occupancyPercent === null
                      ? 'Sin horario'
                      : `${formatChileanNumber(row.occupancyPercent)}%`}
                  </span>
                </div>
                <div className="h-3 overflow-hidden rounded-full bg-slate-100" aria-hidden="true">
                  <div
                    className="h-full rounded-full bg-emerald-500"
                    style={{ width: `${Math.min(Math.max(percent, 2), 100)}%` }}
                  />
                </div>
                <p className="text-xs text-slate-500">
                  {formatMinutesAsHours(row.reservedMinutes)} reservadas de{' '}
                  {formatMinutesAsHours(row.availableMinutes)} disponibles
                </p>
              </div>
            )
          })}
        </div>
      )}
    </Card>
  )
}

function ProspectTable({
  page,
  pageSize,
  prospects,
  onPageChange,
}: {
  page: number
  pageSize: number
  prospects: ReportsSummaryResponse['prospects']
  onPageChange: (page: number) => void
}) {
  const totalPages = Math.max(1, Math.ceil(prospects.total / pageSize))

  return (
    <Card className="p-0">
      <div className="flex flex-col gap-3 border-b border-slate-200 px-5 py-4 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
            Vista operacional
          </p>
          <h2 className="mt-2 text-xl font-semibold text-slate-900">Prospectos y actividad</h2>
          <p className="mt-1 text-sm text-slate-500">{prospects.total} registro(s) encontrado(s)</p>
        </div>
      </div>

      {prospects.items.length === 0 ? (
        <div className="px-5 py-8">
          <EmptyState
            description="No se encontraron prospectos para el periodo y filtros seleccionados."
            title="Sin resultados"
            variant="card"
          />
        </div>
      ) : (
        <DataTableShell
          caption="Listado de prospectos con actividad comercial, estado de atencion y acceso al detalle."
          columns={[
            'Contacto',
            'Telefono',
            'Ultimo contacto',
            'Etapa',
            'Responsable',
            'Proxima cita',
            'Sucursal',
            'Servicio de interes',
            'Estado atencion',
          ]}
          rows={prospects.items.map((p) => buildProspectRow(p))}
        />
      )}

      {totalPages > 1 ? (
        <div className="flex items-center justify-between border-t border-slate-200 px-5 py-3">
          <span className="text-sm text-slate-500">
            Pagina {page + 1} de {totalPages}
          </span>
          <div className="flex gap-2">
            <Button
              disabled={page === 0}
              onClick={() => onPageChange(page - 1)}
              variant="secondary"
            >
              Anterior
            </Button>
            <Button
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
              variant="secondary"
            >
              Siguiente
            </Button>
          </div>
        </div>
      ) : null}
    </Card>
  )
}

function buildProspectRow(p: ReportsProspectRowResponse) {
  return {
    id: p.id,
    href: `/prospects/${p.id}`,
    cells: [
      <span key={`${p.id}-name`} className="font-medium text-slate-900">
        {p.name}
      </span>,
      <span key={`${p.id}-phone`} className="text-slate-600">
        {formatMaskedPhone(p.phone)}
      </span>,
      <span key={`${p.id}-contact`} className="text-slate-600">
        {formatChileanShortDate(p.lastContact)}
      </span>,
      <StatusBadge
        key={`${p.id}-stage`}
        label={STAGE_LABELS[p.stage] ?? p.stage}
        tone={STAGE_TONES[p.stage] ?? 'neutral'}
      />,
      <span key={`${p.id}-resp`} className="text-slate-600">
        {p.responsible ?? '---'}
      </span>,
      <span key={`${p.id}-next`} className="text-slate-600">
        {formatChileanShortDate(p.nextAppointment)}
      </span>,
      <span key={`${p.id}-loc`} className="text-slate-600">
        {p.location ?? '---'}
      </span>,
      <span key={`${p.id}-svc`} className="text-slate-600">
        {p.serviceInterest ?? '---'}
      </span>,
      <StatusBadge
        key={`${p.id}-attn`}
        label={p.attentionStatus}
        tone={ATTENTION_TONES[p.attentionStatus] ?? 'neutral'}
      />,
    ],
  }
}
