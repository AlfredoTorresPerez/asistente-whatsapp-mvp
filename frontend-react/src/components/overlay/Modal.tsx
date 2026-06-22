import type { PropsWithChildren } from 'react'

type ModalProps = PropsWithChildren<{
  open: boolean
  onClose?: () => void
  maxWidthClassName?: string
}>

export function Modal({
  children,
  maxWidthClassName = 'max-w-[460px]',
  onClose,
  open,
}: ModalProps) {
  if (!open) {
    return null
  }

  return (
    <div
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center bg-slate-950/45 px-4 backdrop-blur-[2px]"
      role="dialog"
    >
      {onClose ? (
        <button
          aria-label="Cerrar modal"
          className="absolute inset-0"
          onClick={onClose}
          type="button"
        />
      ) : null}
      <div
        className={[
          'relative z-10 w-full rounded-[24px] border border-[var(--color-border)] bg-white p-6 shadow-[var(--shadow-modal)] sm:p-7',
          maxWidthClassName,
        ]
          .join(' ')
          .trim()}
      >
        {children}
      </div>
    </div>
  )
}
