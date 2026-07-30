import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getPublicCenterBySlug } from '../../../services/api/centrosApi'
import type { PublicCenterResponse } from '../../../services/api/types'
import { LandingImage } from '../../../components/ui/LandingImage'
import { DEFAULT_LANDING_IMAGE } from '../../../lib/landingImages'

type ViewState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; data: PublicCenterResponse }

const SECTIONS = ['inicio', 'servicios', 'promociones', 'testimonios', 'contacto'] as const

// Directorio físico en Windows:
// C:\mvp\asistente_impl_codex\asistente\frontend-react\public\images\dinamicas
// Las referencias del navegador deben comenzar en /images/dinamicas, sin incluir /public.
const STATIC_IMAGE_BASE = '/images/dinamicas'

const STATIC_IMAGES = {
  referenceFull: `${STATIC_IMAGE_BASE}/landing_referencia_completa.png`,
  previewAll: `${STATIC_IMAGE_BASE}/vista_previa_todas_las_imagenes.jpg`,
  logoHorizontal: `${STATIC_IMAGE_BASE}/logo_horizontal_centro_estetico_bella.png`,
  logoIcon: `${STATIC_IMAGE_BASE}/icono_logo_loto.png`,
  hero: `${STATIC_IMAGE_BASE}/hero_marca_centro_estetico_bella.png`,
  services: [
    `${STATIC_IMAGE_BASE}/servicio_limpieza_facial_profunda.png`,
    `${STATIC_IMAGE_BASE}/servicio_hidratacion_acido_hialuronico.png`,
    `${STATIC_IMAGE_BASE}/servicio_tratamiento_reductor.png`,
    `${STATIC_IMAGE_BASE}/servicio_depilacion_laser.png`,
  ],
  about: `${STATIC_IMAGE_BASE}/sobre_nosotros_cabina_estetica.png`,
  promotionPortrait: `${STATIC_IMAGE_BASE}/promocion_limpieza_hidratacion_retrato.png`,
  callToActionReception: `${STATIC_IMAGE_BASE}/cta_recepcion_centro_estetico.png`,
  map: `${STATIC_IMAGE_BASE}/mapa_ubicacion_centro_estetico.png`,
  whatsappFloating: `${STATIC_IMAGE_BASE}/icono_whatsapp_flotante.png`,
} as const

function normalizeImageKey(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
}

function resolveDynamicServiceImage(serviceName: string, index: number): string {
  const normalized = normalizeImageKey(serviceName)

  if (normalized.includes('limpieza') || normalized.includes('facial profunda')) {
    return STATIC_IMAGES.services[0]
  }
  if (normalized.includes('hialuron') || normalized.includes('hidratacion')) {
    return STATIC_IMAGES.services[1]
  }
  if (normalized.includes('reductor') || normalized.includes('corporal')) {
    return STATIC_IMAGES.services[2]
  }
  if (normalized.includes('depilacion') || normalized.includes('laser')) {
    return STATIC_IMAGES.services[3]
  }

  return STATIC_IMAGES.services[index % STATIC_IMAGES.services.length] ?? STATIC_IMAGES.services[0]
}

const BENEFIT_ICONS: Record<string, string> = {
  certificate: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
  heart:
    'M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z',
  device:
    'M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z',
  location:
    'M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z M15 11a3 3 0 11-6 0 3 3 0 016 0z',
  star: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z',
  sparkles:
    'M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z',
}

function resolveBenefitIcon(icon: string): string {
  return BENEFIT_ICONS[icon] ?? BENEFIT_ICONS.star
}

