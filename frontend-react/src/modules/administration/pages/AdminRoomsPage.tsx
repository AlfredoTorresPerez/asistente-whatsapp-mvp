import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { StatusBadge as Badge } from '../../../components/ui/StatusBadge'
import { Button } from '../../../components/ui/Button'
import { DataTableShell, type DataTableShellRow } from '../../../components/ui/DataTableShell'
import { FilterBar } from '../../../components/ui/FilterBar'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { listRoomsRequest } from '../../../services/api/roomsApi'
import { getBusinessLocationsRequest } from '../../../services/api/businessLocationsApi'

const ROOM_TYPE_LABELS: Record<string, string> = {
  FACIAL: 'Facial',
  CORPORAL: 'Corporal',
  FACIAL_CORPORAL: 'Facial/Corporal',
  DEPILACION_MANOS: 'Depilacion/Manos',
  MANOS_PIES: 'Manos/Pies',
  PELUQUERIA: 'Peluqueria',
  MAQUILLAJE: 'Maquillaje',
  CONSULTORIO: 'Consultorio',
  MULTIPROPOSITO: 'Multiproposito',
}

function roomTypeLabel(type: string) {
  return ROOM_TYPE_LABELS[type] ?? type
}

export function AdminRoomsPage() {
  const [search, setSearch] = useState('')
  const [locationId, setLocationId] = useState('')
  const [roomType, setRoomType] = useState('')
  const [active, setActive] = useState('')

  const locationsQuery = useQuery({
    queryKey: ['administration', 'locations', 'all'],
    queryFn: () => getBusinessLocationsRequest({ activeOnly: false }),
  })

  const roomsQuery = useQuery({
    queryKey: ['administration', 'rooms', search, locationId, roomType, active],
    queryFn: () =>
      listRoomsRequest({
        search,
        locationId: locationId || undefined,
        roomType: roomType || undefined,
        size: 50,
        active: active === '' ? undefined : active === 'true',
      }),
    placeholderData: keepPreviousData,
  })

  const locationOptions = [
    { label: 'Todas las sedes', value: '' },
    ...(locationsQuery.data ?? []).map((l) => ({ label: l.name, value: l.id })),
  ]

  const activeOptions = [
    { label: 'Todas', value: '' },
    { label: 'Activas', value: 'true' },
    { label: 'Inactivas', value: 'false' },
  ]

  const rows: DataTableShellRow[] = (roomsQuery.data?.items ?? []).map((r) => ({
    id: r.id,
    href: `/admin/rooms/${r.id}/edit`,
    cells: [
      <div key={`${r.id}-name`} className="flex items-center gap-2">
        {r.color ? (
          <span
            className="inline-block h-3 w-3 rounded-full"
            style={{ backgroundColor: r.color }}
          />
        ) : null}
        <div>
          <p className="font-semibold text-slate-950">{r.name}</p>
          <p className="mt-1 text-xs text-slate-500">{r.code}</p>
        </div>
      </div>,
      <Badge key={`${r.id}-type`} label={roomTypeLabel(r.roomType)} tone="info" />,
      <span key={`${r.id}-capacity`} className="text-sm text-slate-600">
        {r.capacity} puesto(s)
      </span>,
      <span key={`${r.id}-location`} className="text-sm text-slate-600">
        {r.locationName}
      </span>,
      <StatusBadge
        key={`${r.id}-status`}
        label={r.active ? 'Activa' : 'Inactiva'}
        tone={r.active ? 'success' : 'neutral'}
      />,
    ],
  }))

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Link to="/admin/rooms/new">
              <Button>Crear cabina</Button>
            </Link>
            <Link to="/admin">
              <Button variant="secondary">Volver</Button>
            </Link>
          </>
        }
        description="Gestiona cabinas, salas y recursos del centro estetico por sede."
        eyebrow="Administración"
        title="Cabinas y recursos"
      />

      <FilterBar>
        <Input
          label="Buscar"
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Nombre o codigo"
          value={search}
        />
        <Select
          label="Sede"
          onChange={(event) => setLocationId(event.target.value)}
          options={locationOptions}
          value={locationId}
        />
        <Select
          label="Estado"
          onChange={(event) => setActive(event.target.value)}
          options={activeOptions}
          value={active}
        />
      </FilterBar>

      {roomsQuery.isPending ? (
        <LoadingState message="Cargando cabinas y recursos del centro." variant="page" />
      ) : null}

      {roomsQuery.isError ? (
        <ErrorState
          description="No pudimos recuperar las cabinas y recursos."
          onRetry={() => void roomsQuery.refetch()}
          title="No fue posible cargar cabinas"
        />
      ) : null}

      {roomsQuery.data && rows.length > 0 ? (
        <DataTableShell
          caption={`${roomsQuery.data.totalItems} cabina(s) registradas`}
          columns={['Nombre', 'Tipo', 'Capacidad', 'Sede', 'Estado']}
          rows={rows}
        />
      ) : null}

      {roomsQuery.data && rows.length === 0 ? (
        <EmptyState
          description="Crea la primera cabina o sala para asociarla a servicios y disponibilidad en agenda."
          primaryAction={{ label: 'Crear cabina', to: '/admin/rooms/new' }}
          title="No hay cabinas registradas"
          variant="card"
        />
      ) : null}
    </section>
  )
}
