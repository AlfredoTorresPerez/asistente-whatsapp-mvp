export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'link'
export type ButtonSize = 'sm' | 'md' | 'lg'

export type ButtonStyleOptions = {
  variant?: ButtonVariant
  size?: ButtonSize
  fullWidth?: boolean
}

export function buttonClassName({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
}: ButtonStyleOptions = {}) {
  const base =
    'inline-flex items-center justify-center gap-2 rounded-[14px] font-semibold transition duration-200 focus-visible:outline-none focus-visible:ring-4 disabled:cursor-not-allowed disabled:opacity-60'
  const sizeClass = {
    sm: 'h-10 px-3.5 text-sm',
    md: 'h-11 px-4 text-sm',
    lg: 'h-12 px-5 text-base',
  }[size]
  const variantClass = {
    primary:
      'bg-[var(--color-primary)] text-white shadow-[0_12px_30px_rgba(36,83,255,0.24)] hover:bg-[var(--color-primary-strong)] focus-visible:ring-[var(--color-primary)]/15',
    secondary:
      'border border-[var(--color-border)] bg-white text-[var(--color-primary)] shadow-[0_8px_20px_rgba(15,23,42,0.03)] hover:border-[#C9D5EA] hover:bg-[#F8FAFF] focus-visible:ring-[var(--color-primary)]/12',
    ghost:
      'border border-transparent bg-transparent text-[var(--color-text)] hover:bg-slate-100 focus-visible:ring-slate-200',
    danger:
      'bg-[var(--color-danger)] text-white shadow-[0_12px_30px_rgba(239,68,68,0.22)] hover:bg-red-600 focus-visible:ring-red-100',
    link: 'h-auto rounded-none px-0 text-[var(--color-primary)] hover:text-[var(--color-primary-strong)] focus-visible:ring-[var(--color-primary)]/12',
  }[variant]

  return [base, sizeClass, variantClass, fullWidth ? 'w-full' : ''].join(' ').trim()
}
