import type { InputHTMLAttributes } from 'react'
import { forwardRef, useState } from 'react'
import { Input } from './Input'

type PasswordFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  error?: string
  hint?: string
}

export const PasswordField = forwardRef<HTMLInputElement, PasswordFieldProps>(function PasswordField(
  { error, hint, label, placeholder = 'Ingresa tu contrasena', ...props },
  ref,
) {
  const [visible, setVisible] = useState(false)

  return (
    <Input
      {...props}
      ref={ref}
      error={error}
      hint={hint}
      label={label}
      placeholder={placeholder}
      trailing={
        <button
          aria-label={visible ? 'Ocultar contrasena' : 'Mostrar contrasena'}
          className="inline-flex h-8 w-8 items-center justify-center rounded-full text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          onClick={() => setVisible((currentValue) => !currentValue)}
          type="button"
        >
          {visible ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      }
      type={visible ? 'text' : 'password'}
    />
  )
})

function EyeIcon() {
  return (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M2.5 12C4.6 7.83 8 5.5 12 5.5C16 5.5 19.4 7.83 21.5 12C19.4 16.17 16 18.5 12 18.5C8 18.5 4.6 16.17 2.5 12Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M3 3L21 21"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="M10.73 5.67C11.15 5.56 11.57 5.5 12 5.5C16 5.5 19.4 7.83 21.5 12C20.68 13.63 19.65 15 18.45 16.08"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="M14.12 14.12C13.58 14.66 12.82 15 12 15C10.34 15 9 13.66 9 12C9 11.18 9.34 10.42 9.88 9.88"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <path
        d="M6.11 6.11C4.67 7.16 3.45 8.45 2.5 12C4.6 16.17 8 18.5 12 18.5C13.71 18.5 15.3 18.07 16.71 17.31"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}
