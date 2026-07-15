import type { MouseEventHandler } from 'react'
import { Button } from '../ui/Button'

type ErrorStateVariant = 'page' | 'card' | 'inline'

type ErrorStateProps = {
  title: string
  description: string
  onRetry?: MouseEventHandler<HTMLButtonElement>
  retryLabel?: string
  variant?: ErrorStateVariant
}

export function ErrorState({
  description,
  onRetry,
  retryLabel = 'Reintentar',
  title,
  variant = 'page',
}: ErrorStateProps) {
  const spacingClass = {
    page: 'p-8',
    card: 'p-6',
    inline: 'p-5',
  }[variant]

  return (
    <section
      className={[
        'rounded-[24px] border border-red-200 bg-white text-red-950 shadow-[var(--shadow-card)]',
        spacingClass,
      ]
        .join(' ')
        .trim()}
      role="alert"
    >
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-red-500">
        <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 8V12" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
          <path d="M12 16H12.01" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
          <path
            d="M10.29 3.86L1.82 18A2 2 0 0 0 3.53 21H20.47A2 2 0 0 0 22.18 18L13.71 3.86A2 2 0 0 0 10.29 3.86Z"
            stroke="currentColor"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
          />
        </svg>
      </div>
      <p className="mt-4 text-xs font-semibold uppercase tracking-[0.28em] text-red-700">
        Estado de error
      </p>
      <h2 className="mt-3 text-2xl font-semibold text-[var(--color-text)]">{title}</h2>
      <p className="mt-3 max-w-xl text-sm leading-6 text-red-700">{description}</p>

      {onRetry ? (
        <div className="mt-5">
          <Button onClick={onRetry} size="sm" variant="danger">
            {retryLabel}
          </Button>
        </div>
      ) : null}
    </section>
  )
}
