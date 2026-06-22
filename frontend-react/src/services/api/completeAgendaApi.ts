import { apiFetch } from './httpClient'
import type {
  AgendaAvailabilityRequest,
  AgendaAvailabilityResponse,
  AgendaBlockRequest,
  AgendaBlockResponse,
  AgendaCalendarResponse,
  AgendaCancelRequest,
  AgendaFilterOptionsResponse,
  AgendaRescheduleRequest,
  BookingDetailResponse,
  CreateTemporaryAgendaBookingRequest,
} from './types'

function toSearchParams(filters: Record<string, string | undefined | null>) {
  const searchParams = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.set(key, value)
    }
  })
  const queryString = searchParams.toString()
  return queryString ? `?${queryString}` : ''
}

export function getAgendaAvailabilityRequest(payload: AgendaAvailabilityRequest) {
  return apiFetch<AgendaAvailabilityResponse>('/agenda/availability', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createTemporaryAgendaBookingRequest(payload: CreateTemporaryAgendaBookingRequest) {
  return apiFetch<BookingDetailResponse>('/agenda/temporary-bookings', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}


export function getAgendaFilterOptionsRequest(filters: { locationId?: string } = {}) {
  return apiFetch<AgendaFilterOptionsResponse>(`/agenda/filter-options${toSearchParams(filters)}`)
}

export function getAgendaCalendarRequest(filters: {
  from?: string
  to?: string
  locationId?: string
  professionalId?: string
  roomId?: string
  serviceId?: string
  status?: string
}) {
  return apiFetch<AgendaCalendarResponse>(`/agenda/calendar${toSearchParams(filters)}`)
}

export function rescheduleAgendaBookingRequest(bookingId: string, payload: AgendaRescheduleRequest) {
  return apiFetch<BookingDetailResponse>(`/agenda/bookings/${bookingId}/reschedule`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function cancelAgendaBookingRequest(bookingId: string, payload: AgendaCancelRequest) {
  return apiFetch<BookingDetailResponse>(`/agenda/bookings/${bookingId}/cancel`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function createAgendaBlockRequest(payload: AgendaBlockRequest) {
  return apiFetch<AgendaBlockResponse>('/agenda/blocks', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
