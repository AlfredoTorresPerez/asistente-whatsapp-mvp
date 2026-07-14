import { apiFetch } from './httpClient'
import type { ReportsSummaryResponse } from './types'

type ReportsSummaryRequest = {
  from?: string
  to?: string
  locationId?: string
  professionalId?: string
  serviceId?: string
  bookingStatus?: string
  ownerUserId?: string
  page?: number
  size?: number
}

export function getReportsSummaryRequest(params: ReportsSummaryRequest) {
  const searchParams = new URLSearchParams()
  if (params.from) searchParams.set('from', params.from)
  if (params.to) searchParams.set('to', params.to)
  if (params.locationId) searchParams.set('locationId', params.locationId)
  if (params.professionalId) searchParams.set('professionalId', params.professionalId)
  if (params.serviceId) searchParams.set('serviceId', params.serviceId)
  if (params.bookingStatus) searchParams.set('bookingStatus', params.bookingStatus)
  if (params.ownerUserId) searchParams.set('ownerUserId', params.ownerUserId)
  if (params.page !== undefined) searchParams.set('page', String(params.page))
  if (params.size !== undefined) searchParams.set('size', String(params.size))

  return apiFetch<ReportsSummaryResponse>(`/reports/summary?${searchParams.toString()}`)
}
