import type { ButtonHTMLAttributes, ReactNode } from 'react'
import type { ButtonStyleOptions } from './buttonStyles'
import { buttonClassName } from './buttonStyles'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> &
  ButtonStyleOptions & {
    leadingIcon?: ReactNode
    loading?: boolean
  }

export function Button({
  children,
  className,
  disabled,
  fullWidth,
  leadingIcon,
  loading = false,
  size,
  type = 'button',
  variant,
  ...props
}: ButtonProps) {
  return (
    <button
      className={[buttonClassName({ variant, size, fullWidth }), className ?? '']
        .join(' ')
        .trim()}
      disabled={disabled || loading}
      type={type}
      {...props}
    >
      {loading ? (
        <span aria-hidden="true" className="inline-flex">
          <svg className="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="9" stroke="currentColor" strokeOpacity="0.24" strokeWidth="2" />
            <path d="M21 12A9 9 0 0 0 12 3" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
          </svg>
        </span>
      ) : leadingIcon ? (
        <span aria-hidden="true">{leadingIcon}</span>
      ) : null}
      <span>{children}</span>
    </button>
  )
}
