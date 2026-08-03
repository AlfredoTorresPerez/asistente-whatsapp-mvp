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
import { createRoomRequest, getRoomRequest, updateRoomRequest } from '../../../services/api/roomsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'
import { ApiClientError } from '../../../services/api/httpClient'
import type { RoomRequest, RoomResponse } from '../../../services/api/types'

type FormState = {
  code: string
  name: string
  roomType: string
  capacity: string
  description: string
  color: string
  notes: string
  active: string
  locationId: string
}

const DEFAULT_COLOR = '#0EA5E9'

const ROOM_TYPE_OPTIONS = [
  { label: 'Facial', value: 'FACIAL' },
  { label: 'Corporal', value: 'CORPORAL' },
  { label: 'Facial/Corporal', value: 'FACIAL_CORPORAL' },
  { label: 'Depilacion/Manos', value: 'DEPILACION_MANOS' },
  { label: 'Manos/Pies', value: 'MANOS_PIES' },
  { label: 'Peluqueria', value: 'PELUQUERIA' },
  { label: 'Maquillaje', value: 'MAQUILLAJE' },
  { label: 'Consultorio', value: 'CONSULTORIO' },
  { label: 'Multiproposito', value: 'MULTIPROPOSITO' },
]

function toInitialForm(r?: RoomResponse): FormState {
  return {
    code: r?.code ?? '',
    name: r?.name ?? '',
    roomType: r?.roomType ?? 'FACIAL',
    capacity: r?.capacity != null ? String(r.capacity) : '1',
    description: r?.description ?? '',
    color: r?.color ?? DEFAULT_COLOR,
    notes: r?.notes ?? '',
    active: r?.active != null ? (r.active ? 'true' : 'false') : 'true',
    locationId: r?.locationId ?? '',
  }
}

function toRequest(form: FormState): RoomRequest {
  return {
    code: form.code.trim(),
    name: form.name.trim(),
    roomType: form.roomType,
    capacity: form.capacity ? Number(form.capacity) : 1,
    description: form.description.trim() || null,
    color: form.color.trim() || null,
    notes: form.notes.trim() || null,
    active: form.active === 'true',
    locationId: form.locationId || null,
  }
}

export function AdminRoomFormPage() {
  const { roomId } = useParams()
  const isEdit = Boolean(roomId)

  const roomQuery = useQuery({
    enabled: isEdit,
    queryKey: ['administration', 'rooms', roomId],
    queryFn: () => getRoomRequest(roomId ?? ''),
  })

  const locationsQuery = useQuery({
    queryKey: ['administration', 'locations', 'all'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: false }),
  })

  if ((isEdit && roomQuery.isPending) || locationsQuery.isPending) {
    return <LoadingState message="Cargando datos de la cabina." variant="page" />
  }

  if (roomQuery.isError || locationsQuery.isError) {
    return (
      <ErrorState
        description="No pudimos recuperar los datos de la cabina o las sedes."
        onRetry={() => {
          void roomQuery.refetch()
          void locationsQuery.refetch()
        }}
        title="No fue posible cargar el formulario"
      />
    )
  }

  return (
    <AdminRoomForm
      isEdit={isEdit}
      key={roomQuery.data?.id ?? 'new-room'}
      locations={locationsQuery.data ?? []}
      room={roomQuery.data}
      roomId={roomId}
    />
  )
}

function AdminRoomForm({
  isEdit,
  locations,
  room,
  roomId,
}: {
  isEdit: boolean
  locations: { id: string; name: string }[]
  room?: RoomResponse
  roomId?: string
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [form, setForm] = useState<FormState>(() => toInitialForm(room))
  const [formError, setFormError] = useState<string | null>(null)

  const locationOptions = [
    { label: 'Selecciona una sede', value: '' },
    ...locations.map((l) => ({ label: l.name, value: l.id })),
  ]

  const mutation = useMutation({
    mutationFn: () =>
      isEdit && roomId
        ? updateRoomRequest(roomId, toRequest(form))
        : createRoomRequest(toRequest(form)),
    onError: (error) => {
      setFormError(
        error instanceof ApiClientError ? error.message : 'No fue posible guardar la cabina.',
      )
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'rooms'] })
      showToast({
        title: isEdit ? 'Cabina actualizada' : 'Cabina creada',
        description: 'Los cambios ya estan disponibles.',
        tone: 'success',
      })
      navigate('/admin/rooms')
    },
  })

  const submit = () => {
    setFormError(null)
    if (!form.name.trim()) {
      setFormError('El nombre es obligatorio.')
      return
    }
    if (!form.code.trim()) {
      setFormError('El codigo es obligatorio.')
      return
    }
    if (!form.locationId) {
      setFormError('Selecciona una sede para la cabina.')
      return
    }
    mutation.mutate()
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Link to="/admin/rooms">
            <Button variant="secondary">Volver</Button>
          </Link>
        }
        description="Define los datos de la cabina, sala o recurso del centro estetico."
        eyebrow="Administración"
        title={isEdit ? 'Editar cabina' : 'Crear cabina'}
      />

      <Card>
        <div className="mb-6">
          <p className="text-lg font-semibold text-slate-950">Datos de la cabina</p>
          <p className="mt-1 text-sm text-slate-600">
            Campos marcados con * son obligatorios.
          </p>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label="Nombre *"
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            value={form.name}
          />
          <Input
            label="Codigo *"
            hint="Identificador unico por sede"
            onChange={(event) => setForm({ ...form, code: event.target.value })}
            value={form.code}
          />
          <Select
            label="Tipo *"
            onChange={(event) => setForm({ ...form, roomType: event.target.value })}
            options={ROOM_TYPE_OPTIONS}
            value={form.roomType}
          />
          <Input
            label="Capacidad"
            hint="Puestos de atencion"
            onChange={(event) => setForm({ ...form, capacity: event.target.value })}
            type="number"
            value={form.capacity}
          />
          <Select
            label="Sede *"
            onChange={(event) => setForm({ ...form, locationId: event.target.value })}
            options={locationOptions}
            value={form.locationId}
          />
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">
              Color en agenda
            </label>
            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-3 py-2.5">
              <input
                aria-label="Seleccionar color de la cabina"
                className="h-10 w-12 cursor-pointer rounded-lg border border-slate-200 bg-white p-1"
                onChange={(event) => setForm({ ...form, color: event.target.value })}
                type="color"
                value={form.color || DEFAULT_COLOR}
              />
              <span className="text-sm text-slate-600">Selecciona el color visible en agenda</span>
            </div>
          </div>
          <Select
            label="Estado"
            onChange={(event) => setForm({ ...form, active: event.target.value })}
            options={[
              { label: 'Activa', value: 'true' },
              { label: 'Inactiva', value: 'false' },
            ]}
            value={form.active}
          />
          <div className="md:col-span-2">
            <Input
              label="Descripcion"
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              value={form.description}
            />
          </div>
          <div className="md:col-span-2">
            <Input
              label="Notas internas"
              onChange={(event) => setForm({ ...form, notes: event.target.value })}
              value={form.notes}
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
            {isEdit ? 'Guardar cambios' : 'Crear cabina'}
          </Button>
          <Link to="/admin/rooms">
            <Button variant="secondary">Cancelar</Button>
          </Link>
        </div>
      </Card>
    </section>
  )
}