export function CenterPublicPageDinamica({ slug: slugProp }: { slug?: string } = {}) {
  const { slug: slugParam } = useParams<{ slug: string }>()
  const slug = slugProp || slugParam || 'centro-estetico-bella'
  const [state, setState] = useState<ViewState>({ status: 'loading' })
  const [menuOpen, setMenuOpen] = useState(false)

  const sectionRefs = useRef<Record<string, HTMLElement | null>>({})
  const [activeSection, setActiveSection] = useState<string>('inicio')

  useEffect(() => {
    if (!slug) return
    setState({ status: 'loading' })
    getPublicCenterBySlug(slug)
      .then((data) => setState({ status: 'loaded', data }))
      .catch(() => setState({ status: 'error', message: 'Centro no encontrado' }))
  }, [slug])

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setActiveSection(entry.target.id)
          }
        }
      },
      { rootMargin: '-80px 0px -60% 0px', threshold: 0 },
    )

    const refs = sectionRefs.current
    for (const id of SECTIONS) {
      const el = refs[id]
      if (el) observer.observe(el)
    }

    return () => observer.disconnect()
  }, [state.status])

  const scrollToSection = useCallback((id: string) => {
    setMenuOpen(false)
    const el = document.getElementById(id)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [])

  const handleWhatsAppRedirect = useCallback(
    (origin: string, serviceId?: string, promotionId?: string) => {
      if (!slug) return
      const params = new URLSearchParams({ origen: origin })
      if (serviceId) params.set('servicioId', serviceId)
      if (promotionId) params.set('promocionId', promotionId)
      const url = `/centros/${slug}/whatsapp?${params.toString()}`
      const newWindow = window.open(url, '_blank', 'noopener,noreferrer')
      if (newWindow) {
        newWindow.opener = null
      }
    },
    [slug],
  )

  const handleAgendaRedirect = useCallback(() => {
    const url = '/reservar'
    const newWindow = window.open(url, '_blank', 'noopener,noreferrer')
    if (newWindow) {
      newWindow.opener = null
    }
  }, [])

  if (state.status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center" role="status">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-pink-200 border-t-pink-500" />
        <span className="sr-only">Cargando...</span>
      </div>
    )
  }

  if (state.status === 'error') {
    return (
      <div
        className="flex min-h-screen flex-col items-center justify-center gap-4 px-4"
        role="alert"
      >
        <h1 className="text-2xl font-bold text-gray-800">{state.message}</h1>
        <p className="text-gray-500">Verifica que la dirección sea correcta o vuelve al inicio.</p>
        <Link
          to="/"
          className="rounded-2xl bg-pink-500 px-6 py-3 font-semibold text-white transition hover:bg-pink-600"
        >
          Volver al inicio
        </Link>
      </div>
    )
  }

  const { data } = state
  const { company, services = [], promotions = [], locations = [], page: cfg } = data
  const primary = cfg.primaryColor || '#EC4899'
  const secondary = cfg.secondaryColor || '#8B5CF6'
  const activePromotions = promotions.filter((p) => {
    if (!p.endsOn) return true
    return new Date(p.endsOn) >= new Date()
  })
  const featuredPromotion = activePromotions.length > 0 ? activePromotions[0] : null
  const firstLocation = locations.length > 0 ? locations[0] : null
  const heroImageUrl = cfg.heroImageUrl || STATIC_IMAGES.hero
  const logoUrl = cfg.headerLogoUrl || STATIC_IMAGES.logoHorizontal
  const footerLogoUrl = cfg.headerLogoUrl || STATIC_IMAGES.logoIcon
  const displayServices = services.slice(0, 4)

  return (
    <div className="min-h-screen bg-white font-sans text-gray-900">
      <a
        href="#inicio"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-2xl focus:bg-white focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-pink-600 focus:shadow-lg"
      >
        Saltar al contenido principal
      </a>

      <header className="fixed left-0 right-0 top-0 z-40 border-b border-gray-100/80 bg-white/95 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:py-4">
          <button
            onClick={() => scrollToSection('inicio')}
            className="flex items-center gap-2 bg-transparent"
            aria-label="Ir al inicio"
          >
            {logoUrl ? (
              <img alt="Logo" className="h-10 w-auto rounded-xl" src={logoUrl} />
            ) : (
              <div
                className="flex h-10 w-10 items-center justify-center rounded-xl text-lg font-bold text-white"
                style={{ background: `linear-gradient(135deg, ${primary}, ${secondary})` }}
                aria-hidden="true"
              >
                {company.name ? company.name.charAt(0).toUpperCase() : '?'}
              </div>
            )}
            <span className="hidden text-lg font-bold text-gray-800 sm:inline">
              {company.businessName || company.name}
            </span>
          </button>

          <nav className="hidden items-center gap-6 md:flex" aria-label="Navegación principal">
            {SECTIONS.map((sec) => (
              <button
                key={sec}
                onClick={() => scrollToSection(sec)}
                className={`text-sm font-medium capitalize transition ${
                  activeSection === sec ? 'text-pink-600' : 'text-gray-600 hover:text-pink-600'
                }`}
              >
                {sec === 'inicio' ? 'Inicio' : sec}
              </button>
            ))}
            <button
              onClick={() => handleAgendaRedirect()}
              className="inline-flex items-center gap-2 rounded-2xl bg-pink-500 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-pink-600"
              aria-label={`Abrir agenda en línea de ${company.name}`}
            >
              <CalendarIcon className="h-4 w-4" />
              Agenda en línea
            </button>
            <button
              onClick={() => handleWhatsAppRedirect('encabezado')}
              className="inline-flex items-center gap-2 rounded-2xl bg-[#25D366] px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#20BD5A]"
              aria-label={`Abrir conversación de WhatsApp con ${company.name}`}
            >
              <WhatsAppIcon className="h-4 w-4" />
              Reservar por WhatsApp
            </button>
          </nav>

          <button
            onClick={() => setMenuOpen(!menuOpen)}
            className="flex items-center justify-center rounded-xl p-2 text-gray-600 transition hover:bg-gray-100 md:hidden"
            aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={menuOpen}
          >
            {menuOpen ? (
              <svg
                className="h-6 w-6"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg
                className="h-6 w-6"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16m-7 6h7" />
              </svg>
            )}
          </button>
        </div>

        {menuOpen && (
          <nav
            className="border-t border-gray-100 bg-white px-4 pb-4 pt-2 md:hidden"
            aria-label="Navegación móvil"
          >
            <div className="flex flex-col gap-3">
              {SECTIONS.map((sec) => (
                <button
                  key={sec}
                  onClick={() => scrollToSection(sec)}
                  className={`rounded-xl px-4 py-2.5 text-left text-sm font-medium transition ${
                    activeSection === sec
                      ? 'bg-pink-50 text-pink-600'
                      : 'text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  {sec === 'inicio' ? 'Inicio' : sec}
                </button>
              ))}
              <button
                onClick={() => handleAgendaRedirect()}
                className="inline-flex items-center justify-center gap-2 rounded-2xl bg-pink-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-pink-600"
              >
                <CalendarIcon className="h-4 w-4" />
                Agenda en línea
              </button>
              <button
                onClick={() => handleWhatsAppRedirect('encabezado')}
                className="inline-flex items-center justify-center gap-2 rounded-2xl bg-[#25D366] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#20BD5A]"
              >
                <WhatsAppIcon className="h-4 w-4" />
                Reservar por WhatsApp
              </button>
            </div>
          </nav>
        )}
      </header>

      <main>
        <section
          id="inicio"
          ref={(el) => {
            sectionRefs.current.inicio = el
          }}
          className="relative min-h-[80vh] overflow-hidden pt-20"
          style={{ backgroundColor: '#fefcfd' }}
        >
          <div className="mx-auto grid min-h-[70vh] max-w-6xl items-center gap-8 px-4 pb-16 pt-8 md:grid-cols-2 md:pt-16">
            <div className="z-10">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-pink-500">
                {company.businessName || company.name}
              </p>
              <h1 className="mt-4 text-4xl font-bold leading-tight tracking-tight text-gray-900 md:text-5xl lg:text-6xl">
                {cfg.welcomeTitle || 'Tu centro de estética de confianza'}
              </h1>
              <p className="mt-4 max-w-xl text-base leading-relaxed text-gray-600 md:text-lg">
                {cfg.welcomeSubtitle ||
                  company.description ||
                  'Expertos en belleza y bienestar con atención personalizada.'}
              </p>
              <div className="mt-8 flex flex-wrap items-center gap-4">
                <button
                  onClick={() => scrollToSection('servicios')}
                  className="inline-flex items-center gap-2 rounded-2xl border-2 border-gray-200 bg-white px-8 py-4 text-base font-semibold text-gray-700 shadow-sm transition hover:border-pink-200 hover:text-pink-600"
                >
                  Ver servicios
                </button>
              </div>
            </div>
            <div className="relative flex items-center justify-center md:justify-end">
              <div
                className="relative aspect-[4/3] w-full max-w-lg overflow-hidden rounded-[2.5rem] shadow-2xl"
                style={{ backgroundColor: '#fdf2f8' }}
              >
                <LandingImage
                  src={heroImageUrl}
                  fallbackSrc={DEFAULT_LANDING_IMAGE}
                  alt={`Imagen principal de ${company.name}`}
                  className="h-full w-full"
                  width={1491}
                  height={1055}
                  objectFit="cover"
                />
              </div>
              <div
                className="absolute -bottom-4 -left-4 h-32 w-32 rounded-full opacity-20"
                style={{ background: `radial-gradient(circle, ${primary}, transparent)` }}
                aria-hidden="true"
              />
            </div>
          </div>
        </section>

        <section
          id="servicios"
          ref={(el) => {
            sectionRefs.current.servicios = el
          }}
          className="scroll-mt-20 py-16 md:py-24"
          style={{ backgroundColor: '#fefcfd' }}
        >
          <div className="mx-auto max-w-6xl px-4">
            <div className="mx-auto max-w-2xl text-center">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-pink-500">
                Servicios
              </p>
              <h2 className="mt-3 text-3xl font-bold text-gray-900 md:text-4xl">
                {cfg.showServices ? 'Nuestros tratamientos' : 'Conoce nuestros tratamientos'}
              </h2>
              <p className="mt-3 text-gray-600">
                Ofrecemos una amplia gama de servicios para el cuidado personal.
              </p>
            </div>

            {cfg.showServices && displayServices.length > 0 ? (
              <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
                {displayServices.map((s, index) => {
                  const serviceImg = resolveDynamicServiceImage(s.name, index)
                  return (
                    <article
                      key={s.id}
                      className="group flex flex-col rounded-2xl border border-gray-100 bg-white shadow-sm transition hover:shadow-md"
                    >
                      <div className="aspect-[4/3] overflow-hidden rounded-t-2xl">
                        <LandingImage
                          src={serviceImg}
                          fallbackSrc={DEFAULT_LANDING_IMAGE}
                          alt={`${s.name} en ${company.name}`}
                          className="h-full w-full transition duration-300 group-hover:scale-105"
                          width={340}
                          height={255}
                          objectFit="cover"
                        />
                      </div>
                      <div className="flex flex-1 flex-col p-5">
                        <h3 className="text-lg font-semibold text-gray-900">{s.name}</h3>
                        {s.description && (
                          <p className="mt-2 flex-1 text-sm leading-relaxed text-gray-500">
                            {s.description}
                          </p>
                        )}
                        <div className="mt-4 flex items-center justify-between text-sm">
                          {s.durationMinutes && (
                            <span className="text-gray-400">{s.durationMinutes} min</span>
                          )}
                          {s.priceBase != null && s.priceBase > 0 && (
                            <span className="font-semibold" style={{ color: primary }}>
                              ${s.priceBase.toLocaleString('es-CL')}
                            </span>
                          )}
                        </div>
                        <button
                          onClick={() => handleWhatsAppRedirect('servicio', s.id)}
                          className="mt-4 w-full rounded-xl bg-gray-50 py-2.5 text-xs font-semibold text-gray-600 transition hover:bg-[#25D366] hover:text-white"
                        >
                          Consultar por WhatsApp
                        </button>
                      </div>
                    </article>
                  )
                })}
              </div>
            ) : (
              <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
                {services.slice(0, 4).map((s, index) => (
                  <article
                    key={s.id}
                    className="flex flex-col rounded-2xl border border-gray-100 bg-white shadow-sm"
                  >
                    <div className="aspect-[4/3] overflow-hidden rounded-t-2xl">
                      <LandingImage
                        src={resolveDynamicServiceImage(s.name, index)}
                        fallbackSrc={DEFAULT_LANDING_IMAGE}
                        alt={`${s.name} en ${company.name}`}
                        className="h-full w-full"
                        width={340}
                        height={255}
                        objectFit="cover"
                      />
                    </div>
                    <div className="flex flex-1 flex-col p-5">
                      <h3 className="text-lg font-semibold text-gray-900">{s.name}</h3>
                      {s.description && (
                        <p className="mt-2 flex-1 text-sm text-gray-500">{s.description}</p>
                      )}
                      <button
                        onClick={() => handleWhatsAppRedirect('servicio', s.id)}
                        className="mt-4 w-full rounded-xl bg-gray-50 py-2.5 text-xs font-semibold text-gray-600 transition hover:bg-[#25D366] hover:text-white"
                      >
                        Consultar por WhatsApp
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>

        {cfg.aboutTitle && (
          <section
            className="scroll-mt-20 py-16 md:py-24"
            style={{ backgroundColor: '#fdfaf7' }}
          >
            <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 md:grid-cols-2">
              <div className="order-2 md:order-1">
                <h2 className="text-3xl font-bold text-gray-900 md:text-4xl">{cfg.aboutTitle}</h2>
                {cfg.aboutText && (
                  <p className="mt-4 text-base leading-relaxed text-gray-600">{cfg.aboutText}</p>
                )}
                <button
                  onClick={() => handleWhatsAppRedirect('contacto')}
                  className="mt-6 inline-flex items-center gap-2 rounded-2xl bg-[#25D366] px-6 py-3 text-sm font-semibold text-white transition hover:bg-[#20BD5A]"
                >
                  <WhatsAppIcon className="h-4 w-4" />
                  Contactar ahora
                </button>
              </div>
              <div className="order-1 md:order-2">
                <div className="aspect-[4/3] overflow-hidden rounded-[2.5rem] bg-pink-50 shadow-lg">
                  <LandingImage
                    src={STATIC_IMAGES.about}
                    fallbackSrc={DEFAULT_LANDING_IMAGE}
                    alt="Interior del centro estético"
                    className="h-full w-full"
                    width={340}
                    height={265}
                    objectFit="cover"
                  />
                </div>
              </div>
            </div>
          </section>
        )}

        {cfg.benefits && cfg.benefits.length > 0 && (
          <section className="scroll-mt-20 py-16 md:py-24">
            <div className="mx-auto max-w-6xl px-4">
              <div className="mx-auto max-w-2xl text-center">
                <h2 className="text-3xl font-bold text-gray-900 md:text-4xl">
                  ¿Por que elegirnos?
                </h2>
                <p className="mt-3 text-gray-600">
                  Nos esforzamos por ofrecer la mejor experiencia.
                </p>
              </div>
              <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
                {cfg.benefits.map((b, i) => (
                  <div
                    key={i}
                    className="rounded-2xl border border-gray-100 bg-white p-6 text-center shadow-sm"
                  >
                    <div
                      className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl text-white"
                      style={{ background: `linear-gradient(135deg, ${primary}, ${secondary})` }}
                      aria-hidden="true"
                    >
                      <svg
                        className="h-7 w-7"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth={1.5}
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d={resolveBenefitIcon(b.icon)}
                        />
                      </svg>
                    </div>
                    <h3 className="mt-4 text-base font-semibold text-gray-900">{b.title}</h3>
                    <p className="mt-2 text-sm leading-relaxed text-gray-500">{b.text}</p>
                  </div>
                ))}
              </div>
            </div>
          </section>
        )}

        {cfg.showPromotions && featuredPromotion && (
          <section
            id="promociones"
            ref={(el) => {
              sectionRefs.current.promociones = el
            }}
            className="scroll-mt-20 py-16 md:py-24"
            style={{ backgroundColor: '#fdfaf7' }}
          >
            <div className="mx-auto max-w-6xl px-4">
              <div className="mx-auto max-w-2xl text-center">
                <p className="text-sm font-semibold uppercase tracking-[0.2em] text-pink-500">
                  Promoción
                </p>
                <h2 className="mt-3 text-3xl font-bold text-gray-900 md:text-4xl">
                  Promoción destacada
                </h2>
              </div>
              <div
                className="mx-auto mt-10 max-w-5xl overflow-hidden rounded-[2rem] shadow-xl"
                style={{ background: `linear-gradient(135deg, ${primary}, ${secondary})` }}
              >
                <div className="flex flex-col md:flex-row">
                  <div className="aspect-[4/3] md:aspect-auto md:w-2/5">
                    <LandingImage
                      src={STATIC_IMAGES.promotionPortrait}
                      fallbackSrc={DEFAULT_LANDING_IMAGE}
                      alt=""
                      className="pointer-events-none h-full w-full"
                      width={576}
                      height={500}
                      objectFit="cover"
                    />
                  </div>
                  <div className="flex flex-1 flex-col justify-center p-6 text-white md:p-8">
                    <p className="text-xs font-semibold uppercase tracking-[0.15em] opacity-80">
                      Promoción destacada
                    </p>
                    <h3 className="mt-2 text-2xl font-bold md:text-3xl">
                      {featuredPromotion.name}
                    </h3>
                    {featuredPromotion.description && (
                      <p className="mt-3 text-sm leading-relaxed opacity-90">
                        {featuredPromotion.description}
                      </p>
                    )}
                    <div className="mt-5 flex flex-wrap items-center gap-3">
                      <span className="rounded-xl bg-white/20 px-4 py-2 text-base font-bold backdrop-blur-sm">
                        {featuredPromotion.discountType === 'PERCENTAGE'
                          ? `${featuredPromotion.discountValue}% de descuento`
                          : `$${featuredPromotion.discountValue.toLocaleString('es-CL')} de descuento`}
                      </span>
                      {featuredPromotion.endsOn && (
                        <span className="text-xs opacity-80">
                          Válido hasta{' '}
                          {new Date(featuredPromotion.endsOn).toLocaleDateString('es-CL')}
                        </span>
                      )}
                    </div>
                    <p className="mt-4 text-sm font-semibold opacity-90">
                      Reserva hoy por WhatsApp &mdash; Cupos limitados
                    </p>
                    <button
                      type="button"
                      onClick={() =>
                        handleWhatsAppRedirect('promocion', undefined, featuredPromotion.id)
                      }
                      className="mt-4 inline-flex items-center justify-center gap-2 self-start rounded-2xl bg-white px-6 py-3 text-sm font-bold text-gray-900 shadow transition hover:bg-gray-100"
                    >
                      <WhatsAppIcon className="h-4 w-4 text-[#25D366]" />
                      Escríbenos ahora
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        )}

        {cfg.showTestimonials && cfg.testimonials && cfg.testimonials.length > 0 && (
          <section
            id="testimonios"
            ref={(el) => {
              sectionRefs.current.testimonios = el
            }}
            className="scroll-mt-20 py-16 md:py-24"
            style={{ backgroundColor: '#fefcfd' }}
          >
            <div className="mx-auto max-w-6xl px-4">
              <div className="mx-auto max-w-2xl text-center">
                <p className="text-sm font-semibold uppercase tracking-[0.2em] text-pink-500">
                  Testimonios
                </p>
                <h2 className="mt-3 text-3xl font-bold text-gray-900 md:text-4xl">
                  Lo que dicen nuestros clientes
                </h2>
              </div>
              <div className="mt-12 grid gap-6 md:grid-cols-3">
                {cfg.testimonials.slice(0, 3).map((t, i) => (
                  <div
                    key={i}
                    className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm"
                  >
                    <div
                      className="flex items-center gap-1"
                      aria-label={`${t.rating ?? 5} de 5 estrellas`}
                    >
                      {Array.from({ length: 5 }, (_, j) => (
                        <svg
                          key={j}
                          className={`h-4 w-4 ${j < (t.rating ?? 5) ? 'text-amber-400' : 'text-gray-200'}`}
                          fill="currentColor"
                          viewBox="0 0 20 20"
                        >
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                      ))}
                    </div>
                    <p className="mt-3 text-sm leading-relaxed text-gray-600 italic">
                      &ldquo;{t.text}&rdquo;
                    </p>
                    <p className="mt-3 text-sm font-semibold text-gray-800">
                      {t.name || 'Cliente'}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </section>
        )}

        <section className="py-16 md:py-24" style={{ backgroundColor: '#fdfaf7' }}>
          <div className="mx-auto max-w-6xl px-4">
            <div
              className="overflow-hidden rounded-[2rem] shadow-sm"
              style={{ background: 'linear-gradient(135deg, #fdf2f8, #fff1f2)' }}
            >
              <div className="flex flex-col md:flex-row md:items-center">
                <div className="aspect-[4/3] md:aspect-auto md:w-2/5">
                  <LandingImage
                    src={STATIC_IMAGES.callToActionReception}
                    fallbackSrc={DEFAULT_LANDING_IMAGE}
                    alt=""
                    className="pointer-events-none h-full w-full"
                    width={644}
                    height={324}
                    objectFit="cover"
                  />
                </div>
                <div className="flex flex-1 flex-col items-center px-6 py-8 text-center md:items-start md:px-10 md:text-left">
                  <h2 className="text-2xl font-bold text-gray-900 md:text-3xl">
                    ¿Lista para tu mejor versión?
                  </h2>
                  <p className="mt-3 max-w-md text-base text-gray-600">
                    Agenda tu evaluación y comienza hoy tu transformación.
                  </p>
                  <div className="mt-6 flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
                    <button
                      type="button"
                      onClick={() => handleAgendaRedirect()}
                      className="inline-flex items-center justify-center gap-2 rounded-2xl bg-pink-500 px-6 py-3 font-bold text-white shadow transition hover:bg-pink-600"
                    >
                      <CalendarIcon className="h-5 w-5" />
                      Agenda en línea
                    </button>
                    <button
                      type="button"
                      onClick={() => handleWhatsAppRedirect('llamado')}
                      className="inline-flex items-center justify-center gap-2 rounded-2xl border-2 border-[#25D366] bg-white px-6 py-3 font-bold text-[#20BD5A] transition hover:bg-green-50"
                    >
                      <WhatsAppIcon className="h-5 w-5" />
                      Escríbenos por WhatsApp
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section
          id="contacto"
          ref={(el) => {
            sectionRefs.current.contacto = el
          }}
          className="scroll-mt-20 py-16 md:py-24"
        >
          <div className="mx-auto max-w-6xl px-4">
            <div className="mx-auto max-w-2xl text-center">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-pink-500">
                Contacto
              </p>
              <h2 className="mt-3 text-3xl font-bold text-gray-900 md:text-4xl">
                Visítanos o escríbenos
              </h2>
            </div>
            <div className="mt-12 grid gap-8 md:grid-cols-2">
              <div className="space-y-6">
                {firstLocation && (
                  <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
                    <h3 className="text-lg font-semibold text-gray-900">{firstLocation.name}</h3>
                    <div className="mt-4 space-y-3 text-sm text-gray-600">
                      {firstLocation.address && (
                        <p className="flex items-start gap-2">
                          <svg
                            className="mt-0.5 h-4 w-4 shrink-0 text-pink-500"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth={2}
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                            />
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                            />
                          </svg>
                          <span>
                            {firstLocation.address}
                            {firstLocation.commune ? `, ${firstLocation.commune}` : ''}
                            {firstLocation.city ? `, ${firstLocation.city}` : ''}
                          </span>
                        </p>
                      )}
                      {company.phone && (
                        <p className="flex items-center gap-2">
                          <svg
                            className="h-4 w-4 shrink-0 text-pink-500"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth={2}
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"
                            />
                          </svg>
                          <span>{company.phone}</span>
                        </p>
                      )}
                      {company.email && (
                        <p className="flex items-center gap-2">
                          <svg
                            className="h-4 w-4 shrink-0 text-pink-500"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth={2}
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                            />
                          </svg>
                          <span>{company.email}</span>
                        </p>
                      )}
                      <p className="flex items-center gap-2 text-gray-400">
                        <svg
                          className="h-4 w-4 shrink-0"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth={2}
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                          />
                        </svg>
                        <span>{firstLocation.timezone || 'America/Santiago'}</span>
                      </p>
                    </div>
                  </div>
                )}

                <button
                  onClick={() => handleWhatsAppRedirect('contacto')}
                  className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#25D366] px-6 py-4 text-base font-bold text-white shadow transition hover:bg-[#20BD5A]"
                >
                  <WhatsAppIcon className="h-5 w-5" />
                  Escríbenos por WhatsApp
                </button>
              </div>

              <div className="overflow-hidden rounded-2xl shadow-sm">
                <LandingImage
                  src={STATIC_IMAGES.map}
                  fallbackSrc={DEFAULT_LANDING_IMAGE}
                  alt={`Mapa de ubicación de ${company.name}`}
                  className="w-full"
                  width={340}
                  height={250}
                  objectFit="cover"
                />
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-gray-100" style={{ backgroundColor: '#1e1e2e' }}>
        <div className="mx-auto max-w-6xl px-4 py-12">
          <div className="grid gap-8 md:grid-cols-4">
            <div className="md:col-span-2">
              <div className="flex items-center gap-2">
                {footerLogoUrl ? (
                  <img
                    alt={`Isotipo de ${company.name}`}
                    className="h-10 w-10 rounded-lg object-contain"
                    src={footerLogoUrl}
                  />
                ) : (
                  <div
                    className="flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold text-white"
                    style={{ background: `linear-gradient(135deg, ${primary}, ${secondary})` }}
                  >
                    {company.name ? company.name.charAt(0).toUpperCase() : '?'}
                  </div>
                )}
                <span className="text-base font-bold text-white">
                  {company.businessName || company.name}
                </span>
              </div>
              <p className="mt-4 text-sm leading-relaxed text-gray-400">
                {cfg.aboutText || 'Expertos en belleza y bienestar.'}
              </p>
            </div>
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-gray-300">
                Enlaces
              </h3>
              <ul className="mt-4 space-y-2">
                {SECTIONS.map((sec) => (
                  <li key={sec}>
                    <button
                      onClick={() => scrollToSection(sec)}
                      className="text-sm text-gray-400 transition hover:text-white"
                    >
                      {sec === 'inicio' ? 'Inicio' : sec.charAt(0).toUpperCase() + sec.slice(1)}
                    </button>
                  </li>
                ))}
              </ul>
            </div>
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-gray-300">
                Contacto
              </h3>
              <ul className="mt-4 space-y-2 text-sm text-gray-400">
                {company.phone && <li>{company.phone}</li>}
                {company.email && <li className="break-all">{company.email}</li>}
                {firstLocation?.address && <li>{firstLocation.address}</li>}
                <li>
                  <button
                    onClick={() => handleWhatsAppRedirect('flotante')}
                    className="inline-flex items-center gap-1.5 text-[#25D366] transition hover:text-[#20BD5A]"
                  >
                    <WhatsAppIcon className="h-3.5 w-3.5" />
                    Escríbenos
                  </button>
                </li>
              </ul>
            </div>
          </div>
          <div className="mt-10 border-t border-gray-700/50 pt-6 text-center text-sm text-gray-500">
            <p>
              &copy; {new Date().getFullYear()} {company.name || 'Centro Estético'} &mdash; Todos
              los derechos reservados.
            </p>
            <p className="mt-1">
              <Link to="/terminos" className="hover:text-gray-300">
                Términos y condiciones
              </Link>
              {' | '}
              <Link to="/privacidad" className="hover:text-gray-300">
                Política de privacidad
              </Link>
            </p>
          </div>
        </div>
      </footer>

      <button
        type="button"
        onClick={() => handleWhatsAppRedirect('flotante')}
        className="fixed bottom-6 right-6 z-50 flex h-16 w-16 items-center justify-center rounded-full bg-transparent shadow-2xl transition hover:scale-105 hover:shadow-xl focus:outline-none focus:ring-4 focus:ring-[#25D366]/50"
        style={{ bottom: 'calc(1.5rem + env(safe-area-inset-bottom, 0px))' }}
        aria-label={`Abrir conversación de WhatsApp con ${company.name}`}
      >
        <img
          src={STATIC_IMAGES.whatsappFloating}
          alt=""
          aria-hidden="true"
          className="h-full w-full rounded-full object-contain"
        />
      </button>
    </div>
  )
}

function CalendarIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <rect height="14" rx="3" strokeWidth="2" width="14" x="5" y="6" />
      <path d="M8 4V8" strokeLinecap="round" strokeWidth="2" />
      <path d="M16 4V8" strokeLinecap="round" strokeWidth="2" />
      <path d="M3 10h18" strokeLinecap="round" strokeWidth="2" />
    </svg>
  )
}

function WhatsAppIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
    </svg>
  )
}
