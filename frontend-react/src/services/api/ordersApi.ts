import { apiFetch } from './httpClient'
import type {
  CreateOrderItemRequest,
  CreateOrderRequest,
  OrderDetailResponse,
  OrderSummaryResponse,
  PagedResponse,
  RegisterPaymentRequest,
  SendOrderSummaryResponse,
  UpdateOrderRequest,
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

export function listOrders(params: {
  page?: number
  size?: number
  search?: string
  status?: string
  paymentStatus?: string
} = {}) {
  return apiFetch<PagedResponse<OrderSummaryResponse>>(`/orders${toQueryString(params)}`)
}

export function createOrder(request: CreateOrderRequest) {
  return apiFetch<OrderDetailResponse>('/orders', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function createOrderFromConversation(conversationId: string, request: CreateOrderRequest) {
  return apiFetch<OrderDetailResponse>(`/orders/from-conversation/${conversationId}`, {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function createOrderFromProspect(prospectId: string, request: CreateOrderRequest) {
  return apiFetch<OrderDetailResponse>(`/orders/from-prospect/${prospectId}`, {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function getOrder(orderId: string) {
  return apiFetch<OrderDetailResponse>(`/orders/${orderId}`)
}

export function updateOrder(orderId: string, request: UpdateOrderRequest) {
  return apiFetch<OrderDetailResponse>(`/orders/${orderId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  })
}

export function addOrderItems(orderId: string, items: CreateOrderItemRequest[]) {
  return apiFetch<OrderDetailResponse>(`/orders/${orderId}/items`, {
    method: 'POST',
    body: JSON.stringify(items),
  })
}

export function updateOrderStatus(orderId: string, status: string) {
  return apiFetch<OrderDetailResponse>(`/orders/${orderId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export function registerOrderPayment(orderId: string, request: RegisterPaymentRequest) {
  return apiFetch<OrderDetailResponse>(`/orders/${orderId}/payment`, {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function sendOrderSummary(orderId: string) {
  return apiFetch<SendOrderSummaryResponse>(`/orders/${orderId}/send-summary`, {
    method: 'POST',
  })
}
