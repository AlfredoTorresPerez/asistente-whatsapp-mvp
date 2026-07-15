import { apiFetch } from './httpClient'
import type { BusinessLocationResponse, UpsertBusinessLocationRequest } from './types'

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

export function getBusinessLocationsRequest(filters: { activeOnly?: boolean } = {}) {
  return apiFetch<BusinessLocationResponse[]>(`/business-locations${toSearchParams(filters)}`)
}

export function createBusinessLocationRequest(payload: UpsertBusinessLocationRequest) {
  return apiFetch<BusinessLocationResponse>('/business-locations', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateBusinessLocationRequest(
  locationId: string,
  payload: UpsertBusinessLocationRequest,
) {
  return apiFetch<BusinessLocationResponse>(`/business-locations/${locationId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deactivateBusinessLocationRequest(locationId: string) {
  return apiFetch<BusinessLocationResponse>(`/business-locations/${locationId}`, {
    method: 'DELETE',
  })
}
