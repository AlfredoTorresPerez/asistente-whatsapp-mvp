import { Navigate, useLocation } from 'react-router-dom'
import { LoadingState } from '../components/feedback/LoadingState'
import { useShellSession } from '../lib/shellSession'
import { PrivateLayout } from './layouts/PrivateLayout'
import { PublicLayout } from './layouts/PublicLayout'

export function PublicRouteShell() {
  const { isAuthenticated, status } = useShellSession()

  if (status === 'loading') {
    return <FullscreenLoadingState />
  }

  if (isAuthenticated) {
    return <Navigate replace to="/dashboard" />
  }

  return <PublicLayout />
}

export function PrivateRouteShell() {
  const { isAuthenticated, status } = useShellSession()
  const location = useLocation()

  if (status === 'loading') {
    return <FullscreenLoadingState />
  }

  if (!isAuthenticated) {
    return <Navigate replace state={{ from: location.pathname }} to="/login" />
  }

  return <PrivateLayout />
}

function FullscreenLoadingState() {
  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(191,219,254,0.38),_transparent_45%),linear-gradient(180deg,_#f7fbff_0%,_#eef4ff_100%)] px-4 py-10">
      <div className="mx-auto flex min-h-[80vh] max-w-5xl items-center justify-center">
        <div className="w-full max-w-xl rounded-[2rem] border border-white/70 bg-white/88 p-8 shadow-[0_32px_80px_rgba(15,23,42,0.12)] backdrop-blur">
          <LoadingState message="Comprobando credenciales y restaurando la sesion del usuario." />
        </div>
      </div>
    </div>
  )
}
