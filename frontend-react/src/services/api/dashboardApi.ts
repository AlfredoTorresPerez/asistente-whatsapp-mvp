import { apiFetch } from './httpClient'
import type { DashboardSummaryResponse } from './types'

type DashboardSummaryRequest = {
  from: string
  to: string
  ownerUserId?: string
}

export function getDashboardSummaryRequest({
  from,
  ownerUserId,
  to,
}: DashboardSummaryRequest) {
  const searchParams = new URLSearchParams({
    from,
    to,
  })

  if (ownerUserId) {
    searchParams.set('ownerUserId', ownerUserId)
  }

  return apiFetch<DashboardSummaryResponse>(`/dashboard/summary?${searchParams.toString()}`)
}
