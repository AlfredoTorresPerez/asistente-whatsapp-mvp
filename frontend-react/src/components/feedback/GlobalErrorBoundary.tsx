import { Component, type ErrorInfo, type ReactNode } from 'react'
import { ErrorState } from './ErrorState'
import { clientErrorReporter } from '../../services/observability/clientErrorReporter'

type GlobalErrorBoundaryProps = {
  children: ReactNode
}

type GlobalErrorBoundaryState = {
  hasError: boolean
}

const windowErrorEventName = 'error'
const windowUnhandledRejectionEventName = 'unhandledrejection'

export class GlobalErrorBoundary extends Component<
  GlobalErrorBoundaryProps,
  GlobalErrorBoundaryState
> {
  state: GlobalErrorBoundaryState = { hasError: false }

  static getDerivedStateFromError(): GlobalErrorBoundaryState {
    return { hasError: true }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    clientErrorReporter.report(error, this.resolveComponentName(errorInfo.componentStack ?? undefined))
  }

  componentDidMount() {
    window.addEventListener(windowErrorEventName, this.handleWindowError)
    window.addEventListener(windowUnhandledRejectionEventName, this.handleUnhandledRejection)
  }

  componentWillUnmount() {
    window.removeEventListener(windowErrorEventName, this.handleWindowError)
    window.removeEventListener(windowUnhandledRejectionEventName, this.handleUnhandledRejection)
  }

  private handleWindowError = (event: ErrorEvent) => {
    if (!event.message && !event.error) {
      return
    }

    clientErrorReporter.report(event.error ?? event.message, 'window')
  }

  private handleUnhandledRejection = (event: PromiseRejectionEvent) => {
    clientErrorReporter.report(event.reason, 'unhandled-rejection')
  }

  private resolveComponentName(componentStack?: string) {
    const match = componentStack?.match(/at\s+([A-Z][A-Za-z0-9_.]*)/)
    return match?.[1] ?? 'GlobalErrorBoundary'
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="flex min-h-screen items-center justify-center bg-[var(--color-background)] p-6">
          <ErrorState
            description="La aplicacion se detuvo por un problema inesperado. El error fue registrado para su revision. Puedes reintentar recargando la pagina."
            onRetry={() => window.location.reload()}
            retryLabel="Recargar pagina"
            title="Ocurrio un error inesperado"
          />
        </main>
      )
    }

    return this.props.children
  }
}
