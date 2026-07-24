import type { AgendaCalendarItemResponse } from '../services/api/types'

export function normalizeWhatsAppPhone(phone: string): string {
  let digits = phone.replace(/\D/g, '')

  if (!digits) {
    throw new Error('El numero de telefono no puede estar vacio.')
  }

  if (digits.startsWith('00')) {
    digits = digits.slice(2)
  }

  if (digits.startsWith('0') && digits.length > 11) {
    digits = digits.slice(1)
  }

  if (!digits.startsWith('56')) {
    digits = '56' + digits
  }

  if (digits.length > 12) {
    const match = digits.match(/56(9\d{8})$/)
    if (match) {
      digits = '56' + match[1]
    }
  }

  if (digits.length < 10) {
    throw new Error('El numero de telefono debe tener al menos 10 digitos.')
  }

  if (digits.length > 12) {
    throw new Error('El numero de telefono no puede tener mas de 12 digitos.')
  }

  return digits
}

export function buildWhatsAppUrl(phone: string, message?: string): string {
  const normalized = normalizeWhatsAppPhone(phone)

  if (!message?.trim()) {
    return `https://wa.me/${normalized}`
  }

  return `https://wa.me/${normalized}?text=${encodeURIComponent(message.trim())}`
}

export function buildAppointmentWhatsAppMessage(
  item: AgendaCalendarItemResponse,
): string {
  const parts: string[] = ['Hola ' + (item.customerName ?? '') + '.']
  parts.push('')
  parts.push('Tu cita ha sido confirmada.')
  parts.push('')

  if (item.serviceName ?? item.subject) {
    parts.push('Servicio: ' + (item.serviceName ?? item.subject))
  }

  const dateLabel = item.dateLocal ?? item.startsAt
  if (dateLabel) {
    const d = new Date(dateLabel)
    const day = d.toLocaleDateString('es-CL', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    })
    parts.push('Fecha: ' + day)
  }

  if (item.startTimeLocal) {
    parts.push('Hora: ' + item.startTimeLocal)
  }

  if (item.professionalName) {
    parts.push('Profesional: ' + item.professionalName)
  }

  if (item.locationName) {
    parts.push('Sucursal: ' + item.locationName)
  }

  if (item.roomName) {
    parts.push('Cabina: ' + item.roomName)
  }

  parts.push('')
  parts.push('Te esperamos.')

  return parts.join('\n')
}

export function buildPublicWhatsAppUrl(message?: string): string | null {
  const number = import.meta.env.VITE_PUBLIC_WHATSAPP_NUMBER
  if (!number) return null
  return buildWhatsAppUrl(number, message)
}

export function openWhatsAppUrl(url: string): boolean {
  if (!url.startsWith('https://wa.me/')) {
    return false
  }

  const win = window.open(url, '_blank', 'noopener,noreferrer')
  return win !== null
}
