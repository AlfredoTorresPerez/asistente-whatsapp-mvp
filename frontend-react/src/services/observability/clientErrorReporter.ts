import { apiFetch } from '../api/httpClient'
import type { ClientErrorReport, StatusResponse } from '../api/types'

const logPrefix = '[Frontend - capa de interfaz]'

const truncationLimits = {
  message: 500,
  stack: 8000,
  url: 500,
  component: 120,
  errorType: 80,
} as const

class FrontendClientErrorReporter {
  private readonly enabled: boolean
  private readonly minReportIntervalMs: number
  private lastReportAtMs = 0

  constructor() {
    this.enabled = this.resolveEnabled()
    this.minReportIntervalMs = Number(
      import.meta.env.VITE_CLIENT_ERROR_REPORT_INTERVAL_MS ?? 2000,
    )
  }

  report(error: unknown, component?: string) {
    if (!this.enabled) {
      return
    }

    const now = Date.now()
    if (this.lastReportAtMs > 0 && now - this.lastReportAtMs < this.minReportIntervalMs) {
      return
    }
    this.lastReportAtMs = now

    const normalized = this.normalizeError(error)
    const payload: ClientErrorReport = {
      message: this.truncate(normalized.message, 'message'),
      stack: this.truncate(normalized.stack, 'stack'),
      url: this.truncate(
        typeof window !== 'undefined' ? window.location.href : 'no-browser',
        'url',
      ),
      component: this.truncate(component ?? 'unknown', 'component'),
      errorType: this.truncate(normalized.errorType, 'errorType'),
    }

    this.send(payload)
  }

  private async send(payload: ClientErrorReport) {
    try {
      await apiFetch<StatusResponse>('/observability/client-errors', {
        auth: false,
        body: JSON.stringify(payload),
        method: 'POST',
      })
      console.debug(logPrefix, 'Error de cliente reportado al backend', payload)
    } catch (error) {
      console.warn(logPrefix, 'No se pudo reportar el error de cliente', { payload, error })
    }
  }

  private normalizeError(error: unknown) {
    if (error instanceof Error) {
      return {
        errorType: error.name || 'Error',
        message: error.message || 'Error sin mensaje',
        stack: error.stack ?? '',
      }
    }

    if (typeof error === 'string') {
      return {
        errorType: 'UnhandledError',
        message: error || 'Promesa rechazada sin motivo',
        stack: '',
      }
    }

    return {
      errorType: 'UnhandledError',
      message: String(error ?? 'Promesa rechazada sin motivo'),
      stack: '',
    }
  }

  private truncate(value: string, field: keyof typeof truncationLimits) {
    const limit = truncationLimits[field]
    if (value.length <= limit) {
      return value
    }

    const suffix = '...[TRUNCADO]'
    return `${value.slice(0, limit - suffix.length)}${suffix}`
  }

  private resolveEnabled() {
    const configuredValue = import.meta.env.VITE_CLIENT_ERROR_REPORTING_ENABLED
    if (configuredValue === undefined) {
      return import.meta.env.DEV
    }

    return String(configuredValue).toLowerCase() === 'true'
  }
}

export const clientErrorReporter = new FrontendClientErrorReporter()
