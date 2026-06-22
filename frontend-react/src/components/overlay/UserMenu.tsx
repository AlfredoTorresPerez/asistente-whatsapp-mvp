import { Link } from 'react-router-dom'
import type { ShellUser } from '../../lib/shellSession'
import { AppLogo } from '../ui/AppLogo'
import { StatusBadge } from '../ui/StatusBadge'

type UserMenuProps = {
  open: boolean
  user: ShellUser
  onNavigate: () => void
  onLogout: () => void
}

export function UserMenu({ onLogout, onNavigate, open, user }: UserMenuProps) {
  if (!open) {
    return null
  }

  return (
    <div className="absolute right-4 top-[calc(100%+0.9rem)] z-30 w-full max-w-[320px]">
      <div className="rounded-[24px] border border-[var(--color-border)] bg-white p-5 shadow-[0_24px_70px_rgba(15,23,42,0.14)]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <AppLogo compact className="mb-4" />
            <p className="text-lg font-semibold text-[var(--color-text)]">{user.name}</p>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">{user.email}</p>
          </div>
          <StatusBadge label={user.role} tone="info" />
        </div>

        <p className="mt-4 rounded-2xl bg-[#F8FAFF] px-4 py-3 text-sm text-[var(--color-text-secondary)]">
          Gestiona tu perfil, cambia la contrasena y cierra sesion desde este menu.
        </p>

        <div className="mt-5 grid gap-2">
          <Link
            className="flex items-center justify-between rounded-[14px] px-3 py-3 text-sm font-medium text-[var(--color-text)] transition hover:bg-slate-50"
            onClick={onNavigate}
            to="/profile"
          >
            Ver perfil
            <span className="text-slate-400">/</span>
          </Link>
          <Link
            className="flex items-center justify-between rounded-[14px] px-3 py-3 text-sm font-medium text-[var(--color-text)] transition hover:bg-slate-50"
            onClick={onNavigate}
            to="/profile/change-password"
          >
            Cambiar contrasena
            <span className="text-slate-400">/</span>
          </Link>
          <Link
            className="flex items-center justify-between rounded-[14px] px-3 py-3 text-sm font-medium text-[var(--color-text)] transition hover:bg-slate-50"
            onClick={onNavigate}
            to="/notifications"
          >
            Notificaciones
            <span className="text-slate-400">/</span>
          </Link>
          <button
            className="mt-2 flex items-center justify-between rounded-[14px] px-3 py-3 text-sm font-semibold text-red-600 transition hover:bg-red-50"
            onClick={onLogout}
            type="button"
          >
            Cerrar sesion
            <span className="text-red-300">/</span>
          </button>
        </div>
      </div>
    </div>
  )
}
