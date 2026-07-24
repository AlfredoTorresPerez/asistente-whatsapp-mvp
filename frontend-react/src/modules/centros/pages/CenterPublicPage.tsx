import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getPublicCenterBySlug } from '../../../services/api/centrosApi'
import type { PublicCenterResponse } from '../../../services/api/types'

type ViewState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; data: PublicCenterResponse }

export function CenterPublicPage() {
  const { slug } = useParams<{ slug: string }>()
  const [state, setState] = useState<ViewState>({ status: 'loading' })

  useEffect(() => {
    if (!slug) return
    setState({ status: 'loading' })
    getPublicCenterBySlug(slug)
      .then((data) => setState({ status: 'loaded', data }))
      .catch(() => setState({ status: 'error', message: 'Centro no encontrado' }))
  }, [slug])

  function handleWhatsAppClick(waUrl: string) {
    window.open(waUrl, '_blank', 'noopener')
  }

  if (state.status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-pink-200 border-t-pink-500" />
      </div>
    )
  }

  if (state.status === 'error') {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4">
        <h1 className="text-2xl font-bold text-gray-800">{state.message}</h1>
        <Link to="/" className="text-pink-600 underline hover:text-pink-800">
          Volver al inicio
        </Link>
      </div>
    )
  }

  const { data } = state
  const { company, services = [], promotions = [], locations = [], whatsapp, page: cfg } = data
  const primary = cfg.primaryColor || '#EC4899'
  const secondary = cfg.secondaryColor || '#8B5CF6'

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#fefcfd' }}>
      <header
        className="mx-auto flex max-w-6xl items-center justify-between px-4 py-6"
      >
        <div className="flex items-center gap-2">
          <div
            className="flex h-10 w-10 items-center justify-center rounded-xl text-lg font-bold text-white"
            style={{ background: `linear-gradient(135deg, ${primary}, ${secondary})` }}
          >
            {company.name ? company.name.charAt(0).toUpperCase() : '?'}
          </div>
          <span className="text-lg font-bold text-gray-800">{company.name}</span>
        </div>
      </header>

      <section className="mx-auto max-w-4xl px-4 pb-8 pt-12 text-center">
        <h1 className="text-4xl font-bold tracking-tight text-gray-900 md:text-5xl">
          {cfg.welcomeTitle || company.name}
        </h1>
        {cfg.welcomeSubtitle && (
          <p className="mx-auto mt-4 max-w-2xl text-lg text-gray-600">
            {cfg.welcomeSubtitle}
          </p>
        )}
        {whatsapp && (
          <WhatsAppButton waUrl={whatsapp.waUrl} />
        )}
      </section>

      {cfg.showServices && services.length > 0 && (
        <section className="mx-auto max-w-6xl px-4 py-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Servicios</h2>
          {groupBy(services, 'categoryName').map(([cat, items]) => (
            <div key={cat} className="mb-8">
              <h3 className="text-lg font-semibold text-gray-700 mb-3 border-b border-gray-200 pb-2">{cat || 'Otros'}</h3>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {items.map((s) => (
                  <div
                    key={s.id}
                    className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm"
                  >
                    <h4 className="font-semibold text-gray-900">{s.name}</h4>
                    {s.description && (
                      <p className="mt-1 text-sm text-gray-500 line-clamp-2">{s.description}</p>
                    )}
                    <div className="mt-3 flex items-center justify-between text-sm">
                      {s.durationMinutes && (
                        <span className="text-gray-400">{s.durationMinutes} min</span>
                      )}
                      {s.priceBase != null && s.priceBase > 0 && (
                        <span className="font-semibold" style={{ color: primary }}>
                          ${s.priceBase.toLocaleString('es-CL')}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </section>
      )}

      {cfg.showPromotions && promotions.length > 0 && (
        <section className="mx-auto max-w-6xl px-4 py-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Promociones</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {promotions.map((p) => (
              <div
                key={p.id}
                className="rounded-2xl p-5 shadow-sm text-white"
                style={{ background: `linear-gradient(135deg, ${primary}, ${secondary})` }}
              >
                <h3 className="font-semibold">{p.name}</h3>
                <p className="mt-1 text-sm opacity-90 line-clamp-2">{p.description}</p>
                <div className="mt-3 text-sm opacity-80">
                  {p.discountType === 'PERCENTAGE' ? `${p.discountValue}% OFF` : `$${p.discountValue.toLocaleString('es-CL')} OFF`}
                  {p.endsOn && ` — hasta ${new Date(p.endsOn).toLocaleDateString('es-CL')}`}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {locations.length > 0 && (
        <section className="mx-auto max-w-6xl px-4 py-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Sucursales</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {locations.map((l) => (
              <div
                key={l.id}
                className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm"
              >
                <h3 className="font-semibold text-gray-900">{l.name}</h3>
                {l.address && <p className="mt-1 text-sm text-gray-500">{l.address}</p>}
                {l.commune && <p className="text-sm text-gray-400">{l.commune}</p>}
                {l.phone && <p className="mt-2 text-sm text-gray-600">{l.phone}</p>}
              </div>
            ))}
          </div>
        </section>
      )}

      <footer className="border-t border-gray-100 py-24 text-center text-sm text-gray-500">
        <p>&copy; {new Date().getFullYear()} {company.name || 'Centro Estético'} &mdash; Todos los derechos reservados.</p>
      </footer>

      <div className="fixed bottom-0 left-0 right-0 z-40 border-t border-white/20 bg-white/95 shadow-2xl backdrop-blur-sm">
        <div className="mx-auto flex max-w-2xl items-center justify-center gap-4 px-4 py-3 sm:py-4">
          <a
            href="/reservar"
            className="flex flex-1 items-center justify-center gap-2 rounded-2xl px-6 py-3 text-base font-bold text-white shadow-lg transition hover:opacity-90 sm:text-lg"
            style={{ background: `linear-gradient(135deg, ${primary}, ${secondary})` }}
          >
            <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            Agendar ahora
          </a>
          {whatsapp && (
            <a
              href={whatsapp.waUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center justify-center rounded-2xl px-5 py-3 text-white shadow-lg transition hover:opacity-90"
              style={{ backgroundColor: '#25D366' }}
            >
              <WhatsAppIcon />
            </a>
          )}
        </div>
      </div>


    </div>
  )
}

function WhatsAppButton({ waUrl }: { waUrl: string }) {
  return (
    <a
      href={waUrl}
      target="_blank"
      rel="noopener noreferrer"
      className="mt-8 inline-flex cursor-pointer items-center gap-2 rounded-2xl px-8 py-4 text-lg font-semibold text-white shadow-lg transition hover:shadow-xl"
      style={{ backgroundColor: '#25D366' }}
    >
      <WhatsAppIcon />
      Chatea con nosotros
    </a>
  )
}

function groupBy<T>(items: T[], key: keyof T): [string, T[]][] {
  const map = new Map<string, T[]>()
  for (const item of items) {
    const k = String(item[key] ?? '')
    if (!map.has(k)) map.set(k, [])
    map.get(k)!.push(item)
  }
  return [...map.entries()]
}

function WhatsAppIcon() {
  return (
    <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
    </svg>
  )
}


