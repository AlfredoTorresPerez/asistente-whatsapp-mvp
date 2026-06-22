import { apiFetch } from './httpClient'
import type {
  MultisiteCatalogAvailabilityResponse,
  MultisiteChannelResponse,
  MultisiteLocationSummaryResponse,
  MultisiteProfessionalResponse,
  ProfessionalScheduleResponse,
  UpdateChannelLocationRequest,
  UpsertCatalogAvailabilityRequest,
  UpsertProfessionalScheduleRequest,
  UpsertUserLocationAccessRequest,
  UserLocationAccessResponse,
} from './types'

function toSearchParams(filters: Record<string, string | number | boolean | undefined | null>) {
  const searchParams = new URLSearchParams()

  Object.entries(filters).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    searchParams.set(key, String(value))
  })

  const queryString = searchParams.toString()
  return queryString ? `?${queryString}` : ''
}

export function getMultisiteSummaryRequest() {
  return apiFetch<MultisiteLocationSummaryResponse[]>('/multisite/summary')
}

export function getMultisiteCatalogAvailabilityRequest(filters: { locationId?: string | null } = {}) {
  return apiFetch<MultisiteCatalogAvailabilityResponse[]>(`/multisite/catalog-availability${toSearchParams(filters)}`)
}

export function updateMultisiteCatalogAvailabilityRequest(payload: UpsertCatalogAvailabilityRequest) {
  return apiFetch<MultisiteCatalogAvailabilityResponse[]>('/multisite/catalog-availability', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function getMultisiteProfessionalsRequest() {
  return apiFetch<MultisiteProfessionalResponse[]>('/multisite/professionals')
}

export function getMultisiteSchedulesRequest(filters: { locationId?: string | null } = {}) {
  return apiFetch<ProfessionalScheduleResponse[]>(`/multisite/professional-schedules${toSearchParams(filters)}`)
}

export function createMultisiteScheduleRequest(payload: UpsertProfessionalScheduleRequest) {
  return apiFetch<ProfessionalScheduleResponse[]>('/multisite/professional-schedules', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getMultisiteUserAccessRequest() {
  return apiFetch<UserLocationAccessResponse[]>('/multisite/user-access')
}

export function updateMultisiteUserAccessRequest(payload: UpsertUserLocationAccessRequest) {
  return apiFetch<UserLocationAccessResponse[]>('/multisite/user-access', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function getMultisiteChannelsRequest() {
  return apiFetch<MultisiteChannelResponse[]>('/multisite/channels')
}

export function updateMultisiteChannelLocationRequest(channelId: string, payload: UpdateChannelLocationRequest) {
  return apiFetch<MultisiteChannelResponse[]>(`/multisite/channels/${channelId}/location`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
