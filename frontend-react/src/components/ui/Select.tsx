import type { ReactNode, SelectHTMLAttributes } from 'react'
import { forwardRef } from 'react'

type SelectOption = {
  label: string
  value: string
}

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label?: string
  error?: string
  hint?: string
  leadingIcon?: ReactNode
  options: SelectOption[]
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { className, error, hint, id, label, leadingIcon, options, ...props },
  ref,
) {
  const selectId = id ?? props.name

  return (
    <label className="block">
      {label ? (
        <span className="mb-2.5 block text-sm font-medium text-[#23385F]">{label}</span>
      ) : null}

      <span
        className={[
          'relative flex h-12 items-center rounded-[14px] border bg-white transition',
          error
            ? 'border-red-300 ring-4 ring-red-100/60'
            : 'border-[var(--color-border)] focus-within:border-[var(--color-primary)] focus-within:ring-4 focus-within:ring-[var(--color-primary)]/12',
        ]
          .join(' ')
          .trim()}
      >
        {leadingIcon ? (
          <span className="pl-3 text-slate-400" aria-hidden="true">
            {leadingIcon}
          </span>
        ) : null}

        <select
          ref={ref}
          className={[
            'h-full w-full appearance-none rounded-[14px] bg-transparent px-4 pr-10 text-sm text-[var(--color-text)] outline-none',
            leadingIcon ? 'pl-3' : '',
            className ?? '',
          ]
            .join(' ')
            .trim()}
          id={selectId}
          {...props}
        >
          {options.map((option) => (
            <option key={`${selectId}-${option.value}`} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" aria-hidden="true">
          <svg className="h-4 w-4" fill="none" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
            <path d="M5 7.5L10 12.5L15 7.5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
          </svg>
        </span>
      </span>

      {error ? (
        <span className="mt-2 block text-sm text-red-700">{error}</span>
      ) : hint ? (
        <span className="mt-2 block text-xs leading-5 text-slate-500">{hint}</span>
      ) : null}
    </label>
  )
})
