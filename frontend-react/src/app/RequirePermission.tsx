import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { usePermissions } from '../hooks/usePermissions'

export function RequirePermission({
  permission,
  fallback,
}: {
  permission: string
  fallback?: ReactNode
}) {
  const { hasPermission } = usePermissions()

  if (!hasPermission(permission)) {
    return fallback ?? <Navigate replace to="/dashboard" />
  }

  return null
}

export function RequireAnyPermission({
  permissions,
  fallback,
}: {
  permissions: string[]
  fallback?: ReactNode
}) {
  const { hasAnyPermission } = usePermissions()

  if (!hasAnyPermission(...permissions)) {
    return fallback ?? <Navigate replace to="/dashboard" />
  }

  return null
}

export function RequireAllPermissions({
  permissions,
  fallback,
}: {
  permissions: string[]
  fallback?: ReactNode
}) {
  const { hasAllPermissions } = usePermissions()

  if (!hasAllPermissions(...permissions)) {
    return fallback ?? <Navigate replace to="/dashboard" />
  }

  return null
}