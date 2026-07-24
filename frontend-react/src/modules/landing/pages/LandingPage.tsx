import { Navigate } from 'react-router-dom'

const DEFAULT_SLUG = import.meta.env.VITE_DEFAULT_CENTER_SLUG ?? 'bella-centro-estetica'

export function LandingPage() {
  return <Navigate replace to={`/centros/${DEFAULT_SLUG}`} />
}
