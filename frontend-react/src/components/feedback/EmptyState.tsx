import { Link } from 'react-router-dom'
import { buttonClassName } from '../ui/buttonStyles'

type EmptyStateVariant = 'page' | 'card' | 'table'

type EmptyStateAction = {
  label: string
  to: string
}

type EmptyStateProps = {
  title: string
  description: string
  primaryAction?: EmptyStateAction
  secondaryAction?: EmptyStateAction
  variant?: EmptyStateVariant
}

export function EmptyState({
  description,
  primaryAction,
  secondaryAction,
  title,
  variant = 'page',
}: EmptyStateProps) {
  const spacingClass = {
    page: 'p-8',
    card: 'p-6',
    table: 'p-6',
  }[variant]

  return (
    <section
      className={[
        'rounded-[24px] border border-dashed border-[var(--color-border)] bg-white text-center shadow-[var(--shadow-card)]',
        spacingClass,
      ]
        .join(' ')
        .trim()}
    >
      <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-[24px] bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
        <svg
          className="h-10 w-10"
          fill="none"
          viewBox="0 0 40 40"
          xmlns="http://www.w3.org/2000/svg"
        >
          <rect height="22" rx="6" stroke="currentColor" strokeWidth="2" width="22" x="9" y="9" />
          <path
            d="M14 24L18 19L22 22L27 16"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
          />
        </svg>
      </div>
      <p className="mt-5 text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
        Estado vacio
      </p>
      <h2 className="mt-3 text-[28px] font-semibold text-[var(--color-text)]">{title}</h2>
      <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-[var(--color-text-secondary)]">
        {description}
      </p>

      {primaryAction || secondaryAction ? (
        <div className="mt-5 flex flex-wrap justify-center gap-3">
          {primaryAction ? (
            <Link className={buttonClassName({ variant: 'primary' })} to={primaryAction.to}>
              {primaryAction.label}
            </Link>
          ) : null}
          {secondaryAction ? (
            <Link className={buttonClassName({ variant: 'secondary' })} to={secondaryAction.to}>
              {secondaryAction.label}
            </Link>
          ) : null}
        </div>
      ) : null}
    </section>
  )
}
