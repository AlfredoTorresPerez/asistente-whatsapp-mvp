import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { Textarea } from '../../../components/ui/Textarea'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { DEFAULT_RULE_TYPE_OPTIONS, formatRuleType } from '../lib/ruleTypeLabels'
import {
  createAestheticRule,
  getAestheticRule,
  updateAestheticRule,
} from '../../../services/api/aestheticApi'
import type {
  AestheticBusinessRuleResponse,
  UpsertAestheticBusinessRuleRequest,
} from '../../../services/api/types'

type FormState = {
  code: string
  description: string
  lifecycleStatus: RuleLifecycleStatus
  name: string
  priority: string
  rulePayload: string
  ruleType: string
}

type RuleLifecycleStatus = 'draft' | 'testing' | 'published' | 'paused' | 'archived'

const emptyForm: FormState = {
  code: '',
  description: '',
  lifecycleStatus: 'draft',
  name: '',
  priority: '100',
  rulePayload: '{\n  "condiciones": [],\n  "acciones": []\n}',
  ruleType: 'GENERAL',
}

const selectClassName =
  'h-12 w-full rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)] focus:ring-4 focus:ring-[var(--color-primary)]/12'

const ruleTypeOptions = DEFAULT_RULE_TYPE_OPTIONS

const ruleStatusOptions: { label: string; value: RuleLifecycleStatus }[] = [
  { label: 'Borrador', value: 'draft' },
  { label: 'En prueba', value: 'testing' },
  { label: 'Publicada', value: 'published' },
  { label: 'Pausada', value: 'paused' },
  { label: 'Archivada', value: 'archived' },
]

function nullable(value: string) {
  return value.trim() === '' ? null : value.trim()
}

function numberValue(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 100
}

function parseRulePayload(payload: string) {
  const parsed = JSON.parse(payload || '{}')
  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    throw new Error('La configuración estructurada debe contener condición y acción.')
  }
  return parsed as Record<string, unknown>
}

function getLifecycleStatus(rule: AestheticBusinessRuleResponse): RuleLifecycleStatus {
  try {
    const payload = parseRulePayload(rule.rulePayload || '{}')
    const status = String(payload.estadoRegla ?? payload.lifecycleStatus ?? '').toLowerCase()
    if (['draft', 'testing', 'published', 'paused', 'archived'].includes(status)) {
      return status as RuleLifecycleStatus
    }
  } catch {
    return rule.active ? 'published' : 'paused'
  }
  return rule.active ? 'published' : 'paused'
}

function hasTestCase(payload: Record<string, unknown>) {
  const testCases = payload.casosPrueba ?? payload.testCases
  return Array.isArray(testCases) && testCases.length > 0
}

function fromRule(rule: AestheticBusinessRuleResponse): FormState {
  return {
    code: rule.code,
    description: rule.description,
    lifecycleStatus: getLifecycleStatus(rule),
    name: rule.name,
    priority: String(rule.priority),
    rulePayload: rule.rulePayload || '{}',
    ruleType: rule.ruleType,
  }
}

