import { createContext, useContext } from 'react'
import type {
  AuthUserResponse,
  ChangePasswordRequest,
  UpdateProfileRequest,
  UserProfileResponse,
} from '../services/api/types'

export type ShellUser = {
  id: string
  name: string
  firstName: string
  lastName: string
  email: string
  role: string
  businessId: string
  businessName: string
  timezone: string
  phone: string | null
  permissions: string[]
}

export type StoredShellSession = {
  accessToken: string
  expiresAt: string
  user: ShellUser
}

export type ShellSessionStatus = 'loading' | 'authenticated' | 'unauthenticated'

export type ShellSessionContextValue = {
  status: ShellSessionStatus
  isAuthenticated: boolean
  user: ShellUser | null
  signIn: (credentials: { email: string; password: string }) => Promise<void>
  signOut: () => Promise<boolean>
  updateProfile: (request: UpdateProfileRequest) => Promise<UserProfileResponse>
  changePassword: (request: ChangePasswordRequest) => Promise<void>
  syncBusinessName: (businessName: string) => void
}

export const SHELL_SESSION_STORAGE_KEY = 'asistente-whatsapp.session'

export const ShellSessionContext = createContext<ShellSessionContextValue | null>(null)

export function toShellUserFromAuthResponse(user: AuthUserResponse): ShellUser {
  return {
    id: user.id,
    name: `${user.firstName} ${user.lastName}`,
    firstName: user.firstName,
    lastName: user.lastName,
    email: user.email,
    role: user.role,
    businessId: user.businessId,
    businessName: user.businessName,
    timezone: user.timezone,
    phone: null,
    permissions: user.permissions ?? [],
  }
}

export function mergeShellUserProfile(user: ShellUser, profile: UserProfileResponse): ShellUser {
  return {
    ...user,
    name: `${profile.firstName} ${profile.lastName}`,
    firstName: profile.firstName,
    lastName: profile.lastName,
    email: profile.email,
    phone: profile.phone,
    timezone: profile.timezone,
    role: profile.role,
    businessName: profile.businessName,
    permissions: user.permissions, // keep existing permissions
  }
}

export function readStoredShellSessionSnapshot() {
  try {
    const rawValue = window.sessionStorage.getItem(SHELL_SESSION_STORAGE_KEY)
    if (!rawValue) {
      return null
    }

    const parsedValue = JSON.parse(rawValue) as StoredShellSession
    if (!parsedValue.accessToken || !parsedValue.expiresAt || !parsedValue.user) {
      window.sessionStorage.removeItem(SHELL_SESSION_STORAGE_KEY)
      return null
    }

    if (Date.parse(parsedValue.expiresAt) <= Date.now()) {
      window.sessionStorage.removeItem(SHELL_SESSION_STORAGE_KEY)
      return null
    }

    return parsedValue
  } catch {
    return null
  }
}

export function writeStoredShellSessionSnapshot(session: StoredShellSession | null) {
  try {
    if (!session) {
      window.sessionStorage.removeItem(SHELL_SESSION_STORAGE_KEY)
      return
    }

    window.sessionStorage.setItem(SHELL_SESSION_STORAGE_KEY, JSON.stringify(session))
  } catch {
    // Ignora errores de almacenamiento del navegador.
  }
}

export function useShellSession() {
  const context = useContext(ShellSessionContext)

  if (!context) {
    throw new Error('useShellSession debe usarse dentro de ShellSessionProvider')
  }

  return context
}
