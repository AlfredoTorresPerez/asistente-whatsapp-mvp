import { ApiClientError, apiFetch } from './httpClient'

function enc(v: string): string {
  return v.replace(/-/g, '%2D').replace(/_/g, '%5F')
}
import type {
  AgendaAvailabilityRequest,
  AgendaAvailabilityResponse,
  BookingConfirmationLinkResponse,
  BookingDetailResponse,
  BookingPaymentResponse,
  BookingPublicActionLinkResponse,
  BookingSummaryResponse,
  CancelBookingRequest,
  CreateBookingCancellationLinkRequest,
  CreateBookingConfirmationLinkRequest,
  CreateBookingFromConversationRequest,
  CreateBookingFromLeadRequest,
  CreateBookingPaymentLinkRequest,
  CreateBookingRequest,
  CreateBookingRescheduleLinkRequest,
  CreatePublicBookingRequest,
  CreatePublicBookingResponse,
  CustomerBookingItemResponse,
  CustomerBookingRescheduleRequest,
  CustomerBookingReschedulePreviewResponse,
  PagedResponse,
  PublicBookingCancellationFromConfirmationRequest,
  PublicBookingCancellationResponse,
  PublicBookingConfirmationResponse,
  PublicBookingPaymentDetailResponse,
  PublicBookingRescheduleFromConfirmationRequest,
  PublicBookingRescheduleRequest,
  PublicBookingRescheduleResponse,
  PublicCategoryResponse,
  PublicCustomerInfoResponse,
  PublicServiceBranchResponse,
  PublicServiceDetailResponse,
  PublicServiceItemResponse,
  RefundBookingPaymentRequest,
  RegisterBookingManualPaymentRequest,
  RescheduleBookingRequest,
  UpdateBookingRequest,
} from './types'

type GetBookingsFilters = {
  page?: number
  size?: number
  from?: string
  to?: string
  search?: string
  status?: string
  assignedUserId?: string
}

