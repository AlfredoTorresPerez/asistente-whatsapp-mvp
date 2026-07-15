import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { PasswordField } from '../../../components/ui/PasswordField'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { useShellSession } from '../../../lib/shellSession'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { ApiClientError } from '../../../services/api/httpClient'
import { loginSchema } from '../schema'
import type { LoginFormValues } from '../schema'

type LocationState = {
  from?: string
}

export function LoginPage() {
  const { signIn } = useShellSession()
  const location = useLocation()
  const navigate = useNavigate()
  const isOnline = useOnlineStatus()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const state = location.state as LocationState | null
  const showDemoCredentials = import.meta.env.VITE_SHOW_DEMO_CREDENTIALS === 'true'
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    if (!isOnline) {
      setSubmitError('No hay conexion. Reintenta cuando vuelva la red.')
      return
    }

    setSubmitError(null)
    try {
      await signIn(values)
      navigate(state?.from ?? '/dashboard', { replace: true })
    } catch (error) {
      if (error instanceof ApiClientError) {
        setSubmitError(error.message)
        return
      }

      setSubmitError('No pudimos iniciar sesion. Reintenta en unos minutos.')
    }
  })

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
            Acceso seguro
          </p>
          <h2 className="mt-3 text-[30px] leading-[1.1] font-semibold text-[var(--color-text)]">
            Iniciar sesion
          </h2>
          <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">
            Ingresa con tu cuenta para gestionar conversaciones, prospectos y pedidos desde un solo
            lugar.
          </p>
        </div>
        <StatusBadge label="Demo" tone="info" />
      </div>

      {showDemoCredentials ? (
        <Card className="overflow-hidden bg-[linear-gradient(135deg,#F8FAFF_0%,#EEF4FF_100%)] p-0">
          <div className="flex items-start gap-4 px-5 py-5">
            <span className="inline-flex h-12 w-12 items-center justify-center rounded-[18px] bg-[var(--color-primary)] text-white shadow-[0_14px_30px_rgba(36,83,255,0.18)]">
              <LockShieldIcon />
            </span>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-[var(--color-text)]">Modo demo local</p>
              <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
                Las credenciales de prueba deben estar documentadas solo para el entorno local
                controlado.
              </p>
            </div>
          </div>
        </Card>
      ) : null}

      <form className="space-y-5" onSubmit={onSubmit}>
        <Input
          autoComplete="email"
          error={errors.email?.message}
          label="Correo"
          leadingIcon={<MailIcon />}
          placeholder="nombre@empresa.cl"
          type="email"
          {...register('email')}
        />

        <PasswordField
          autoComplete="current-password"
          error={errors.password?.message}
          label="Contrasena"
          placeholder="Ingresa tu contrasena"
          {...register('password')}
        />

        {submitError ? (
          <div className="rounded-[18px] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {submitError}
          </div>
        ) : null}

        <Button disabled={!isOnline || isSubmitting} fullWidth loading={isSubmitting} type="submit">
          {isSubmitting ? 'Ingresando...' : 'Ingresar'}
        </Button>
      </form>

      <div className="flex flex-col gap-3 rounded-[20px] border border-[var(--color-border)] bg-[#F9FBFF] px-4 py-4 text-sm text-[var(--color-text-secondary)] sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="font-medium text-[var(--color-text)]">Sesion protegida</p>
          <p className="mt-1 text-xs leading-5">
            El token se guarda en la sesion del navegador y protege el acceso privado.
          </p>
        </div>
        <Link className={buttonClassName({ variant: 'link' })} to="/forgot-password">
          Recuperar contrasena
        </Link>
      </div>
    </div>
  )
}

function MailIcon() {
  return (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M4 7.5C4 6.12 5.12 5 6.5 5H17.5C18.88 5 20 6.12 20 7.5V16.5C20 17.88 18.88 19 17.5 19H6.5C5.12 19 4 17.88 4 16.5V7.5Z"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      <path
        d="M5 8L12 13L19 8"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function LockShieldIcon() {
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
        d="M9.5 11.5V10.75C9.5 9.23 10.73 8 12.25 8C13.77 8 15 9.23 15 10.75V11.5"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="1.8"
      />
      <rect
        height="4.5"
        rx="1.4"
        stroke="currentColor"
        strokeWidth="1.8"
        width="6"
        x="9"
        y="11.5"
      />
    </svg>
  )
}
