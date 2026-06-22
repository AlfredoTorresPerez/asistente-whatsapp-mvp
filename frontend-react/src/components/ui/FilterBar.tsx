import type { PropsWithChildren, ReactNode } from 'react'

type FilterBarProps = PropsWithChildren<{
  actions?: ReactNode
}>

export function FilterBar({ actions, children }: FilterBarProps) {
  return (
    <section className="rounded-[1.75rem] border border-[var(--color-border)] bg-white p-5 shadow-[var(--shadow-card)]">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div className="grid flex-1 gap-3 md:grid-cols-2 xl:grid-cols-4">{children}</div>
        {actions ? <div className="flex flex-wrap gap-3">{actions}</div> : null}
      </div>
    </section>
  )
}
