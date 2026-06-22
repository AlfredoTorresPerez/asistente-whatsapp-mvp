import { useMemo, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { OfflineBanner } from '../../components/feedback/OfflineBanner'
import { AppLogo } from '../../components/ui/AppLogo'
import { Select } from '../../components/ui/Select'
import { useOnlineStatus } from '../../lib/useOnlineStatus'

type PublicLayoutVariant = {
  heading: string
  accent: string
  description: string
  trustTitle: string
  trustText: string
  features: Array<{
    title: string
    description: string
    tone: 'blue' | 'green' | 'violet'
  }>
  footer: string
}

const LANGUAGE_OPTIONS = [
  { label: 'Espanol', value: 'es' },
  { label: 'English', value: 'en' },
  { label: 'Portugues', value: 'pt' },
]

const PUBLIC_LAYOUT_VARIANTS: Record<string, PublicLayoutVariant> = {
  '/login': {
    heading: 'Gestiona tu negocio desde conversaciones',
    accent: 'que generan resultados',
    description:
      'Automatiza conversaciones, prospectos, reservas y pedidos desde una sola plataforma.',
    trustTitle: 'Acceso seguro',
    trustText: 'Tus datos estan protegidos con cifrado de nivel empresarial.',
    features: [
      {
        title: 'Responde 24/7',
        description: 'Automatiza respuestas y nunca dejes a un cliente esperando.',
        tone: 'green',
      },
      {
        title: 'Captura prospectos',
        description: 'Convierte conversaciones en oportunidades de negocio.',
        tone: 'blue',
      },
      {
        title: 'Genera reportes',
        description: 'Toma decisiones con datos claros y en tiempo real.',
        tone: 'violet',
      },
    ],
    footer: 'Plataforma segura y confiable',
  },
  '/forgot-password': {
    heading: 'Recupera tu cuenta y manten tu negocio',
    accent: 'siempre en marcha',
    description:
      'Te ayudamos a recuperar el acceso a tu cuenta de forma segura y rapida.',
    trustTitle: 'Tu seguridad es nuestra prioridad',
    trustText: 'Usamos doble nivel empresarial para proteger tu informacion.',
    features: [
      {
        title: 'Recuperacion segura',
        description: 'Proceso verificado para proteger tus accesos.',
        tone: 'green',
      },
      {
        title: 'Acceso protegido',
        description: 'Tus datos y conversaciones estan cifrados con altos estandares.',
        tone: 'blue',
      },
      {
        title: 'Soporte rapido',
        description: 'Nuestro equipo esta listo para ayudarte si lo necesitas.',
        tone: 'violet',
      },
    ],
    footer: 'Proceso seguro y verificado',
  },
  '/forgot-password/sent': {
    heading: 'Revisa tu bandeja y recupera tu acceso con',
    accent: 'seguridad',
    description:
      'El enlace que enviamos te permitira volver a tu cuenta y continuar operando tu negocio sin interrupciones.',
    trustTitle: 'Tu seguridad es nuestra prioridad',
    trustText: 'Usamos doble nivel empresarial para proteger tu informacion.',
    features: [
      {
        title: 'Enlace seguro',
        description: 'Enviamos enlaces unicos que expiran para proteger la cuenta.',
        tone: 'green',
      },
      {
        title: 'Acceso protegido',
        description: 'Tus formularios y conversaciones estan siempre resguardados.',
        tone: 'blue',
      },
      {
        title: 'Soporte disponible',
        description: 'Nuestro equipo esta listo para ayudarte si lo necesitas.',
        tone: 'violet',
      },
    ],
    footer: 'Si no ves el correo, revisa spam o espera unos minutos.',
  },
  '/reset-password': {
    heading: 'Recupera tu cuenta y manten tu negocio',
    accent: 'siempre en marcha',
    description:
      'Te ayudamos a proteger tu cuenta con una nueva contrasena segura.',
    trustTitle: 'Tu seguridad es nuestra prioridad',
    trustText: 'Usamos doble nivel empresarial para proteger tu informacion.',
    features: [
      {
        title: 'Contrasena segura',
        description: 'Crea una contrasena fuerte y unica para proteger tu cuenta.',
        tone: 'green',
      },
      {
        title: 'Acceso protegido',
        description: 'Tus datos y conversaciones estan resguardados con altos estandares.',
        tone: 'blue',
      },
      {
        title: 'Proceso verificado',
        description: 'Este enlace es unico y temporal por tu seguridad.',
        tone: 'violet',
      },
    ],
    footer: 'Tu seguridad es nuestra prioridad',
  },
}

export function PublicLayout() {
  const isOnline = useOnlineStatus()
  const location = useLocation()
  const [language, setLanguage] = useState('es')
  const variant = useMemo(() => {
    if (location.pathname.startsWith('/reset-password')) {
      return PUBLIC_LAYOUT_VARIANTS['/reset-password']
    }

    return PUBLIC_LAYOUT_VARIANTS[location.pathname] ?? PUBLIC_LAYOUT_VARIANTS['/login']
  }, [location.pathname])

  return (
    <div className="min-h-screen bg-[#F7F8FC]">
      <OfflineBanner visible={!isOnline} />
      <div className="mx-auto grid min-h-screen max-w-[1660px] gap-6 px-4 py-4 lg:grid-cols-[0.92fr_1.08fr] lg:px-6 lg:py-6">
        <section className="relative overflow-hidden rounded-[32px] border border-white/10 bg-[radial-gradient(circle_at_top_left,rgba(36,83,255,0.18),transparent_35%),linear-gradient(180deg,#081A3A_0%,#0E2C63_100%)] px-6 py-6 text-white shadow-[0_28px_80px_rgba(15,23,42,0.2)] sm:px-8 sm:py-8 lg:px-8 lg:py-9">
          <div className="absolute inset-0 bg-[linear-gradient(140deg,transparent_0%,rgba(255,255,255,0.05)_42%,transparent_80%)]" />
          <div className="relative flex h-full flex-col">
            <AppLogo inverted />

            <div className="mt-10 max-w-[380px]">
              <h1 className="text-[34px] leading-[1.08] font-semibold">
                {variant.heading}{' '}
                <span className="text-[#4ADE80]">{variant.accent}</span>
              </h1>
              <p className="mt-5 text-sm leading-7 text-blue-50/82">{variant.description}</p>
            </div>

            <div className="mt-8 grid gap-3">
              {variant.features.map((feature) => (
                <div
                  key={feature.title}
                  className="flex items-start gap-3 rounded-[20px] border border-white/10 bg-white/6 px-4 py-4 backdrop-blur-sm"
                >
                  <span
                    className={[
                      'mt-0.5 inline-flex h-10 w-10 items-center justify-center rounded-2xl',
                      feature.tone === 'green'
                        ? 'bg-emerald-400/14 text-emerald-300'
                        : feature.tone === 'blue'
                          ? 'bg-blue-400/14 text-blue-200'
                          : 'bg-violet-400/14 text-violet-200',
                    ]
                      .join(' ')
                      .trim()}
                    aria-hidden="true"
                  >
                    <FeatureIcon tone={feature.tone} />
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-white">{feature.title}</p>
                    <p className="mt-1 text-xs leading-5 text-blue-50/72">
                      {feature.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-auto pt-8">
              <div className="rounded-[22px] border border-white/10 bg-white/6 px-4 py-4 backdrop-blur-sm">
                <div className="flex items-center gap-3">
                  <span className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-400/14 text-emerald-200">
                    <ShieldIcon />
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-white">{variant.trustTitle}</p>
                    <p className="mt-1 text-xs leading-5 text-blue-50/72">{variant.trustText}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="flex min-h-full flex-col justify-center rounded-[32px] border border-white/80 bg-[linear-gradient(180deg,#FFFFFF_0%,#F8FAFF_100%)] px-4 py-5 shadow-[0_24px_80px_rgba(15,23,42,0.08)] sm:px-7 sm:py-7 lg:px-8 lg:py-8">
          <div className="flex justify-end">
            <div className="w-full max-w-[144px]">
              <Select
                aria-label="Idioma"
                className="text-xs font-medium"
                name="public-language"
                onChange={(event) => setLanguage(event.target.value)}
                options={LANGUAGE_OPTIONS}
                value={language}
              />
            </div>
          </div>

          <div className="mx-auto flex w-full max-w-[430px] flex-1 items-center py-4 sm:py-6">
            <div className="w-full rounded-[28px] border border-[var(--color-border)] bg-white p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:p-8">
              <Outlet />
            </div>
          </div>

          <div className="pt-2 text-center text-xs text-slate-500">{variant.footer}</div>
        </section>
      </div>
    </div>
  )
}

function FeatureIcon({ tone }: { tone: 'blue' | 'green' | 'violet' }) {
  if (tone === 'green') {
    return (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 6V18" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
        <path d="M7 11L12 6L17 11" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
      </svg>
    )
  }

  if (tone === 'violet') {
    return (
      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path d="M7 17V9" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
        <path d="M12 17V5" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
        <path d="M17 17V12" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
      </svg>
    )
  }

  return (
    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <rect height="14" rx="3" stroke="currentColor" strokeWidth="2" width="14" x="5" y="5" />
      <path d="M9 9H15" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
      <path d="M9 13H13" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
    </svg>
  )
}

function ShieldIcon() {
  return (
    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 3L19 6V11.5C19 16.1 15.8 20.37 12 21.5C8.2 20.37 5 16.1 5 11.5V6L12 3Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
      <path d="M9.5 12.2L11.4 14.1L14.9 10.6" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" />
    </svg>
  )
}
