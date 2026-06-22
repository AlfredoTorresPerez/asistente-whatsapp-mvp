export const bookingStatusOptions = [
  { value: 'REQUESTED', label: 'Solicitada' },
  { value: 'PENDIENTE_CONFIRMACION', label: 'Pendiente de confirmacion' },
  { value: 'CONFIRMED', label: 'Confirmada' },
  { value: 'REPROGRAMADA', label: 'Reprogramada' },
  { value: 'RESCHEDULED', label: 'Reprogramada (legacy)' },
  { value: 'CANCELADA', label: 'Cancelada' },
  { value: 'CANCELLED', label: 'Cancelada (legacy)' },
  { value: 'COMPLETED', label: 'Completada' },
  { value: 'EXPIRADA', label: 'Expirada' },
  { value: 'NO_SHOW', label: 'No asistio' },
  { value: 'ATTENDED', label: 'Atendida' },
]

export function getBookingStatusLabel(status: string) {
  return bookingStatusOptions.find((option) => option.value === status)?.label ?? status
}

export function getBookingStatusTone(status: string) {
  switch (status) {
    case 'REQUESTED':
    case 'PENDIENTE_CONFIRMACION':
      return 'warning'
    case 'CONFIRMED':
    case 'ATTENDED':
      return 'success'
    case 'RESCHEDULED':
    case 'REPROGRAMADA':
      return 'info'
    case 'CANCELLED':
    case 'CANCELADA':
    case 'EXPIRADA':
    case 'NO_SHOW':
      return 'danger'
    case 'COMPLETED':
      return 'neutral'
    default:
      return 'neutral'
  }
}
