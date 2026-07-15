import { apiFetch } from './httpClient'
import type {
  AestheticBusinessRuleResponse,
  AestheticCategoryResponse,
  AestheticIntentLogResponse,
  AestheticProductResponse,
  AestheticServiceResponse,
  IntentAnalysisRequest,
  IntentAnalysisResponse,
  UpsertAestheticBusinessRuleRequest,
  UpsertAestheticProductRequest,
  UpsertAestheticServiceRequest,
  PagedResponse,
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

export function listAestheticServiceCategories(
  params: { page?: number; size?: number; active?: boolean } = {},
) {
  return apiFetch<PagedResponse<AestheticCategoryResponse>>(
    `/esthetic/service-categories${toQueryString(params)}`,
  )
}

export function listAestheticProductCategories(
  params: { page?: number; size?: number; active?: boolean } = {},
) {
  return apiFetch<PagedResponse<AestheticCategoryResponse>>(
    `/esthetic/product-categories${toQueryString(params)}`,
  )
}

export function listAestheticServices(
  params: {
    page?: number
    size?: number
    search?: string
    categoryCode?: string
    active?: boolean
  } = {},
) {
  return apiFetch<PagedResponse<AestheticServiceResponse>>(
    `/esthetic/services${toQueryString(params)}`,
  )
}

export function getAestheticService(serviceId: string) {
  return apiFetch<AestheticServiceResponse>(`/esthetic/services/${serviceId}`)
}

export function createAestheticService(request: UpsertAestheticServiceRequest) {
  return apiFetch<AestheticServiceResponse>('/esthetic/services', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function updateAestheticService(serviceId: string, request: UpsertAestheticServiceRequest) {
  return apiFetch<AestheticServiceResponse>(`/esthetic/services/${serviceId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function listAestheticProducts(
  params: {
    page?: number
    size?: number
    search?: string
    categoryCode?: string
    active?: boolean
    lowStockOnly?: boolean
  } = {},
) {
  return apiFetch<PagedResponse<AestheticProductResponse>>(
    `/esthetic/products${toQueryString(params)}`,
  )
}

export function getAestheticProduct(productId: string) {
  return apiFetch<AestheticProductResponse>(`/esthetic/products/${productId}`)
}

export function createAestheticProduct(request: UpsertAestheticProductRequest) {
  return apiFetch<AestheticProductResponse>('/esthetic/products', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function updateAestheticProduct(productId: string, request: UpsertAestheticProductRequest) {
  return apiFetch<AestheticProductResponse>(`/esthetic/products/${productId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function listAestheticRules(
  params: {
    page?: number
    size?: number
    ruleType?: string
    active?: boolean
  } = {},
) {
  return apiFetch<PagedResponse<AestheticBusinessRuleResponse>>(
    `/esthetic/rules${toQueryString(params)}`,
  )
}

export function getAestheticRule(ruleId: string) {
  return apiFetch<AestheticBusinessRuleResponse>(`/esthetic/rules/${ruleId}`)
}

export function createAestheticRule(request: UpsertAestheticBusinessRuleRequest) {
  return apiFetch<AestheticBusinessRuleResponse>('/esthetic/rules', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function updateAestheticRule(ruleId: string, request: UpsertAestheticBusinessRuleRequest) {
  return apiFetch<AestheticBusinessRuleResponse>(`/esthetic/rules/${ruleId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function analyzeAestheticIntent(request: IntentAnalysisRequest) {
  return apiFetch<IntentAnalysisResponse>('/esthetic/intent/analyze', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function listAestheticIntentLogs(params: { page?: number; size?: number } = {}) {
  return apiFetch<PagedResponse<AestheticIntentLogResponse>>(
    `/esthetic/intent/logs${toQueryString(params)}`,
  )
}
