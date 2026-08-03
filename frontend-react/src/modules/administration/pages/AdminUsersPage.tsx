import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
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
import { ConfirmDialog } from '../../../components/overlay/ConfirmDialog'
import { useToast } from '../../../lib/toast'
import {
  deactivateAdminUserRequest,
  listAdminRolesRequest,
  listAdminUsersRequest,
  resetAdminUserAccessRequest,
  revokeAdminUserSessionsRequest,
} from '../../../services/api/administrationApi'
import type { AdminUserResponse } from '../../../services/api/types'

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

function roleTone(role: string) {
  return role === 'OWNER' || role === 'ADMIN' ? 'info' : 'neutral'
}

function mfaLabel(role: string) {
  return role === 'OWNER' || role === 'ADMIN' ? 'Requerido' : 'Opcional'
}

export function AdminUsersPage() {
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [search, setSearch] = useState('')
  const [role, setRole] = useState('')
  const [status, setStatus] = useState('')
  const [pendingAction, setPendingAction] = useState<{
    action: 'deactivate' | 'revoke' | 'reset'
    user: AdminUserResponse
  } | null>(null)

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
  const roleLabels = new Map((rolesQuery.data ?? []).map((role) => [role.code, role.name]))

  const invalidateUsers = async () => {
    await queryClient.invalidateQueries({ queryKey: ['administration', 'users'] })
    await queryClient.invalidateQueries({ queryKey: ['administration', 'summary'] })
  }

  const deactivateMutation = useMutation({
    mutationFn: deactivateAdminUserRequest,
    onSuccess: async () => {
      await invalidateUsers()
      showToast({
        title: 'Usuario desactivado',
        description: 'El usuario ya no puede iniciar nuevas sesiones.',
        tone: 'success',
      })
    },
    onError: () =>
      showToast({
        title: 'No se pudo desactivar',
        description: 'Revisa permisos y estado del usuario antes de reintentar.',
        tone: 'error',
      }),
  })

  const revokeSessionsMutation = useMutation({
    mutationFn: revokeAdminUserSessionsRequest,
    onSuccess: async () => {
      await invalidateUsers()
      showToast({
        title: 'Sesiones revocadas',
        description: 'Las sesiones activas del usuario fueron cerradas.',
        tone: 'success',
      })
    },
    onError: () =>
      showToast({
        title: 'No se pudieron revocar sesiones',
        description: 'Reintenta en unos segundos.',
        tone: 'error',
      }),
  })

  const resetAccessMutation = useMutation({
    mutationFn: resetAdminUserAccessRequest,
    onSuccess: async () => {
      await invalidateUsers()
      showToast({
        title: 'Acceso restablecido',
        description: 'Se solicito un enlace de acceso de un solo uso para el usuario.',
        tone: 'success',
      })
    },
    onError: () =>
      showToast({
        title: 'No se pudo restablecer acceso',
        description: 'Revisa el correo y estado del usuario antes de reintentar.',
        tone: 'error',
      }),
  })

  const confirmAction = () => {
    if (!pendingAction) return
    const { action, user } = pendingAction
    setPendingAction(null)
    if (action === 'deactivate') {
      deactivateMutation.mutate(user.id)
    } else if (action === 'revoke') {
      revokeSessionsMutation.mutate(user.id)
    } else {
      resetAccessMutation.mutate(user.id)
    }
  }

  const rows: DataTableShellRow[] = (usersQuery.data?.items ?? []).map((user) => ({
    id: user.id,
    cells: [
      <div key={`${user.id}-name`}>
        <p className="font-semibold text-slate-950">
          {user.firstName} {user.lastName}
        </p>
        <p className="mt-1 text-xs text-slate-500">{user.email}</p>
      </div>,
      <StatusBadge
        key={`${user.id}-role`}
        label={roleLabels.get(user.role) ?? user.role}
        tone={roleTone(user.role)}
      />,
      <StatusBadge
        key={`${user.id}-status`}
        label={statusLabel(user.status)}
        tone={statusTone(user.status)}
      />,
      <span key={`${user.id}-locations`}>Segun rol y sucursal</span>,
      <StatusBadge
        key={`${user.id}-mfa`}
        label={mfaLabel(user.role)}
        tone={user.role === 'OWNER' || user.role === 'ADMIN' ? 'warning' : 'neutral'}
      />,
      <span key={`${user.id}-failed`}>{user.failedLoginAttempts}</span>,
      user.lastLoginAt ? dayjs(user.lastLoginAt).format('DD/MM/YYYY HH:mm') : 'Sin acceso reciente',
      dayjs(user.createdAt).format('DD/MM/YYYY'),
      <div key={`${user.id}-actions`} className="flex flex-wrap justify-end gap-2">
        <Link to={`/admin/users/${user.id}/edit`}>
          <Button size="sm" variant="secondary">
            Ver
          </Button>
        </Link>
        <Link to={`/admin/users/${user.id}/edit`}>
          <Button size="sm" variant="secondary">
            Editar
          </Button>
        </Link>
        <select
          aria-label={`Acciones para ${user.firstName} ${user.lastName}`}
          className="rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 text-sm font-semibold text-slate-700"
          defaultValue=""
          onChange={(event) => {
            const action = event.target.value as 'deactivate' | 'revoke' | 'reset' | ''
            event.target.value = ''
            if (action) {
              setPendingAction({ action, user })
            }
          }}
        >
          <option value="">Mas acciones</option>
          <option value="reset">Restablecer acceso</option>
          <option value="revoke">Revocar sesiones</option>
          {user.status === 'ACTIVE' ? <option value="deactivate">Desactivar</option> : null}
        </select>
      </div>,
    ],
  }))

  const actionCopy = pendingAction
    ? {
        deactivate: {
          title: 'Desactivar usuario',
          description: `Se desactivara el acceso de ${pendingAction.user.firstName} ${pendingAction.user.lastName} y se revocaran sus sesiones activas.`,
          label: 'Desactivar',
          tone: 'danger' as const,
        },
        revoke: {
          title: 'Revocar sesiones',
          description: `Se cerraran las sesiones activas de ${pendingAction.user.firstName} ${pendingAction.user.lastName}.`,
          label: 'Revocar sesiones',
          tone: 'danger' as const,
        },
        reset: {
          title: 'Restablecer acceso',
          description: `Se enviara un enlace de un solo uso a ${pendingAction.user.email}.`,
          label: 'Restablecer acceso',
          tone: 'neutral' as const,
        },
      }[pendingAction.action]
    : null

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
        description="Gestiona usuarios, roles operativos, estado de acceso, MFA y sesiones del negocio."
        eyebrow="Administración"
        title="Usuarios y roles"
      />

      <FilterBar>
        <Input
          label="Buscar"
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Nombre o correo"
          value={search}
        />
        <Select
          label="Rol"
          onChange={(event) => setRole(event.target.value)}
          options={roleOptions}
          value={role}
        />
        <Select
          label="Estado"
          onChange={(event) => setStatus(event.target.value)}
          options={statusOptions}
          value={status}
        />
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
          columns={[
            'Usuario',
            'Rol',
            'Estado',
            'Sucursales',
            'MFA',
            'Intentos fallidos',
            'Ultimo acceso',
            'Creacion',
            'Acciones',
          ]}
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

      <ConfirmDialog
        confirmLabel={actionCopy?.label ?? 'Confirmar'}
        confirmLoading={
          deactivateMutation.isPending ||
          revokeSessionsMutation.isPending ||
          resetAccessMutation.isPending
        }
        description={actionCopy?.description ?? ''}
        onCancel={() => setPendingAction(null)}
        onConfirm={confirmAction}
        open={Boolean(pendingAction)}
        title={actionCopy?.title ?? 'Confirmar accion'}
        tone={actionCopy?.tone ?? 'neutral'}
      />
    </section>
  )
}
