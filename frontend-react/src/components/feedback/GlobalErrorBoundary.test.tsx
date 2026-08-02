import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clientErrorReporter } from '../../services/observability/clientErrorReporter'
import { GlobalErrorBoundary } from './GlobalErrorBoundary'

function Bomb(): never {
  throw new Error('Fallo del componente')
}

describe('GlobalErrorBoundary', () => {
  let reportSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    reportSpy = vi.spyOn(clientErrorReporter, 'report').mockImplementation(() => undefined)
    vi.spyOn(console, 'error').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('muestra la pantalla de error cuando un hijo lanza una excepcion', async () => {
    render(
      <GlobalErrorBoundary>
        <Bomb />
      </GlobalErrorBoundary>,
    )

    expect(await screen.findByText('Ocurrio un error inesperado')).toBeInTheDocument()
    expect(reportSpy).toHaveBeenCalledTimes(1)
    expect(reportSpy).toHaveBeenCalledWith(expect.any(Error), 'Bomb')
  })

  it('reporta errores globales de window', () => {
    render(
      <GlobalErrorBoundary>
        <div>Contenido sano</div>
      </GlobalErrorBoundary>,
    )

    window.dispatchEvent(new ErrorEvent('error', { error: new Error('Error de window'), message: 'Error de window' }))

    expect(reportSpy).toHaveBeenCalledTimes(1)
    expect(reportSpy).toHaveBeenCalledWith(expect.any(Error), 'window')
  })

  it('reporta rechazos de promesas no manejadas', () => {
    render(
      <GlobalErrorBoundary>
        <div>Contenido sano</div>
      </GlobalErrorBoundary>,
    )

    window.dispatchEvent(
      new PromiseRejectionEvent('unhandledrejection', {
        promise: Promise.reject(new Error('Promesa rota')).catch(() => undefined),
        reason: new Error('Promesa rota'),
      }),
    )

    expect(reportSpy).toHaveBeenCalledTimes(1)
    expect(reportSpy).toHaveBeenCalledWith(expect.any(Error), 'unhandled-rejection')
  })
})
