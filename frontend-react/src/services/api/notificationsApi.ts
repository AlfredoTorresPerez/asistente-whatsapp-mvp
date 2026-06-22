import { apiFetch } from './httpClient'
import type {
  NotificationReadResponse,
  NotificationResponse,
  NotificationsReadAllResponse,
  PagedResponse,
} from './types'

type NotificationsListRequest = {
  page?: number
  size?: number
  search?: string
  status?: string
  type?: string
}

export function getNotificationsRequest({
  page = 0,
  search,
  size = 20,
  status,
  type,
}: NotificationsListRequest = {}) {
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  })

  if (search) {
    searchParams.set('search', search)
  }

  if (status) {
    searchParams.set('status', status)
  }

  if (type) {
    searchParams.set('type', type)
  }

  return apiFetch<PagedResponse<NotificationResponse>>(`/notifications?${searchParams.toString()}`)
}

export function markNotificationAsReadRequest(notificationId: string) {
  return apiFetch<NotificationReadResponse>(`/notifications/${notificationId}/read`, {
    method: 'PATCH',
  })
}

export function markAllNotificationsAsReadRequest() {
  return apiFetch<NotificationsReadAllResponse>('/notifications/read-all', {
    method: 'PATCH',
  })
}
