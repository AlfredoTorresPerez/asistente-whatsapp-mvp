import { zodResolver } from '@hookform/resolvers/zod'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
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
import { useShellSession } from '../../../lib/shellSession'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { getLeadsRequest } from '../../../services/api/leadsApi'
import {
  getLeadOriginLabel,
  getLeadStageLabel,
  getLeadStageTone,
  leadOriginOptions,
} from '../leadOptions'

const fieldClassName =
  'w-full rounded-2xl border border-[var(--color-border)] bg-white px-4 py-3 text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

const PAGE_SIZE = 10

const filtersSchema = z.object({
  search: z.string().max(80, 'La busqueda no puede superar los 80 caracteres.'),
  stage: z.string(),
  origin: z.string(),
  assignedUserId: z.string(),
})

type FiltersValues = z.infer<typeof filtersSchema>

const defaultFilters: FiltersValues = {
  search: '',
  stage: '',
  origin: '',
  assignedUserId: '',
}

export function ProspectsPage() {
  const navigate = useNavigate()
  const { user } = useShellSession()
  const isOnline = useOnlineStatus()
  const [page, setPage] = useState(0)
  const [appliedFilters, setAppliedFilters] = useState<FiltersValues>(defaultFilters)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FiltersValues>({
    resolver: zodResolver(filtersSchema),
    defaultValues: defaultFilters,
  })

  const leadsQuery = useQuery({
    queryKey: ['leads', 'list', appliedFilters, page],
    queryFn: () =>
      getLeadsRequest({
        page,
        size: PAGE_SIZE,
        search: appliedFilters.search || undefined,
        stage: appliedFilters.stage || undefined,
        origin: appliedFilters.origin || undefined,
        assignedUserId: appliedFilters.assignedUserId || undefined,
      }),
    placeholderData: keepPreviousData,
    refetchInterval: isOnline ? 30_000 : false,
  })

  const onSubmitFilters = handleSubmit(async (values) => {
    setPage(0)
    setAppliedFilters(values)
  })

  const clearFilters = () => {
    reset(defaultFilters)
    setPage(0)
    setAppliedFilters(defaultFilters)
  }

  const leads = leadsQuery.data?.items ?? []

  return (
    <section className="space-y-6">
      <PageHeader
        actions={<Button onClick={() => navigate('/prospects/new')}>Crear prospecto</Button>}
        description="Embudo comercial para prospectos demo con filtros por estado, origen y responsable, ademas de acceso al detalle y seguimiento."
        eyebrow="Prospectos"
        title="Prospectos"
      />

      {!isOnline ? (
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-sm font-semibold text-amber-900">Estado sin conexion</p>
          <p className="mt-2 text-sm leading-6 text-amber-800">
            Puedes revisar los ultimos datos cacheados, pero no se sincronizaran cambios nuevos
            hasta recuperar internet.
          </p>
        </Card>
      ) : null}

      <form onSubmit={onSubmitFilters}>
        <FilterBar
          actions={
            <>
              <Button disabled={isSubmitting} loading={isSubmitting} type="submit">
                Aplicar filtros
              </Button>
              <Button onClick={clearFilters} variant="secondary">
                Limpiar filtros
              </Button>
            </>
          }
        >
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Busqueda</span>
            <input
              className={fieldClassName}
              placeholder="Buscar por nombre, telefono, correo o nota"
              type="search"
              {...register('search')}
            />
            {errors.search ? (
              <span className="mt-2 block text-sm text-red-700">{errors.search.message}</span>
            ) : null}
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Estado</span>
            <select className={fieldClassName} {...register('stage')}>
              <option value="">Todos</option>
              <option value="NEW">Nueva</option>
              <option value="CONTACTED">Contactada</option>
              <option value="INTERESTED">Interesada</option>
              <option value="SCHEDULED">Agendada</option>
              <option value="WON">Ganada</option>
              <option value="LOST">Perdida</option>
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Origen</span>
            <select className={fieldClassName} {...register('origin')}>
              {leadOriginOptions.map((option) => (
                <option key={`lead-origin-${option.value || 'all'}`} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Responsable</span>
            <select className={fieldClassName} {...register('assignedUserId')}>
              <option value="">Todos</option>
              <option value={user?.id ?? ''}>{user?.name ?? 'Mi usuario'}</option>
            </select>
          </label>
        </FilterBar>
      </form>

      {leadsQuery.isPending && !leadsQuery.data ? (
        <LoadingState
          message="Cargando prospectos demo, etapas activas y ultima actividad comercial."
          variant="table"
        />
      ) : null}

      {leadsQuery.isError && !leadsQuery.data ? (
        <ErrorState
          description="No pudimos recuperar el listado de prospectos. Reintenta para volver a cargar el embudo."
          onRetry={() => void leadsQuery.refetch()}
          title="No fue posible cargar los prospectos"
        />
      ) : null}

      {leadsQuery.data && leads.length === 0 ? (
        <EmptyState
          description="No hay prospectos que coincidan con los filtros actuales."
          primaryAction={{ label: 'Crear prospecto', to: '/prospects/new' }}
          title="Sin prospectos"
        />
      ) : null}

      {leads.length > 0 ? (
        <Card className="space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-[var(--color-text)]">
                Detalle de prospectos
              </p>
              <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
                Listado tabular con 10 registros por pagina y acceso directo al detalle.
              </p>
            </div>
            <StatusBadge label={`${leadsQuery.data?.totalItems ?? 0} registro(s)`} tone="info" />
          </div>

          <div className="overflow-hidden rounded-[1.5rem] border border-[var(--color-border)]">
            <div className="overflow-x-auto">
              <table className="min-w-full border-separate border-spacing-0">
                <thead>
                  <tr className="bg-slate-50">
                    {[
                      'Prospecto',
                      'Contacto',
                      'Estado',
                      'Origen',
                      'Responsable',
                      'Ultima actualizacion',
                      'Accion',
                    ].map((column) => (
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
                  {leads.map((lead) => (
                    <tr key={lead.id} className="transition hover:bg-slate-50">
                      <td className="border-b border-[var(--color-border)] px-5 py-4 text-sm text-slate-700">
                        <p className="font-semibold text-[var(--color-text)]">{lead.displayName}</p>
                        <p className="mt-1 text-xs uppercase tracking-[0.16em] text-slate-500">
                          {lead.conversationId ? 'Ligado a conversacion' : 'Alta manual'}
                        </p>
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4 text-sm text-slate-700">
                        <p>{lead.phone}</p>
                        <p className="mt-1 text-[var(--color-text-secondary)]">
                          {lead.email ?? 'Sin correo registrado'}
                        </p>
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4 text-sm text-slate-700">
                        <StatusBadge
                          label={getLeadStageLabel(lead.stage)}
                          tone={getLeadStageTone(lead.stage)}
                        />
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4 text-sm text-slate-700">
                        {getLeadOriginLabel(lead.sourceType)}
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4 text-sm text-slate-700">
                        {lead.assignedUserName ?? 'Sin asignar'}
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4 text-sm text-slate-700">
                        {dayjs(lead.updatedAt).format('DD MMM YYYY HH:mm')}
                      </td>
                      <td className="border-b border-[var(--color-border)] px-5 py-4 text-right text-sm">
                        <Button
                          onClick={() => navigate(`/prospects/${lead.id}`)}
                          size="sm"
                          variant="secondary"
                        >
                          Ver detalle
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-5">
            <p className="text-sm text-slate-600">
              Pagina {(leadsQuery.data?.page ?? 0) + 1} de{' '}
              {Math.max(leadsQuery.data?.totalPages ?? 1, 1)} · 10 registros por pagina
            </p>
            <div className="flex gap-2">
              <Button
                disabled={page === 0 || leadsQuery.isFetching}
                onClick={() => setPage((currentPage) => Math.max(currentPage - 1, 0))}
                size="sm"
                variant="secondary"
              >
                Anterior
              </Button>
              <Button
                disabled={
                  !leadsQuery.data ||
                  leadsQuery.data.totalPages === 0 ||
                  page >= leadsQuery.data.totalPages - 1 ||
                  leadsQuery.isFetching
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
