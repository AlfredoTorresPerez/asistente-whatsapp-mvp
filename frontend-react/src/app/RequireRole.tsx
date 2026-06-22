import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useShellSession } from '../lib/shellSession'

export function RequireRole({
  allowedRoles,
  children,
}: {
  allowedRoles: string[]
  children: ReactNode
}) {
  const { user } = useShellSession()

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate replace to="/dashboard" />
  }

  return children
}
