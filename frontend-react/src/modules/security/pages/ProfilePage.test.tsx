import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AppProviders } from '../../../app/providers/AppProviders'
import { SHELL_SESSION_STORAGE_KEY } from '../../../lib/shellSession'
import { ProfilePage } from './ProfilePage'

const mockUser = {
  id: '40000000-0000-0000-0000-000000000001',
  name: 'Carla Mendez',
  firstName: 'Carla',
  lastName: 'Mendez',
  email: 'admin@demo.cl',
  role: 'OWNER',
  businessId: '11111111-1111-1111-1111-111111111111',
  businessName: 'Centro Estetico Bella',
  timezone: 'America/Santiago',
  phone: '+56955550101',
  permissions: ['ALL'],
}

function renderPage() {
  window.sessionStorage.setItem(
    SHELL_SESSION_STORAGE_KEY,
    JSON.stringify({
      accessToken: 'test-jwt',
      expiresAt: new Date(Date.now() + 900_000).toISOString(),
      user: mockUser,
    }),
  )

  return render(
    <AppProviders>
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    </AppProviders>,
  )
}

describe('ProfilePage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('renders loading state initially', () => {
    renderPage()
    expect(screen.getByText(/Cargando datos del usuario/)).toBeInTheDocument()
  })

  it('renders profile data after loading', async () => {
    renderPage()
    expect(await screen.findByDisplayValue('Carla')).toBeInTheDocument()
    expect(await screen.findByDisplayValue('Mendez')).toBeInTheDocument()
    expect(await screen.findByText(/admin@demo.cl/)).toBeInTheDocument()
    expect(await screen.findByText(/Centro Estetico Bella/)).toBeInTheDocument()
  })

  it('shows error state when API fails', async () => {
    const { server } = await import('../../../test/mocks/server')
    const { http, HttpResponse } = await import('msw')
    server.use(
      http.get('*/api/v1/users/me', () => new HttpResponse(null, { status: 500 })),
    )

    renderPage()
    expect(await screen.findByText(/Perfil no disponible/, {}, { timeout: 3000 })).toBeInTheDocument()
  })

  it('submits form and shows success toast', async () => {
    const user = userEvent.setup()
    renderPage()

    const firstNameInput = await screen.findByDisplayValue('Carla')
    await user.clear(firstNameInput)
    await user.type(firstNameInput, 'Carla Editada')

    const saveButtons = await screen.findAllByRole('button', { name: /Guardar cambios/ })
    await user.click(saveButtons[0])

    await waitFor(() => {
      expect(screen.getByText(/Perfil actualizado/)).toBeInTheDocument()
    })
  })

  it('disables save button when form is not dirty', async () => {
    renderPage()
    const saveButtons = await screen.findAllByRole('button', { name: /Guardar cambios/ })
    for (const btn of saveButtons) {
      expect(btn).toBeDisabled()
    }
  })

  it('cancels and navigates to dashboard', async () => {
    const user = userEvent.setup()
    renderPage()

    const cancelButton = await screen.findByRole('button', { name: /Cancelar/ })
    await user.click(cancelButton)
  })

  it('shows link to change password page', async () => {
    renderPage()
    expect(await screen.findByText(/Cambiar contrasena/)).toBeInTheDocument()
    expect(await screen.findByText(/Ir a cambiar contrasena/)).toBeInTheDocument()
  })
})