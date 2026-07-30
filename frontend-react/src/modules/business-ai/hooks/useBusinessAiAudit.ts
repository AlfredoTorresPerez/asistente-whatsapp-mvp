import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { keepPreviousData } from '@tanstack/react-query'
import { listAestheticIntentLogs } from '../../../services/api/aestheticApi'
import { PAGE_SIZE } from '../lib/constants'
import { getAuditTotalPages, paginateAuditLogs, sortAuditLogsDescending } from '../lib/businessAiHelpers'

export function useBusinessAiAudit() {
  const [auditPage, setAuditPage] = useState(0)

  const logsQuery = useQuery({
    queryKey: ['business-ai', 'intent-logs'],
    queryFn: () => listAestheticIntentLogs({ size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  })

  const logs = useMemo(() => logsQuery.data?.items ?? [], [logsQuery.data?.items])

  const sortedLogs = useMemo(() => sortAuditLogsDescending(logs), [logs])

  const totalPages = getAuditTotalPages(logs.length, 5)
  const paginatedLogs = paginateAuditLogs(sortedLogs, auditPage, 5)

  return {
    logs,
    sortedLogs,
    paginatedLogs,
    auditPage,
    setAuditPage,
    totalPages,
    totalLogs: logs.length,
    isLoading: logsQuery.isLoading,
  }
}
