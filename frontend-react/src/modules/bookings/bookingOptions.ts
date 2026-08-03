export const bookingStatusOptions = [
  { value: 'SOLICITADA', label: 'Solicitada' },
  { value: 'PENDIENTE_CONFIRMACION', label: 'Pendiente de confirmación' },
  { value: 'CONFIRMADA', label: 'Confirmada' },
  { value: 'EN_ATENCION', label: 'En atención' },
  { value: 'COMPLETADA', label: 'Completada' },
  { value: 'REPROGRAMADA', label: 'Reprogramada' },
  { value: 'CANCELADA', label: 'Cancelada' },
  { value: 'NO_ASISTE', label: 'Inasistencia' },
  { value: 'EXPIRADA', label: 'Expirada' },
  { value: 'PENDIENTE_PAGO', label: 'Pendiente de pago' },
  { value: 'REPROGRAMACION_PENDIENTE', label: 'Reprogramación pendiente' },
  { value: 'CANCELADA_POR_CLIENTE', label: 'Cancelada por cliente' },
]

const bookingStatusAliases: Record<string, string> = {
  REQUESTED: 'SOLICITADA',
  TEMPORARY: 'PENDIENTE_CONFIRMACION',
  CONFIRMED: 'CONFIRMADA',
  SCHEDULED: 'CONFIRMADA',
  IN_PROGRESS: 'EN_ATENCION',
  RESCHEDULED: 'REPROGRAMADA',
  CANCELLED: 'CANCELADA',
  CANCELED: 'CANCELADA',
  COMPLETED: 'COMPLETADA',
  ATTENDED: 'COMPLETADA',
  ATENDIDA: 'COMPLETADA',
  NO_SHOW: 'NO_ASISTE',
  RELEASED: 'EXPIRADA',
  EXPIRED: 'EXPIRADA',
}

export function normalizeBookingStatus(status: string | null | undefined) {
  if (!status) return ''
  const normalized = status.trim().toUpperCase()
  return bookingStatusAliases[normalized] ?? normalized
}

export function getBookingStatusLabel(status: string) {
  const normalized = normalizeBookingStatus(status)
  return bookingStatusOptions.find((option) => option.value === normalized)?.label ?? normalized
}

export function getBookingStatusTone(status: string) {
  switch (normalizeBookingStatus(status)) {
    case 'SOLICITADA':
    case 'PENDIENTE_CONFIRMACION':
    case 'PENDIENTE_PAGO':
    case 'REPROGRAMACION_PENDIENTE':
      return 'warning'
    case 'CONFIRMADA':
    case 'COMPLETADA':
      return 'success'
    case 'EN_ATENCION':
      return 'info'
    case 'REPROGRAMADA':
      return 'info'
    case 'CANCELADA':
    case 'CANCELADA_POR_CLIENTE':
    case 'EXPIRADA':
    case 'NO_ASISTE':
      return 'danger'
    default:
      return 'neutral'
  }
}
