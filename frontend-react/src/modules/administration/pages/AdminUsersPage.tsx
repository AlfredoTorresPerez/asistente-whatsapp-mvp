import { keepPreviousData, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../../../components/feedback/EmptyState'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { DataTableShell, type DataTableShellRow } from '../../../components/ui/DataTableShell'
import { FilterBar } from '../../../components/ui/FilterBar'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { listAdminRolesRequest, listAdminUsersRequest } from '../../../services/api/administrationApi'

const statusOptions = [
  { label: 'Todos', value: '' },
  { label: 'Activos', value: 'ACTIVE' },
  { label: 'Inactivos', value: 'INACTIVE' },
  { label: 'Bloqueados', value: 'LOCKED' },
]

function statusTone(status: string) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'LOCKED') return 'danger'
  return 'neutral'
}

function statusLabel(status: string) {
  if (status === 'ACTIVE') return 'Activo'
  if (status === 'LOCKED') return 'Bloqueado'
  return 'Inactivo'
}

export function AdminUsersPage() {
  const [search, setSearch] = useState('')
  const [role, setRole] = useState('')
  const [status, setStatus] = useState('')

  const rolesQuery = useQuery({
    queryKey: ['administration', 'roles'],
    queryFn: listAdminRolesRequest,
  })

  const usersQuery = useQuery({
    queryKey: ['administration', 'users', search, role, status],
    queryFn: () => listAdminUsersRequest({ role, search, size: 50, status }),
    placeholderData: keepPreviousData,
  })

  const roleOptions = [
    { label: 'Todos', value: '' },
    ...(rolesQuery.data ?? []).map((role) => ({ label: role.name, value: role.code })),
  ]

  const rows: DataTableShellRow[] = (usersQuery.data?.items ?? []).map((user) => ({
    id: user.id,
    href: `/admin/users/${user.id}/edit`,
    cells: [
      <div key={`${user.id}-name`}>
        <p className="font-semibold text-slate-950">{user.firstName} {user.lastName}</p>
        <p className="mt-1 text-xs text-slate-500">{user.email}</p>
      </div>,
      <StatusBadge key={`${user.id}-role`} label={user.role} tone={user.role === 'OWNER' ? 'info' : 'neutral'} />,
      <StatusBadge key={`${user.id}-status`} label={statusLabel(user.status)} tone={statusTone(user.status)} />,
      user.lastLoginAt ? dayjs(user.lastLoginAt).format('DD/MM/YYYY HH:mm') : 'Sin acceso reciente',
    ],
  }))

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <>
            <Link to="/admin/users/new">
              <Button>Crear usuario</Button>
            </Link>
            <Link to="/admin">
              <Button variant="secondary">Volver</Button>
            </Link>
          </>
        }
        description="Gestiona usuarios del negocio, roles operativos y estado de acceso para la demo administrativa."
        eyebrow="Administracion"
        title="Usuarios y roles"
      />

      <FilterBar>
        <Input label="Buscar" onChange={(event) => setSearch(event.target.value)} placeholder="Nombre o correo" value={search} />
        <Select label="Rol" onChange={(event) => setRole(event.target.value)} options={roleOptions} value={role} />
        <Select label="Estado" onChange={(event) => setStatus(event.target.value)} options={statusOptions} value={status} />
      </FilterBar>

      {usersQuery.isPending ? (
        <LoadingState message="Cargando usuarios y roles del negocio." variant="page" />
      ) : null}

      {usersQuery.isError ? (
        <ErrorState
          description="No pudimos recuperar los usuarios administrativos."
          onRetry={() => void usersQuery.refetch()}
          title="No fue posible cargar usuarios"
        />
      ) : null}

      {usersQuery.data && rows.length > 0 ? (
        <DataTableShell
          caption={`${usersQuery.data.totalItems} usuario(s) registrados`}
          columns={['Usuario', 'Rol', 'Estado', 'Ultimo acceso']}
          rows={rows}
        />
      ) : null}

      {usersQuery.data && rows.length === 0 ? (
        <EmptyState
          description="Crea el primer usuario operativo para separar la cuenta administradora de la atencion diaria."
          primaryAction={{ label: 'Crear usuario', to: '/admin/users/new' }}
          title="No hay usuarios administrativos"
          variant="card"
        />
      ) : null}
    </section>
  )
}
