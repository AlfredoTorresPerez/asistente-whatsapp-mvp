import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { FilterBar } from '../../../components/ui/FilterBar'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import {
  formatEstadoRegistro,
  getRegistroTone,
  isRegistroActivo,
} from '../../../lib/statusFormatters'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { listAestheticRules, updateAestheticRule } from '../../../services/api/aestheticApi'
import type { AestheticBusinessRuleResponse } from '../../../services/api/types'
import { DEFAULT_RULE_TYPE_OPTIONS, formatRuleType } from '../lib/ruleTypeLabels'

type ActiveFilter = '' | 'true' | 'false'
type RuleLifecycleStatus = 'draft' | 'testing' | 'published' | 'paused' | 'archived'

const fieldClassName =
  'h-11 w-full rounded-2xl border border-[var(--color-border)] bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100'

const PAGE_SIZE = 10

const RULE_STATUS_LABELS: Record<RuleLifecycleStatus, string> = {
  archived: 'Archivada',
  draft: 'Borrador',
  paused: 'Pausada',
  published: 'Publicada',
  testing: 'En prueba',
}

function parseRulePayload(payload: string) {
  try {
    const parsed = JSON.parse(payload || '{}')
    return typeof parsed === 'object' && parsed !== null ? parsed as Record<string, unknown> : {}
  } catch {
    return {}
  }
}

function getRuleStatus(rule: AestheticBusinessRuleResponse): RuleLifecycleStatus {
  const payload = parseRulePayload(rule.rulePayload)
  const status = String(payload.estadoRegla ?? payload.lifecycleStatus ?? '').toLowerCase()
  if (['draft', 'testing', 'published', 'paused', 'archived'].includes(status)) {
    return status as RuleLifecycleStatus
  }
  return rule.active ? 'published' : 'paused'
}

function getRuleStatusTone(status: RuleLifecycleStatus) {
  switch (status) {
    case 'published':
      return 'success'
    case 'testing':
      return 'info'
    case 'paused':
      return 'warning'
    case 'archived':
      return 'neutral'
    case 'draft':
    default:
      return 'neutral'
  }
}

function buildPayloadWithStatus(rule: AestheticBusinessRuleResponse, active: boolean) {
  const payload = parseRulePayload(rule.rulePayload)
  return JSON.stringify({
    ...payload,
    estadoRegla: active ? 'published' : 'paused',
  })
}

