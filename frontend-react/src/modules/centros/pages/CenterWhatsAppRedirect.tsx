import { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { getPublicCenterWhatsAppRedirectUrl } from '../../../services/api/centrosApi'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export function CenterWhatsAppRedirect() {
  const { slug } = useParams<{ slug: string }>()

  useEffect(() => {
    if (slug) {
      window.location.href = `${apiBaseUrl}${getPublicCenterWhatsAppRedirectUrl(slug)}`
    }
  }, [slug])

  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="h-10 w-10 animate-spin rounded-full border-4 border-pink-200 border-t-pink-500" />
    </div>
  )
}
