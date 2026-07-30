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
import { listProfessionalsRequest } from '../../../services/api/professionalsApi'

export function AdminProfessionalsPage() {
  const [search, setSearch] = useState('')
  const [active, setActive] = useState('')

  const professionalsQuery = useQuery({
    queryKey: ['administration', 'professionals', search, active],
    queryFn: () =>
      listProfessionalsRequest({
        search,
        size: 50,
        active: active === '' ? undefined : active === 'true',
      }),
    placeholderData: keepPreviousData,
  })

  const activeOptions = [
    { label: 'Todos', value: '' },
    { label: 'Activos', value: 'true' },
    { label: 'Inactivos', value: 'false' },
  ]

  const rows: DataTableShellRow[] = (professionalsQuery.data?.items ?? []).map((p) => ({
    id: p.id,
    href: `/admin/professionals/${p.id}/edit`,
    cells: [
      <div key={`${p.id}-name`} className="flex items-center gap-2">
        {p.color ? (
          <span
            className="inline-block h-3 w-3 rounded-full"
            style={{ backgroundColor: p.color }}
          />
        ) : null}
        <div>
          <p className="font-semibold text-slate-950">{p.displayName ?? p.fullName}</p>
          <p className="mt-1 text-xs text-slate-500">{p.specialty}</p>
        </div>
      </div>,
      p.email ? (
        <a key={`${p.id}-email`} className="text-sm text-slate-600" href={`mailto:${p.email}`}>
          {p.email}
        </a>
      ) : (
        <span key={`${p.id}-noemail`} className="text-sm text-slate-400">
          Sin email
        </span>
      ),
      <div key={`${p.id}-locations`} className="flex flex-wrap gap-1">
        {p.locationNames.length > 0
          ? p.locationNames.map((name) => (
              <Badge key={name} label={name} tone="neutral" />
            ))
          : <span className="text-sm text-slate-400">Sin sede</span>}
      </div>,
      <StatusBadge
        key={`${p.id}-status`}
        label={p.active ? 'Activo' : 'Inactivo'}
        tone={p.active ? 'success' : 'neutral'}
      />,
    ],
  }))

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Link to="/admin/professionals/new">
              <Button>Crear profesional</Button>
            </Link>
            <Link to="/admin">
              <Button variant="secondary">Volver</Button>
            </Link>
          </>
        }
        description="Gestiona profesionales del centro estetico, sus especialidades, datos de contacto y sedes asignadas."
        eyebrow="Administración"
        title="Profesionales"
      />

      <FilterBar>
        <Input
          label="Buscar"
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Nombre, especialidad o email"
          value={search}
        />
        <Select
          label="Estado"
          onChange={(event) => setActive(event.target.value)}
          options={activeOptions}
          value={active}
        />
      </FilterBar>

      {professionalsQuery.isPending ? (
        <LoadingState message="Cargando profesionales del centro estetico." variant="page" />
      ) : null}

      {professionalsQuery.isError ? (
        <ErrorState
          description="No pudimos recuperar los profesionales."
          onRetry={() => void professionalsQuery.refetch()}
          title="No fue posible cargar profesionales"
        />
      ) : null}

      {professionalsQuery.data && rows.length > 0 ? (
        <DataTableShell
          caption={`${professionalsQuery.data.totalItems} profesional(es) registrados`}
          columns={['Nombre', 'Email', 'Sedes', 'Estado']}
          rows={rows}
        />
      ) : null}

      {professionalsQuery.data && rows.length === 0 ? (
        <EmptyState
          description="Crea el primer profesional del centro estetico para asociarlo a servicios, agenda y reservas."
          primaryAction={{ label: 'Crear profesional', to: '/admin/professionals/new' }}
          title="No hay profesionales registrados"
          variant="card"
        />
      ) : null}
    </section>
  )
}
