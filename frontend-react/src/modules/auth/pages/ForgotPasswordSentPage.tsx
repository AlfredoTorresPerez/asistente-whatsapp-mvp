import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { StatusBadge } from '../../../components/ui/StatusBadge'
import { buttonClassName } from '../../../components/ui/buttonStyles'
import { useToast } from '../../../lib/toast'
import { forgotPasswordRequest } from '../../../services/api/authApi'
import { ApiClientError } from '../../../services/api/httpClient'

type LocationState = {
  email?: string
}

export function ForgotPasswordSentPage() {
  const location = useLocation()
  const { showToast } = useToast()
  const state = location.state as LocationState | null
  const [isResending, setIsResending] = useState(false)

  async function handleResend() {
    if (!state?.email) {
      return
    }

    try {
      setIsResending(true)
      await forgotPasswordRequest(state.email)
      showToast({
        title: 'Solicitud registrada',
        description: 'Si el correo esta registrado, recibira instrucciones de recuperacion.',
        tone: 'success',
      })
    } catch (error) {
      showToast({
        title: 'No pudimos registrar la solicitud',
        description:
          error instanceof ApiClientError ? error.message : 'Reintenta en unos minutos.',
        tone: 'error',
      })
    } finally {
      setIsResending(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
            Confirmacion
          </p>
          <h2 className="mt-3 text-[30px] leading-[1.1] font-semibold text-[var(--color-text)]">
            Solicitud registrada
          </h2>
          <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">
            Si el correo esta registrado y el servicio de email esta habilitado, enviaremos
            instrucciones para recuperar el acceso.
          </p>
        </div>
        <StatusBadge label="Completado" tone="success" />
      </div>

      <Card className="overflow-hidden bg-[linear-gradient(135deg,#ECFDF3_0%,#F7FFF9_100%)] p-0">
        <div className="flex items-start gap-4 px-6 py-6">
          <span className="inline-flex h-14 w-14 items-center justify-center rounded-[20px] bg-emerald-500 text-white shadow-[0_12px_32px_rgba(16,185,129,0.22)]">
            <CheckMailIcon />
          </span>
          <div>
            <p className="text-sm font-semibold text-[var(--color-text)]">
              Revisa tu correo si la cuenta existe
            </p>
            <p className="mt-2 text-sm leading-6 text-[var(--color-text-secondary)]">
              Revisa bandeja de entrada y spam. En modo local el backend puede simular el
              envio y dejar trazas seguras en logs.
            </p>
          </div>
        </div>
      </Card>

      <Card className="bg-[#F9FBFF]">
        <p className="text-sm font-semibold text-[var(--color-text)]">Siguiente paso</p>
        <ul className="mt-3 space-y-2 text-sm leading-6 text-[var(--color-text-secondary)]">
          <li className="flex gap-3">
            <span className="mt-2 h-1.5 w-1.5 rounded-full bg-[var(--color-primary)]" />
            Abre el enlace recibido desde un dispositivo confiable.
          </li>
          <li className="flex gap-3">
            <span className="mt-2 h-1.5 w-1.5 rounded-full bg-emerald-500" />
            Define una nueva contrasena siguiendo la politica minima.
          </li>
        </ul>
      </Card>

      <div className="flex flex-col gap-3 sm:flex-row">
        <Link className={buttonClassName({ variant: 'primary' })} to="/login">
          Volver al inicio
        </Link>
        <Button
          disabled={!state?.email || isResending}
          loading={isResending}
          onClick={() => {
            void handleResend()
          }}
          variant="secondary"
        >
          {isResending ? 'Reenviando...' : 'Reenviar enlace'}
        </Button>
      </div>
    </div>
  )
}

function CheckMailIcon() {
  return (
    <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
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
      <path
        d="M9 15L11 17L15.5 12.5"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}
