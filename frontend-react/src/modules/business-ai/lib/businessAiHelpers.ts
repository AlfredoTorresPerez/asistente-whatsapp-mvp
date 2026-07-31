export const BUSINESS_AI_AUDIT_PAGE_SIZE = 5

export type BusinessHoursDay = {
  day: string
  startTime: string
  endTime: string
}

export const DEFAULT_BUSINESS_HOURS: BusinessHoursDay[] = [
  { day: 'Lunes', startTime: '09:00', endTime: '18:00' },
  { day: 'Martes', startTime: '09:00', endTime: '18:00' },
  { day: 'Miércoles', startTime: '09:00', endTime: '18:00' },
  { day: 'Jueves', startTime: '09:00', endTime: '18:00' },
  { day: 'Viernes', startTime: '09:00', endTime: '18:00' },
  { day: 'Sábado', startTime: '09:00', endTime: '13:00' },
  { day: 'Domingo', startTime: '', endTime: '' },
]

export type AuditLike = {
  createdAt: string
}

export function sortAuditLogsDescending<T extends AuditLike>(logs: T[]) {
  return [...logs].sort((left, right) => {
    const leftTime = new Date(left.createdAt).getTime()
    const rightTime = new Date(right.createdAt).getTime()
    return rightTime - leftTime
  })
}

export function paginateAuditLogs<T>(
  logs: T[],
  page: number,
  pageSize = BUSINESS_AI_AUDIT_PAGE_SIZE,
) {
  const safePage = Math.max(0, page)
  return logs.slice(safePage * pageSize, safePage * pageSize + pageSize)
}

export function getAuditTotalPages(totalItems: number, pageSize = BUSINESS_AI_AUDIT_PAGE_SIZE) {
  return Math.max(1, Math.ceil(totalItems / pageSize))
}

export function matchesSearch(row: { title: string; category: string; description: string }, search: string) {
  if (!search.trim()) return true
  const normalizedSearch = search.trim().toLowerCase()
  return [row.title, row.category, row.description].join(' ').toLowerCase().includes(normalizedSearch)
}

export function isBusinessHourRangeValid(day: BusinessHoursDay) {
  if (!day.startTime && !day.endTime) {
    return true
  }

  if (!day.startTime || !day.endTime) {
    return false
  }

  return day.startTime < day.endTime
}

type BuildPromptConfig = {
  active: boolean
  mode: 'suggest' | 'auto'
  tone: 'Cercano' | 'Profesional' | 'Comercial'
  language: string
  escalationThreshold: string
  allowBooking: boolean
  allowPrices: boolean
  allowPromotions: boolean
  requireAvailabilityCheck: boolean
}

export function buildPrompt(config: BuildPromptConfig, assistantPrompt: string) {
  const toneText =
    config.tone === 'Profesional'
      ? 'formal, claro y técnicamente preciso'
      : config.tone === 'Comercial'
        ? 'orientado a venta, persuasivo y entusiasta'
        : 'cercano, cálido y con un toque humano'

  const modeText =
    config.mode === 'auto'
      ? 'Modo de operación: AUTO. El asistente puede responder automáticamente cuando su confianza es alta.'
      : 'Modo de operación: SUGERIR. El asistente solo sugiere respuestas para aprobación humana; nunca responde por sí mismo.'

  const languageText =
    config.language === 'en'
      ? 'Responde en inglés.'
      : config.language === 'pt'
        ? 'Responde en portugués.'
        : 'Responde en español.'

  const scopeLines = [
    config.allowBooking ? '- Gestionar citas, reservas, reprogramaciones y cancelaciones.' : '- No gestionar citas ni reservas.',
    config.allowPrices ? '- Informar precios y cotizaciones.' : '- No informar precios ni cotizaciones.',
    config.allowPromotions ? '- Informar promociones vigentes.' : '- No promocionar ofertas ni descuentos.',
  ]

  return `${assistantPrompt.trim()}

Configuración operativa del negocio:
- Tono de atención: ${toneText}.
- ${languageText}
- ${modeText}
- Umbral de confianza para responder: ${config.escalationThreshold}%.
- ${config.requireAvailabilityCheck ? 'Debe consultar la agenda digital antes de confirmar disponibilidad.' : 'Puede responder sin consultar la agenda digital.'}
- Alcance permitido:
${scopeLines.join('\n')}
- Si el mensaje está fuera del alcance permitido o la confianza es menor al umbral, deriva a atención humana.
`
}
