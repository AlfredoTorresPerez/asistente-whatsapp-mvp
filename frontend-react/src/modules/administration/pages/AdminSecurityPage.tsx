import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useToast } from '../../../lib/toast'
import {
  getSecurityPolicyRequest,
  updateSecurityPolicyRequest,
} from '../../../services/api/administrationApi'
import { ApiClientError } from '../../../services/api/httpClient'
import type { SecurityPolicyRequest, SecurityPolicyResponse } from '../../../services/api/types'

type PolicyForm = SecurityPolicyRequest

function toPolicyForm(policy: SecurityPolicyResponse): PolicyForm {
  return {
    maxFailedLoginAttempts: policy.maxFailedLoginAttempts,
    passwordMinLength: policy.passwordMinLength,
    requireNumber: policy.requireNumber,
    requireSymbol: policy.requireSymbol,
    requireUppercase: policy.requireUppercase,
    sessionTimeoutMinutes: policy.sessionTimeoutMinutes,
  }
}

export function AdminSecurityPage() {
  const policyQuery = useQuery({
    queryKey: ['administration', 'security'],
    queryFn: getSecurityPolicyRequest,
  })

  if (policyQuery.isPending) {
    return (
      <LoadingState
        message="Cargando politicas de seguridad y auditoria reciente."
        variant="page"
      />
    )
  }

  if (policyQuery.isError || !policyQuery.data) {
    return (
      <ErrorState
        description="No pudimos recuperar las politicas de seguridad."
        onRetry={() => void policyQuery.refetch()}
        title="No fue posible cargar seguridad"
      />
    )
  }

  return <SecurityPolicyForm key={policyQuery.data.updatedAt} policy={policyQuery.data} />
}

function SecurityPolicyForm({ policy }: { policy: SecurityPolicyResponse }) {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [form, setForm] = useState<PolicyForm>(() => toPolicyForm(policy))
  const [formError, setFormError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => updateSecurityPolicyRequest(form),
    onError: (error) => {
      setFormError(
        error instanceof ApiClientError ? error.message : 'No fue posible guardar la seguridad.',
      )
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'security'] })
      await queryClient.invalidateQueries({ queryKey: ['administration', 'summary'] })
      showToast({
        title: 'Seguridad actualizada',
        description: 'La politica de acceso quedo sincronizada con el backend.',
        tone: 'success',
      })
    },
  })

  const submit = () => {
    setFormError(null)
    mutation.mutate()
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Link to="/admin">
            <Button variant="secondary">Volver</Button>
          </Link>
        }
        description="Politicas de contrasena, bloqueo por intentos y senales de auditoria para el negocio."
        eyebrow="Seguridad"
        title="Seguridad"
      />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="Usuarios activos" tone="success" value={String(policy.activeUsers)} />
        <MetricCard
          label="Bloqueados"
          tone={policy.lockedUsers > 0 ? 'warning' : 'success'}
          value={String(policy.lockedUsers)}
        />
        <MetricCard
          label="Eventos 7 dias"
          tone="info"
          value={String(policy.auditEventsLast7Days)}
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <Card>
          <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-lg font-semibold text-slate-950">Politicas de acceso</p>
              <p className="mt-1 text-sm text-slate-600">
                Ultima actualizacion: {dayjs(policy.updatedAt).format('DD/MM/YYYY HH:mm')}
              </p>
            </div>
            <StatusBadge label="Operativo" tone="success" />
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <Input
              label="Timeout de sesion (min)"
              max="1440"
              min="5"
              onChange={(event) =>
                setForm({ ...form, sessionTimeoutMinutes: Number(event.target.value) })
              }
              type="number"
              value={form.sessionTimeoutMinutes}
            />
            <Input
              label="Longitud minima"
              max="72"
              min="8"
              onChange={(event) =>
                setForm({ ...form, passwordMinLength: Number(event.target.value) })
              }
              type="number"
              value={form.passwordMinLength}
            />
            <Input
              label="Maximo intentos fallidos"
              max="20"
              min="3"
              onChange={(event) =>
                setForm({ ...form, maxFailedLoginAttempts: Number(event.target.value) })
              }
              type="number"
              value={form.maxFailedLoginAttempts}
            />
          </div>

          <div className="mt-5 grid gap-3 md:grid-cols-3">
            <CheckboxCard
              checked={form.requireUppercase}
              label="Mayuscula"
              onChange={(checked) => setForm({ ...form, requireUppercase: checked })}
            />
            <CheckboxCard
              checked={form.requireNumber}
              label="Numero"
              onChange={(checked) => setForm({ ...form, requireNumber: checked })}
            />
            <CheckboxCard
              checked={form.requireSymbol}
              label="Simbolo"
              onChange={(checked) => setForm({ ...form, requireSymbol: checked })}
            />
          </div>

          {formError ? (
            <div className="mt-5 rounded-[18px] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {formError}
            </div>
          ) : null}

          <div className="mt-6 flex flex-wrap gap-3">
            <Button loading={mutation.isPending} onClick={submit}>
              Guardar seguridad
            </Button>
            <Link to="/admin">
              <Button variant="secondary">Cancelar</Button>
            </Link>
          </div>
        </Card>

        <Card>
          <p className="text-lg font-semibold text-slate-950">Resumen operativo</p>
          <div className="mt-5 space-y-4">
            <SecurityLine label="Sesion" value={`${form.sessionTimeoutMinutes} minutos`} />
            <SecurityLine label="Contrasena" value={`${form.passwordMinLength}+ caracteres`} />
            <SecurityLine label="Bloqueo" value={`${form.maxFailedLoginAttempts} intentos`} />
            <SecurityLine
              label="Reglas activas"
              value={
                [
                  form.requireUppercase ? 'mayuscula' : null,
                  form.requireNumber ? 'numero' : null,
                  form.requireSymbol ? 'simbolo' : null,
                ]
                  .filter(Boolean)
                  .join(', ') || 'basicas'
              }
            />
          </div>
        </Card>
      </div>
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
    <Card>
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <StatusBadge label="Actual" tone={tone} />
      </div>
      <p className="mt-4 text-2xl font-semibold text-slate-950">{value}</p>
    </Card>
  )
}

function CheckboxCard({
  checked,
  label,
  onChange,
}: {
  checked: boolean
  label: string
  onChange: (checked: boolean) => void
}) {
  return (
    <label className="flex cursor-pointer items-center gap-3 rounded-[18px] border border-[var(--color-border)] bg-white px-4 py-3 text-sm font-medium text-slate-700">
      <input
        checked={checked}
        className="h-4 w-4 accent-[var(--color-primary)]"
        onChange={(event) => onChange(event.target.checked)}
        type="checkbox"
      />
      {label}
    </label>
  )
}

function SecurityLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-[var(--color-border)] pb-3 last:border-b-0">
      <span className="text-sm text-slate-500">{label}</span>
      <span className="text-sm font-semibold text-slate-950">{value}</span>
    </div>
  )
}
