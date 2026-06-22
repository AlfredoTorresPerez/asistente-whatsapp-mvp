import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState'
import { LoadingState } from '../../../components/feedback/LoadingState'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { PasswordField } from '../../../components/ui/PasswordField'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { PASSWORD_POLICY_HINT } from '../../../lib/passwordPolicy'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { useToast } from '../../../lib/toast'
import {
  resetPasswordRequest,
  validateResetPasswordTokenRequest,
} from '../../../services/api/authApi'
import { ApiClientError } from '../../../services/api/httpClient'
import { resetPasswordSchema } from '../schema'
import type { ResetPasswordFormValues } from '../schema'

export function ResetPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { showToast } = useToast()
  const isOnline = useOnlineStatus()
  const token = searchParams.get('token')
  const tokenValidationQuery = useQuery({
    queryKey: ['reset-password-token', token],
    queryFn: () => validateResetPasswordTokenRequest(token ?? ''),
    enabled: Boolean(token),
  })
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
  })
  const resetPasswordMutation = useMutation({
    mutationFn: (values: ResetPasswordFormValues) =>
      resetPasswordRequest(token ?? '', values.newPassword, values.confirmPassword),
    onSuccess: () => {
      showToast({
        title: 'Contrasena restablecida',
        description: 'Ya puedes iniciar sesion con tu nueva contrasena.',
        tone: 'success',
      })
      navigate('/login', { replace: true })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    try {
      await resetPasswordMutation.mutateAsync(values)
    } catch (error) {
      if (error instanceof ApiClientError) {
        Object.entries(error.fieldErrors).forEach(([fieldName, message]) => {
          setError(fieldName as keyof ResetPasswordFormValues, { message })
        })
        return
      }

      showToast({
        title: 'No pudimos actualizar la contrasena',
        description: 'Reintenta con un enlace vigente.',
        tone: 'error',
      })
    }
  })

  if (!token) {
    return (
      <div className="space-y-5">
        <Card className="border-red-200 bg-red-50">
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-red-600">
            Enlace invalido
          </p>
          <h2 className="mt-3 text-[28px] font-semibold text-[var(--color-text)]">
            Token faltante
          </h2>
          <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">
            Necesitas abrir esta pantalla desde un enlace de recuperacion valido.
          </p>
        </Card>
        <Link className={buttonClassName({ variant: 'link' })} to="/login">
          Volver al inicio
        </Link>
      </div>
    )
  }

  if (tokenValidationQuery.isLoading) {
    return <LoadingState message="Comprobando que el enlace de recuperacion siga vigente." />
  }

  if (tokenValidationQuery.isError || !tokenValidationQuery.data?.valid) {
    return (
      <ErrorState
        description="El enlace de restablecimiento es invalido o ya expiro."
        onRetry={() => navigate('/login')}
        retryLabel="Volver al inicio"
        title="Enlace no disponible"
      />
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
            Restablecer acceso
          </p>
          <h2 className="mt-3 text-[30px] leading-[1.1] font-semibold text-[var(--color-text)]">
            Nueva contrasena
          </h2>
          <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">
            El enlace fue verificado. Define una nueva contrasena para volver a tu panel de
            trabajo con seguridad.
          </p>
        </div>
        <StatusBadge label="Enlace verificado" tone="success" />
      </div>

      <Card tone="accent" className="bg-[linear-gradient(135deg,#EEF4FF_0%,#F7FAFF_100%)]">
        <div className="flex items-start gap-4">
          <span className="inline-flex h-12 w-12 items-center justify-center rounded-[18px] bg-white text-[var(--color-primary)] shadow-[0_10px_25px_rgba(36,83,255,0.12)]">
            <ShieldCheckIcon />
          </span>
          <div>
            <p className="text-sm font-semibold text-[var(--color-text)]">Politica minima</p>
            <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
              {PASSWORD_POLICY_HINT}
            </p>
          </div>
        </div>
      </Card>

      <form className="space-y-5" onSubmit={onSubmit}>
        <PasswordField
          autoComplete="new-password"
          error={errors.newPassword?.message}
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

        <div className="flex flex-col gap-3 sm:flex-row">
          <Button
            disabled={!isOnline || isSubmitting || resetPasswordMutation.isPending}
            loading={isSubmitting || resetPasswordMutation.isPending}
            type="submit"
          >
            {isSubmitting || resetPasswordMutation.isPending
              ? 'Guardando...'
              : 'Guardar nueva contrasena'}
          </Button>
          <Link className={buttonClassName({ variant: 'secondary' })} to="/login">
            Cancelar
          </Link>
        </div>
      </form>
    </div>
  )
}

function ShieldCheckIcon() {
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
        d="M9.5 12.25L11.3 14.05L14.8 10.55"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}
