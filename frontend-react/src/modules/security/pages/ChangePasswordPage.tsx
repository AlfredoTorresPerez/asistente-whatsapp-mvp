import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PageHeader } from '../../../components/ui/PageHeader'
import { PasswordField } from '../../../components/ui/PasswordField'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { PASSWORD_POLICY_HINT } from '../../../lib/passwordPolicy'
import { useShellSession } from '../../../lib/shellSession'
import { useToast } from '../../../lib/toast'
import { ApiClientError } from '../../../services/api/httpClient'
import { changePasswordSchema } from '../schema'
import type { ChangePasswordFormValues } from '../schema'

export function ChangePasswordPage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const { changePassword } = useShellSession()
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
    setError,
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
  })

  const mutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      showToast({
        title: 'Contrasena actualizada',
        description: 'La nueva contrasena ya quedo activa para tu cuenta.',
        tone: 'success',
      })
      navigate('/profile', { replace: true })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync(values)
    } catch (error) {
      if (error instanceof ApiClientError) {
        Object.entries(error.fieldErrors).forEach(([fieldName, message]) => {
          setError(fieldName as keyof ChangePasswordFormValues, { message })
        })
        return
      }

      showToast({
        title: 'No pudimos cambiar la contrasena',
        description: 'Reintenta en unos minutos.',
        tone: 'error',
      })
    }
  })

  return (
    <div className="space-y-6">
      <PageHeader
        actions={
          <Link className={buttonClassName({ variant: 'secondary' })} to="/profile">
            Volver al perfil
          </Link>
        }
        eyebrow="Seguridad"
        description="Actualiza la credencial principal de acceso usando la politica minima definida para Fase 1."
        title="Cambiar contrasena"
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.1fr)_360px]">
        <form className="space-y-6" onSubmit={onSubmit}>
          <Card>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-[var(--color-text)]">Credenciales</p>
                <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
                  Para completar el cambio necesitamos tu contrasena actual y la nueva combinacion
                  segura.
                </p>
              </div>
              <StatusBadge label="Sesion protegida" tone="success" />
            </div>

            <div className="mt-6 space-y-5">
              <PasswordField
                autoComplete="current-password"
                error={errors.currentPassword?.message}
                label="Contrasena actual"
                placeholder="Ingresa tu contrasena actual"
                {...register('currentPassword')}
              />

              <PasswordField
                autoComplete="new-password"
                error={errors.newPassword?.message}
                hint={PASSWORD_POLICY_HINT}
                label="Nueva contrasena"
                placeholder="Crea una nueva contrasena"
                {...register('newPassword')}
              />

              <PasswordField
                autoComplete="new-password"
                error={errors.confirmPassword?.message}
                label="Confirmar contrasena"
                placeholder="Repite la nueva contrasena"
                {...register('confirmPassword')}
              />
            </div>
          </Card>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button loading={isSubmitting || mutation.isPending} type="submit">
              {isSubmitting || mutation.isPending ? 'Guardando...' : 'Guardar nueva contrasena'}
            </Button>
            <button
              className={buttonClassName({ variant: 'secondary' })}
              onClick={() => navigate('/profile')}
              type="button"
            >
              Cancelar
            </button>
          </div>
        </form>

        <div className="space-y-6">
          <Card className="bg-[linear-gradient(135deg,#F8FAFF_0%,#EEF4FF_100%)]">
            <div className="flex items-start gap-4">
              <span className="inline-flex h-12 w-12 items-center justify-center rounded-[18px] bg-white text-[var(--color-primary)] shadow-[0_10px_24px_rgba(36,83,255,0.12)]">
                <ShieldIcon />
              </span>
              <div>
                <p className="text-sm font-semibold text-[var(--color-text)]">Politica minima</p>
                <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
                  {PASSWORD_POLICY_HINT}
                </p>
              </div>
            </div>
          </Card>

          <Card className="bg-[#F9FBFF]">
            <p className="text-sm font-semibold text-[var(--color-text)]">Buenas practicas</p>
            <ul className="mt-4 space-y-3 text-sm leading-6 text-[var(--color-text-secondary)]">
              <li className="flex gap-3">
                <span className="mt-2 h-1.5 w-1.5 rounded-full bg-[var(--color-primary)]" />
                Evita reutilizar contrasenas de otros servicios.
              </li>
              <li className="flex gap-3">
                <span className="mt-2 h-1.5 w-1.5 rounded-full bg-emerald-500" />
                Confirma el cambio solo desde un equipo confiable.
              </li>
            </ul>
          </Card>
        </div>
      </div>
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
      <path d="M12 8V12.5" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
      <circle cx="12" cy="15.5" fill="currentColor" r="1" />
    </svg>
  )
}
