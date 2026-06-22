import type { PropsWithChildren } from 'react'
import { useEffect, useState } from 'react'
import { Toast } from '../components/overlay/Toast'
import { ToastContext } from './toast'

type ToastTone = 'success' | 'error' | 'warning'

type ToastItem = {
  id: string
  title: string
  description?: string
  tone: ToastTone
}

export function ToastProvider({ children }: PropsWithChildren) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  useEffect(() => {
    if (toasts.length === 0) {
      return undefined
    }

    const latestToast = toasts[toasts.length - 1]
    const timeoutId = window.setTimeout(() => {
      setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== latestToast.id))
    }, 4200)

    return () => window.clearTimeout(timeoutId)
  }, [toasts])

  const showToast = (toast: Omit<ToastItem, 'id'>) => {
    setToasts((currentToasts) => [
      ...currentToasts,
      {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        ...toast,
      },
    ])
  }

  const removeToast = (toastId: string) => {
    setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== toastId))
  }

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-full max-w-sm flex-col gap-3">
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            description={toast.description}
            id={toast.id}
            onClose={removeToast}
            title={toast.title}
            tone={toast.tone}
          />
        ))}
      </div>
    </ToastContext.Provider>
  )
}
