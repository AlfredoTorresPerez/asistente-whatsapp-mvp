import type { ReactNode, TextareaHTMLAttributes } from 'react'
import { forwardRef } from 'react'

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label?: string
  error?: string
  hint?: string
  leadingIcon?: ReactNode
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { className, error, hint, id, label, leadingIcon, rows = 5, ...props },
  ref,
) {
  const textareaId = id ?? props.name

  return (
    <label className="block">
      {label ? (
        <span className="mb-2.5 block text-sm font-medium text-[#23385F]">{label}</span>
      ) : null}

      <span
        className={[
          'relative flex rounded-[18px] border bg-white transition',
          error
            ? 'border-red-300 ring-4 ring-red-100/60'
            : 'border-[var(--color-border)] focus-within:border-[var(--color-primary)] focus-within:ring-4 focus-within:ring-[var(--color-primary)]/12',
        ]
          .join(' ')
          .trim()}
      >
        {leadingIcon ? (
          <span className="pl-3 pt-3 text-slate-400" aria-hidden="true">
            {leadingIcon}
          </span>
        ) : null}

        <textarea
          ref={ref}
          className={[
            'min-h-[132px] w-full resize-y rounded-[18px] bg-transparent px-4 py-3 text-sm text-[var(--color-text)] outline-none placeholder:text-slate-400',
            leadingIcon ? 'pl-3' : '',
            className ?? '',
          ]
            .join(' ')
            .trim()}
          id={textareaId}
          rows={rows}
          {...props}
        />
      </span>

      {error ? (
        <span className="mt-2 block text-sm text-red-700">{error}</span>
      ) : hint ? (
        <span className="mt-2 block text-xs leading-5 text-slate-500">{hint}</span>
      ) : null}
    </label>
  )
})
