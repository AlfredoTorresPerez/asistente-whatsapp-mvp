import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { useToast } from '../../../lib/toast'
import {
  createProfessionalRequest,
  getProfessionalRequest,
  updateProfessionalRequest,
} from '../../../services/api/professionalsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { ApiClientError } from '../../../services/api/httpClient'
import type { ProfessionalRequest, ProfessionalResponse } from '../../../services/api/types'

type FormState = {
  fullName: string
  displayName: string
  specialty: string
  email: string
  phone: string
  description: string
  color: string
  maxDailyBookings: string
  qualificationLevel: string
  certificationRef: string
  active: string
  locationIds: string[]
}

function toInitialForm(p?: ProfessionalResponse): FormState {
  return {
    fullName: p?.fullName ?? '',
    displayName: p?.displayName ?? '',
    specialty: p?.specialty ?? '',
    email: p?.email ?? '',
    phone: p?.phone ?? '',
    description: p?.description ?? '',
    color: p?.color ?? '',
    maxDailyBookings: p?.maxDailyBookings != null ? String(p.maxDailyBookings) : '',
    qualificationLevel: p?.qualificationLevel != null ? String(p.qualificationLevel) : '',
    certificationRef: p?.certificationRef ?? '',
    active: p?.active != null ? (p.active ? 'true' : 'false') : 'true',
    locationIds: p?.locationIds ?? [],
  }
}

function toRequest(form: FormState): ProfessionalRequest {
  return {
    fullName: form.fullName.trim(),
    displayName: form.displayName.trim() || null,
    specialty: form.specialty.trim() || null,
    email: form.email.trim() || null,
    phone: form.phone.trim() || null,
    description: form.description.trim() || null,
    color: form.color.trim() || null,
    maxDailyBookings: form.maxDailyBookings ? Number(form.maxDailyBookings) : null,
    qualificationLevel: form.qualificationLevel ? Number(form.qualificationLevel) : null,
    certificationRef: form.certificationRef.trim() || null,
    active: form.active === 'true',
    locationIds: form.locationIds.length > 0 ? form.locationIds : null,
  }
}

export function AdminProfessionalFormPage() {
  const { professionalId } = useParams()
  const isEdit = Boolean(professionalId)

  const professionalQuery = useQuery({
    enabled: isEdit,
    queryKey: ['administration', 'professionals', professionalId],
    queryFn: () => getProfessionalRequest(professionalId ?? ''),
  })

  const locationsQuery = useQuery({
    queryKey: ['administration', 'locations', 'all'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: false }),
  })

  if ((isEdit && professionalQuery.isPending) || locationsQuery.isPending) {
    return <LoadingState message="Cargando datos del profesional." variant="page" />
  }

  if (professionalQuery.isError || locationsQuery.isError) {
    return (
      <ErrorState
        description="No pudimos recuperar los datos del profesional o las sedes."
        onRetry={() => {
          void professionalQuery.refetch()
          void locationsQuery.refetch()
        }}
        title="No fue posible cargar el formulario"
      />
    )
  }

  return (
    <AdminProfessionalForm
      isEdit={isEdit}
      key={professionalQuery.data?.id ?? 'new-professional'}
      locations={locationsQuery.data ?? []}
      professional={professionalQuery.data}
      professionalId={professionalId}
    />
  )
}

