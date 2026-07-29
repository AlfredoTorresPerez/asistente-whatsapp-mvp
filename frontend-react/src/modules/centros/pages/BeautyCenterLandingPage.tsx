import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { LandingImage } from '../../../components/ui/LandingImage'
import { buildPublicWhatsAppUrl } from '../../../lib/whatsapp'
import { IMAGES, DEFAULT_LANDING_IMAGE } from '../../../lib/landingImages'
import {
  usePublicLandingContent,
  usePublicServicesContent,
  usePublicCategoriesContent,
} from '../hooks/usePublicContent'
import type { PublicContentItemResponse } from '../../../services/api/types'

const WHATSAPP_MESSAGE_DEFAULT =
  'Hola, quiero solicitar información y agendar una hora en Centro Estética Bella.'

const FALLBACK_SERVICES = [
  {
    name: 'Limpiezas faciales',
    slug: 'limpiezas-faciales',
    description: 'Tratamientos diseñados para limpiar, renovar y mejorar la apariencia de tu piel.',
    message:
      'Hola, quiero información sobre el servicio de limpieza facial de Centro Estética Bella.',
  },
  {
    name: 'Hidratación',
    slug: 'hidratacion',
    description: 'Recupera luminosidad, suavidad y equilibrio con tratamientos personalizados.',
    message: 'Hola, quiero información sobre el servicio de hidratación de Centro Estética Bella.',
  },
  {
    name: 'Depilación',
    slug: 'depilacion',
    description: 'Alternativas profesionales para una piel suave y cuidada.',
    message: 'Hola, quiero información sobre el servicio de depilación de Centro Estética Bella.',
  },
  {
    name: 'Tratamientos estéticos',
    slug: 'tratamientos-esteticos',
    description: 'Servicios adaptados a tus necesidades y objetivos de bienestar.',
    message: 'Hola, quiero información sobre los tratamientos estéticos de Centro Estética Bella.',
  },
  {
    name: 'Alisado',
    slug: 'alisado',
    description: 'Tratamiento profesional para alisar y domesticar el cabello.',
    message: 'Hola, quiero información sobre el servicio de alisado de Centro Estética Bella.',
  },
  {
    name: 'Asesoría estética',
    slug: 'asesoria-estetica',
    description: 'Orientación personalizada para elegir los tratamientos ideales para ti.',
    message: 'Hola, quiero información sobre la asesoría estética de Centro Estética Bella.',
  },
  {
    name: 'Baby face',
    slug: 'babyface',
    description: 'Tratamiento facial revitalizante para una apariencia fresca y juvenil.',
    message: 'Hola, quiero información sobre el tratamiento baby face de Centro Estética Bella.',
  },
  {
    name: 'Bushing',
    slug: 'bushing',
    description: 'Técnica especializada para el cuidado y modelado de cejas y pestañas.',
    message: 'Hola, quiero información sobre el servicio de bushing de Centro Estética Bella.',
  },
]

const BENEFITS = [
  {
    icon: '👩‍⚕️',
    title: 'Profesionales expertas',
    text: 'Equipo altamente capacitado con años de experiencia en estética y bienestar.',
  },
  {
    icon: '✨',
    title: 'Atención personalizada',
    text: 'Cada tratamiento se adapta a tus necesidades y objetivos específicos.',
  },
  {
    icon: '🏆',
    title: 'Productos de calidad',
    text: 'Trabajamos con marcas premium para garantizar los mejores resultados.',
  },
  {
    icon: '🏠',
    title: 'Ambiente cómodo y seguro',
    text: 'Un espacio pensado para que te sientas como en casa.',
  },
  {
    icon: '💬',
    title: 'Reserva rápida por WhatsApp',
    text: 'Agenda tu cita en minutos a través de nuestro canal directo.',
  },
]

const PROMOTIONS = [
  {
    name: 'Limpieza facial',
    description: 'Limpieza facial profunda con productos premium.',
    originalPrice: '$35.000',
    promoPrice: '$25.000',
    validUntil: '31 de agosto, 2026',
    message:
      'Hola, quiero información sobre la promoción de limpieza facial de Centro Estética Bella.',
  },
]

function openWhatsApp(message?: string) {
  const url = buildPublicWhatsAppUrl(message)
  if (url) {
    window.open(url, '_blank', 'noopener,noreferrer')
  }
}

function WhatsAppIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
    </svg>
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

function StarIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
    </svg>
  )
}

