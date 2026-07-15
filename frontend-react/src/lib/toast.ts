import { createContext, useContext } from 'react'

type ToastTone = 'success' | 'error' | 'warning'

export type ToastPayload = {
  title: string
  description?: string
  tone: ToastTone
}

type ToastContextValue = {
  showToast: (toast: ToastPayload) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast() {
  const context = useContext(ToastContext)

  if (!context) {
    throw new Error('useToast debe usarse dentro de ToastProvider')
  }

  return context
}
