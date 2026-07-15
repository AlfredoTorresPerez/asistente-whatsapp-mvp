import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PageHeader } from '../../../components/ui/PageHeader'
import { Select } from '../../../components/ui/Select'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { useShellSession } from '../../../lib/shellSession'
import { useToast } from '../../../lib/toast'
import { getCurrentProfileRequest } from '../../../services/api/profileApi'
import { profileSchema } from '../schema'
import type { ProfileFormValues } from '../schema'

const BASE_TIMEZONE_OPTIONS = [
  { label: 'America/Santiago', value: 'America/Santiago' },
  { label: 'America/Bogota', value: 'America/Bogota' },
  { label: 'America/Buenos_Aires', value: 'America/Buenos_Aires' },
  { label: 'America/Mexico_City', value: 'America/Mexico_City' },
]

export function ProfilePage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const { updateProfile } = useShellSession()
  const {
    data: profile,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['profile'],
    queryFn: getCurrentProfileRequest,
  })
  const {
    formState: { errors, isSubmitting, isDirty },
    handleSubmit,
    register,
    reset,
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      phone: '',
      timezone: 'America/Santiago',
    },
  })

  useEffect(() => {
    if (!profile) {
      return
    }

    reset({
      firstName: profile.firstName,
      lastName: profile.lastName,
      phone: profile.phone ?? '',
      timezone: profile.timezone,
    })
  }, [profile, reset])

  const updateMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: () => {
      showToast({
        title: 'Perfil actualizado',
        description: 'Los cambios del perfil se guardaron correctamente.',
        tone: 'success',
      })
    },
  })

  const timezoneOptions = !profile?.timezone
    ? BASE_TIMEZONE_OPTIONS
    : BASE_TIMEZONE_OPTIONS.some((option) => option.value === profile.timezone)
      ? BASE_TIMEZONE_OPTIONS
      : [{ label: profile.timezone, value: profile.timezone }, ...BASE_TIMEZONE_OPTIONS]

  const onSubmit = handleSubmit(async (values) => {
    const updatedProfile = await updateMutation.mutateAsync(values)
    reset({
      firstName: updatedProfile.firstName,
      lastName: updatedProfile.lastName,
      phone: updatedProfile.phone ?? '',
      timezone: updatedProfile.timezone,
    })
  })

  if (isLoading) {
    return <LoadingState message="Cargando datos del usuario y zona horaria configurada." />
  }

  if (isError || !profile) {
    return (
      <ErrorState
        description="No pudimos cargar el perfil del usuario autenticado."
        onRetry={() => {
          void refetch()
        }}
        retryLabel="Reintentar"
        title="Perfil no disponible"
      />
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        actions={
          <>
            <Link
              className={buttonClassName({ variant: 'secondary' })}
              to="/profile/change-password"
            >
              Cambiar contrasena
            </Link>
            <Button
              disabled={!isDirty || isSubmitting || updateMutation.isPending}
              loading={isSubmitting || updateMutation.isPending}
              type="submit"
              form="profile-form"
            >
              {isSubmitting || updateMutation.isPending ? 'Guardando...' : 'Guardar cambios'}
            </Button>
          </>
        }
        eyebrow="Mi cuenta"
        description="Gestiona tus datos personales, el canal de contacto y la zona horaria operativa de tu negocio."
        title="Perfil de usuario"
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.15fr)_360px]">
        <form className="space-y-6" id="profile-form" onSubmit={onSubmit}>
          <Card>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-[var(--color-text)]">Datos personales</p>
                <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
                  Esta informacion aparece en el menu de usuario y se usa para personalizar la
                  experiencia.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <StatusBadge label={profile.role} tone="info" />
                <StatusBadge label="Activo" tone="success" />
              </div>
            </div>

            <div className="mt-6 grid gap-5 md:grid-cols-2">
              <Input error={errors.firstName?.message} label="Nombre" {...register('firstName')} />
              <Input error={errors.lastName?.message} label="Apellido" {...register('lastName')} />
              <Input disabled label="Correo" value={profile.email} className="text-slate-500" />
              <Input
                error={errors.phone?.message}
                hint="Usa formato internacional, por ejemplo +56911112222."
                label="Telefono"
                placeholder="+56911112222"
                {...register('phone')}
              />
              <div className="md:col-span-2">
                <Select
                  error={errors.timezone?.message}
                  hint="Usamos esta zona horaria para agenda, recordatorios y reportes."
                  label="Zona horaria"
                  options={timezoneOptions}
                  {...register('timezone')}
                />
              </div>
            </div>
          </Card>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button
              disabled={!isDirty || isSubmitting || updateMutation.isPending}
              loading={isSubmitting || updateMutation.isPending}
              type="submit"
            >
              {isSubmitting || updateMutation.isPending ? 'Guardando...' : 'Guardar cambios'}
            </Button>
            <button
              className={buttonClassName({ variant: 'secondary' })}
              onClick={() => navigate('/dashboard')}
              type="button"
            >
              Cancelar
            </button>
          </div>
        </form>

        <div className="space-y-6">
          <Card className="bg-[linear-gradient(135deg,#F8FAFF_0%,#EEF4FF_100%)]">
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
              Resumen
            </p>
            <h2 className="mt-3 text-2xl font-semibold text-[var(--color-text)]">
              {profile.firstName} {profile.lastName}
            </h2>
            <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
              {profile.businessName}
            </p>

            <div className="mt-6 grid gap-3">
              <InfoRow label="Rol del usuario" value={profile.role} />
              <InfoRow label="Correo de acceso" value={profile.email} />
              <InfoRow label="Zona horaria" value={profile.timezone} />
            </div>
          </Card>

          <Card className="bg-[#F9FBFF]">
            <div className="flex items-start gap-3">
              <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] bg-white text-[var(--color-primary)] shadow-[0_10px_24px_rgba(36,83,255,0.1)]">
                <ShieldIcon />
              </span>
              <div>
                <p className="text-sm font-semibold text-[var(--color-text)]">Seguridad basica</p>
                <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
                  Cambia tu contrasena cuando sea necesario y manten actualizado tu contacto para
                  recuperacion.
                </p>
                <Link
                  className="mt-4 inline-flex text-sm font-semibold text-[var(--color-primary)] transition hover:text-[var(--color-primary-strong)]"
                  to="/profile/change-password"
                >
                  Ir a cambiar contrasena
                </Link>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[18px] border border-[var(--color-border)] bg-white px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">{label}</p>
      <p className="mt-2 text-sm font-medium text-[var(--color-text)]">{value}</p>
    </div>
  )
}

function ShieldIcon() {
  return (
    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M12 3L19 6V11.5C19 16.1 15.8 20.37 12 21.5C8.2 20.37 5 16.1 5 11.5V6L12 3Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="M9.5 12.2L11.4 14.1L14.9 10.6"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}
