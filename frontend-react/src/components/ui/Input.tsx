import type { InputHTMLAttributes, ReactNode } from 'react'
import { forwardRef } from 'react'

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  label?: string
  error?: string
  hint?: string
  leadingIcon?: ReactNode
  trailing?: ReactNode
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  {
    className,
    error,
    hint,
    id,
    label,
    leadingIcon,
    trailing,
    type = 'text',
    ...props
  },
  ref,
) {
  const inputId = id ?? props.name

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

        <input
          ref={ref}
          className={[
            'h-full w-full rounded-[14px] bg-transparent px-4 text-sm text-[var(--color-text)] outline-none placeholder:text-slate-400',
            leadingIcon ? 'pl-3' : '',
            trailing ? 'pr-12' : '',
            className ?? '',
          ]
            .join(' ')
            .trim()}
          id={inputId}
          type={type}
          {...props}
        />

        {trailing ? (
          <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
            {trailing}
          </span>
        ) : null}
      </span>

      {error ? (
        <span className="mt-2 block text-sm text-red-700">{error}</span>
      ) : hint ? (
        <span className="mt-2 block text-xs leading-5 text-slate-500">{hint}</span>
      ) : null}
    </label>
  )
})
