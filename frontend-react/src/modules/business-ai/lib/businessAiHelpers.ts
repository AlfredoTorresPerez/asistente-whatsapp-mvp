export const BUSINESS_AI_AUDIT_PAGE_SIZE = 5

export type BusinessHoursDay = {
  day: string
  startTime: string
  endTime: string
}

export const DEFAULT_BUSINESS_HOURS: BusinessHoursDay[] = [
  { day: 'Lunes', startTime: '09:00', endTime: '18:00' },
  { day: 'Martes', startTime: '09:00', endTime: '18:00' },
  { day: 'Miércoles', startTime: '09:00', endTime: '18:00' },
  { day: 'Jueves', startTime: '09:00', endTime: '18:00' },
  { day: 'Viernes', startTime: '09:00', endTime: '18:00' },
  { day: 'Sábado', startTime: '09:00', endTime: '13:00' },
  { day: 'Domingo', startTime: '', endTime: '' },
]

export type AuditLike = {
  createdAt: string
}

export function sortAuditLogsDescending<T extends AuditLike>(logs: T[]) {
  return [...logs].sort((left, right) => {
    const leftTime = new Date(left.createdAt).getTime()
    const rightTime = new Date(right.createdAt).getTime()
    return rightTime - leftTime
  })
}

export function paginateAuditLogs<T>(
  logs: T[],
  page: number,
  pageSize = BUSINESS_AI_AUDIT_PAGE_SIZE,
) {
  const safePage = Math.max(0, page)
  return logs.slice(safePage * pageSize, safePage * pageSize + pageSize)
}

export function getAuditTotalPages(totalItems: number, pageSize = BUSINESS_AI_AUDIT_PAGE_SIZE) {
  return Math.max(1, Math.ceil(totalItems / pageSize))
}

export function matchesSearch(row: { title: string; category: string; description: string }, search: string) {
  if (!search.trim()) return true
  const normalizedSearch = search.trim().toLowerCase()
  return [row.title, row.category, row.description].join(' ').toLowerCase().includes(normalizedSearch)
}

export function isBusinessHourRangeValid(day: BusinessHoursDay) {
  if (!day.startTime && !day.endTime) {
    return true
  }

  if (!day.startTime || !day.endTime) {
    return false
  }

  return day.startTime < day.endTime
}
