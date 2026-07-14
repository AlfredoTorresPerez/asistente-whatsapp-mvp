import { useShellSession } from '../lib/shellSession'

export function usePermissions() {
  const { user } = useShellSession()

  const permissions = user?.permissions ?? []

  const hasPermission = (permission: string): boolean => {
    return permissions.includes(permission)
  }

  const hasAnyPermission = (...permissionsToCheck: string[]): boolean => {
    return permissionsToCheck.some((p) => permissions.includes(p))
  }

  const hasAllPermissions = (...permissionsToCheck: string[]): boolean => {
    return permissionsToCheck.every((p) => permissions.includes(p))
  }

  return {
    permissions,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
  }
}