function ScrollLink({
  targetId,
  children,
  className,
}: {
  targetId: string
  children: React.ReactNode
  className?: string
}) {
  const handleClick = useCallback(() => {
    const el = document.getElementById(targetId)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [targetId])

  return (
    <button onClick={handleClick} className={className} type="button">
      {children}
    </button>
  )
}

function ServiceCard({
  imageUrl,
  text,
  message,
}: {
  imageUrl: string | null
  text: string
  message: string
}) {
  return (
    <article className="group flex flex-col rounded-2xl border border-rose-100/60 bg-white shadow-sm transition hover:shadow-md">
      <div className="aspect-[4/3] overflow-hidden rounded-t-2xl">
        <LandingImage
          src={imageUrl}
          fallbackSrc={IMAGES.HERO_SECONDARY}
          alt={text}
          className="h-full w-full transition duration-300 group-hover:scale-105"
          width={340}
          height={255}
          objectFit="cover"
        />
      </div>
      <div className="flex flex-1 flex-col p-5">
        <p className="mt-0 flex-1 text-sm leading-relaxed text-gray-600">{text}</p>
        <button
          onClick={() => openWhatsApp(message)}
          className="mt-4 w-full rounded-xl bg-gray-50 py-2.5 text-xs font-semibold text-gray-600 transition hover:bg-[#25D366] hover:text-white"
        >
          Consultar por WhatsApp
        </button>
      </div>
    </article>
  )
}

function ContentCard({ item }: { item: PublicContentItemResponse }) {
  return (
    <article className="group flex flex-col rounded-2xl border border-rose-100/60 bg-white shadow-sm transition hover:shadow-md">
      {item.imageUrl && (
        <div className="aspect-[4/3] overflow-hidden rounded-t-2xl">
          <LandingImage
            src={item.imageUrl}
            fallbackSrc={DEFAULT_LANDING_IMAGE}
            alt={item.text}
            className="h-full w-full transition duration-300 group-hover:scale-105"
            width={340}
            height={255}
            objectFit="cover"
          />
        </div>
      )}
      <div className="flex flex-1 flex-col p-5">
        <p className="flex-1 text-sm leading-relaxed text-gray-600">{item.text}</p>
      </div>
    </article>
  )
}

function LandingSectionHeader({
  label,
  title,
  description,
}: {
  label?: string
  title: string
  description?: string
}) {
  return (
    <div className="mx-auto max-w-2xl text-center">
      {label && (
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-rose-400">{label}</p>
      )}
      <h2 className="mt-3 text-3xl font-bold text-gray-900 md:text-4xl">{title}</h2>
      {description && <p className="mt-3 text-gray-600">{description}</p>}
    </div>
  )
}

export function BeautyCenterLandingPage() {
  const { items: landingItems, isLoading: landingLoading } = usePublicLandingContent()
  const { items: serviceItems, isLoading: servicesLoading } = usePublicServicesContent()
  const { items: categoryItems, isLoading: categoriesLoading } = usePublicCategoriesContent()

  const hasLandingData = !landingLoading && landingItems.length > 0
  const hasServiceData = !servicesLoading && serviceItems.length > 0

  const heroLandingItem = landingItems.find((i) => i.imageUrl) ?? landingItems[0]
  const heroImageUrl = heroLandingItem?.imageUrl ?? IMAGES.HERO

  return (
    <div className="min-h-screen bg-[#fefcfb] font-sans text-gray-800">
      <a
        href="#inicio"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-xl focus:bg-white focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-rose-600 focus:shadow-lg"
      >
        Saltar al contenido principal
      </a>

      <header className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 md:py-6">
        <div className="flex items-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-rose-300 to-rose-500 text-sm font-bold text-white shadow-sm">
            CB
          </div>
          <span className="text-base font-semibold tracking-tight text-gray-800">
            Centro Estética Bella
          </span>
        </div>
        <nav className="hidden items-center gap-6 md:flex" aria-label="Navegación principal">
          <ScrollLink
            targetId="servicios"
            className="text-sm font-medium text-gray-500 transition hover:text-rose-600"
          >
            Servicios
          </ScrollLink>
          <ScrollLink
            targetId="beneficios"
            className="text-sm font-medium text-gray-500 transition hover:text-rose-600"
          >
            Beneficios
          </ScrollLink>
          <ScrollLink
            targetId="promociones"
            className="text-sm font-medium text-gray-500 transition hover:text-rose-600"
          >
            Promociones
          </ScrollLink>
          <ScrollLink
            targetId="contacto"
            className="text-sm font-medium text-gray-500 transition hover:text-rose-600"
          >
            Contacto
          </ScrollLink>
          <button
            onClick={() => openWhatsApp(WHATSAPP_MESSAGE_DEFAULT)}
            className="inline-flex items-center gap-2 rounded-xl bg-[#25D366] px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#20BD5A]"
            aria-label="Abrir conversación de WhatsApp con Centro Estética Bella"
          >
            <WhatsAppIcon className="h-4 w-4" />
            Agendar por WhatsApp
          </button>
        </nav>
        <button
          onClick={() => openWhatsApp(WHATSAPP_MESSAGE_DEFAULT)}
          className="inline-flex items-center gap-2 rounded-xl bg-[#25D366] px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-[#20BD5A] md:hidden"
          aria-label="Abrir conversación de WhatsApp"
        >
          <WhatsAppIcon className="h-4 w-4" />
          Agendar
        </button>
      </header>

      <main>
        <section id="inicio" className="mx-auto max-w-7xl px-4 pb-12 pt-8 md:pb-20 md:pt-16">
          <div className="grid items-center gap-10 md:grid-cols-2 md:gap-16">
            <div className="order-2 md:order-1">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-rose-400">
                Centro Estética Bella
              </p>
              <h1 className="mt-4 text-4xl font-bold leading-tight tracking-tight text-gray-900 md:text-5xl lg:text-6xl">
                {hasLandingData
                  ? landingItems[0].text
                  : 'Realza tu belleza y disfruta una experiencia pensada para ti'}
              </h1>
              <p className="mt-4 max-w-xl text-base leading-relaxed text-gray-600 md:text-lg">
                {hasLandingData && landingItems.length > 1
                  ? landingItems[1].text
                  : 'En Centro Estética Bella ofrecemos tratamientos faciales, hidratación, depilación y cuidado estético personalizado, en un ambiente profesional, cálido y seguro.'}
              </p>
              <div className="mt-8 flex flex-wrap items-center gap-4">
                <Link
                  to="/reservar"
                  className="inline-flex items-center gap-2.5 rounded-xl bg-[#2563EB] px-8 py-4 text-base font-bold text-white shadow-lg transition hover:bg-[#1D4ED8]"
                >
                  <CalendarIcon className="h-5 w-5" />
                  Agenda en línea
                </Link>
                <ScrollLink
                  targetId="servicios"
                  className="inline-flex items-center gap-2 rounded-xl border-2 border-gray-200 bg-white px-8 py-4 text-base font-semibold text-gray-700 shadow-sm transition hover:border-rose-200 hover:text-rose-600"
                >
                  Ver servicios
                </ScrollLink>
              </div>
            </div>
            <div className="order-1 md:order-2">
              <div className="relative overflow-hidden rounded-[2rem] shadow-xl">
                <picture>
                  <source srcSet={IMAGES.HERO_WEBP} type="image/webp" />
                  <LandingImage
                    src={heroImageUrl}
                    fallbackSrc={DEFAULT_LANDING_IMAGE}
                    alt="Centro Estética Bella, servicios de belleza, bienestar y cuidado profesional"
                    className="w-full"
                    loading="eager"
                    fetchPriority="high"
                    width={1491}
                    height={1055}
                    objectFit="cover"
                  />
                </picture>
              </div>
            </div>
          </div>
        </section>

        {hasLandingData && landingItems.length > 1 && (
          <section className="bg-white py-16 md:py-24">
            <div className="mx-auto max-w-7xl px-4">
              <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                {landingItems.slice(1).map((item) => (
                  <ContentCard key={item.id} item={item} />
                ))}
              </div>
            </div>
          </section>
        )}

        <section id="servicios" className="scroll-mt-16 bg-[#fdf8f6] py-16 md:py-24">
          <div className="mx-auto max-w-7xl px-4">
            <LandingSectionHeader
              label="Servicios"
              title="Nuestros tratamientos"
              description="Descubre nuestra amplia gama de servicios diseñados para realzar tu belleza natural."
            />
            <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {hasServiceData
                ? serviceItems.map((item) => (
                    <ServiceCard
                      key={item.id}
                      imageUrl={item.imageUrl}
                      text={item.text}
                      message={`Hola, quiero información sobre este servicio de Centro Estética Bella.`}
                    />
                  ))
                : FALLBACK_SERVICES.map((service) => (
                    <article
                      key={service.name}
                      className="group flex flex-col rounded-2xl border border-rose-100/60 bg-white shadow-sm transition hover:shadow-md"
                    >
                      <div className="aspect-[4/3] overflow-hidden rounded-t-2xl">
                        <LandingImage
                          src={null}
                          fallbackSrc={IMAGES.HERO_SECONDARY}
                          alt={`${service.name} en Centro Estética Bella`}
                          className="h-full w-full transition duration-300 group-hover:scale-105"
                          width={340}
                          height={255}
                          objectFit="cover"
                        />
                      </div>
                      <div className="flex flex-1 flex-col p-5">
                        <h3 className="text-lg font-semibold text-gray-900">{service.name}</h3>
                        <p className="mt-2 flex-1 text-sm leading-relaxed text-gray-500">
                          {service.description}
                        </p>
                        <button
                          onClick={() => openWhatsApp(service.message)}
                          className="mt-4 w-full rounded-xl bg-gray-50 py-2.5 text-xs font-semibold text-gray-600 transition hover:bg-[#25D366] hover:text-white"
                        >
                          Consultar por WhatsApp
                        </button>
                      </div>
                    </article>
                  ))}
            </div>
          </div>
        </section>

        {!categoriesLoading && categoryItems.length > 0 && (
          <section id="categorias" className="scroll-mt-16 bg-white py-16 md:py-24">
            <div className="mx-auto max-w-7xl px-4">
              <LandingSectionHeader
                label="Categorías"
                title="Explora por categoría"
                description="Encuentra el tratamiento perfecto para ti."
              />
              <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
                {categoryItems.map((item) => (
                  <ContentCard key={item.id} item={item} />
                ))}
              </div>
            </div>
          </section>
        )}

        <section id="sobre-nosotros" className="scroll-mt-16 bg-white py-16 md:py-24">
          <div className="mx-auto max-w-7xl px-4">
            <div className="grid items-center gap-10 md:grid-cols-2 md:gap-16">
              <div className="order-2 md:order-1">
                <p className="text-sm font-semibold uppercase tracking-[0.2em] text-rose-400">
                  Sobre nosotros
                </p>
                <h2 className="mt-3 text-3xl font-bold text-gray-900 md:text-4xl">
                  Un espacio pensado para tu bienestar
                </h2>
                <p className="mt-4 text-base leading-relaxed text-gray-600">
                  En Centro Estética Bella combinamos experiencia, calidez y profesionalismo para
                  ofrecerte una experiencia única de cuidado personal. Nuestras instalaciones están
                  diseñadas para que te sientas cómoda y segura desde el momento en que ingresas.
                </p>
                <p className="mt-3 text-base leading-relaxed text-gray-600">
                  Contamos con cabinas equipadas con tecnología de última generación y un equipo de
                  profesionales apasionados por la estética y el bienestar.
                </p>
              </div>
              <div className="order-1 md:order-2">
                <div className="overflow-hidden rounded-[2rem] shadow-xl">
                  <LandingImage
                    src={IMAGES.INTERIOR}
                    fallbackSrc={DEFAULT_LANDING_IMAGE}
                    alt="Interior de Centro Estética Bella, cabina de atención profesional"
                    className="w-full"
                    width={340}
                    height={265}
                    objectFit="cover"
                  />
                </div>
              </div>
            </div>
          </div>
        </section>

        <section id="beneficios" className="scroll-mt-16 bg-white py-16 md:py-24">
          <div className="mx-auto max-w-7xl px-4">
            <LandingSectionHeader
              title="¿Por qué elegirnos?"
              description="Nos esforzamos por ofrecer la mejor experiencia de cuidado personal."
            />
            <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-5">
              {BENEFITS.map((benefit) => (
                <div
                  key={benefit.title}
                  className="rounded-2xl border border-rose-50 bg-white p-6 text-center shadow-sm"
                >
                  <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-100 to-amber-200 text-2xl shadow-sm">
                    {benefit.icon}
                  </div>
                  <h3 className="mt-4 text-base font-semibold text-gray-900">{benefit.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-500">{benefit.text}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="promociones" className="scroll-mt-16 bg-[#fdf8f6] py-16 md:py-24">
          <div className="mx-auto max-w-7xl px-4">
            <LandingSectionHeader
              label="Promociones"
              title="Ofertas especiales"
              description="Aprovecha nuestros precios promocionales por tiempo limitado."
            />
            <div className="mt-12 grid gap-8 md:grid-cols-2 lg:grid-cols-3">
              {PROMOTIONS.map((promo) => (
                <article
                  key={promo.name}
                  className="group overflow-hidden rounded-2xl border border-rose-100/60 bg-white shadow-sm transition hover:shadow-md"
                >
                  <div className="aspect-[16/9] overflow-hidden">
                    <LandingImage
                      src={IMAGES.PROMOCION}
                      fallbackSrc={DEFAULT_LANDING_IMAGE}
                      alt={`Promoción de ${promo.name} en Centro Estética Bella`}
                      className="h-full w-full transition duration-300 group-hover:scale-105"
                      width={415}
                      height={225}
                      objectFit="cover"
                    />
                  </div>
                  <div className="p-5">
                    <h3 className="text-lg font-semibold text-gray-900">{promo.name}</h3>
                    <p className="mt-2 text-sm leading-relaxed text-gray-500">
                      {promo.description}
                    </p>
                    <div className="mt-3 flex items-baseline gap-2">
                      <span className="text-sm text-gray-400 line-through">
                        {promo.originalPrice}
                      </span>
                      <span className="text-xl font-bold text-rose-500">{promo.promoPrice}</span>
                    </div>
                    <p className="mt-1 text-xs text-gray-400">Válido hasta {promo.validUntil}</p>
                    <button
                      onClick={() => openWhatsApp(promo.message)}
                      className="mt-4 w-full rounded-xl bg-[#25D366] py-2.5 text-xs font-semibold text-white transition hover:bg-[#20BD5A]"
                    >
                      Consultar por WhatsApp
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="bg-gradient-to-br from-rose-50 via-white to-amber-50 py-16 md:py-24">
          <div className="mx-auto max-w-3xl px-4 text-center">
            <h2 className="text-3xl font-bold text-gray-900 md:text-4xl">
              Tu momento de bienestar comienza aquí
            </h2>
            <p className="mt-4 text-lg text-gray-600">
              Conversa directamente con Centro Estética Bella y encuentra el tratamiento adecuado
              para ti.
            </p>
            <button
              onClick={() => openWhatsApp(WHATSAPP_MESSAGE_DEFAULT)}
              className="mt-8 inline-flex items-center gap-2.5 rounded-xl bg-[#25D366] px-8 py-4 text-lg font-bold text-white shadow-lg transition hover:bg-[#20BD5A]"
            >
              <WhatsAppIcon className="h-5 w-5" />
              Reservar mi hora
            </button>
          </div>
        </section>

        <section id="contacto" className="scroll-mt-16 bg-white py-16 md:py-24">
          <div className="mx-auto max-w-7xl px-4">
            <LandingSectionHeader label="Contacto" title="Visítanos" />
            <div className="mt-12 grid items-start gap-10 md:grid-cols-2 md:gap-16">
              <div>
                <div className="space-y-4 text-base text-gray-600">
                  <div>
                    <h3 className="font-semibold text-gray-900">Dirección</h3>
                    <p>Av. Los Pajaritos 1234, Local 5</p>
                    <p>Maipú, Santiago</p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Horario de atención</h3>
                    <p>Lunes a viernes: 10:00 – 19:00</p>
                    <p>Sábado: 10:00 – 14:00</p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Teléfono</h3>
                    <p>+56 9 2730 5158</p>
                  </div>
                  <button
                    onClick={() => openWhatsApp(WHATSAPP_MESSAGE_DEFAULT)}
                    className="mt-4 inline-flex items-center gap-2 rounded-xl bg-[#25D366] px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#20BD5A]"
                    aria-label="Contactar por WhatsApp"
                  >
                    <WhatsAppIcon className="h-4 w-4" />
                    Contactar por WhatsApp
                  </button>
                </div>
              </div>
              <div className="overflow-hidden rounded-[2rem] shadow-xl">
                <LandingImage
                  src={IMAGES.MAPA}
                  fallbackSrc={DEFAULT_LANDING_IMAGE}
                  alt="Mapa de ubicación de Centro Estética Bella en Maipú, Santiago"
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

      <footer className="border-t border-rose-100 bg-white py-10 text-center text-sm text-gray-400">
        <div className="mx-auto max-w-7xl px-4">
          <div className="flex items-center justify-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-rose-300 to-rose-500 text-xs font-bold text-white shadow-sm">
              CB
            </div>
            <span className="text-sm font-semibold text-gray-700">Centro Estética Bella</span>
          </div>
          <p className="mt-4">
            &copy; {new Date().getFullYear()} Centro Estética Bella &mdash; Todos los derechos
            reservados.
          </p>
        </div>
      </footer>
    </div>
  )
}