export function AutomationRuleFormPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { ruleId } = useParams()
  const isEdit = Boolean(ruleId)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [formError, setFormError] = useState<string | null>(null)
  const [loadedRuleId, setLoadedRuleId] = useState('')

  const ruleQuery = useQuery({
    enabled: Boolean(ruleId),
    queryKey: ['aesthetic', 'rules', ruleId],
    queryFn: () => getAestheticRule(ruleId ?? ''),
  })

  if (ruleQuery.data && ruleQuery.data.id !== loadedRuleId) {
    setLoadedRuleId(ruleQuery.data.id)
    setForm(fromRule(ruleQuery.data))
  }

  const mutation = useMutation({
    mutationFn: async () => {
      setFormError(null)
      if (!form.name.trim() || !form.description.trim() || !form.ruleType.trim()) {
        throw new Error('Nombre, descripcion y tipo de regla son obligatorios.')
      }
      const payload = parseRulePayload(form.rulePayload || '{}')
      if (form.lifecycleStatus === 'published' && !isEdit && !hasTestCase(payload)) {
        throw new Error('Guarda la regla en prueba y registra al menos un caso antes de publicarla.')
      }
      const payloadWithLifecycle = JSON.stringify({
        ...payload,
        estadoRegla: form.lifecycleStatus,
      })
      const request: UpsertAestheticBusinessRuleRequest = {
        active: form.lifecycleStatus === 'published',
        code: nullable(form.code),
        description: form.description.trim(),
        name: form.name.trim(),
        priority: numberValue(form.priority),
        rulePayload: payloadWithLifecycle,
        ruleType: form.ruleType.trim(),
      }
      if (ruleId) {
        return updateAestheticRule(ruleId, request)
      }
      return createAestheticRule(request)
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'No fue posible guardar la regla.')
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['aesthetic', 'rules'] })
      await queryClient.invalidateQueries({ queryKey: ['aesthetic'] })
      navigate('/automation-rules')
    },
  })

  const title = isEdit ? 'Editar regla' : 'Crear regla'

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            {isEdit ? (
              <Link
                className={buttonClassName({ variant: 'secondary' })}
                to={`/automation-rules/${ruleId}/test`}
              >
                Probar regla
              </Link>
            ) : null}
            <Link className={buttonClassName({ variant: 'secondary' })} to="/automation-rules">
              Volver a reglas
            </Link>
          </>
        }
        description="Editor para reglas que controlan respuestas de IA, derivaciones, seguridad, promociones y disponibilidad."
        eyebrow="Reglas"
        title={title}
      />

      {ruleQuery.isPending && isEdit ? (
        <LoadingState message="Cargando regla seleccionada." variant="page" />
      ) : null}
      {ruleQuery.isError ? (
        <ErrorState
          description="No fue posible cargar la regla para editar."
          onRetry={() => void ruleQuery.refetch()}
          title="No fue posible cargar la regla"
        />
      ) : null}

      {!isEdit || ruleQuery.data ? (
        <Card className="space-y-6">
          <div className="flex flex-col gap-3 border-b border-[var(--color-border)] pb-5 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--color-text-secondary)]">
                Regla editable
              </p>
              <h2 className="mt-2 text-xl font-semibold text-[var(--color-text)]">{title}</h2>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-[var(--color-text-secondary)]">
                El motor inteligente usa estas reglas para filtrar respuestas, evitar datos
                inventados, derivar riesgos y mantener consistencia comercial.
              </p>
            </div>
            <StatusBadge
              label={ruleStatusOptions.find((option) => option.value === form.lifecycleStatus)?.label ?? 'Borrador'}
              tone={form.lifecycleStatus === 'published' ? 'success' : 'warning'}
            />
          </div>

          {formError ? (
            <div className="rounded-[18px] border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-800">
              {formError}
            </div>
          ) : null}

          <form
            className="space-y-6"
            onSubmit={(event) => {
              event.preventDefault()
              mutation.mutate()
            }}
          >
            <div className="grid gap-4 md:grid-cols-2">
              <Input
                label="Nombre"
                onChange={(event) => setForm({ ...form, name: event.target.value })}
                value={form.name}
              />
              <Input
                label="Código operativo"
                hint="Si queda vacio se genera desde el nombre."
                onChange={(event) => setForm({ ...form, code: event.target.value })}
                value={form.code}
              />
              <label className="block">
                <span className="mb-2.5 block text-sm font-medium text-[#23385F]">
                  Tipo de regla
                </span>
                <select
                  className={selectClassName}
                  onChange={(event) => setForm({ ...form, ruleType: event.target.value })}
                  value={form.ruleType}
                >
                  {ruleTypeOptions.map((option) => (
                    <option key={option} value={option}>
                      {formatRuleType(option)}
                    </option>
                  ))}
                </select>
              </label>
              <Input
                label="Prioridad"
                min="1"
                max="999"
                onChange={(event) => setForm({ ...form, priority: event.target.value })}
                type="number"
                value={form.priority}
              />
              <label className="block">
                <span className="mb-2.5 block text-sm font-medium text-[#23385F]">
                  Estado
                </span>
                <select
                  className={selectClassName}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      lifecycleStatus: event.target.value as RuleLifecycleStatus,
                    })
                  }
                  value={form.lifecycleStatus}
                >
                  {ruleStatusOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <Textarea
              label="Descripcion funcional"
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              value={form.description}
            />
            <Textarea
              hint={
                'Estructura esperada: {"condiciones":["riesgo clínico"],"acciones":["derivar a una persona"],"casosPrueba":["mensaje de ejemplo"]}'
              }
              label="Condición, acción y casos de prueba"
              onChange={(event) => setForm({ ...form, rulePayload: event.target.value })}
              rows={10}
              value={form.rulePayload}
            />

            <div className="flex flex-wrap justify-end gap-3 border-t border-[var(--color-border)] pt-5">
              <Link className={buttonClassName({ variant: 'secondary' })} to="/automation-rules">
                Cancelar
              </Link>
              <Button loading={mutation.isPending} type="submit">
                Guardar regla
              </Button>
            </div>
          </form>
        </Card>
      ) : null}
    </section>
  )
}
