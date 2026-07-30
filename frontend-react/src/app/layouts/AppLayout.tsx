import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ConfirmDialog } from '../../components/overlay/ConfirmDialog'
import { UserMenu } from '../../components/overlay/UserMenu'
import { OfflineBanner } from '../../components/feedback/OfflineBanner'
import { Sidebar } from '../../components/navigation/Sidebar'
import { Topbar } from '../../components/navigation/Topbar'
import { findRouteMeta } from '../../lib/navigation'
import { useShellSession } from '../../lib/shellSession'
import { useToast } from '../../lib/toast'
import { useOnlineStatus } from '../../lib/useOnlineStatus'
import { getNotificationsRequest } from '../../services/api/notificationsApi'

export function AppLayout() {
  const { signOut, user } = useShellSession()
  const { showToast } = useToast()
  const location = useLocation()
  const isOnline = useOnlineStatus()
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
  const [isLogoutDialogOpen, setIsLogoutDialogOpen] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const currentRoute = findRouteMeta(location.pathname)
  const unreadNotificationsQuery = useQuery({
    queryKey: ['notifications', 'unread-count', user?.id],
    queryFn: () =>
      getNotificationsRequest({
        page: 0,
        size: 1,
        status: 'UNREAD',
      }),
    enabled: Boolean(user),
    refetchInterval: isOnline ? 30_000 : false,
  })

  if (!user) {
    return (
      <div className="min-h-screen bg-[var(--color-background)] px-4 py-10">
        <div className="mx-auto max-w-xl rounded-[28px] border border-[var(--color-border)] bg-white p-8 shadow-[var(--shadow-card)]">
          <LoadingState message="Cargando datos del usuario autenticado." />
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-dvh min-h-0 flex-col overflow-hidden bg-[var(--color-background)]">
      <OfflineBanner visible={!isOnline} />
      <div className="mx-auto grid h-full min-h-0 w-full max-w-[1680px] flex-1 gap-4 px-4 py-4 lg:grid-cols-[188px_minmax(0,1fr)] lg:px-5 lg:py-4">
        <Sidebar
          businessName={user.businessName}
          onClose={() => setIsSidebarOpen(false)}
          onLogout={() => setIsLogoutDialogOpen(true)}
          open={isSidebarOpen}
          permissions={user.permissions ?? []}
          role={user.role}
        />

        <main className="flex h-full min-h-0 min-w-0 flex-col gap-4 overflow-hidden">
          <div className="relative shrink-0">
            {isUserMenuOpen ? (
              <button
                aria-label="Cerrar menu de usuario"
                className="fixed inset-0 z-20 bg-transparent"
                onClick={() => setIsUserMenuOpen(false)}
                type="button"
              />
            ) : null}

            <Topbar
              currentDescription={currentRoute?.description ?? ''}
              currentRoute={currentRoute?.title ?? 'Shell privado'}
              isUserMenuOpen={isUserMenuOpen}
              notificationCount={unreadNotificationsQuery.data?.totalItems ?? 0}
              onOpenSidebar={() => setIsSidebarOpen(true)}
              onToggleUserMenu={() => setIsUserMenuOpen((currentValue) => !currentValue)}
              user={user}
            />

            <UserMenu
              onNavigate={() => setIsUserMenuOpen(false)}
              onLogout={() => {
                setIsUserMenuOpen(false)
                setIsLogoutDialogOpen(true)
              }}
              open={isUserMenuOpen}
              user={user}
            />
          </div>

          <div className="min-h-0 min-w-0 flex-1 overflow-y-auto overscroll-contain pb-2 pr-1">
            <Outlet />
          </div>
        </main>
      </div>

      <ConfirmDialog
        confirmLabel="Cerrar sesion"
        confirmLoading={isLoggingOut}
        description="Estas a punto de cerrar sesion en Asistente WhatsApp. Podras volver a ingresar cuando lo necesites."
        onCancel={() => setIsLogoutDialogOpen(false)}
        onConfirm={() => {
          if (isLoggingOut) {
            return
          }

          setIsLoggingOut(true)
          void signOut()
            .then((remoteLogoutSucceeded) => {
              if (!remoteLogoutSucceeded) {
                showToast({
                  title: 'Sesion cerrada localmente',
                  description:
                    'No alcanzamos a confirmar el logout remoto, pero la sesion local ya fue eliminada.',
                  tone: 'warning',
                })
              }
            })
            .finally(() => {
              setIsLoggingOut(false)
            })
          setIsLogoutDialogOpen(false)
        }}
        open={isLogoutDialogOpen}
        title="Cerrar sesion?"
        tone="danger"
      />
    </div>
  )
}
