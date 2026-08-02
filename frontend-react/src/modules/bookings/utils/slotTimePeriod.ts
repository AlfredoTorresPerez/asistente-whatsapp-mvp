import type { AgendaSlotResponse } from '../../../services/api/types'

/** Limite centralizado entre manana (< 12:00) y tarde (>= 12:00). */
export const SLOT_PERIOD_NOON_HOUR = 12

export type SlotTimePeriod = 'MORNING' | 'AFTERNOON'

export const SLOT_PERIOD_LABELS: Record<SlotTimePeriod, string> = {
  MORNING: 'Manana',
  AFTERNOON: 'Tarde',
}

const ISO_START_TIME = /T(\d{1,2}):(\d{2})/

/**
 * Extrae la hora de pared (wall clock) del inicio del slot desde el string
 * ISO-8601 que envia el backend (ej: 2026-08-05T09:00:00-04:00), sin aplicar
 * conversion de zona horaria del navegador.
 */
function extractWallClockMinutes(value: string): number {
  const match = ISO_START_TIME.exec(value)
  if (!match) {
    return 0
  }
  const hours = Number(match[1])
  const minutes = Number(match[2])
  return Number.isFinite(hours) && Number.isFinite(minutes) ? hours * 60 + minutes : 0
}

export function getSlotTimePeriod(startsAt: string): SlotTimePeriod {
  return extractWallClockMinutes(startsAt) < SLOT_PERIOD_NOON_HOUR * 60 ? 'MORNING' : 'AFTERNOON'
}

export function slotIdentity(
  slot: Pick<
    AgendaSlotResponse,
    'startsAt' | 'endsAt' | 'professionalId' | 'roomId' | 'locationId'
  >,
): string {
  return [slot.startsAt, slot.endsAt, slot.professionalId ?? '', slot.roomId ?? '', slot.locationId].join('|')
}

export function compareSlots(a: AgendaSlotResponse, b: AgendaSlotResponse): number {
  const byStart = extractWallClockMinutes(a.startsAt) - extractWallClockMinutes(b.startsAt)
  if (byStart !== 0) {
    return byStart
  }
  const byProfessional = (a.professionalName ?? '').localeCompare(b.professionalName ?? '')
  if (byProfessional !== 0) {
    return byProfessional
  }
  const byRoom = (a.roomName ?? '').localeCompare(b.roomName ?? '')
  if (byRoom !== 0) {
    return byRoom
  }
  return slotIdentity(a).localeCompare(slotIdentity(b))
}
