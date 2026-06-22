import { apiFetch } from './httpClient'
import type {
  CatalogCategoryResponse,
  CatalogProductResponse,
  PagedResponse,
  UpsertCatalogCategoryRequest,
  UpsertCatalogProductRequest,
} from './types'

const toQueryString = (params: Record<string, string | number | boolean | null | undefined>) => {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      searchParams.set(key, String(value))
    }
  })
  const queryString = searchParams.toString()
  return queryString ? `?${queryString}` : ''
}

export function listCatalogProducts(params: {
  page?: number
  size?: number
  search?: string
  categoryCode?: string
  active?: boolean
} = {}) {
  return apiFetch<PagedResponse<CatalogProductResponse>>(`/catalog/products${toQueryString(params)}`)
}

export function getCatalogProduct(productId: string) {
  return apiFetch<CatalogProductResponse>(`/catalog/products/${productId}`)
}

export function createCatalogProduct(request: UpsertCatalogProductRequest) {
  return apiFetch<CatalogProductResponse>('/catalog/products', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function updateCatalogProduct(productId: string, request: UpsertCatalogProductRequest) {
  return apiFetch<CatalogProductResponse>(`/catalog/products/${productId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function updateCatalogProductStatus(productId: string, active: boolean) {
  return apiFetch<CatalogProductResponse>(`/catalog/products/${productId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ active }),
  })
}

export function listCatalogCategories(params: { page?: number; size?: number; active?: boolean } = {}) {
  return apiFetch<PagedResponse<CatalogCategoryResponse>>(`/catalog/categories${toQueryString(params)}`)
}

export function createCatalogCategory(request: UpsertCatalogCategoryRequest) {
  return apiFetch<CatalogCategoryResponse>('/catalog/categories', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}
