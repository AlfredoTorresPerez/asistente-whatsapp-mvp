import { Button } from '../ui/Button'
import { Modal } from './Modal'

type ConfirmDialogTone = 'neutral' | 'danger'

type ConfirmDialogProps = {
  open: boolean
  title: string
  description: string
  confirmLabel: string
  cancelLabel?: string
  tone?: ConfirmDialogTone
  confirmLoading?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  cancelLabel = 'Cancelar',
  confirmLabel,
  confirmLoading = false,
  description,
  onCancel,
  onConfirm,
  open,
  title,
  tone = 'neutral',
}: ConfirmDialogProps) {
  return (
    <Modal maxWidthClassName="max-w-[420px]" onClose={onCancel} open={open}>
      <div className="text-center">
        <span
          className={[
            'mx-auto inline-flex h-16 w-16 items-center justify-center rounded-[22px]',
            tone === 'danger' ? 'bg-red-50 text-red-500' : 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]',
          ]
            .join(' ')
            .trim()}
        >
          <svg className="h-7 w-7" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M8 12H16" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
            <path d="M12 8V16" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
            <path d="M19 6L12 3L5 6V12C5 16.6 8.2 20.86 12 22C15.8 20.86 19 16.6 19 12V6Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
          </svg>
        </span>
        <p className="mt-5 text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
          Confirmacion
        </p>
        <h2 className="mt-3 text-[28px] font-semibold text-[var(--color-text)]">{title}</h2>
        <p className="mt-3 text-sm leading-6 text-[var(--color-text-secondary)]">{description}</p>

        <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Button disabled={confirmLoading} onClick={onCancel} variant="secondary">
            {cancelLabel}
          </Button>
          <Button
            loading={confirmLoading}
            onClick={onConfirm}
            variant={tone === 'danger' ? 'danger' : 'primary'}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
