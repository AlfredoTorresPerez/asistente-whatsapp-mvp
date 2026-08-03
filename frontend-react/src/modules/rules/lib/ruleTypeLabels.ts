const RULE_TYPE_LABELS: Record<string, string> = {
  AI_PROMPT: 'Prompt de IA',
  AI_RESPONSE: 'Respuesta IA',
  ALLOWED: 'Permitido',
  AVAILABILITY: 'Disponibilidad',
  BLOCKED: 'Bloqueado',
  CANCELACION: 'Cancelación',
  CATALOG: 'Catálogo',
  COMMERCIAL: 'Comercial',
  CONTRAINDICACION: 'Contraindicación',
  DISPONIBILIDAD: 'Disponibilidad',
  ESCALATION: 'Derivación humana',
  FAQ: 'Pregunta frecuente',
  GENERAL: 'General',
  HORARIO: 'Horario de atención',
  INTENT: 'Intención',
  KNOWLEDGE: 'Base de conocimiento',
  PAGO: 'Pago',
  POLICY: 'Política',
  PRICE: 'Precio',
  PROMOCION: 'Promoción',
  RECOMENDACION: 'Recomendación',
  REPROGRAMACION: 'Reprogramación',
  RESERVA: 'Reserva',
  SAFETY: 'Seguridad',
  SCHEDULE: 'Horario',
  SEGURIDAD: 'Seguridad',
  STOCK: 'Inventario',
  VENTA_CRUZADA: 'Venta cruzada',
}

export const DEFAULT_RULE_TYPE_OPTIONS = Object.keys(RULE_TYPE_LABELS).sort()

export function formatRuleType(ruleType: string | null | undefined) {
  const normalized = (ruleType ?? '').trim()

  if (!normalized) {
    return 'Sin clasificar'
  }

  if (RULE_TYPE_LABELS[normalized]) {
    return RULE_TYPE_LABELS[normalized]
  }

  const readable = normalized
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')

  return readable || 'Sin clasificar'
}
