type LoadingStateVariant = 'page' | 'card' | 'table' | 'detail'

type LoadingStateProps = {
  message?: string
  variant?: LoadingStateVariant
}

export function LoadingState({
  message = 'Preparando la estructura visual de esta vista.',
  variant = 'page',
}: LoadingStateProps) {
  const lines = {
    page: 4,
    card: 3,
    table: 5,
    detail: 4,
  }[variant]

  return (
    <section className="rounded-[24px] border border-[var(--color-border)] bg-white p-6 shadow-[var(--shadow-card)]">
      <div className="animate-pulse">
        <div className="h-3 w-28 rounded-full bg-slate-200" />
        <div className="mt-4 h-8 w-56 rounded-full bg-slate-200" />

        <div className="mt-6 space-y-3">
          {Array.from({ length: lines }).map((_, index) => (
            <div
              key={`skeleton-${index}`}
              className="h-4 rounded-full bg-slate-100"
              style={{ width: `${96 - index * 10}%` }}
            />
          ))}
        </div>
      </div>

      <div className="mt-6 flex items-center gap-2 text-sm text-[var(--color-text-secondary)]">
        <span className="inline-flex h-2.5 w-2.5 rounded-full bg-[var(--color-primary)]" />
        <p>{message}</p>
      </div>
    </section>
  )
}
