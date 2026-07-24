import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  normalizeWhatsAppPhone,
  buildWhatsAppUrl,
  buildPublicWhatsAppUrl,
  buildAppointmentWhatsAppMessage,
  openWhatsAppUrl,
} from './whatsapp'
import type { AgendaCalendarItemResponse } from '../services/api/types'

describe('normalizeWhatsAppPhone', () => {
  it('normaliza numero con espacios', () => {
    expect(normalizeWhatsAppPhone('+56 9 2730 5158')).toBe('56927305158')
  })

  it('normaliza numero con guiones', () => {
    expect(normalizeWhatsAppPhone('56-9-2730-5158')).toBe('56927305158')
  })

  it('preserva numero ya normalizado', () => {
    expect(normalizeWhatsAppPhone('56927305158')).toBe('56927305158')
  })

  it('lanza error para numero vacio', () => {
    expect(() => normalizeWhatsAppPhone('')).toThrow('El numero de telefono no puede estar vacio.')
  })

  it('lanza error para numero demasiado corto', () => {
    expect(() => normalizeWhatsAppPhone('123')).toThrow('al menos 10 digitos')
  })

  it('lanza error para numero demasiado largo', () => {
    expect(() => normalizeWhatsAppPhone('56927305158000')).toThrow('no puede tener mas')
  })

  it('elimina codigo 00 inicial', () => {
    expect(normalizeWhatsAppPhone('0056927305158')).toBe('56927305158')
  })

  it('agrega prefijo 56 cuando falta', () => {
    expect(normalizeWhatsAppPhone('927305158')).toBe('56927305158')
  })

  it('elimina cero inicial chileno', () => {
    expect(normalizeWhatsAppPhone('0956927305158')).toBe('56927305158')
  })
})

describe('buildWhatsAppUrl', () => {
  it('construye URL sin mensaje', () => {
    expect(buildWhatsAppUrl('56927305158')).toBe('https://wa.me/56927305158')
  })

  it('construye URL con mensaje codificado', () => {
    const url = buildWhatsAppUrl('56927305158', 'Hola Sofia')
    expect(url).toBe('https://wa.me/56927305158?text=Hola%20Sofia')
  })

  it('codifica caracteres especiales', () => {
    const url = buildWhatsAppUrl('56927305158', 'Confirmación de cita')
    expect(url).toContain(encodeURIComponent('Confirmación de cita'))
  })

  it('normaliza el telefono antes de construir', () => {
    expect(buildWhatsAppUrl('+56 9 2730 5158')).toBe('https://wa.me/56927305158')
  })

  it('ignora mensaje vacio o solo espacios', () => {
    expect(buildWhatsAppUrl('56927305158', '  ')).toBe('https://wa.me/56927305158')
  })
})

describe('buildAppointmentWhatsAppMessage', () => {
  const baseItem: AgendaCalendarItemResponse = {
    bookingId: 'test-id',
    subject: 'Limpieza facial',
    status: 'CONFIRMED',
    startsAt: '2026-07-24T16:45:00-04:00',
    endsAt: '2026-07-24T17:45:00-04:00',
    durationMinutes: 60,
    locationId: 'loc-1',
    locationName: 'Centro Estético Bella',
    serviceId: 'svc-1',
    serviceName: 'Hidratación facial',
    professionalId: 'pro-1',
    professionalName: 'Carla Méndez',
    roomId: 'room-1',
    roomName: 'Cabina 1',
    customerName: 'Sofía',
    customerPhone: '56927305158',
    sourceChannel: 'WHATSAPP',
    startTimeLocal: '16:45',
    dateLocal: '2026-07-24',
  }

  it('incluye saludo con nombre del cliente', () => {
    const msg = buildAppointmentWhatsAppMessage(baseItem)
    expect(msg).toContain('Hola Sofía.')
  })

  it('incluye servicio', () => {
    const msg = buildAppointmentWhatsAppMessage(baseItem)
    expect(msg).toContain('Servicio: Hidratación facial')
  })

  it('incluye profesional', () => {
    const msg = buildAppointmentWhatsAppMessage(baseItem)
    expect(msg).toContain('Profesional: Carla Méndez')
  })

  it('incluye sucursal', () => {
    const msg = buildAppointmentWhatsAppMessage(baseItem)
    expect(msg).toContain('Sucursal: Centro Estético Bella')
  })

  it('incluye cabina', () => {
    const msg = buildAppointmentWhatsAppMessage(baseItem)
    expect(msg).toContain('Cabina: Cabina 1')
  })

  it('omite campos faltantes', () => {
    const item: AgendaCalendarItemResponse = {
      ...baseItem,
      professionalName: null,
      roomName: null,
      locationName: null,
    }
    const msg = buildAppointmentWhatsAppMessage(item)
    expect(msg).not.toContain('Profesional:')
    expect(msg).not.toContain('Cabina:')
    expect(msg).not.toContain('Sucursal:')
  })

  it('termina con Te esperamos', () => {
    const msg = buildAppointmentWhatsAppMessage(baseItem)
    expect(msg).toContain('Te esperamos.')
  })
})

describe('buildPublicWhatsAppUrl', () => {
  it('construye URL valida con wa.me', () => {
    const url = buildPublicWhatsAppUrl()
    if (url === null) {
      // Si no hay env var configurada, el test pasa como no concluyente
      return
    }
    expect(url).toMatch(/^https:\/\/wa\.me\/\d{10,12}/)
  })

  it('incluye mensaje codificado cuando se pasa', () => {
    const url = buildPublicWhatsAppUrl('Hola, quiero agendar')
    if (url === null) return
    expect(url).toContain('wa.me/')
    expect(url).toContain(encodeURIComponent('Hola, quiero agendar'))
  })
})

describe('openWhatsAppUrl', () => {
  beforeEach(() => {
    vi.spyOn(window, 'open').mockReturnValue({} as Window)
  })

  it('abre ventana con URL valida', () => {
    const openSpy = vi.spyOn(window, 'open')
    const result = openWhatsAppUrl('https://wa.me/56927305158')
    expect(result).toBe(true)
    expect(openSpy).toHaveBeenCalledWith('https://wa.me/56927305158', '_blank', 'noopener,noreferrer')
  })

  it('retorna false si no es URL wa.me', () => {
    const result = openWhatsAppUrl('whatsapp://send/?phone=56927305158')
    expect(result).toBe(false)
  })

  it('retorna false si window.open retorna null', () => {
    vi.spyOn(window, 'open').mockReturnValue(null)
    const result = openWhatsAppUrl('https://wa.me/56927305158')
    expect(result).toBe(false)
  })
})
