import { describe, expect, it } from 'vitest'
import {
  BUSINESS_AI_AUDIT_PAGE_SIZE,
  DEFAULT_BUSINESS_HOURS,
  getAuditTotalPages,
  isBusinessHourRangeValid,
  paginateAuditLogs,
  sortAuditLogsDescending,
} from './businessAiHelpers'

describe('business AI helpers', () => {
  it('defines the seven required business days with editable hour ranges', () => {
    expect(DEFAULT_BUSINESS_HOURS.map((day) => day.day)).toEqual([
      'Lunes',
      'Martes',
      'Miércoles',
      'Jueves',
      'Viernes',
      'Sábado',
      'Domingo',
    ])
  })

  it('validates start time before end time', () => {
    expect(isBusinessHourRangeValid({ day: 'Lunes', startTime: '09:00', endTime: '18:00' })).toBe(
      true,
    )
    expect(isBusinessHourRangeValid({ day: 'Lunes', startTime: '18:00', endTime: '09:00' })).toBe(
      false,
    )
    expect(isBusinessHourRangeValid({ day: 'Domingo', startTime: '', endTime: '' })).toBe(true)
    expect(isBusinessHourRangeValid({ day: 'Domingo', startTime: '09:00', endTime: '' })).toBe(
      false,
    )
  })

  it('sorts audit entries by newest first and paginates exactly five items', () => {
    const logs = Array.from({ length: 12 }, (_, index) => ({
      id: `log-${index}`,
      createdAt: `2026-05-${String(index + 1).padStart(2, '0')}T10:00:00Z`,
    }))

    const sorted = sortAuditLogsDescending(logs)
    expect(sorted[0].id).toBe('log-11')
    expect(paginateAuditLogs(sorted, 0)).toHaveLength(BUSINESS_AI_AUDIT_PAGE_SIZE)
    expect(paginateAuditLogs(sorted, 0)[0].id).toBe('log-11')
    expect(paginateAuditLogs(sorted, 2)).toHaveLength(2)
    expect(getAuditTotalPages(logs.length)).toBe(3)
  })
})
