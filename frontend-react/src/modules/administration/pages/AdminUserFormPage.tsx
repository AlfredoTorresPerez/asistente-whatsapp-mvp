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
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { useToast } from '../../../lib/toast'
import {
  createAdminUserRequest,
  getAdminUserRequest,
  listAdminRolesRequest,
  updateAdminUserRequest,
} from '../../../services/api/administrationApi'
import { ApiClientError } from '../../../services/api/httpClient'
import type { AdminRoleResponse, AdminUserRequest, AdminUserResponse } from '../../../services/api/types'

type FormState = {
  email: string
  firstName: string
  lastName: string
  phone: string
  role: string
  status: string
  temporaryPassword: string
  timezone: string
}

const statusOptions = [
  { label: 'Activo', value: 'ACTIVE' },
  { label: 'Inactivo', value: 'INACTIVE' },
  { label: 'Bloqueado', value: 'LOCKED' },
]

function toInitialForm(user?: AdminUserResponse): FormState {
  return {
    email: user?.email ?? '',
    firstName: user?.firstName ?? '',
    lastName: user?.lastName ?? '',
    phone: user?.phone ?? '',
    role: user?.role ?? 'AGENT',
    status: user?.status ?? 'ACTIVE',
    temporaryPassword: '',
    timezone: user?.timezone ?? 'America/Santiago',
  }
}

function toRequest(form: FormState, isEdit: boolean): AdminUserRequest {
  return {
    email: form.email.trim(),
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    phone: form.phone.trim() || null,
    role: form.role,
    status: form.status,
    temporaryPassword: isEdit ? null : form.temporaryPassword.trim(),
    timezone: form.timezone.trim() || 'America/Santiago',
  }
}

export function AdminUserFormPage() {
  const { userId } = useParams()
  const isEdit = Boolean(userId)

  const rolesQuery = useQuery({
    queryKey: ['administration', 'roles'],
    queryFn: listAdminRolesRequest,
  })

  const userQuery = useQuery({
    enabled: isEdit,
    queryKey: ['administration', 'users', userId],
    queryFn: () => getAdminUserRequest(userId ?? ''),
  })

  if (rolesQuery.isPending || (isEdit && userQuery.isPending)) {
    return <LoadingState message="Cargando datos del usuario administrativo." variant="page" />
  }

  if (rolesQuery.isError || userQuery.isError) {
    return (
      <ErrorState
        description="No pudimos recuperar roles o datos del usuario."
        onRetry={() => {
          void rolesQuery.refetch()
          void userQuery.refetch()
        }}
        title="No fue posible cargar el formulario"
      />
    )
  }

  return (
    <AdminUserForm
      isEdit={isEdit}
      key={userQuery.data?.id ?? 'new-user'}
      roles={rolesQuery.data ?? []}
      user={userQuery.data}
      userId={userId}
    />
  )
}

function AdminUserForm({
  isEdit,
  roles,
  user,
  userId,
}: {
  isEdit: boolean
  roles: AdminRoleResponse[]
  user?: AdminUserResponse
  userId?: string
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const [form, setForm] = useState<FormState>(() => toInitialForm(user))
  const [formError, setFormError] = useState<string | null>(null)

  const roleOptions = roles.map((role) => ({ label: `${role.name} (${role.permissionCount})`, value: role.code }))

  const mutation = useMutation({
    mutationFn: () => (
      isEdit && userId
        ? updateAdminUserRequest(userId, toRequest(form, true))
        : createAdminUserRequest(toRequest(form, false))
    ),
    onError: (error) => {
      setFormError(error instanceof ApiClientError ? error.message : 'No fue posible guardar el usuario.')
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['administration', 'users'] })
      await queryClient.invalidateQueries({ queryKey: ['administration', 'summary'] })
      showToast({
        title: isEdit ? 'Usuario actualizado' : 'Usuario creado',
        description: 'Los cambios ya estan disponibles en administracion.',
        tone: 'success',
      })
      navigate('/admin/users')
    },
  })

  const submit = () => {
    setFormError(null)
    if (!form.firstName.trim() || !form.lastName.trim() || !form.email.trim()) {
      setFormError('Nombre, apellido y correo son obligatorios.')
      return
    }
    if (!isEdit && !form.temporaryPassword.trim()) {
      setFormError('La contrasena temporal es obligatoria y debe ser unica para este usuario.')
      return
    }
    mutation.mutate()
  }

  return (
    <section className="space-y-6">
      <PageHeader
        actions={
          <Link to="/admin/users">
            <Button variant="secondary">Volver</Button>
          </Link>
        }
        description="Define datos de acceso, rol operativo y estado del usuario."
        eyebrow="Administracion"
        title={isEdit ? 'Editar usuario' : 'Crear usuario'}
      />

      <Card>
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-lg font-semibold text-slate-950">Datos del usuario</p>
            <p className="mt-1 text-sm text-slate-600">Roles disponibles desde la matriz de permisos del backend.</p>
          </div>
          <StatusBadge label={form.status === 'ACTIVE' ? 'Activo' : form.status} tone={form.status === 'ACTIVE' ? 'success' : 'warning'} />
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <Input label="Nombre" onChange={(event) => setForm({ ...form, firstName: event.target.value })} value={form.firstName} />
          <Input label="Apellido" onChange={(event) => setForm({ ...form, lastName: event.target.value })} value={form.lastName} />
          <Input label="Correo" onChange={(event) => setForm({ ...form, email: event.target.value })} type="email" value={form.email} />
          <Input label="Telefono" onChange={(event) => setForm({ ...form, phone: event.target.value })} value={form.phone} />
          <Select label="Rol" onChange={(event) => setForm({ ...form, role: event.target.value })} options={roleOptions} value={form.role} />
          <Select label="Estado" onChange={(event) => setForm({ ...form, status: event.target.value })} options={statusOptions} value={form.status} />
          <Input label="Zona horaria" onChange={(event) => setForm({ ...form, timezone: event.target.value })} value={form.timezone} />
          {!isEdit ? (
            <Input
              hint="Usa una contrasena temporal unica; no se define una por defecto."
              label="Contrasena temporal"
              onChange={(event) => setForm({ ...form, temporaryPassword: event.target.value })}
              type="password"
              value={form.temporaryPassword}
            />
          ) : null}
        </div>

        {formError ? (
          <div className="mt-5 rounded-[18px] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {formError}
          </div>
        ) : null}

        <div className="mt-6 flex flex-wrap gap-3">
          <Button loading={mutation.isPending} onClick={submit}>
            {isEdit ? 'Guardar cambios' : 'Crear usuario'}
          </Button>
          <Link to="/admin/users">
            <Button variant="secondary">Cancelar</Button>
          </Link>
        </div>
      </Card>
    </section>
  )
}
