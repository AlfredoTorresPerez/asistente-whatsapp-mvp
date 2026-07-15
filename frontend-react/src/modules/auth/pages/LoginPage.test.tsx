import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import { AppProviders } from '../../../app/providers/AppProviders'
import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows validation messages for invalid credentials', async () => {
    const user = userEvent.setup()

    render(
      <AppProviders>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </AppProviders>,
    )

    const emailInput = screen.getByLabelText('Correo')
    const passwordInput = screen.getByLabelText('Contrasena')

    await user.clear(emailInput)
    await user.clear(passwordInput)
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Ingresa un correo valido.')).toBeInTheDocument()
    expect(screen.getByText('La contrasena debe tener al menos 8 caracteres.')).toBeInTheDocument()
  })
})
