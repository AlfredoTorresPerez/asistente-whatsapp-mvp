import dayjs from 'dayjs'
import { Link } from 'react-router-dom'
import type { ShellUser } from '../../lib/shellSession'
import { Select } from '../ui/Select'

type TopbarProps = {
  currentRoute: string
  currentDescription: string
  notificationCount: number
  user: ShellUser
  isUserMenuOpen: boolean
  onOpenSidebar: () => void
  onToggleUserMenu: () => void
}

function getInitials(userName: string) {
  return userName
    .split(' ')
    .map((token) => token[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

function formatDateRange() {
  const from = dayjs().subtract(6, 'day').format('DD/MM/YYYY')
  const to = dayjs().format('DD/MM/YYYY')
  return `${from} - ${to}`
}

export function Topbar({
  currentDescription,
  currentRoute,
  isUserMenuOpen,
  notificationCount,
  onOpenSidebar,
  onToggleUserMenu,
  user,
}: TopbarProps) {
  return (
    <header className="rounded-[24px] border border-[var(--color-border)] bg-white px-4 py-4 shadow-[0_18px_50px_rgba(15,23,42,0.07)] lg:px-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <button
            aria-label="Abrir navegacion lateral"
            className="inline-flex h-11 w-11 items-center justify-center rounded-[14px] border border-[var(--color-border)] bg-white text-[var(--color-text)] transition hover:bg-slate-50 lg:hidden"
            onClick={onOpenSidebar}
            type="button"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 7H20" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
              <path d="M4 12H20" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
              <path d="M4 17H14" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
            </svg>
          </button>

          <div className="hidden min-w-[190px] lg:block">
            <Select
              aria-label="Negocio actual"
              className="text-[13px] font-medium"
              name="topbar-business"
              onChange={() => undefined}
              options={[{ label: user.businessName, value: user.businessId }]}
              value={user.businessId}
            />
          </div>

          <div className="hidden min-w-0 xl:block">
            <p className="text-sm font-semibold text-[var(--color-text)]">{currentRoute}</p>
            <p className="truncate text-xs text-[var(--color-text-secondary)]">
              {currentDescription}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-end gap-3">
          <button
            className="inline-flex h-11 items-center gap-2 rounded-[14px] border border-[var(--color-border)] bg-white px-4 text-sm font-medium text-[var(--color-text)] transition hover:bg-slate-50"
            type="button"
          >
            <CalendarIcon />
            <span>{formatDateRange()}</span>
          </button>

          <Link
            aria-label={`Notificaciones (${notificationCount})`}
            className="relative inline-flex h-11 w-11 items-center justify-center rounded-[14px] border border-[var(--color-border)] bg-white text-[var(--color-text)] transition hover:bg-slate-50"
            to="/notifications"
          >
            <BellIcon />
            {notificationCount > 0 ? (
              <span className="absolute right-2 top-2 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white">
                {notificationCount}
              </span>
            ) : null}
          </Link>

          <button
            aria-expanded={isUserMenuOpen}
            aria-haspopup="menu"
            className="inline-flex items-center gap-3 rounded-[16px] border border-[var(--color-border)] bg-white px-3 py-2 text-left shadow-[0_10px_24px_rgba(15,23,42,0.04)] transition hover:bg-slate-50"
            onClick={onToggleUserMenu}
            type="button"
          >
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-sm font-semibold text-[var(--color-primary)]">
              {getInitials(user.name)}
            </span>
            <span className="hidden sm:block">
              <span className="block text-sm font-semibold text-[var(--color-text)]">{user.name}</span>
              <span className="block text-xs text-[var(--color-text-secondary)]">{user.role}</span>
            </span>
            <ChevronDownIcon />
          </button>
        </div>
      </div>
    </header>
  )
}

function CalendarIcon() {
  return (
    <svg className="h-4 w-4 text-slate-500" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <rect height="14" rx="3" stroke="currentColor" strokeWidth="1.8" width="14" x="5" y="6" />
      <path d="M8 4V8" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
      <path d="M16 4V8" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
    </svg>
  )
}

function BellIcon() {
  return (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path d="M15 18C14.57 19.17 13.4 20 12 20C10.6 20 9.43 19.17 9 18" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
      <path d="M6 9C6 5.69 8.69 3 12 3C15.31 3 18 5.69 18 9V12.5L20 15V16H4V15L6 12.5V9Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
    </svg>
  )
}

function ChevronDownIcon() {
  return (
    <svg className="hidden h-4 w-4 text-slate-400 sm:block" fill="none" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
      <path d="M5 7.5L10 12.5L15 7.5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
    </svg>
  )
}
