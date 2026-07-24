import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import { AppProviders } from '../../../app/providers/AppProviders'
import { BeautyCenterLandingPage } from './BeautyCenterLandingPage'
import { IMAGES } from '../../../lib/landingImages'

function renderPage() {
  return render(
    <AppProviders>
      <MemoryRouter>
        <BeautyCenterLandingPage />
      </MemoryRouter>
    </AppProviders>,
  )
}

describe('BeautyCenterLandingPage', () => {
  beforeEach(() => {
    vi.stubGlobal('open', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('se abre sin autenticación', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: /realza tu belleza/i })).toBeInTheDocument()
  })

  it('muestra la imagen principal de la hero con atributos correctos', () => {
    renderPage()
    const img = screen.getByAltText(/centro estética bella, servicios de belleza/i)
    expect(img).toBeInTheDocument()
    expect(img).toHaveAttribute('src', IMAGES.HERO)
    expect(img).toHaveAttribute('loading', 'eager')
    expect(img).toHaveAttribute('fetchpriority', 'high')
  })

  it('muestra los 8 servicios', () => {
    renderPage()
    expect(screen.getByText('Limpiezas faciales')).toBeInTheDocument()
    expect(screen.getByText('Hidratación')).toBeInTheDocument()
    expect(screen.getByText('Depilación')).toBeInTheDocument()
    expect(screen.getByText('Tratamientos estéticos')).toBeInTheDocument()
    expect(screen.getByText('Alisado')).toBeInTheDocument()
    expect(screen.getByText('Asesoría estética')).toBeInTheDocument()
    expect(screen.getByText('Baby face')).toBeInTheDocument()
    expect(screen.getByText('Bushing')).toBeInTheDocument()
  })

  it('cada servicio tiene imagen con texto alternativo descriptivo', () => {
    renderPage()
    const serviceNames = ['Limpiezas faciales', 'Hidratación', 'Depilación', 'Tratamientos estéticos', 'Alisado', 'Asesoría estética', 'Baby face', 'Bushing']
    for (const name of serviceNames) {
      const img = screen.getByAltText(`${name} en Centro Estética Bella`)
      expect(img).toBeInTheDocument()
      expect(img).toHaveAttribute('loading', 'lazy')
    }
  })

  it('muestra los 5 beneficios', () => {
    renderPage()
    expect(screen.getByText('Profesionales expertas')).toBeInTheDocument()
    expect(screen.getByText('Atención personalizada')).toBeInTheDocument()
    expect(screen.getByText('Productos de calidad')).toBeInTheDocument()
    expect(screen.getByText('Ambiente cómodo y seguro')).toBeInTheDocument()
    expect(screen.getByText('Reserva rápida por WhatsApp')).toBeInTheDocument()
  })

  it('muestra la sección Sobre nosotros con imagen interior', () => {
    renderPage()
    expect(screen.getByText('Un espacio pensado para tu bienestar')).toBeInTheDocument()
    const interiorImg = screen.getByAltText(/interior de centro estética bella/i)
    expect(interiorImg).toBeInTheDocument()
    expect(interiorImg).toHaveAttribute('src', IMAGES.INTERIOR)
  })

  it('muestra la sección Promociones', () => {
    renderPage()
    expect(screen.getByText('Ofertas especiales')).toBeInTheDocument()
    expect(screen.getByText('$35.000')).toBeInTheDocument()
    expect(screen.getByText('$25.000')).toBeInTheDocument()
    const promoImg = screen.getByAltText(/promoción de limpieza facial/i)
    expect(promoImg).toBeInTheDocument()
    expect(promoImg).toHaveAttribute('src', IMAGES.PROMOCION)
  })

  it('muestra la sección Contacto con mapa', () => {
    renderPage()
    expect(screen.getByText('Visítanos')).toBeInTheDocument()
    expect(screen.getByText('Av. Los Pajaritos 1234, Local 5')).toBeInTheDocument()
    expect(screen.getByText('Maipú, Santiago')).toBeInTheDocument()
    const mapaImg = screen.getByAltText(/mapa de ubicación/i)
    expect(mapaImg).toBeInTheDocument()
    expect(mapaImg).toHaveAttribute('src', IMAGES.MAPA)
  })

  it('el botón principal abre WhatsApp con el número correcto', async () => {
    const user = userEvent.setup()
    renderPage()
    const primaryBtn = screen.getByRole('button', { name: /agendar por whatsapp/i })
    await user.click(primaryBtn)
    expect(globalThis.open).toHaveBeenCalledWith(
      expect.stringMatching(/^https:\/\/wa\.me\/56927305158\?text=/),
      '_blank',
      'noopener,noreferrer',
    )
  })

  it('el mensaje de WhatsApp incluye el nombre del centro', async () => {
    const user = userEvent.setup()
    renderPage()
    const primaryBtn = screen.getByRole('button', { name: /agendar por whatsapp/i })
    await user.click(primaryBtn)
    const callUrl = vi.mocked(globalThis.open).mock.calls[0][0] as string
    expect(decodeURIComponent(callUrl)).toContain('Centro Estética Bella')
  })

  it('los botones de servicio modifican el mensaje', async () => {
    const user = userEvent.setup()
    renderPage()
    const consultBtns = screen.getAllByText('Consultar por WhatsApp')
    await user.click(consultBtns[0])
    const callUrl = vi.mocked(globalThis.open).mock.calls[0][0] as string
    expect(decodeURIComponent(callUrl)).toContain('limpieza facial')
  })

  it('el botón de promoción abre WhatsApp con mensaje de promoción', async () => {
    const user = userEvent.setup()
    renderPage()
    const consultBtns = screen.getAllByText('Consultar por WhatsApp')
    await user.click(consultBtns[8])
    const callUrl = vi.mocked(globalThis.open).mock.calls[0][0] as string
    expect(decodeURIComponent(callUrl)).toContain('promoción de limpieza facial')
  })

  it('incluye navegación por teclado con skip link', () => {
    renderPage()
    const skipLink = screen.getByText('Saltar al contenido principal')
    expect(skipLink).toBeInTheDocument()
    expect(skipLink).toHaveAttribute('href', '#inicio')
  })

  it('contiene el texto del CTA final', () => {
    renderPage()
    expect(screen.getByText('Tu momento de bienestar comienza aquí')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /reservar mi hora/i })).toBeInTheDocument()
  })

  it('no tiene desplazamiento horizontal en la página', () => {
    const { container } = renderPage()
    const root = container.firstElementChild
    expect(root).not.toBeNull()
    if (root) {
      const style = window.getComputedStyle(root)
      expect(style.overflowX).not.toBe('scroll')
    }
  })
})
