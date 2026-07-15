import { apiFetch } from './httpClient'
import type { CalendarAccountResponse, CalendarListEntry, BookingSyncStatusResponse } from './types'

export function getCalendarStatusRequest() {
  return apiFetch<CalendarAccountResponse[]>('/calendar-integrations/status')
}

export function connectGoogleCalendarRequest() {
  window.location.href = `${import.meta.env.VITE_API_BASE_URL ?? ''}/api/v1/calendar-integrations/google/connect`
}

export function disconnectCalendarRequest(accountId: string) {
  return apiFetch<{ message: string }>(`/calendar-integrations/${accountId}`, {
    method: 'DELETE',
  })
}

export function getCalendarListRequest(accountId: string) {
  return apiFetch<CalendarListEntry[]>(`/calendar-integrations/${accountId}/calendars`)
}

export function selectCalendarRequest(accountId: string, calendarId: string, calendarSummary: string) {
  return apiFetch<CalendarAccountResponse>(`/calendar-integrations/${accountId}/select-calendar`, {
    method: 'POST',
    body: JSON.stringify({ calendarId, calendarSummary }),
  })
}

export function getBookingSyncStatusRequest(bookingId: string) {
  return apiFetch<BookingSyncStatusResponse[]>(`/bookings/${bookingId}/calendar-sync`)
}

export function retryBookingSyncRequest(bookingId: string) {
  return apiFetch<{ message: string }>(`/bookings/${bookingId}/calendar-sync/retry`, {
    method: 'POST',
  })
}
