import { NavLink } from 'react-router-dom'
import { PRIMARY_NAV_ITEMS, canAccessNavigationItem } from '../../lib/navigation'
import { AppLogo } from '../ui/AppLogo'

type SidebarProps = {
  open: boolean
  businessName: string
  role: string
  onClose: () => void
  onLogout: () => void
}

export function Sidebar({ businessName, onClose, onLogout, open, role }: SidebarProps) {
  const visibleNavigationItems = PRIMARY_NAV_ITEMS.filter((item) =>
    canAccessNavigationItem(item, role),
  )

  return (
    <>
      {open ? (
        <button
          aria-label="Cerrar navegacion lateral"
          className="fixed inset-0 z-30 bg-slate-950/50 backdrop-blur-[2px] lg:hidden"
          onClick={onClose}
          type="button"
        />
      ) : null}

      <aside
        className={[
          'app-sidebar fixed inset-y-0 left-0 z-40 flex h-dvh max-h-dvh w-[292px] flex-col overflow-hidden rounded-r-[28px] bg-[radial-gradient(circle_at_top_left,rgba(36,83,255,0.18),transparent_35%),linear-gradient(180deg,#081A3A_0%,#0E2C63_100%)] px-4 py-4 text-white shadow-[0_26px_70px_rgba(15,23,42,0.22)] transition-transform duration-300 lg:static lg:h-full lg:max-h-full lg:min-h-0 lg:w-[188px] lg:rounded-[28px] lg:translate-x-0 lg:px-3 lg:py-3',
          open ? 'translate-x-0' : '-translate-x-full',
        ]
          .join(' ')
          .trim()}
      >
        <div className="flex items-start justify-between gap-3">
          <AppLogo compact inverted />
          <button
            aria-label="Cerrar menu"
            className="rounded-2xl border border-white/15 px-3 py-2 text-sm font-semibold text-blue-50 transition hover:bg-white/10 lg:hidden"
            onClick={onClose}
            type="button"
          >
            Cerrar
          </button>
        </div>

        <nav className="app-sidebar-nav mt-5 flex min-h-0 flex-1 flex-col gap-1 overflow-hidden lg:mt-4">
          {visibleNavigationItems.map((item) => (
            <NavLink
              key={item.path}
              end={item.path === '/admin'}
              onClick={onClose}
              to={item.path}
              className={({ isActive }) =>
                [
                  'group flex min-h-0 items-center gap-2 rounded-[14px] px-2.5 py-1.5 text-[13px] font-medium leading-tight transition lg:gap-1.5 lg:px-2 lg:text-[12px]',
                  isActive
                    ? 'bg-[#3D4BFF] text-white shadow-[inset_0_0_0_1px_rgba(255,255,255,0.08)]'
                    : 'text-white/82 hover:bg-white/8 hover:text-white',
                ].join(' ')
              }
            >
              <span className="inline-flex h-7 w-7 items-center justify-center rounded-xl bg-white/6 text-white/90 group-[.active]:bg-white/12">
                <NavIcon path={item.path} />
              </span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="app-sidebar-footer mt-3 shrink-0 space-y-2">
          <div className="app-sidebar-account rounded-[16px] border border-white/10 bg-white/8 p-2.5">
            <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-100/72">
              Cuenta actual
            </p>
            <div className="mt-2 rounded-[12px] border border-white/10 bg-white/8 px-2.5 py-2">
              <p className="text-sm font-semibold text-white">{businessName}</p>
              <p className="mt-1 text-xs text-emerald-300">Plan Profesional</p>
            </div>

            <div className="app-sidebar-usage mt-2 rounded-[12px] border border-white/10 bg-[#0B2148] px-2.5 py-2">
              <div className="flex items-center justify-between text-[11px] text-blue-100/72">
                <span>Conversaciones / mes</span>
                <span>25%</span>
              </div>
              <p className="mt-2 text-sm font-semibold text-white">1.250 / 5.000</p>
              <div className="mt-2 h-2 overflow-hidden rounded-full bg-white/10">
                <div className="h-full w-1/4 rounded-full bg-emerald-400" />
              </div>
            </div>
          </div>

          <a
            className="flex items-center gap-2 rounded-[14px] border border-white/10 bg-white/8 px-2.5 py-2 text-sm font-medium text-white/88 transition hover:bg-white/12"
            href="mailto:soporte@demo.cl"
          >
            <span className="inline-flex h-7 w-7 items-center justify-center rounded-xl bg-white/8">
              <SupportIcon />
            </span>
            Ayuda y soporte
          </a>

          <button
            className="flex w-full items-center gap-2 rounded-[14px] border border-white/10 bg-[#0B2148] px-2.5 py-2 text-sm font-medium text-white/88 transition hover:bg-white/10"
            onClick={onLogout}
            type="button"
          >
            <span className="inline-flex h-7 w-7 items-center justify-center rounded-xl bg-white/8">
              <LogoutIcon />
            </span>
            Cerrar sesion
          </button>
        </div>
      </aside>
    </>
  )
}

function NavIcon({ path }: { path: string }) {
  const iconProps = {
    className: 'h-4 w-4',
    fill: 'none',
    viewBox: '0 0 24 24',
    xmlns: 'http://www.w3.org/2000/svg',
  }

  switch (path) {
    case '/dashboard':
      return (
        <svg {...iconProps}>
          <path d="M5 12H11V19H5V12Z" stroke="currentColor" strokeWidth="1.8" />
          <path d="M13 5H19V10H13V5Z" stroke="currentColor" strokeWidth="1.8" />
          <path d="M13 12H19V19H13V12Z" stroke="currentColor" strokeWidth="1.8" />
          <path d="M5 5H11V10H5V5Z" stroke="currentColor" strokeWidth="1.8" />
        </svg>
      )
    case '/conversations':
      return (
        <svg {...iconProps}>
          <path
            d="M5 7.5C5 6.12 6.12 5 7.5 5H16.5C17.88 5 19 6.12 19 7.5V13.5C19 14.88 17.88 16 16.5 16H11L7 19V16H7.5C6.12 16 5 14.88 5 13.5V7.5Z"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
        </svg>
      )
    case '/automation-rules':
      return (
        <svg {...iconProps}>
          <path d="M12 5V19" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M7 8H17" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M7 16H17" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <circle cx="7" cy="8" fill="currentColor" r="1.5" />
          <circle cx="17" cy="16" fill="currentColor" r="1.5" />
        </svg>
      )
    case '/prospects':
      return (
        <svg {...iconProps}>
          <path
            d="M12 12C14.21 12 16 10.21 16 8C16 5.79 14.21 4 12 4C9.79 4 8 5.79 8 8C8 10.21 9.79 12 12 12Z"
            stroke="currentColor"
            strokeWidth="1.8"
          />
          <path
            d="M5 19C5 16.79 8.13 15 12 15C15.87 15 19 16.79 19 19"
            stroke="currentColor"
            strokeLinecap="round"
            strokeWidth="1.8"
          />
        </svg>
      )
    case '/agenda':
      return (
        <svg {...iconProps}>
          <rect height="14" rx="2" stroke="currentColor" strokeWidth="1.8" width="16" x="4" y="5" />
          <path d="M8 3V7" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M16 3V7" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M4 11H20" stroke="currentColor" strokeWidth="1.8" />
        </svg>
      )
    case '/appointments':
      return (
        <svg {...iconProps}>
          <rect height="14" rx="3" stroke="currentColor" strokeWidth="1.8" width="14" x="5" y="6" />
          <path d="M8 4V8" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M16 4V8" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
    case '/orders':
      return (
        <svg {...iconProps}>
          <path
            d="M7 7H19L17.5 13H9L7 5H5"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
          <circle cx="10" cy="18" fill="currentColor" r="1.2" />
          <circle cx="17" cy="18" fill="currentColor" r="1.2" />
        </svg>
      )
    case '/catalog':
      return (
        <svg {...iconProps}>
          <path
            d="M7 5H17V19L12 16.5L7 19V5Z"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
        </svg>
      )
    case '/reports':
      return (
        <svg {...iconProps}>
          <path d="M6 18V10" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M12 18V6" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M18 18V13" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
    case '/admin/locations':
      return (
        <svg {...iconProps}>
          <path
            d="M5 20V8.5L12 4L19 8.5V20"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
          <path
            d="M9 20V13H15V20"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.8"
          />
          <path d="M9 9.5H9.01" stroke="currentColor" strokeLinecap="round" strokeWidth="2.4" />
          <path d="M12 9.5H12.01" stroke="currentColor" strokeLinecap="round" strokeWidth="2.4" />
          <path d="M15 9.5H15.01" stroke="currentColor" strokeLinecap="round" strokeWidth="2.4" />
        </svg>
      )
    case '/configuration':
      return (
        <svg {...iconProps}>
          <path
            d="M12 15.5A3.5 3.5 0 1 0 12 8.5A3.5 3.5 0 0 0 12 15.5Z"
            stroke="currentColor"
            strokeWidth="1.8"
          />
          <path
            d="M19.4 15A1.7 1.7 0 0 0 19.7 16.9L19.8 17A2 2 0 0 1 17 19.8L16.9 19.7A1.7 1.7 0 0 0 15 19.4A1.7 1.7 0 0 0 14 21V21.2A2 2 0 0 1 10 21.2V21A1.7 1.7 0 0 0 9 19.4A1.7 1.7 0 0 0 7.1 19.7L7 19.8A2 2 0 0 1 4.2 17L4.3 16.9A1.7 1.7 0 0 0 4.6 15A1.7 1.7 0 0 0 3 14H2.8A2 2 0 0 1 2.8 10H3A1.7 1.7 0 0 0 4.6 9A1.7 1.7 0 0 0 4.3 7.1L4.2 7A2 2 0 0 1 7 4.2L7.1 4.3A1.7 1.7 0 0 0 9 4.6A1.7 1.7 0 0 0 10 3V2.8A2 2 0 0 1 14 2.8V3A1.7 1.7 0 0 0 15 4.6A1.7 1.7 0 0 0 16.9 4.3L17 4.2A2 2 0 0 1 19.8 7L19.7 7.1A1.7 1.7 0 0 0 19.4 9A1.7 1.7 0 0 0 21 10H21.2A2 2 0 0 1 21.2 14H21A1.7 1.7 0 0 0 19.4 15Z"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="1.6"
          />
        </svg>
      )
    default:
      return (
        <svg {...iconProps}>
          <path d="M12 4H20V20H4V4H12Z" stroke="currentColor" strokeWidth="1.8" />
          <path d="M9 10H15" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
          <path d="M9 14H15" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )
  }
}

function SupportIcon() {
  return (
    <svg
      className="h-4 w-4 text-white/88"
      fill="none"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M4 12A8 8 0 0 1 20 12V15A2 2 0 0 1 18 17H16V11H20"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="M4 11H8V17H6A2 2 0 0 1 4 15V11Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path d="M10 20H14" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
    </svg>
  )
}

function LogoutIcon() {
  return (
    <svg
      className="h-4 w-4 text-white/88"
      fill="none"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M14 7L19 12L14 17"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path d="M19 12H10" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
      <path
        d="M10 4H6C4.9 4 4 4.9 4 6V18C4 19.1 4.9 20 6 20H10"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}
