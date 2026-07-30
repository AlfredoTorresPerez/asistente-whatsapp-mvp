import { apiFetch } from './httpClient'
import type { PagedResponse, ProfessionalRequest, ProfessionalResponse } from './types'

export function listProfessionalsRequest(params: {
  page?: number
  size?: number
  search?: string
  active?: boolean
} = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 50))
  if (params.search) query.set('search', params.search)
  if (params.active !== undefined) query.set('active', String(params.active))

  return apiFetch<PagedResponse<ProfessionalResponse>>(
    `/admin/professionals?${query.toString()}`,
  )
}

export function getProfessionalRequest(professionalId: string) {
  return apiFetch<ProfessionalResponse>(`/admin/professionals/${professionalId}`)
}

export function createProfessionalRequest(payload: ProfessionalRequest) {
  return apiFetch<ProfessionalResponse>('/admin/professionals', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateProfessionalRequest(
  professionalId: string,
  payload: ProfessionalRequest,
) {
  return apiFetch<ProfessionalResponse>(`/admin/professionals/${professionalId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}
