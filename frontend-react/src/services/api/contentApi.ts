import { apiFetch } from './httpClient'
import type {
  ContentItemSummaryResponse,
  ContentItemDetailResponse,
  ContentItemListResponse,
  ContentItemListRequest,
  CreateContentItemRequest,
  UpdateContentItemRequest,
  UpdateContentItemStatusRequest,
  PublicContentItemResponse,
  ContentItemImageUploadResponse,
} from './types'

function toQueryString(params: Record<string, string | number | boolean | undefined | null>) {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    searchParams.set(key, String(value))
  })
  const queryString = searchParams.toString()
  return queryString ? `?${queryString}` : ''
}

export function getContentItemsRequest(filters: ContentItemListRequest = {}) {
  return apiFetch<ContentItemListResponse>(`/content-items${toQueryString(filters)}`)
}

export function getContentItemRequest(id: string) {
  return apiFetch<ContentItemDetailResponse>(`/content-items/${id}`)
}

export function createContentItemRequest(payload: CreateContentItemRequest, image?: File) {
  const formData = new FormData()
  formData.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  if (image) {
    formData.append('image', image)
  }
  return apiFetch<ContentItemDetailResponse>('/content-items', {
    method: 'POST',
    body: formData,
  })
}

export function updateContentItemRequest(id: string, payload: UpdateContentItemRequest) {
  return apiFetch<ContentItemDetailResponse>(`/content-items/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function updateContentItemStatusRequest(
  id: string,
  payload: UpdateContentItemStatusRequest,
) {
  return apiFetch<ContentItemDetailResponse>(`/content-items/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function uploadContentItemImageRequest(id: string, image: File) {
  const formData = new FormData()
  formData.append('image', image)
  return apiFetch<ContentItemImageUploadResponse>(`/content-items/${id}/image`, {
    method: 'POST',
    body: formData,
  })
}

export function deleteContentItemImageRequest(id: string) {
  return apiFetch<void>(`/content-items/${id}/image`, {
    method: 'DELETE',
  })
}

export function deleteContentItemRequest(id: string) {
  return apiFetch<void>(`/content-items/${id}`, {
    method: 'DELETE',
  })
}

export function getPublicContentItemsRequest(type?: string) {
  return apiFetch<PublicContentItemResponse[]>(`/public/v1/content-items${toQueryString({ type })}`)
}
