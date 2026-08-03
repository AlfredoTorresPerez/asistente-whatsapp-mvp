import { apiFetch } from './httpClient'
import type {
  AgendaAvailabilityRequest,
  AgendaAvailabilityResponse,
  AgendaBlockRequest,
  AgendaBlockResponse,
  AgendaCalendarResponse,
  AgendaCancelRequest,
  AgendaFilterOptionsResponse,
  AgendaLifecycleRequest,
  AgendaRescheduleRequest,
  BookingDetailResponse,
  BusinessHoursResponse,
  CreateTemporaryAgendaBookingRequest,
  SaveBusinessHoursRequest,
  SaveProfessionalHoursRequest,
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

export function getBusinessHoursRequest(locationId?: string) {
  const params = locationId ? `?locationId=${locationId}` : ''
  return apiFetch<BusinessHoursResponse[]>(`/agenda/business-hours${params}`)
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

export function rescheduleAgendaBookingRequest(
  bookingId: string,
  payload: AgendaRescheduleRequest,
) {
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

export function confirmAgendaBookingRequest(
  bookingId: string,
  payload: AgendaLifecycleRequest = {},
) {
  return apiFetch<BookingDetailResponse>(`/agenda/bookings/${bookingId}/confirm`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function startAgendaBookingServiceRequest(
  bookingId: string,
  payload: AgendaLifecycleRequest = {},
) {
  return apiFetch<BookingDetailResponse>(`/agenda/bookings/${bookingId}/start-service`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function completeAgendaBookingRequest(
  bookingId: string,
  payload: AgendaLifecycleRequest = {},
) {
  return apiFetch<BookingDetailResponse>(`/agenda/bookings/${bookingId}/complete`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function markAgendaBookingNoShowRequest(
  bookingId: string,
  payload: AgendaLifecycleRequest = {},
) {
  return apiFetch<BookingDetailResponse>(`/agenda/bookings/${bookingId}/mark-no-show`, {
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

export function saveBusinessHoursRequest(payload: SaveBusinessHoursRequest) {
  return apiFetch<BusinessHoursResponse[]>('/agenda/business-hours', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function saveProfessionalHoursRequest(payload: SaveProfessionalHoursRequest) {
  return apiFetch<BusinessHoursResponse[]>('/agenda/professional-hours', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
