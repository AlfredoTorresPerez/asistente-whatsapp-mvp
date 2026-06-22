type OfflineBannerProps = {
  visible: boolean
  message?: string
  onRetry?: () => void
}

export function OfflineBanner({
  message = 'Sin conexion. Las acciones sensibles quedan bloqueadas hasta recuperar la red.',
  onRetry,
  visible,
}: OfflineBannerProps) {
  if (!visible) {
    return null
  }

  return (
    <div
      className="border-b border-amber-200 bg-amber-50/95 px-4 py-3 text-sm text-amber-900 backdrop-blur"
      role="status"
    >
      <div className="mx-auto flex max-w-[1600px] items-center justify-between gap-4">
        <span className="inline-flex items-center gap-2">
          <span className="inline-flex h-2.5 w-2.5 rounded-full bg-amber-500" />
          {message}
        </span>
        {onRetry ? (
          <button
            className="rounded-full border border-amber-300 px-3 py-1 font-semibold transition hover:bg-amber-100"
            onClick={onRetry}
            type="button"
          >
            Reintentar ahora
          </button>
        ) : null}
      </div>
    </div>
  )
}