export function AutomationRulesPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const isOnline = useOnlineStatus()
  const [page, setPage] = useState(0)
  const [ruleTypeInput, setRuleTypeInput] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [activeInput, setActiveInput] = useState<ActiveFilter>('')
  const [filters, setFilters] = useState({ active: '' as ActiveFilter, ruleType: '', search: '' })
  const [ruleToToggle, setRuleToToggle] = useState<{
    rule: AestheticBusinessRuleResponse
    active: boolean
  } | null>(null)
  const [inlineError, setInlineError] = useState<string | null>(null)

  const rulesQuery = useQuery({
    queryKey: ['aesthetic', 'rules', page, filters],
    queryFn: () =>
      listAestheticRules({
        active: filters.active === '' ? undefined : filters.active === 'true',
        page,
        ruleType: filters.ruleType || undefined,
        size: PAGE_SIZE,
      }),
    placeholderData: keepPreviousData,
    refetchInterval: isOnline ? 30_000 : false,
  })

  const statusMutation = useMutation({
    mutationFn: async ({
      active,
      rule,
    }: {
      rule: AestheticBusinessRuleResponse
      active: boolean
    }) =>
      updateAestheticRule(rule.id, {
        active,
        code: rule.code,
        description: rule.description,
        name: rule.name,
        priority: rule.priority,
        rulePayload: buildPayloadWithStatus(rule, active),
        ruleType: rule.ruleType,
      }),
    onError: (error) => {
      setInlineError(
        error instanceof Error ? error.message : 'No fue posible actualizar el estado de la regla.',
      )
    },
    onSuccess: async () => {
      setInlineError(null)
      setRuleToToggle(null)
      await queryClient.invalidateQueries({ queryKey: ['aesthetic', 'rules'] })
      await queryClient.invalidateQueries({ queryKey: ['aesthetic'] })
    },
  })

  const rules = useMemo(() => {
    const items = rulesQuery.data?.items ?? []
    if (!filters.search.trim()) return items
    const term = filters.search.trim().toLowerCase()
    return items.filter((rule) =>
      [rule.name, rule.code, rule.description, formatRuleType(rule.ruleType)]
        .join(' ')
        .toLowerCase()
        .includes(term),
    )
  }, [filters.search, rulesQuery.data?.items])
  const ruleTypes = useMemo(
    () =>
      Array.from(
        new Set([...DEFAULT_RULE_TYPE_OPTIONS, ...rules.map((rule) => rule.ruleType)]),
      ).sort(),
    [rules],
  )
  const activeCount = rules.filter((rule) => getRuleStatus(rule) === 'published').length
  const pausedCount = rules.filter((rule) => getRuleStatus(rule) === 'paused').length
  const highPriority = rules.filter((rule) => rule.priority <= 20).length

  const applyFilters = () => {
    setPage(0)
    setFilters({ active: activeInput, ruleType: ruleTypeInput, search: searchInput })
  }

  const clearFilters = () => {
    setPage(0)
    setSearchInput('')
    setRuleTypeInput('')
    setActiveInput('')
    setFilters({ active: '', ruleType: '', search: '' })
  }

  return (
    <section className="space-y-4 overflow-hidden">
      <PageHeader
        actions={<Button onClick={() => navigate('/automation-rules/new')}>Crear regla</Button>}
        description="Reglas del centro estético para que la IA responda según criterios de seguridad, negocio y operación."
        eyebrow="Reglas"
        title="Reglas de automatización e IA"
      />

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Reglas visibles"
          value={String(rulesQuery.data?.totalItems ?? 0)}
          tone="info"
        />
        <MetricCard label="Publicadas en esta página" value={String(activeCount)} tone="success" />
        <MetricCard
          label="Pausadas en esta página"
          value={String(pausedCount)}
          tone={pausedCount > 0 ? 'warning' : 'success'}
        />
        <MetricCard label="Prioridad alta" value={String(highPriority)} tone="warning" />
      </div>

      <Card className="space-y-4 p-4 lg:p-5">
        <FilterBar
          actions={
            <>
              <Button onClick={applyFilters} size="sm" type="button">
                Aplicar filtros
              </Button>
              <Button onClick={clearFilters} size="sm" type="button" variant="secondary">
                Limpiar
              </Button>
            </>
          }
        >
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Búsqueda</span>
            <input
              className={fieldClassName}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Buscar por nombre, tipo o descripción"
              type="search"
              value={searchInput}
            />
          </label>
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Tipo de regla</span>
            <select
              className={fieldClassName}
              onChange={(event) => setRuleTypeInput(event.target.value)}
              value={ruleTypeInput}
            >
              <option value="">Todos</option>
              {ruleTypes.map((type) => (
                <option key={type} value={type}>
                  {formatRuleType(type)}
                </option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-slate-700">Estado</span>
            <select
              className={fieldClassName}
              onChange={(event) => setActiveInput(event.target.value as ActiveFilter)}
              value={activeInput}
            >
              <option value="">Todos</option>
              <option value="true">Publicadas</option>
              <option value="false">Pausadas o archivadas</option>
            </select>
          </label>
        </FilterBar>

        {inlineError ? (
          <div className="rounded-[18px] border border-red-200 bg-red-50 p-3 text-sm font-medium text-red-800">
            {inlineError}
          </div>
        ) : null}

        {rulesQuery.isPending && !rulesQuery.data ? (
          <LoadingState message="Cargando reglas del centro estético." variant="table" />
        ) : null}

        {rulesQuery.isError && !rulesQuery.data ? (
          <ErrorState
            description="No fue posible cargar las reglas. Reintenta en unos segundos."
            onRetry={() => void rulesQuery.refetch()}
            title="No fue posible cargar las reglas"
          />
        ) : null}

        {rulesQuery.data && rules.length === 0 ? (
          <EmptyState
            description="No hay reglas que coincidan con los filtros actuales."
            primaryAction={{ label: 'Crear regla', to: '/automation-rules/new' }}
            title="Sin reglas"
          />
        ) : null}

        {rulesQuery.data ? (
          <div className="grid gap-3" data-testid="rules-list">
            <div className="grid gap-2 rounded-[18px] border border-[var(--color-border)] bg-slate-50 px-4 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 md:grid-cols-[minmax(0,1.1fr)_150px_90px_120px_minmax(0,1.4fr)_auto] md:items-center">
              <span>Regla</span>
              <span>Tipo</span>
              <span>Prioridad</span>
              <span>Estado</span>
              <span>Descripción</span>
              <span className="md:text-right">Acciones</span>
            </div>
            {rules.length === 0 ? (
              <div className="rounded-[20px] border border-[var(--color-border)] bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
                No hay reglas para los filtros seleccionados.
              </div>
            ) : (
              rules.map((rule) => {
                const active = isRegistroActivo(rule.active)
                const lifecycleStatus = getRuleStatus(rule)

                return (
                  <article
                    className={[
                      'grid gap-3 rounded-[20px] border p-4 transition md:grid-cols-[minmax(0,1.1fr)_150px_90px_120px_minmax(0,1.4fr)_auto] md:items-start',
                      active
                        ? 'border-[var(--color-border)] bg-white'
                        : 'border-amber-200 bg-amber-50/70',
                    ].join(' ')}
                    key={rule.id}
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-[var(--color-text)]">
                        {rule.name}
                      </p>
                      <p className="mt-1 truncate text-xs uppercase tracking-[0.16em] text-slate-500">
                        {rule.code}
                      </p>
                    </div>
                    <p className="text-sm font-semibold text-slate-700">
                      {formatRuleType(rule.ruleType)}
                    </p>
                    <p className="text-sm text-slate-700">Prioridad {rule.priority}</p>
                    <StatusBadge
                      label={RULE_STATUS_LABELS[lifecycleStatus]}
                      tone={getRuleStatusTone(lifecycleStatus)}
                    />
                    <p className="text-sm leading-5 text-slate-700">
                      {rule.description}
                    </p>
                    <div className="flex flex-wrap justify-end gap-2">
                      <Link
                        className={buttonClassName({ size: 'sm', variant: 'secondary' })}
                        title={`Probar ${rule.name}`}
                        to={`/automation-rules/${rule.id}/test`}
                      >
                        Probar
                      </Link>
                      {active ? (
                        <Link
                          className={buttonClassName({ size: 'sm', variant: 'secondary' })}
                          title={`Editar ${rule.name}`}
                          to={`/automation-rules/${rule.id}/edit`}
                        >
                          Editar
                        </Link>
                      ) : (
                        <span
                          aria-disabled="true"
                          className={`${buttonClassName({ size: 'sm', variant: 'secondary' })} pointer-events-none opacity-60`}
                          title="No se puede editar una regla desactivada."
                        >
                          Editar
                        </span>
                      )}
                      <Button
                        disabled={statusMutation.isPending}
                        onClick={() => setRuleToToggle({ active: !active, rule })}
                        size="sm"
                        title={active ? `Desactivar ${rule.name}` : `Activar ${rule.name}`}
                        variant={active ? 'danger' : 'secondary'}
                      >
                        {active ? 'Desactivar' : 'Activar'}
                      </Button>
                    </div>
                  </article>
                )
              })
            )}
          </div>
        ) : null}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
          <p className="text-sm text-slate-600">
            Página {(rulesQuery.data?.page ?? 0) + 1} de{' '}
            {Math.max(rulesQuery.data?.totalPages ?? 1, 1)} · 10 registros por página
          </p>
          <div className="flex gap-2">
            <Button
              disabled={page === 0 || rulesQuery.isFetching}
              onClick={() => setPage((current) => Math.max(current - 1, 0))}
              size="sm"
              variant="secondary"
            >
              Anterior
            </Button>
            <Button
              disabled={
                !rulesQuery.data ||
                rulesQuery.data.totalPages === 0 ||
                page >= rulesQuery.data.totalPages - 1 ||
                rulesQuery.isFetching
              }
              onClick={() => setPage((current) => current + 1)}
              size="sm"
              variant="secondary"
            >
              Siguiente
            </Button>
          </div>
        </div>
      </Card>

      <ConfirmDialog
        confirmLabel={ruleToToggle?.active ? 'Activar' : 'Desactivar'}
        confirmLoading={statusMutation.isPending}
        description={
          ruleToToggle
            ? `La regla ${ruleToToggle.rule.name} quedara ${ruleToToggle.active ? 'activa' : 'desactivada'}, pero no será eliminada físicamente.`
            : 'Confirma el cambio de estado de la regla.'
        }
        onCancel={() => setRuleToToggle(null)}
        onConfirm={() => {
          if (ruleToToggle) {
            statusMutation.mutate(ruleToToggle)
          }
        }}
        open={Boolean(ruleToToggle)}
        title={ruleToToggle?.active ? 'Activar regla' : 'Desactivar regla'}
        tone={ruleToToggle?.active ? 'neutral' : 'danger'}
      />
    </section>
  )
}

function MetricCard({
  label,
  tone,
  value,
}: {
  label: string
  tone: 'success' | 'warning' | 'info'
  value: string
}) {
  return (
    <Card className="p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--color-text-secondary)]">
            {label}
          </p>
          <p className="mt-2 text-2xl font-semibold tracking-[-0.03em] text-[var(--color-text)]">
            {value}
          </p>
        </div>
        <StatusBadge label="Actual" tone={tone} />
      </div>
    </Card>
  )
}
