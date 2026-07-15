export type BadgeTone = 'success' | 'warning' | 'danger' | 'neutral' | 'info'

const normalizeStatusKey = (value: string | null | undefined) =>
  String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '_')

const statusLabels: Record<string, string> = {
  active: 'Activo',
  approved: 'Aprobado',
  canceled: 'Cancelado',
  cancelled: 'Cancelado',
  closed: 'Cerrado',
  completed: 'Completado',
  confirmed: 'Confirmado',
  delivered: 'Entregado',
  disabled: 'Desactivado',
  draft: 'Pendiente',
  dry_run: 'Simulación',
  enabled: 'Activo',
  escalated: 'Derivado a humano',
  failed: 'Rechazado',
  inactive: 'Desactivado',
  interested: 'Interesado',
  new: 'Nuevo',
  open: 'Abierto',
  paid: 'Pagado',
  partial: 'Pago parcial',
  partially_paid: 'Pago parcial',
  pending: 'Pendiente',
  preparing: 'En preparación',
  processing: 'En proceso',
  qualified: 'Calificado',
  read: 'Leído',
  ready: 'Listo',
  rejected: 'Rechazado',
  requested: 'Solicitado',
  rescheduled: 'Reprogramado',
  resolved: 'Resuelto',
  refunded: 'Reembolsado',
  scheduled: 'Programado',
  sent: 'Enviado',
  simulated: 'Simulado',
  unread: 'No leído',
  unpaid: 'Pendiente de pago',
  voided: 'Anulado',
}

const paymentLabels: Record<string, string> = {
  approved: 'Aprobado',
  canceled: 'Anulado',
  cancelled: 'Anulado',
  expired: 'Expirado',
  failed: 'Rechazado',
  not_required: 'No requerido',
  paid: 'Pagado',
  partial: 'Pago parcial',
  partially_paid: 'Pago parcial',
  pending: 'Pendiente de pago',
  refunded: 'Reembolsado',
  rejected: 'Rechazado',
  unpaid: 'Pendiente de pago',
  voided: 'Anulado',
}

const orderLabels: Record<string, string> = {
  canceled: 'Cancelado',
  cancelled: 'Cancelado',
  closed: 'Cerrado',
  completed: 'Cerrado',
  confirmed: 'Confirmado',
  delivered: 'Entregado',
  draft: 'Pendiente',
  pending: 'Pendiente',
  preparing: 'En preparación',
  processing: 'En proceso',
  ready: 'Listo',
}

export function formatEstado(value: string | null | undefined) {
  const key = normalizeStatusKey(value)
  return statusLabels[key] ?? value ?? 'Sin clasificar'
}

export function formatEstadoRegistro(active: boolean | string | null | undefined) {
  if (typeof active === 'boolean') {
    return active ? 'Activo' : 'Desactivado'
  }

  const key = normalizeStatusKey(active)
  if (['active', 'enabled', 'activo'].includes(key)) return 'Activo'
  if (['inactive', 'disabled', 'inactivo', 'desactivado', 'paused', 'pausado'].includes(key))
    return 'Desactivado'
  return formatEstado(active)
}

export function formatEstadoPago(value: string | null | undefined) {
  const key = normalizeStatusKey(value)
  return paymentLabels[key] ?? formatEstado(value)
}

export function formatEstadoPedido(value: string | null | undefined) {
  const key = normalizeStatusKey(value)
  return orderLabels[key] ?? formatEstado(value)
}

export function formatEstadoActividad(value: string | null | undefined) {
  return formatEstado(value)
}

export function getEstadoTone(value: string | null | undefined): BadgeTone {
  const key = normalizeStatusKey(value)

  if (
    [
      'active',
      'approved',
      'completed',
      'confirmed',
      'delivered',
      'enabled',
      'paid',
      'read',
      'resolved',
    ].includes(key)
  ) {
    return 'success'
  }

  if (
    [
      'cancelled',
      'canceled',
      'disabled',
      'expired',
      'failed',
      'inactive',
      'rejected',
      'voided',
    ].includes(key)
  ) {
    return 'danger'
  }

  if (
    [
      'draft',
      'pending',
      'partially_paid',
      'partial',
      'preparing',
      'processing',
      'requested',
      'rescheduled',
      'unpaid',
      'unread',
    ].includes(key)
  ) {
    return 'warning'
  }

  if (['new', 'open', 'qualified', 'ready', 'scheduled', 'sent'].includes(key)) {
    return 'info'
  }

  return 'neutral'
}

export function getRegistroTone(active: boolean | string | null | undefined): BadgeTone {
  return formatEstadoRegistro(active) === 'Activo' ? 'success' : 'warning'
}

export function isRegistroActivo(active: boolean | string | null | undefined) {
  return formatEstadoRegistro(active) === 'Activo'
}
