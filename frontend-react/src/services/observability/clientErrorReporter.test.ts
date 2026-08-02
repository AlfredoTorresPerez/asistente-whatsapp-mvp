import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clientErrorReporter } from './clientErrorReporter'

const fixedStartTime = new Date('2026-01-01T00:00:00.000Z')

function stubFetchSuccess(fetchMock: ReturnType<typeof vi.fn>) {
  fetchMock.mockResolvedValue(
    new Response(JSON.stringify({ status: 'ACCEPTED' }), {
      headers: { 'Content-Type': 'application/json' },
      status: 202,
    }),
  )
  vi.stubGlobal('fetch', fetchMock)
}

describe('clientErrorReporter', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(fixedStartTime)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('reporta un Error con todos los campos', async () => {
    const fetchMock = vi.fn()
    stubFetchSuccess(fetchMock)

    const error = new Error('Fallo de prueba')
    clientErrorReporter.report(error, 'MiComponente')
    await vi.runAllTimersAsync()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/observability/client-errors')
    expect(init.method).toBe('POST')
    const body = JSON.parse(String(init.body))
    expect(body.message).toBe('Fallo de prueba')
    expect(body.errorType).toBe('Error')
    expect(body.component).toBe('MiComponente')
    expect(body.url).toBe('http://localhost:3000/')
    expect(typeof body.stack).toBe('string')
  })

  it('reporta una promesa rechazada sin Error', async () => {
    const fetchMock = vi.fn()
    stubFetchSuccess(fetchMock)
    vi.setSystemTime(new Date(fixedStartTime.getTime() + 10_000))

    clientErrorReporter.report('Fallo de promesa')
    await vi.runAllTimersAsync()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    const body = JSON.parse(String(init.body))
    expect(body.message).toBe('Fallo de promesa')
    expect(body.errorType).toBe('UnhandledError')
  })

  it('trunca campos segun los limites del backend', async () => {
    const fetchMock = vi.fn()
    stubFetchSuccess(fetchMock)
    vi.setSystemTime(new Date(fixedStartTime.getTime() + 20_000))

    clientErrorReporter.report(new Error('x'.repeat(1000)), 'C'.repeat(300))
    await vi.runAllTimersAsync()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    const body = JSON.parse(String(init.body))
    expect(body.message.length).toBeLessThanOrEqual(500)
    expect(body.message).toContain('[TRUNCADO]')
    expect(body.component.length).toBeLessThanOrEqual(120)
    expect(body.component).toContain('[TRUNCADO]')
  })

  it('aplica intervalo minimo entre reportes', async () => {
    const fetchMock = vi.fn()
    stubFetchSuccess(fetchMock)
    vi.setSystemTime(new Date(fixedStartTime.getTime() + 30_000))

    clientErrorReporter.report(new Error('Primero'))
    await vi.runAllTimersAsync()
    clientErrorReporter.report(new Error('Segundo inmediato'))
    await vi.runAllTimersAsync()

    expect(fetchMock).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(2500)
    clientErrorReporter.report(new Error('Tercero despues del intervalo'))
    await vi.runAllTimersAsync()

    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
