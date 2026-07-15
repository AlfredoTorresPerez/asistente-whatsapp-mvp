type TraceLevel = 'debug' | 'error' | 'info' | 'warn'

type TraceData = Record<string, unknown> | unknown[] | string | number | boolean | null | undefined

export type TraceOperation = {
  componentName: string
  methodName: string
  purpose: string
  data?: TraceData
  correlationId?: string
}

export type TraceContext = TraceOperation & {
  correlationId: string
  startedAtMs: number
}

const sensitiveKeyFragments = [
  'authorization',
  'accessToken',
  'access_token',
  'apiKey',
  'api_key',
  'bearer',
  'cardNumber',
  'clave',
  'confirmPassword',
  'cookie',
  'currentPassword',
  'cvv',
  'documentNumber',
  'newPassword',
  'password',
  'refreshToken',
  'refresh_token',
  'secret',
  'token',
]

class FrontendTraceService {
  private readonly enabled: boolean
  private readonly maxPayloadLength: number

  constructor() {
    this.enabled = this.resolveEnabled()
    this.maxPayloadLength = Number(import.meta.env.VITE_TRACE_MAX_PAYLOAD_LENGTH ?? 1200)
  }

  createCorrelationId() {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return `REQ-${crypto.randomUUID()}`
    }

    return `REQ-${Date.now()}-${Math.random().toString(16).slice(2)}`
  }

  start(operation: TraceOperation): TraceContext {
    const context: TraceContext = {
      ...operation,
      correlationId: operation.correlationId ?? this.createCorrelationId(),
      startedAtMs: performance.now(),
    }

    this.write('info', 'Inicio de ejecucion', {
      componentName: context.componentName,
      methodName: context.methodName,
      purpose: context.purpose,
      correlationId: context.correlationId,
      data: context.data,
    })

    return context
  }

  end(context: TraceContext, data?: TraceData) {
    this.write('info', 'Fin de ejecucion', {
      componentName: context.componentName,
      methodName: context.methodName,
      purpose: context.purpose,
      correlationId: context.correlationId,
      executionTimeMs: Math.round(performance.now() - context.startedAtMs),
      result: 'SUCCESS',
      data,
    })
  }

  warn(context: TraceContext | TraceOperation, message: string, data?: TraceData) {
    this.write('warn', message, {
      componentName: context.componentName,
      methodName: context.methodName,
      purpose: context.purpose,
      correlationId: context.correlationId,
      data,
    })
  }

  error(context: TraceContext | TraceOperation, error: unknown, data?: TraceData) {
    this.write('error', 'Error de ejecucion', {
      componentName: context.componentName,
      methodName: context.methodName,
      purpose: context.purpose,
      correlationId: context.correlationId,
      executionTimeMs:
        'startedAtMs' in context ? Math.round(performance.now() - context.startedAtMs) : undefined,
      result: 'ERROR',
      error: this.normalizeError(error),
      data,
    })
  }

  info(operation: TraceOperation, message: string, data?: TraceData) {
    this.write('info', message, {
      componentName: operation.componentName,
      methodName: operation.methodName,
      purpose: operation.purpose,
      correlationId: operation.correlationId,
      data,
    })
  }

  debug(operation: TraceOperation, message: string, data?: TraceData) {
    this.write('debug', message, {
      componentName: operation.componentName,
      methodName: operation.methodName,
      purpose: operation.purpose,
      correlationId: operation.correlationId,
      data,
    })
  }

  traceSync<T>(operation: TraceOperation, handler: () => T): T {
    const context = this.start(operation)
    try {
      const result = handler()
      this.end(context)
      return result
    } catch (error) {
      this.error(context, error)
      throw error
    }
  }

  async traceAsync<T>(operation: TraceOperation, handler: () => Promise<T>): Promise<T> {
    const context = this.start(operation)
    try {
      const result = await handler()
      this.end(context)
      return result
    } catch (error) {
      this.error(context, error)
      throw error
    }
  }

  sanitize(data: TraceData): TraceData {
    return this.sanitizeValue(data, 0) as TraceData
  }

  private write(level: TraceLevel, message: string, rawPayload: Record<string, unknown>) {
    if (!this.enabled) {
      return
    }

    const payload = this.sanitizeValue(
      {
        layer: 'frontend',
        layerDescription: 'capa de interfaz',
        level: level.toUpperCase(),
        timestamp: new Date().toISOString(),
        ...rawPayload,
      },
      0,
    )

    const prefix = `[Frontend - capa de interfaz] ${message}`

    switch (level) {
      case 'debug':
        console.debug(prefix, payload)
        break
      case 'error':
        console.error(prefix, payload)
        break
      case 'warn':
        console.warn(prefix, payload)
        break
      default:
        console.info(prefix, payload)
        break
    }
  }

  private sanitizeValue(value: unknown, depth: number): unknown {
    if (value === null || value === undefined) {
      return value
    }

    if (depth > 3) {
      return this.truncate(String(value))
    }

    if (typeof value === 'string') {
      return this.truncate(value)
    }

    if (typeof value === 'number' || typeof value === 'boolean') {
      return value
    }

    if (value instanceof Error) {
      return this.normalizeError(value)
    }

    if (Array.isArray(value)) {
      return value.slice(0, 20).map((item) => this.sanitizeValue(item, depth + 1))
    }

    if (typeof value === 'object') {
      const safeObject: Record<string, unknown> = {}
      Object.entries(value as Record<string, unknown>)
        .slice(0, 40)
        .forEach(([key, item]) => {
          safeObject[key] = this.isSensitiveKey(key)
            ? '[REDACTADO]'
            : this.sanitizeValue(item, depth + 1)
        })
      return safeObject
    }

    return this.truncate(String(value))
  }

  private normalizeError(error: unknown) {
    if (error instanceof Error) {
      return {
        name: error.name,
        message: error.message,
      }
    }

    return {
      name: 'UnknownError',
      message: String(error),
    }
  }

  private isSensitiveKey(key: string) {
    const normalizedKey = key.replace(/[-_]/g, '').toLowerCase()
    return sensitiveKeyFragments.some((fragment) =>
      normalizedKey.includes(fragment.replace(/[-_]/g, '').toLowerCase()),
    )
  }

  private truncate(value: string) {
    if (value.length <= this.maxPayloadLength) {
      return value
    }

    return `${value.slice(0, this.maxPayloadLength)}...[TRUNCADO]`
  }

  private resolveEnabled() {
    const configuredValue = import.meta.env.VITE_FRONTEND_TRACE_ENABLED
    if (configuredValue === undefined) {
      return import.meta.env.DEV
    }

    return String(configuredValue).toLowerCase() === 'true'
  }
}

export const traceService = new FrontendTraceService()
