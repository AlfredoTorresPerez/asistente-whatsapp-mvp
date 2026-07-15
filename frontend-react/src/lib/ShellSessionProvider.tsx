import type { PropsWithChildren } from 'react'
import { useEffect, useState } from 'react'
import { loginRequest, logoutRequest, meRequest } from '../services/api/authApi'
import {
  changePasswordRequest as changePasswordApiRequest,
  getCurrentProfileRequest,
  updateCurrentProfileRequest,
} from '../services/api/profileApi'
import {
  mergeShellUserProfile,
  readStoredShellSessionSnapshot,
  ShellSessionContext,
  toShellUserFromAuthResponse,
  writeStoredShellSessionSnapshot,
} from './shellSession'

export function ShellSessionProvider({ children }: PropsWithChildren) {
  const [initialStoredSession] = useState(readStoredShellSessionSnapshot)
  const [status, setStatus] = useState<'loading' | 'authenticated' | 'unauthenticated'>(
    initialStoredSession ? 'loading' : 'unauthenticated',
  )
  const [session, setSession] = useState(initialStoredSession)

  useEffect(() => {
    function handleSessionExpired() {
      writeStoredShellSessionSnapshot(null)
      setSession(null)
      setStatus('unauthenticated')
    }

    window.addEventListener('shell-session-expired', handleSessionExpired)
    return () => window.removeEventListener('shell-session-expired', handleSessionExpired)
  }, [])

  useEffect(() => {
    if (!initialStoredSession) {
      return
    }

    const storedSession = initialStoredSession
    let active = true

    async function restoreSession() {
      try {
        const currentUser = await meRequest()
        const currentProfile = await getCurrentProfileRequest().catch(() => null)
        if (!active) {
          return
        }

        const restoredSession = {
          ...storedSession,
          user: {
            ...storedSession.user,
            ...toShellUserFromAuthResponse(currentUser),
            phone: currentProfile?.phone ?? storedSession.user.phone,
            businessName: currentProfile?.businessName ?? currentUser.businessName,
            role: currentProfile?.role ?? currentUser.role,
          },
        }
        writeStoredShellSessionSnapshot(restoredSession)
        setSession(restoredSession)
        setStatus('authenticated')
      } catch {
        if (!active) {
          return
        }

        writeStoredShellSessionSnapshot(null)
        setSession(null)
        setStatus('unauthenticated')
      }
    }

    void restoreSession()

    return () => {
      active = false
    }
  }, [initialStoredSession])

  const signIn = async (credentials: { email: string; password: string }) => {
    const response = await loginRequest(credentials.email, credentials.password)
    const currentProfile = await getCurrentProfileRequest().catch(() => null)
    const authenticatedUser = toShellUserFromAuthResponse(response.user)
    const nextSession = {
      accessToken: response.accessToken,
      expiresAt: new Date(Date.now() + response.expiresInSeconds * 1000).toISOString(),
      user: currentProfile
        ? mergeShellUserProfile(authenticatedUser, currentProfile)
        : authenticatedUser,
    }
    writeStoredShellSessionSnapshot(nextSession)
    setSession(nextSession)
    setStatus('authenticated')
  }

  const signOut = async () => {
    let remoteLogoutSucceeded = true

    try {
      if (session?.accessToken) {
        await logoutRequest()
      }
    } catch {
      remoteLogoutSucceeded = false
    } finally {
      writeStoredShellSessionSnapshot(null)
      setSession(null)
      setStatus('unauthenticated')
    }

    return remoteLogoutSucceeded
  }

  const updateProfile = async (request: {
    firstName: string
    lastName: string
    phone: string
    timezone: string
  }) => {
    const profile = await updateCurrentProfileRequest(request)

    if (session) {
      const nextSession = {
        ...session,
        user: mergeShellUserProfile(session.user, profile),
      }
      writeStoredShellSessionSnapshot(nextSession)
      setSession(nextSession)
    }

    return profile
  }

  const changePassword = async (request: {
    currentPassword: string
    newPassword: string
    confirmPassword: string
  }) => {
    await changePasswordApiRequest(request)
  }

  const syncBusinessName = (businessName: string) => {
    if (!session) {
      return
    }

    const nextSession = {
      ...session,
      user: {
        ...session.user,
        businessName,
      },
    }
    writeStoredShellSessionSnapshot(nextSession)
    setSession(nextSession)
  }

  return (
    <ShellSessionContext.Provider
      value={{
        status,
        isAuthenticated: status === 'authenticated',
        user: session?.user ?? null,
        signIn,
        signOut,
        updateProfile,
        changePassword,
        syncBusinessName,
      }}
    >
      {children}
    </ShellSessionContext.Provider>
  )
}
