import {
  readStoredShellSessionSnapshot,
  writeStoredShellSessionSnapshot,
} from '../../lib/shellSession'
import { traceService } from '../trace'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
const correlationHeaderName = 'X-Correlation-Id'

type ApiFetchOptions = RequestInit & {
  auth?: boolean
}

type ApiErrorPayload = {
  status?: number
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}

export class ApiClientError extends Error {
  code: string
  fieldErrors: Record<string, string>
  status: number

  constructor(payload: ApiErrorPayload) {
    super(payload.message ?? 'Ocurrio un error al procesar la solicitud.')
    this.name = 'ApiClientError'
    this.code = payload.code ?? 'HTTP_ERROR'
    this.fieldErrors = payload.fieldErrors ?? {}
    this.status = payload.status ?? 500
  }
}

export async function apiFetch<T>(path: string, init?: ApiFetchOptions) {
  const headers = new Headers(init?.headers)
  const requiresAuth = init?.auth ?? true
  const storedSession = requiresAuth ? readStoredShellSessionSnapshot() : null
  const method = init?.method ?? 'GET'
  const safePath = sanitizePath(path)
  const callerName = resolveCallerName()
  const correlationId = headers.get(correlationHeaderName) ?? traceService.createCorrelationId()
  const traceContext = traceService.start({
    componentName: callerName.includes('.') ? callerName.split('.')[0] : 'apiFetch',
    methodName: `${callerName} -> ${method} ${safePath}`,
    purpose:
      'Ejecutar funcion de frontend, enviar solicitud HTTP al backend y registrar trazabilidad de la operacion',
    correlationId,
    data: {
      path: safePath,
      method,
      requiresAuth,
      requestBody: safeRequestBody(init?.body),
    },
  })

  if (init?.body && !headers.has('Content-Type') && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  if (storedSession?.accessToken) {
    headers.set('Authorization', `Bearer ${storedSession.accessToken}`)
  }

  headers.set(correlationHeaderName, correlationId)

  try {
    const response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers,
    })

    const responseCorrelationId = response.headers.get(correlationHeaderName) ?? correlationId
    const contentType = response.headers.get('content-type') ?? ''
    const isJson = contentType.includes('application/json')
    const payload = isJson ? ((await response.json()) as ApiErrorPayload | T) : null

    if (!response.ok) {
      if (requiresAuth && response.status === 401) {
        writeStoredShellSessionSnapshot(null)
        window.dispatchEvent(new CustomEvent('shell-session-expired'))
      }

      const apiError = new ApiClientError({
        status: response.status,
        ...(payload as ApiErrorPayload | null),
      })

      if (response.status === 429) {
        console.warn('[Frontend - capa de interfaz] Demasiadas solicitudes', {
          method,
          path: safePath,
          status: 429,
          correlationId: responseCorrelationId,
        })
      } else {
        traceService.error(traceContext, apiError, {
          path: safePath,
          method,
          status: response.status,
          responseCorrelationId,
          errorCode: apiError.code,
          fieldErrors: apiError.fieldErrors,
        })
      }

      throw apiError
    }

    traceService.end(traceContext, {
      path: safePath,
      method,
      status: response.status,
      responseCorrelationId,
    })

    if (payload === null) {
      return undefined as T
    }

    return payload as T
  } catch (error) {
    if (!(error instanceof ApiClientError)) {
      traceService.error(traceContext, error, {
        path: safePath,
        method,
        result: 'NETWORK_OR_UNEXPECTED_ERROR',
      })
    }
    throw error
  }
}

function safeRequestBody(body: BodyInit | null | undefined) {
  if (!body) {
    return undefined
  }

  if (typeof body !== 'string') {
    return '[CUERPO_NO_SERIALIZABLE]'
  }

  try {
    return traceService.sanitize(JSON.parse(body))
  } catch {
    return traceService.sanitize(body)
  }
}

function sanitizePath(path: string) {
  const [basePath, queryString] = path.split('?')
  if (!queryString) {
    return basePath
  }

  const searchParams = new URLSearchParams(queryString)
  const sanitizedParams = new URLSearchParams()

  searchParams.forEach((value, key) => {
    const normalizedKey = key.replace(/[-_]/g, '').toLowerCase()
    const isSensitive = ['authorization', 'password', 'secret', 'token'].some((fragment) =>
      normalizedKey.includes(fragment),
    )
    sanitizedParams.set(key, isSensitive ? '[REDACTADO]' : value)
  })

  const sanitizedQueryString = sanitizedParams.toString()
  return sanitizedQueryString ? `${basePath}?${sanitizedQueryString}` : basePath
}

function resolveCallerName() {
  const stack = new Error().stack
  if (!stack) {
    return 'apiFetch'
  }

  const callerLine = stack
    .split('\n')
    .map((line) => line.trim())
    .find(
      (line) =>
        !line.includes('resolveCallerName') && !line.includes('apiFetch') && line.includes('at '),
    )

  const match = callerLine?.match(/at\s+([^\s(]+)/)
  return match?.[1] ?? 'apiFetch'
}
