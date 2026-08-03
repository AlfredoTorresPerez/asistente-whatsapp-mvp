import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { CommercialWhatsAppQr } from '../../../components/whatsapp/CommercialWhatsAppQr'
import { Modal } from '../../../components/overlay/Modal'
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
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
  getBusinessLocationCommercialQrRequest,
  getBusinessLocationsRequest,
  updateBusinessLocationRequest,
} from '../../../services/api/businessLocationsApi'
import {
  getBusinessHoursRequest,
  saveBusinessHoursRequest,
} from '../../../services/api/completeAgendaApi'
import type {
  BusinessHoursResponse,
  BusinessLocationResponse,
  UpsertBusinessLocationRequest,
} from '../../../services/api/types'

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

const DAY_LABELS: Record<number, string> = {
  1: 'Lunes',
  2: 'Martes',
  3: 'Miércoles',
  4: 'Jueves',
  5: 'Viernes',
  6: 'Sábado',
  7: 'Domingo',
}

const defaultHours: { dayOfWeek: number; startTime: string; endTime: string }[] = [
  { dayOfWeek: 1, startTime: '09:00', endTime: '18:00' },
  { dayOfWeek: 2, startTime: '09:00', endTime: '18:00' },
  { dayOfWeek: 3, startTime: '09:00', endTime: '18:00' },
  { dayOfWeek: 4, startTime: '09:00', endTime: '18:00' },
  { dayOfWeek: 5, startTime: '09:00', endTime: '18:00' },
  { dayOfWeek: 6, startTime: '09:00', endTime: '13:00' },
  { dayOfWeek: 7, startTime: '', endTime: '' },
]

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
  const [businessHours, setBusinessHours] =
    useState<{ dayOfWeek: number; startTime: string; endTime: string }[]>(defaultHours)
  const [businessHoursSaved, setBusinessHoursSaved] = useState(false)
  const [locationToDeactivate, setLocationToDeactivate] = useState<BusinessLocationResponse | null>(null)

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
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && location.active) ||
        (statusFilter === 'INACTIVE' && !location.active)
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
          general:
            Object.keys(nextErrors).length > 0
              ? 'Corrige los campos marcados antes de guardar la sucursal.'
              : error.message,
        })
        showToast({
          title: 'No se pudo guardar la sucursal',
          description:
            Object.keys(nextErrors).length > 0 ? 'Hay campos con datos inválidos.' : error.message,
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
        description: 'La sucursal ya no queda disponible para nuevas operaciones.',
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

  const businessHoursQuery = useQuery({
    queryKey: ['agenda-business-hours', form.id],
    queryFn: () => getBusinessHoursRequest(form.id!),
    enabled: !!form.id,
  })

  const [qrLocationId, setQrLocationId] = useState<string | null>(null)

  const commercialQrQuery = useQuery({
    queryKey: ['business-location-commercial-qr', qrLocationId],
    queryFn: () => getBusinessLocationCommercialQrRequest(qrLocationId!),
    enabled: Boolean(qrLocationId),
    retry: false,
  })

  const saveHoursMutation = useMutation({
    mutationFn: async () => {
      if (!form.id) throw new Error('Selecciona una sucursal primero.')
      const activeHours = businessHours.filter((h) => h.startTime && h.endTime)
      return saveBusinessHoursRequest({ locationId: form.id, hours: activeHours })
    },
    onSuccess: () => {
      showToast({
        title: 'Horarios guardados',
        description: 'Los horarios de atención se actualizaron correctamente.',
        tone: 'success',
      })
      setBusinessHoursSaved(true)
    },
    onError: (error) => {
      showToast({
        title: 'No se pudieron guardar los horarios',
        description: error instanceof Error ? error.message : 'Intenta nuevamente.',
        tone: 'error',
      })
    },
  })

  useEffect(() => {
    if (businessHoursQuery.data) {
      const serverHours = businessHoursQuery.data.map((h: BusinessHoursResponse) => ({
        dayOfWeek: h.dayOfWeek,
        startTime: h.startTime.slice(0, 5),
        endTime: h.endTime.slice(0, 5),
      }))
      if (serverHours.length > 0) {
        setBusinessHours(serverHours)
        setBusinessHoursSaved(true)
      }
    }
  }, [businessHoursQuery.data])

  useEffect(() => {
    if (!form.id) {
      setBusinessHours(defaultHours)
      setBusinessHoursSaved(false)
    }
  }, [form.id])

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
      const {
        [field]: _discardedFieldError,
        general: _discardedGeneralError,
        ...remainingErrors
      } = current
      void _discardedFieldError
      void _discardedGeneralError
      return remainingErrors
    })
  }

  const executeLocationAction = (location: BusinessLocationResponse, action: string) => {
    if (!action) return
    if (action === 'view' || action === 'edit') {
      editLocation(location)
      return
    }
    if (action === 'professionals') {
      navigate('/admin/professionals')
      return
    }
    if (action === 'rooms') {
      navigate('/admin/rooms')
      return
    }
    if (action === 'qr') {
      setQrLocationId(location.active ? location.id : null)
      return
    }
    if (action === 'deactivate') {
      setLocationToDeactivate(location)
    }
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
      document
        .getElementById('location-form')
        ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  const editLocation = (location: BusinessLocationResponse) => {
    setFormErrors({})
    setForm(toFormState(location))
    window.requestAnimationFrame(() => {
      document
        .getElementById('location-form')
        ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
            <Button onClick={startNewLocation}>Nueva sucursal</Button>
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
            <MetricCard
              label="Sedes activas"
              value={String(activeLocations.length)}
              helper={`De ${locations.length} sedes totales`}
              icon="🏢"
            />
            <MetricCard
              label="Sedes inactivas"
              value={String(inactiveLocations.length)}
              helper="No disponibles para nuevas citas"
              icon="⏸"
            />
            <MetricCard
              label="Zona horaria"
              value="America/Santiago"
              helper="Configuracion operativa base"
              icon="🕒"
            />
            <MetricCard
              label="Uso operativo"
              value="Agenda"
              helper="Citas y conversaciones por sucursal"
              icon="📅"
            />
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
                  onChange={(event) =>
                    setStatusFilter(event.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE')
                  }
                  value={statusFilter}
                >
                  <option value="ALL">Estado: Todos</option>
                  <option value="ACTIVE">Estado: Activas</option>
                  <option value="INACTIVE">Estado: Inactivas</option>
                </select>
              </div>
            </div>

            <div className="overflow-x-auto rounded-2xl border border-[var(--color-border)] bg-white">
              <div className="hidden grid-cols-[80px_minmax(160px,1.4fr)_minmax(160px,1fr)_minmax(100px,0.8fr)_110px_150px] items-center border-b border-[var(--color-border)] bg-slate-50 px-4 py-3 text-xs font-bold uppercase tracking-[0.14em] text-slate-500 xl:grid">
                <span>Código</span>
                <span>Sucursal</span>
                <span>Dirección</span>
                <span>Comuna</span>
                <span>Estado</span>
                <span>Acciones</span>
              </div>

              {filteredLocations.map((location) => (
                <div
                  className="grid gap-2 border-b border-[var(--color-border)] px-4 py-3 last:border-b-0 xl:grid-cols-[80px_minmax(160px,1.4fr)_minmax(160px,1fr)_minmax(100px,0.8fr)_110px_150px] xl:items-center"
                  key={location.id}
                >
                  <span className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                    {location.code}
                  </span>
                  <div>
                    <p className="truncate font-semibold text-slate-950">{location.name}</p>
                    {location.code.toLowerCase() === 'principal' ? (
                      <span className="mt-1 inline-flex rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700">
                        Principal
                      </span>
                    ) : null}
                  </div>
                  <p className="truncate text-sm text-slate-600">
                    {location.address ?? 'Sin dirección'}
                  </p>
                  <p className="truncate text-sm text-slate-600">
                    {location.commune ?? location.city ?? 'Sin comuna'}
                  </p>
                  <StatusBadge
                    label={location.active ? 'Activa' : 'Inactiva'}
                    tone={location.active ? 'success' : 'neutral'}
                  />
                  <select
                    className="h-10 rounded-[12px] border border-[var(--color-border)] bg-white px-3 text-sm font-semibold text-slate-700"
                    defaultValue=""
                    onChange={(event) => {
                      executeLocationAction(location, event.target.value)
                      event.target.value = ''
                    }}
                  >
                    <option value="">Acciones</option>
                    <option value="view">Ver</option>
                    <option value="edit">Editar</option>
                    <option value="professionals">Profesionales</option>
                    <option value="rooms">Cabinas</option>
                    <option value="qr" disabled={!location.active}>QR comercial</option>
                    <option value="deactivate" disabled={!location.active}>Desactivar</option>
                  </select>
                </div>
              ))}

              {filteredLocations.length === 0 ? (
                <p className="px-4 py-8 text-center text-sm text-slate-500">
                  No hay sucursales que coincidan con los filtros seleccionados.
                </p>
              ) : null}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-600">
              <span>
                Mostrando {filteredLocations.length} de {locations.length} sedes.
              </span>
              <span className="font-semibold text-slate-700">
                Las citas deben considerar sucursal, profesional y horario.
              </span>
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
                El código debe ser único dentro del negocio. Usa nombres claros porque se mostrarán
                en agenda y conversaciones.
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
                  placeholder="+56XXXXXXXXX"
                  value={form.phone}
                  onChange={(event) => updateField('phone', event.target.value)}
                />
                <Input
                  error={formErrors.whatsappNumber}
                  label="WhatsApp"
                  placeholder="+56XXXXXXXXX"
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

          {form.id ? (
            <Card className="space-y-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                  Horarios de atención
                </p>
                <h2 className="mt-2 text-xl font-semibold text-slate-950">
                  Horarios de {form.name}
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  Configura el horario de atención para cada día de la semana. Estos horarios se
                  usan en agenda, disponibilidad y reservas.
                </p>
              </div>

              {businessHoursQuery.isPending ? (
                <p className="text-sm text-slate-500">Cargando horarios...</p>
              ) : null}

              <div className="grid gap-2">
                <div className="hidden grid-cols-[1fr_150px_150px_48px] gap-3 px-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 sm:grid">
                  <span>Día</span>
                  <span>Hora Inicio</span>
                  <span>Hora Término</span>
                  <span></span>
                </div>
                {businessHours.map((entry, index) => (
                  <div
                    className="grid gap-2 rounded-[18px] border border-[var(--color-border)] bg-white p-3 sm:grid-cols-[1fr_150px_150px_48px] sm:items-center"
                    key={entry.dayOfWeek}
                  >
                    <p className="text-sm font-semibold text-slate-950">
                      {DAY_LABELS[entry.dayOfWeek]}
                    </p>
                    <label className="block">
                      <span className="mb-1 block text-xs font-semibold text-slate-500 sm:hidden">
                        Hora Inicio
                      </span>
                      <input
                        className="h-10 w-full rounded-[14px] border border-[var(--color-border)] px-3 text-sm outline-none focus:border-[var(--color-primary)]"
                        onChange={(event) => {
                          setBusinessHours((prev) =>
                            prev.map((h, i) =>
                              i === index ? { ...h, startTime: event.target.value } : h,
                            ),
                          )
                          setBusinessHoursSaved(false)
                        }}
                        type="time"
                        value={entry.startTime}
                      />
                    </label>
                    <label className="block">
                      <span className="mb-1 block text-xs font-semibold text-slate-500 sm:hidden">
                        Hora Término
                      </span>
                      <input
                        className="h-10 w-full rounded-[14px] border border-[var(--color-border)] px-3 text-sm outline-none focus:border-[var(--color-primary)]"
                        onChange={(event) => {
                          setBusinessHours((prev) =>
                            prev.map((h, i) =>
                              i === index ? { ...h, endTime: event.target.value } : h,
                            ),
                          )
                          setBusinessHoursSaved(false)
                        }}
                        type="time"
                        value={entry.endTime}
                      />
                    </label>
                    <div className="flex items-center justify-end">
                      {entry.startTime && entry.endTime ? (
                        <span
                          className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-emerald-50 text-xs text-emerald-600"
                          title="Activo"
                        >
                          ✓
                        </span>
                      ) : (
                        <span
                          className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-xs text-slate-400"
                          title="Sin atención"
                        >
                          —
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4">
                <div className="flex items-center gap-2 text-sm text-slate-600">
                  <StatusBadge
                    label={businessHoursSaved ? 'Guardado' : 'Sin guardar'}
                    tone={businessHoursSaved ? 'success' : 'info'}
                  />
                  <span>
                    Estos horarios se reflejan en agenda completa, disponibilidad y reservas.
                  </span>
                </div>
                <Button
                  loading={saveHoursMutation.isPending}
                  onClick={() => saveHoursMutation.mutate()}
                >
                  Guardar horarios
                </Button>
              </div>
            </Card>
          ) : null}
        </>
      ) : null}

      <Modal
        maxWidthClassName="max-w-[520px]"
        onClose={() => setQrLocationId(null)}
        open={Boolean(qrLocationId)}
      >
        <div className="space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                QR comercial
              </p>
              {commercialQrQuery.data ? (
                <h3 className="mt-1 text-lg font-semibold text-slate-950">
                  {commercialQrQuery.data.locationName}
                </h3>
              ) : null}
            </div>
            <button
              className="inline-flex h-8 w-8 items-center justify-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              onClick={() => setQrLocationId(null)}
              type="button"
            >
              <svg
                className="h-5 w-5"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {commercialQrQuery.isLoading ? (
            <div className="flex animate-pulse items-center gap-4">
              <div className="h-40 w-40 flex-shrink-0 rounded-xl bg-slate-200" />
              <div className="flex-1 space-y-3">
                <div className="h-4 w-24 rounded bg-slate-200" />
                <div className="h-3 w-48 rounded bg-slate-200" />
                <div className="h-10 w-36 rounded-xl bg-slate-200" />
              </div>
            </div>
          ) : commercialQrQuery.isError ? (
            <div className="rounded-xl bg-rose-50 px-4 py-4 text-sm text-rose-700">
              <p className="font-semibold">No fue posible cargar el QR</p>
              <p className="mt-1">
                {commercialQrQuery.error instanceof ApiClientError
                  ? commercialQrQuery.error.message
                  : 'Verifica que WhatsApp Cloud API este configurado y la sucursal este activa.'}
              </p>
            </div>
          ) : commercialQrQuery.data ? (
            <CommercialWhatsAppQr
              displayPhoneNumber={commercialQrQuery.data.displayPhoneNumber}
              prefilledMessage={commercialQrQuery.data.prefilledMessage}
              waUrl={commercialQrQuery.data.waUrl}
            />
          ) : null}
        </div>
      </Modal>

      <ConfirmDialog
        confirmLabel="Desactivar"
        confirmLoading={deactivateMutation.isPending}
        description={
          locationToDeactivate
            ? `La sucursal ${locationToDeactivate.name} dejara de recibir nuevas reservas.`
            : 'Confirma la desactivacion de la sucursal.'
        }
        onCancel={() => setLocationToDeactivate(null)}
        onConfirm={() => {
          if (locationToDeactivate) {
            setForm(toFormState(locationToDeactivate))
            deactivateMutation.mutate(locationToDeactivate.id, {
              onSettled: () => setLocationToDeactivate(null),
            })
          }
        }}
        open={Boolean(locationToDeactivate)}
        title="Desactivar sucursal"
        tone="danger"
      />
    </section>
  )
}

function MetricCard({
  helper,
  icon,
  label,
  value,
}: {
  helper: string
  icon: string
  label: string
  value: string
}) {
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

  addMaxLengthError(
    errors,
    'address',
    form.address,
    255,
    'La dirección no puede superar 255 caracteres.',
  )
  addMaxLengthError(errors, 'city', form.city, 120, 'La ciudad no puede superar 120 caracteres.')
  addMaxLengthError(
    errors,
    'commune',
    form.commune,
    120,
    'La comuna no puede superar 120 caracteres.',
  )
  addMaxLengthError(errors, 'phone', form.phone, 30, 'El teléfono no puede superar 30 caracteres.')
  addMaxLengthError(
    errors,
    'whatsappNumber',
    form.whatsappNumber,
    30,
    'El WhatsApp no puede superar 30 caracteres.',
  )

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
