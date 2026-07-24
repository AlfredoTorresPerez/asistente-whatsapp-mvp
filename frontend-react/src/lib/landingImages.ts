export const IMAGES = {
  HERO: '/images/centro-estetica-bella.png',
  HERO_WEBP: '/images/centro-estetica-bella.webp',
  HERO_SECONDARY: '/images/hero_tratamiento_facial.png',
  INTERIOR: '/images/interior_centro_estetico.png',
  MAPA: '/images/mapa_centro_estetica_bella.png',
  PROMOCION: '/images/promocion_limpieza_facial.png',
  SERVICIO_ALISADO: '/images/servicio_alisado.png',
  SERVICIO_ASESORIA: '/images/servicio_asesoria_estetica.png',
  SERVICIO_BABYFACE: '/images/servicio_babyface.png',
  SERVICIO_BUSHING: '/images/servicio_bushing.png',
} as const

export const DEFAULT_LANDING_IMAGE = IMAGES.HERO_SECONDARY

type ServiceImageParams = {
  slug?: string | null
  categorySlug?: string | null
  serviceCode?: string | null
  imageUrl?: string | null
}

const serviceImageMap: Record<string, string> = {
  'alisado': IMAGES.SERVICIO_ALISADO,
  'asesoria-estetica': IMAGES.SERVICIO_ASESORIA,
  'asesoria_estetica': IMAGES.SERVICIO_ASESORIA,
  'babyface': IMAGES.SERVICIO_BABYFACE,
  'baby-face': IMAGES.SERVICIO_BABYFACE,
  'bushing': IMAGES.SERVICIO_BUSHING,
  'limpieza-facial': IMAGES.SERVICIO_BABYFACE,
  'limpiezas-faciales': IMAGES.SERVICIO_BABYFACE,
  'hidratacion': IMAGES.SERVICIO_ALISADO,
  'hidratación': IMAGES.SERVICIO_ALISADO,
  'depilacion': IMAGES.SERVICIO_BUSHING,
  'depilación': IMAGES.SERVICIO_BUSHING,
  'tratamientos-esteticos': IMAGES.SERVICIO_ASESORIA,
  'tratamientos-estéticos': IMAGES.SERVICIO_ASESORIA,
}

export function resolveServiceImage(service: ServiceImageParams): string {
  if (service.imageUrl) {
    return service.imageUrl
  }

  const key = service.slug ?? service.categorySlug ?? service.serviceCode
  if (key && serviceImageMap[key]) {
    return serviceImageMap[key]
  }

  return DEFAULT_LANDING_IMAGE
}
