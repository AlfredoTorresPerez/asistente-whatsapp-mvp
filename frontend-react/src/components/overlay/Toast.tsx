type ToastTone = 'success' | 'error' | 'warning'

export type ToastProps = {
  id: string
  title: string
  description?: string
  tone?: ToastTone
  onClose: (id: string) => void
}

const toneStyles: Record<ToastTone, string> = {
  success: 'border-emerald-200 text-emerald-900',
  error: 'border-red-200 text-red-900',
  warning: 'border-amber-200 text-amber-900',
}

const toneIcons: Record<ToastTone, string> = {
  success: 'bg-emerald-100 text-emerald-700',
  error: 'bg-red-100 text-red-700',
  warning: 'bg-amber-100 text-amber-700',
}

export function Toast({
  description,
  id,
  onClose,
  title,
  tone = 'success',
}: ToastProps) {
  return (
    <div
      className={[
        'pointer-events-auto w-full rounded-[22px] border bg-white px-4 py-4 shadow-[0_20px_45px_rgba(15,23,42,0.12)]',
        toneStyles[tone],
      ]
        .join(' ')
        .trim()}
      role="status"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <span className={['inline-flex h-10 w-10 items-center justify-center rounded-2xl', toneIcons[tone]].join(' ')}>
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              {tone === 'success' ? (
                <path d="M5 12.5L9.5 17L19 7.5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
              ) : tone === 'error' ? (
                <>
                  <path d="M12 8V12" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
                  <path d="M12 16H12.01" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
                  <path d="M10.29 3.86L1.82 18A2 2 0 0 0 3.53 21H20.47A2 2 0 0 0 22.18 18L13.71 3.86A2 2 0 0 0 10.29 3.86Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
                </>
              ) : (
                <>
                  <path d="M12 8V12" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
                  <path d="M12 16H12.01" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
                  <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
                </>
              )}
            </svg>
          </span>
          <div>
            <p className="text-sm font-semibold">{title}</p>
            {description ? <p className="mt-1 text-sm leading-6 opacity-90">{description}</p> : null}
          </div>
        </div>

        <button
          aria-label="Cerrar notificacion"
          className="rounded-full px-2 py-1 text-xs font-semibold opacity-80 transition hover:bg-slate-100 hover:opacity-100"
          onClick={() => onClose(id)}
          type="button"
        >
          Cerrar
        </button>
      </div>
    </div>
  )
}