function toSearchParams(filters: Record<string, string | number | undefined | null>) {
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

export function getBookingsRequest(filters: GetBookingsFilters) {
  return apiFetch<PagedResponse<BookingSummaryResponse>>(`/bookings${toSearchParams(filters)}`)
}

export function createBookingRequest(payload: CreateBookingRequest) {
  return apiFetch<BookingDetailResponse>('/bookings', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getBookingDetailRequest(bookingId: string) {
  return apiFetch<BookingDetailResponse>(`/bookings/${bookingId}`)
}

export function updateBookingRequest(bookingId: string, payload: UpdateBookingRequest) {
  return apiFetch<BookingDetailResponse>(`/bookings/${bookingId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function rescheduleBookingRequest(bookingId: string, payload: RescheduleBookingRequest) {
  return apiFetch<BookingDetailResponse>(`/bookings/${bookingId}/reschedule`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function cancelBookingRequest(bookingId: string, payload: CancelBookingRequest) {
  return apiFetch<BookingDetailResponse>(`/bookings/${bookingId}/cancel`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function createBookingPaymentLinkRequest(
  bookingId: string,
  payload: CreateBookingPaymentLinkRequest = {},
) {
  return apiFetch<BookingPaymentResponse>(`/bookings/${bookingId}/payment-link`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function registerBookingManualPaymentRequest(
  bookingId: string,
  payload: RegisterBookingManualPaymentRequest,
) {
  return apiFetch<BookingPaymentResponse>(`/bookings/${bookingId}/payments/manual`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function refundBookingPaymentRequest(
  bookingId: string,
  paymentId: string,
  payload: RefundBookingPaymentRequest = {},
) {
  return apiFetch<BookingPaymentResponse>(`/bookings/${bookingId}/payments/${paymentId}/refund`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function createBookingFromConversationRequest(
  conversationId: string,
  payload: CreateBookingFromConversationRequest,
) {
  return apiFetch<BookingDetailResponse>(`/conversations/${conversationId}/appointments`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createBookingFromLeadRequest(
  leadId: string,
  payload: CreateBookingFromLeadRequest,
) {
  return apiFetch<BookingDetailResponse>(`/prospects/${leadId}/appointments`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createBookingConfirmationLinkRequest(
  bookingId: string,
  payload: CreateBookingConfirmationLinkRequest = {},
) {
  return apiFetch<BookingConfirmationLinkResponse>(`/bookings/${bookingId}/confirmation-link`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createBookingRescheduleLinkRequest(
  bookingId: string,
  payload: CreateBookingRescheduleLinkRequest,
) {
  return apiFetch<BookingPublicActionLinkResponse>(`/bookings/${bookingId}/reschedule-link`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createBookingCancellationLinkRequest(
  bookingId: string,
  payload: CreateBookingCancellationLinkRequest = {},
) {
  return apiFetch<BookingPublicActionLinkResponse>(`/bookings/${bookingId}/cancellation-link`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getPublicBookingConfirmationRequest(token: string) {
  return apiFetch<PublicBookingConfirmationResponse>(`/public/booking-confirmations/${token}`, {
    auth: false,
  })
}

export async function confirmPublicBookingRequest(token: string) {
  try {
    return await apiFetch<PublicBookingConfirmationResponse>(
      `/public/booking-confirmations/${token}/confirm`,
      {
        method: 'POST',
        auth: false,
      },
    )
  } catch (error) {
    if (error instanceof ApiClientError && (error.status === 403 || error.status === 405)) {
      return apiFetch<PublicBookingConfirmationResponse>(
        `/public/booking-confirmations/${token}/confirm?fallback=${Date.now()}`,
        {
          method: 'GET',
          auth: false,
          headers: {
            'Cache-Control': 'no-store',
          },
        },
      )
    }

    throw error
  }
}

export function getPublicBookingConfirmationAvailabilityRequest(
  token: string,
  date: string,
  maxSlots = 12,
) {
  const searchParams = new URLSearchParams({ date, maxSlots: String(maxSlots) })
  return apiFetch<AgendaAvailabilityResponse>(
    `/public/booking-confirmations/${token}/availability?${searchParams.toString()}`,
    {
      auth: false,
    },
  )
}

export function reschedulePublicBookingFromConfirmationRequest(
  token: string,
  payload: PublicBookingRescheduleFromConfirmationRequest,
) {
  return apiFetch<PublicBookingConfirmationResponse>(
    `/public/booking-confirmations/${token}/reschedule`,
    {
      method: 'POST',
      auth: false,
      body: JSON.stringify(payload),
    },
  )
}

export function getPublicBookingPaymentDetailRequest(paymentId: string) {
  return apiFetch<PublicBookingPaymentDetailResponse>(
    `/public/booking-payments/${paymentId}/detail`,
    {
      auth: false,
    },
  )
}

export function simulateBookingPaymentRequest(paymentId: string, action: 'APPROVED' | 'REJECTED') {
  return apiFetch<BookingPaymentResponse>(`/public/booking-payments/${paymentId}/simulate`, {
    method: 'POST',
    auth: false,
    body: JSON.stringify({ action }),
  })
}

export function cancelPublicBookingFromConfirmationRequest(
  token: string,
  payload: PublicBookingCancellationFromConfirmationRequest,
) {
  return apiFetch<PublicBookingConfirmationResponse>(
    `/public/booking-confirmations/${token}/cancel`,
    {
      method: 'POST',
      auth: false,
      body: JSON.stringify(payload),
    },
  )
}

export function getPublicBookingRescheduleRequest(token: string) {
  return apiFetch<PublicBookingRescheduleResponse>(`/public/booking-reschedules/${token}`, {
    auth: false,
  })
}

export function confirmPublicBookingRescheduleRequest(token: string) {
  return apiFetch<PublicBookingRescheduleResponse>(`/public/booking-reschedules/${token}/confirm`, {
    method: 'POST',
    auth: false,
  })
}

export function rejectPublicBookingRescheduleRequest(token: string) {
  return apiFetch<PublicBookingRescheduleResponse>(`/public/booking-reschedules/${token}/reject`, {
    method: 'POST',
    auth: false,
  })
}

export function getPublicBookingRescheduleAvailabilityRequest(
  token: string,
  bookingId: string,
  serviceId: string,
  locationId: string,
  date: string,
  maxSlots = 40,
) {
  const searchParams = new URLSearchParams({
    serviceId,
    locationId,
    date,
    maxSlots: String(maxSlots),
  })
  return apiFetch<AgendaAvailabilityResponse>(
    `/public/booking-reschedules/${token}/${bookingId}/availability?${searchParams.toString()}`,
    {
      auth: false,
    },
  )
}

export function reschedulePublicBookingRequest(
  token: string,
  bookingId: string,
  payload: PublicBookingRescheduleRequest,
) {
  return apiFetch<CustomerBookingItemResponse>(
    `/public/booking-reschedules/${token}/${bookingId}/reschedule`,
    {
      method: 'POST',
      auth: false,
      body: JSON.stringify(payload),
    },
  )
}

export function getPublicBookingCancellationRequest(token: string) {
  return apiFetch<PublicBookingCancellationResponse>(`/public/booking-cancellations/${token}`, {
    auth: false,
  })
}

export function confirmPublicBookingCancellationRequest(token: string, reason?: string) {
  return apiFetch<PublicBookingCancellationResponse>(
    `/public/booking-cancellations/${token}/confirm`,
    {
      method: 'POST',
      auth: false,
      body: JSON.stringify({ reason }),
    },
  )
}

export function getPublicLandingCategoriesRequest() {
  return apiFetch<PublicCategoryResponse[]>('/public/landing/categories', { auth: false })
}

export function getPublicLandingServicesByCategoryRequest(categoryCode: string) {
  return apiFetch<PublicServiceItemResponse[]>(
    `/public/landing/categories/${categoryCode}/services`,
    { auth: false },
  )
}

export function getPublicLandingServiceDetailRequest(serviceId: string) {
  return apiFetch<PublicServiceDetailResponse>(`/public/landing/services/${serviceId}`, {
    auth: false,
  })
}

export function getPublicLandingServiceBranchesRequest(serviceId: string) {
  return apiFetch<PublicServiceBranchResponse[]>(`/public/landing/services/${serviceId}/branches`, {
    auth: false,
  })
}

export function getPublicLandingAvailabilityRequest(payload: AgendaAvailabilityRequest) {
  return apiFetch<AgendaAvailabilityResponse>('/public/landing/availability', {
    method: 'POST',
    auth: false,
    body: JSON.stringify(payload),
  })
}

export function getPublicLandingCustomerInfoRequest(token: string) {
  return apiFetch<PublicCustomerInfoResponse>(`/public/landing/customer-info/${token}`, {
    auth: false,
  })
}

export function createPublicLandingBookingRequest(payload: CreatePublicBookingRequest) {
  return apiFetch<CreatePublicBookingResponse>('/public/landing/bookings', {
    method: 'POST',
    auth: false,
    body: JSON.stringify(payload),
  })
}

export function getCustomerBookingsRequest(token: string) {
  return apiFetch<CustomerBookingItemResponse[]>(`/public/customer-bookings/${enc(token)}`, {
    auth: false,
  })
}

export function cancelCustomerBookingRequest(token: string, bookingId: string, reason?: string) {
  return apiFetch<{ status: string; bookingId: string }>(
    `/public/customer-bookings/${enc(token)}/${bookingId}/cancel`,
    {
      method: 'POST',
      auth: false,
      body: JSON.stringify({ reason }),
    },
  )
}

export function getCustomerBookingReschedulePreviewRequest(token: string, bookingId: string) {
  return apiFetch<CustomerBookingReschedulePreviewResponse>(
    `/public/customer-bookings/${enc(token)}/${bookingId}/reschedule`,
    {
      auth: false,
    },
  )
}

export function getCustomerBookingRescheduleAvailabilityRequest(
  token: string,
  bookingId: string,
  serviceId: string,
  locationId: string,
  date: string,
) {
  const searchParams = new URLSearchParams({ serviceId, locationId, date })
  return apiFetch<AgendaAvailabilityResponse>(
    `/public/customer-bookings/${enc(token)}/${bookingId}/reschedule/availability?${searchParams.toString()}`,
    {
      auth: false,
    },
  )
}

export function rescheduleCustomerBookingRequest(
  token: string,
  bookingId: string,
  payload: CustomerBookingRescheduleRequest,
) {
  return apiFetch<CustomerBookingItemResponse>(
    `/public/customer-bookings/${enc(token)}/${bookingId}/reschedule`,
    {
      method: 'POST',
      auth: false,
      body: JSON.stringify(payload),
    },
  )
}
