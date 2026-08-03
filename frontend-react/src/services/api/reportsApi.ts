import { readStoredShellSessionSnapshot } from '../../lib/shellSession'
import { apiFetch, ApiClientError } from './httpClient'
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

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

function buildReportsSearchParams(params: ReportsSummaryRequest) {
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
  return searchParams
}

export function getReportsSummaryRequest(params: ReportsSummaryRequest) {
  const searchParams = buildReportsSearchParams(params)
  return apiFetch<ReportsSummaryResponse>(`/reports/summary?${searchParams.toString()}`)
}

export async function downloadReportsCsvRequest(params: ReportsSummaryRequest) {
  const searchParams = buildReportsSearchParams(params)
  const session = readStoredShellSessionSnapshot()
  const response = await fetch(`${apiBaseUrl}/reports/summary.csv?${searchParams.toString()}`, {
    headers: session?.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : undefined,
  })

  if (!response.ok) {
    const contentType = response.headers.get('content-type') ?? ''
    const payload = contentType.includes('application/json')
      ? await response.json().catch(() => ({}))
      : {}
    throw new ApiClientError({
      status: response.status,
      ...payload,
    })
  }

  return response.blob()
}
