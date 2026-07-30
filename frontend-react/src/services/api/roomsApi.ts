import { apiFetch } from './httpClient'
import type { PagedResponse, RoomRequest, RoomResponse } from './types'

export function listRoomsRequest(params: {
  page?: number
  size?: number
  search?: string
  locationId?: string
  roomType?: string
  active?: boolean
} = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 50))
  if (params.search) query.set('search', params.search)
  if (params.locationId) query.set('locationId', params.locationId)
  if (params.roomType) query.set('roomType', params.roomType)
  if (params.active !== undefined) query.set('active', String(params.active))

  return apiFetch<PagedResponse<RoomResponse>>(`/admin/rooms?${query.toString()}`)
}

export function getRoomRequest(roomId: string) {
  return apiFetch<RoomResponse>(`/admin/rooms/${roomId}`)
}

export function createRoomRequest(payload: RoomRequest) {
  return apiFetch<RoomResponse>('/admin/rooms', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateRoomRequest(roomId: string, payload: RoomRequest) {
  return apiFetch<RoomResponse>(`/admin/rooms/${roomId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}
