import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { useOnlineStatus } from '../../../lib/useOnlineStatus'
import { forgotPasswordRequest } from '../../../services/api/authApi'
import { ApiClientError } from '../../../services/api/httpClient'
import { forgotPasswordSchema } from '../schema'
import type { ForgotPasswordFormValues } from '../schema'

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const isOnline = useOnlineStatus()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: 'admin@demo.cl',
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    try {
      setSubmitError(null)
      await forgotPasswordRequest(values.email)
      navigate('/forgot-password/sent', {
        state: { email: values.email },
      })
    } catch (error) {
      if (error instanceof ApiClientError) {
        setSubmitError(error.message)
        return
      }

      setSubmitError('No pudimos procesar la solicitud. Reintenta nuevamente.')
    }
  })

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
            Recuperacion
          </p>
          <h2 className="mt-3 text-[30px] leading-[1.1] font-semibold text-[var(--color-text)]">
            Recuperar contrasena
          </h2>
          <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">
            Si el correo esta registrado, enviaremos instrucciones para que el usuario pueda
            volver a entrar sin perder acceso al negocio.
          </p>
        </div>
        <StatusBadge label="Seguro" tone="success" />
      </div>

      <Card tone="accent" className="bg-[linear-gradient(135deg,#EEF4FF_0%,#F8FAFF_100%)]">
        <div className="flex items-start gap-4">
          <span className="inline-flex h-12 w-12 items-center justify-center rounded-[18px] bg-white text-[var(--color-primary)] shadow-[0_10px_25px_rgba(36,83,255,0.12)]">
            <MailKeyIcon />
          </span>
          <div>
            <p className="text-sm font-semibold text-[var(--color-text)]">Proceso guiado</p>
            <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
              El backend responde de forma generica para proteger la cuenta y registra el
              resultado sin exponer detalles sensibles.
            </p>
          </div>
        </div>
      </Card>

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

        {submitError ? (
          <div className="rounded-[18px] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {submitError}
          </div>
        ) : null}

        <Button disabled={!isOnline || isSubmitting} fullWidth loading={isSubmitting} type="submit">
          {isSubmitting ? 'Enviando...' : 'Enviar enlace'}
        </Button>
      </form>

      <div className="flex items-center justify-between rounded-[20px] border border-[var(--color-border)] bg-[#F9FBFF] px-4 py-4 text-sm text-[var(--color-text-secondary)]">
        <span>Si el correo existe, recibiras instrucciones temporales para restablecerla.</span>
        <Link className={buttonClassName({ variant: 'link' })} to="/login">
          Volver al inicio
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

function MailKeyIcon() {
  return (
    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M4 8C4 6.34 5.34 5 7 5H17C18.66 5 20 6.34 20 8V16C20 17.66 18.66 19 17 19H7C5.34 19 4 17.66 4 16V8Z"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      <path
        d="M5 8.5L12 13L19 8.5"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <circle cx="17.5" cy="6.5" r="2" stroke="currentColor" strokeWidth="1.8" />
      <path d="M17.5 8.5V11" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
      <path d="M17.5 11H19.5" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
    </svg>
  )
}