function AdminProfessionalForm({
  isEdit,
  locations,
  professional,
  professionalId,
}: {
  isEdit: boolean
  locations: { id: string; name: string }[]
  professional?: ProfessionalResponse
  professionalId?: string
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [form, setForm] = useState<FormState>(() => toInitialForm(professional))
  const [formError, setFormError] = useState<string | null>(null)

  const locationOptions = locations.map((l) => ({ label: l.name, value: l.id }))

  const mutation = useMutation({
    mutationFn: () =>
      isEdit && professionalId
        ? updateProfessionalRequest(professionalId, toRequest(form))
        : createProfessionalRequest(toRequest(form)),
    onError: (error) => {
      setFormError(
        error instanceof ApiClientError ? error.message : 'No fue posible guardar el profesional.',
      )
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'professionals'] })
      showToast({
        title: isEdit ? 'Profesional actualizado' : 'Profesional creado',
        description: 'Los cambios ya estan disponibles.',
        tone: 'success',
      })
      navigate('/admin/professionals')
    },
  })

  const submit = () => {
    setFormError(null)
    if (!form.fullName.trim()) {
      setFormError('El nombre completo es obligatorio.')
      return
    }
    mutation.mutate()
  }

  const toggleLocation = (locationId: string) => {
    setForm((prev) => ({
      ...prev,
      locationIds: prev.locationIds.includes(locationId)
        ? prev.locationIds.filter((id) => id !== locationId)
        : [...prev.locationIds, locationId],
    }))
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Link to="/admin/professionals">
            <Button variant="secondary">Volver</Button>
          </Link>
        }
        description="Datos personales, especialidad, contacto y sedes donde atiende el profesional."
        eyebrow="Administración"
        title={isEdit ? 'Editar profesional' : 'Crear profesional'}
      />

      <Card>
        <div className="mb-6">
          <p className="text-lg font-semibold text-slate-950">Datos del profesional</p>
          <p className="mt-1 text-sm text-slate-600">
            Campos marcados con * son obligatorios.
          </p>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label="Nombre completo *"
            onChange={(event) => setForm({ ...form, fullName: event.target.value })}
            value={form.fullName}
          />
          <Input
            label="Nombre para mostrar"
            hint="Si se deja vacio, se usa el nombre completo."
            onChange={(event) => setForm({ ...form, displayName: event.target.value })}
            value={form.displayName}
          />
          <Input
            label="Especialidad"
            onChange={(event) => setForm({ ...form, specialty: event.target.value })}
            value={form.specialty}
          />
          <Input
            label="Email"
            onChange={(event) => setForm({ ...form, email: event.target.value })}
            type="email"
            value={form.email}
          />
          <Input
            label="Telefono"
            onChange={(event) => setForm({ ...form, phone: event.target.value })}
            value={form.phone}
          />
          <Input
            label="Color"
            hint="Hex: #E8F4FD"
            onChange={(event) => setForm({ ...form, color: event.target.value })}
            placeholder="#"
            value={form.color}
          />
          <Input
            label="Cupo diario maximo"
            hint="Reservas por dia (opcional)"
            onChange={(event) => setForm({ ...form, maxDailyBookings: event.target.value })}
            type="number"
            value={form.maxDailyBookings}
          />
          <Input
            label="Nivel de calificacion"
            hint="0-5 (opcional)"
            onChange={(event) => setForm({ ...form, qualificationLevel: event.target.value })}
            type="number"
            value={form.qualificationLevel}
          />
          <Input
            label="Referencia de certificacion"
            onChange={(event) => setForm({ ...form, certificationRef: event.target.value })}
            value={form.certificationRef}
          />
          <Select
            label="Estado"
            onChange={(event) => setForm({ ...form, active: event.target.value })}
            options={[
              { label: 'Activo', value: 'true' },
              { label: 'Inactivo', value: 'false' },
            ]}
            value={form.active}
          />
          <div className="md:col-span-2">
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Sedes donde atiende
            </label>
            <div className="flex flex-wrap gap-3">
              {locationOptions.map((opt) => (
                <label key={opt.value} className="flex items-center gap-2 text-sm">
                  <input
                    checked={form.locationIds.includes(opt.value)}
                    className="h-4 w-4 rounded border-slate-300"
                    onChange={() => toggleLocation(opt.value)}
                    type="checkbox"
                  />
                  {opt.label}
                </label>
              ))}
              {locationOptions.length === 0 ? (
                <p className="text-sm text-slate-400">
                  No hay sedes disponibles. Crea una sede primero.
                </p>
              ) : null}
            </div>
          </div>
          <div className="md:col-span-2">
            <Input
              label="Descripcion"
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              value={form.description}
            />
          </div>
        </div>

        {formError ? (
          <div className="mt-5 rounded-[18px] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {formError}
          </div>
        ) : null}

        <div className="mt-6 flex flex-wrap gap-3">
          <Button loading={mutation.isPending} onClick={submit}>
            {isEdit ? 'Guardar cambios' : 'Crear profesional'}
          </Button>
          <Link to="/admin/professionals">
            <Button variant="secondary">Cancelar</Button>
          </Link>
        </div>
      </Card>
    </section>
  )
}
