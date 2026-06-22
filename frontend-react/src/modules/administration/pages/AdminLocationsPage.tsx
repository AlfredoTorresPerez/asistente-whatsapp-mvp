import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiClientError } from '../../../services/api/httpClient'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useToast } from '../../../lib/toast'
import {
  createBusinessLocationRequest,
  deactivateBusinessLocationRequest,
  getBusinessLocationsRequest,
  updateBusinessLocationRequest,
} from '../../../services/api/businessLocationsApi'
import type { BusinessLocationResponse, UpsertBusinessLocationRequest } from '../../../services/api/types'

type FormState = {
  id: string | null
  code: string
  name: string
  address: string
  city: string
  commune: string
  phone: string
  whatsappNumber: string
  timezone: string
}

type FormErrors = Partial<Record<keyof FormState, string>> & {
  general?: string
}

const emptyForm: FormState = {
  id: null,
  code: '',
  name: '',
  address: '',
  city: '',
  commune: '',
  phone: '',
  whatsappNumber: '',
  timezone: 'America/Santiago',
}

export function AdminLocationsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [form, setForm] = useState<FormState>(emptyForm)
  const [formErrors, setFormErrors] = useState<FormErrors>({})
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')

  const locationsQuery = useQuery({
    queryKey: ['business-locations', 'admin'],
    queryFn: () => getBusinessLocationsRequest(),
  })

  const locations = useMemo(() => locationsQuery.data ?? [], [locationsQuery.data])
  const activeLocations = locations.filter((location) => location.active)
  const inactiveLocations = locations.filter((location) => !location.active)
  const filteredLocations = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase()
    return locations.filter((location) => {
      const matchesStatus = statusFilter === 'ALL'
        || (statusFilter === 'ACTIVE' && location.active)
        || (statusFilter === 'INACTIVE' && !location.active)
      const haystack = [
        location.code,
        location.name,
        location.address,
        location.city,
        location.commune,
        location.phone,
        location.whatsappNumber,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return matchesStatus && (!normalizedSearch || haystack.includes(normalizedSearch))
    })
  }, [locations, search, statusFilter])

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = toPayload(form)
      return form.id
        ? updateBusinessLocationRequest(form.id, payload)
        : createBusinessLocationRequest(payload)
    },
    onSuccess: (location) => {
      showToast({
        title: form.id ? 'Sucursal actualizada' : 'Sucursal creada',
        description: `${location.name} quedo disponible para agenda y conversaciones.`,
        tone: 'success',
      })
      setFormErrors({})
      void queryClient.invalidateQueries({ queryKey: ['business-locations'] })
      setForm(toFormState(location))
    },
    onError: (error) => {
      if (error instanceof ApiClientError) {
        if (error.status === 401) {
          setFormErrors({ general: 'Tu sesión expiró. Inicia sesión nuevamente para continuar.' })
          showToast({
            title: 'Sesion expirada',
            description: 'Vuelve a iniciar sesión para guardar la sucursal.',
            tone: 'error',
          })
          navigate('/login', { replace: true })
          return
        }

        const nextErrors = toFormErrors(error.fieldErrors)
        setFormErrors({
          ...nextErrors,
          general: Object.keys(nextErrors).length > 0
            ? 'Corrige los campos marcados antes de guardar la sucursal.'
            : error.message,
        })
        showToast({
          title: 'No se pudo guardar la sucursal',
          description: Object.keys(nextErrors).length > 0
            ? 'Hay campos con datos inválidos.'
            : error.message,
          tone: 'error',
        })
        return
      }

      setFormErrors({ general: 'No fue posible guardar la sucursal. Intenta nuevamente.' })
      showToast({
        title: 'No se pudo guardar la sucursal',
        description: 'Revisa código, nombre y datos operativos antes de reintentar.',
        tone: 'error',
      })
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: deactivateBusinessLocationRequest,
    onSuccess: () => {
      showToast({
        title: 'Sucursal desactivada',
        description: 'La sucursal ya no queda disponible para nuevas operaciónes.',
        tone: 'success',
      })
      void queryClient.invalidateQueries({ queryKey: ['business-locations'] })
      setForm(emptyForm)
    },
    onError: () => {
      showToast({
        title: 'No se pudo desactivar la sucursal',
        description: 'Verifica que exista otra sucursal activa antes de desactivar.',
        tone: 'error',
      })
    },
  })

  const updateField = (field: keyof FormState, value: string) => {
    setForm((current) => {
      const normalizedValue = field === 'code' ? normalizeLocationCode(value) : value
      const nextForm = { ...current, [field]: normalizedValue }

      if (field === 'name' && !current.code.trim()) {
        nextForm.code = normalizeLocationCode(value)
      }

      return nextForm
    })
    setFormErrors((current) => {
      const { [field]: _discardedFieldError, general: _discardedGeneralError, ...remainingErrors } = current
      void _discardedFieldError
      void _discardedGeneralError
      return remainingErrors
    })
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const validationErrors = validateLocationForm(form)
    setFormErrors(validationErrors)

    if (Object.keys(validationErrors).length > 0) {
      showToast({
        title: 'Revisa la información de la sucursal',
        description: 'Corrige los campos marcados antes de guardar.',
        tone: 'error',
      })
      return
    }

    saveMutation.mutate()
  }

  const startNewLocation = () => {
    setForm(emptyForm)
    setFormErrors({})
    window.requestAnimationFrame(() => {
      document.getElementById('location-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  const editLocation = (location: BusinessLocationResponse) => {
    setFormErrors({})
    setForm(toFormState(location))
    window.requestAnimationFrame(() => {
      document.getElementById('location-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <div className="flex flex-wrap gap-3">
            <Button onClick={() => navigate('/admin')} variant="secondary">
              Volver a administracion
            </Button>
            <Button onClick={startNewLocation}>
              Nueva sucursal
            </Button>
          </div>
        }
        description="Administra las sucursales del negocio para agenda, conversaciones, profesionales y configuración operativa."
        eyebrow="Administración"
        title="Sedes del negocio"
      />

      {locationsQuery.isPending ? (
        <LoadingState message="Cargando sedes del negocio." variant="page" />
      ) : null}

      {locationsQuery.isError ? (
        <ErrorState
          description="No pudimos recuperar las sedes configuradas."
          onRetry={() => void locationsQuery.refetch()}
          title="No fue posible cargar sedes"
        />
      ) : null}

      {!locationsQuery.isError ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <MetricCard label="Sedes activas" value={String(activeLocations.length)} helper={`De ${locations.length} sedes totales`} icon="🏢" />
            <MetricCard label="Sedes inactivas" value={String(inactiveLocations.length)} helper="No disponibles para nuevas citas" icon="⏸" />
            <MetricCard label="Zona horaria" value="America/Santiago" helper="Configuracion operativa base" icon="🕒" />
            <MetricCard label="Uso operativo" value="Agenda" helper="Citas y conversaciones por sucursal" icon="📅" />
          </div>

          <Card className="space-y-5">
            <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
              <div>
                <h2 className="text-xl font-semibold text-slate-950">Sucursales registradas</h2>
                <p className="mt-1 text-sm text-slate-500">
                  Estas sedes quedan visibles en agenda, conversaciones y flujos de atención.
                </p>
              </div>
              <div className="flex flex-col gap-3 md:flex-row">
                <input
                  className="h-11 min-w-[280px] rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm text-slate-700 outline-none transition focus:border-blue-300 focus:ring-4 focus:ring-blue-100"
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Buscar por sucursal, dirección o teléfono..."
                  type="search"
                  value={search}
                />
                <select
                  className="h-11 rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm font-semibold text-slate-700 outline-none"
                  onChange={(event) => setStatusFilter(event.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE')}
                  value={statusFilter}
                >
                  <option value="ALL">Estado: Todos</option>
                  <option value="ACTIVE">Estado: Activas</option>
                  <option value="INACTIVE">Estado: Inactivas</option>
                </select>
              </div>
            </div>

            <div className="overflow-hidden rounded-2xl border border-[var(--color-border)] bg-white">
              <div className="hidden grid-cols-[110px_minmax(150px,1.1fr)_minmax(220px,1.3fr)_140px_150px_170px_160px_120px_120px] items-center border-b border-[var(--color-border)] bg-slate-50 px-4 py-3 text-xs font-bold uppercase tracking-[0.14em] text-slate-500 xl:grid">
                <span>Código</span>
                <span>Sucursal</span>
                <span>Dirección</span>
                <span>Comuna</span>
                <span>Teléfono</span>
                <span>WhatsApp</span>
                <span>Zona horaria</span>
                <span>Estado</span>
                <span>Acciones</span>
              </div>

              {filteredLocations.map((location) => (
                <div
                  className="grid gap-3 border-b border-[var(--color-border)] px-4 py-4 last:border-b-0 xl:grid-cols-[110px_minmax(150px,1.1fr)_minmax(220px,1.3fr)_140px_150px_170px_160px_120px_120px] xl:items-center"
                  key={location.id}
                >
                  <span className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">{location.code}</span>
                  <div>
                    <p className="font-semibold text-slate-950">{location.name}</p>
                    {location.code.toLowerCase() === 'principal' ? (
                      <span className="mt-1 inline-flex rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700">
                        Principal
                      </span>
                    ) : null}
                  </div>
                  <p className="text-sm text-slate-600">{location.address ?? 'Sin dirección'}</p>
                  <p className="text-sm text-slate-600">{location.commune ?? location.city ?? 'Sin comuna'}</p>
                  <p className="text-sm font-semibold text-blue-600">{location.phone ?? 'Sin teléfono'}</p>
                  <p className="text-sm text-slate-600">{location.whatsappNumber ?? 'Sin WhatsApp'}</p>
                  <p className="text-sm text-slate-600">{location.timezone}</p>
                  <StatusBadge label={location.active ? 'Activa' : 'Inactiva'} tone={location.active ? 'success' : 'neutral'} />
                  <div className="flex flex-wrap gap-2">
                    <Button onClick={() => editLocation(location)} size="sm" variant="secondary">
                      Editar
                    </Button>
                    {location.active ? (
                      <Button
                        loading={deactivateMutation.isPending && form.id === location.id}
                        onClick={() => {
                          setForm(toFormState(location))
                          deactivateMutation.mutate(location.id)
                        }}
                        size="sm"
                        variant="secondary"
                      >
                        Desactivar
                      </Button>
                    ) : null}
                  </div>
                </div>
              ))}

              {filteredLocations.length === 0 ? (
                <p className="px-4 py-8 text-center text-sm text-slate-500">
                  No hay sucursales que coincidan con los filtros seleccionados.
                </p>
              ) : null}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-600">
              <span>Mostrando {filteredLocations.length} de {locations.length} sedes.</span>
              <span className="font-semibold text-slate-700">Las citas deben considerar sucursal, profesional y horario.</span>
            </div>
          </Card>

          <Card className="space-y-5" id="location-form">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                {form.id ? 'Editar sucursal' : 'Nueva sucursal'}
              </p>
              <h2 className="mt-2 text-xl font-semibold text-slate-950">
                Datos operativos de la sucursal
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                El código debe ser único dentro del negocio. Usa nombres claros porque se mostrarán en agenda y conversaciones.
              </p>
            </div>

            <form className="space-y-4" onSubmit={submit}>
              {formErrors.general ? (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-800">
                  {formErrors.general}
                </div>
              ) : null}

              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input
                  error={formErrors.code}
                  hint="Se autogenera desde el nombre. Usa solo letras, numeros, guion o guion bajo."
                  label="Código"
                  value={form.code}
                  onChange={(event) => updateField('code', event.target.value)}
                />
                <Input
                  error={formErrors.name}
                  label="Nombre de sucursal"
                  value={form.name}
                  onChange={(event) => updateField('name', event.target.value)}
                />
                <Input
                  error={formErrors.timezone}
                  label="Zona horaria"
                  value={form.timezone}
                  onChange={(event) => updateField('timezone', event.target.value)}
                />
              </div>
              <Input
                error={formErrors.address}
                label="Dirección"
                value={form.address}
                onChange={(event) => updateField('address', event.target.value)}
              />
              <div className="grid gap-4 md:grid-cols-2">
                <Input
                  error={formErrors.city}
                  label="Ciudad"
                  value={form.city}
                  onChange={(event) => updateField('city', event.target.value)}
                />
                <Input
                  error={formErrors.commune}
                  label="Comuna"
                  value={form.commune}
                  onChange={(event) => updateField('commune', event.target.value)}
                />
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <Input
                  error={formErrors.phone}
                  label="Teléfono"
                  value={form.phone}
                  onChange={(event) => updateField('phone', event.target.value)}
                />
                <Input
                  error={formErrors.whatsappNumber}
                  label="WhatsApp"
                  value={form.whatsappNumber}
                  onChange={(event) => updateField('whatsappNumber', event.target.value)}
                />
              </div>

              <div className="flex flex-wrap justify-end gap-3">
                <Button onClick={() => setForm(emptyForm)} type="button" variant="secondary">
                  Limpiar formulario
                </Button>
                {form.id ? (
                  <Button
                    loading={deactivateMutation.isPending}
                    onClick={() => form.id && deactivateMutation.mutate(form.id)}
                    type="button"
                    variant="secondary"
                  >
                    Desactivar
                  </Button>
                ) : null}
                <Button loading={saveMutation.isPending} type="submit">
                  Guardar sucursal
                </Button>
              </div>
            </form>
          </Card>
        </>
      ) : null}
    </section>
  )
}

function MetricCard({ helper, icon, label, value }: { helper: string; icon: string; label: string; value: string }) {
  return (
    <Card className="flex items-center gap-4">
      <span className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-2xl">
        {icon}
      </span>
      <span>
        <span className="block text-2xl font-semibold text-slate-950">{value}</span>
        <span className="block text-sm font-semibold text-slate-700">{label}</span>
        <span className="mt-1 block text-xs text-slate-500">{helper}</span>
      </span>
    </Card>
  )
}

function toFormState(location: BusinessLocationResponse): FormState {
  return {
    id: location.id,
    code: location.code,
    name: location.name,
    address: location.address ?? '',
    city: location.city ?? '',
    commune: location.commune ?? '',
    phone: location.phone ?? '',
    whatsappNumber: location.whatsappNumber ?? '',
    timezone: location.timezone,
  }
}

function toPayload(form: FormState): UpsertBusinessLocationRequest {
  return {
    code: normalizeLocationCode(form.code),
    name: form.name.trim(),
    address: toNullableTrimmedValue(form.address),
    city: toNullableTrimmedValue(form.city),
    commune: toNullableTrimmedValue(form.commune),
    phone: toNullableTrimmedValue(form.phone),
    whatsappNumber: toNullableTrimmedValue(form.whatsappNumber),
    timezone: form.timezone.trim() || 'America/Santiago',
    active: true,
  }
}

function validateLocationForm(form: FormState): FormErrors {
  const errors: FormErrors = {}
  const code = normalizeLocationCode(form.code)
  const name = form.name.trim()
  const timezone = form.timezone.trim()

  if (!code) {
    errors.code = 'Código obligatorio.'
  } else if (code.length < 2) {
    errors.code = 'El código debe tener al menos 2 caracteres.'
  } else if (code.length > 50) {
    errors.code = 'El código no puede superar 50 caracteres.'
  } else if (!/^[a-z0-9][a-z0-9_-]{1,49}$/.test(code)) {
    errors.code = 'El código debe usar letras, numeros, guion o guion bajo, sin espacios ni tildes.'
  }

  if (!name) {
    errors.name = 'Nombre obligatorio.'
  } else if (name.length > 150) {
    errors.name = 'El nombre no puede superar 150 caracteres.'
  }

  if (!timezone) {
    errors.timezone = 'Zona horaria obligatoria.'
  } else if (timezone.length > 60) {
    errors.timezone = 'La zona horaria no puede superar 60 caracteres.'
  }

  addMaxLengthError(errors, 'address', form.address, 255, 'La dirección no puede superar 255 caracteres.')
  addMaxLengthError(errors, 'city', form.city, 120, 'La ciudad no puede superar 120 caracteres.')
  addMaxLengthError(errors, 'commune', form.commune, 120, 'La comuna no puede superar 120 caracteres.')
  addMaxLengthError(errors, 'phone', form.phone, 30, 'El teléfono no puede superar 30 caracteres.')
  addMaxLengthError(errors, 'whatsappNumber', form.whatsappNumber, 30, 'El WhatsApp no puede superar 30 caracteres.')

  return errors
}

function normalizeLocationCode(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9_-]/g, '')
    .replace(/-+/g, '-')
    .replace(/_+/g, '_')
    .replace(/^[^a-z0-9]+/, '')
    .replace(/[^a-z0-9]+$/, '')
    .slice(0, 50)
}

function toNullableTrimmedValue(value: string) {
  const trimmedValue = value.trim()
  return trimmedValue ? trimmedValue : null
}

function addMaxLengthError(
  errors: FormErrors,
  field: keyof FormState,
  value: string,
  maxLength: number,
  message: string,
) {
  if (value.trim().length > maxLength) {
    errors[field] = message
  }
}

function toFormErrors(fieldErrors: Record<string, string>): FormErrors {
  const supportedFields = new Set<keyof FormState>([
    'code',
    'name',
    'address',
    'city',
    'commune',
    'phone',
    'whatsappNumber',
    'timezone',
  ])
  const errors: FormErrors = {}

  Object.entries(fieldErrors).forEach(([field, message]) => {
    if (supportedFields.has(field as keyof FormState)) {
      errors[field as keyof FormState] = toUserFriendlyFieldError(field, message)
    }
  })

  return errors
}

function toUserFriendlyFieldError(field: string, message: string) {
  if (field === 'code' && message.includes('obligatorio')) {
    return 'Código obligatorio.'
  }

  if (field === 'name' && message.includes('obligatorio')) {
    return 'Nombre obligatorio.'
  }

  return message
}
