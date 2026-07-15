import type { ConversationSummaryResponse } from '../../../services/api/types'

export type ConversationInboxCategory =
  'ALL' | 'UNREAD' | 'ASSIGNED' | 'PENDING' | 'RESOLVED' | 'ARCHIVED'
export type ConversationCustomerTag = 'NEW' | 'CUSTOMER' | 'VIP' | 'LEAD' | 'UNKNOWN'

export type ConversationInboxFilters = {
  category: ConversationInboxCategory
  status: string
  assignee: string
  tag: string
  search: string
}

export const CONVERSATION_INBOX_DEFAULT_PAGE_SIZE = 10

export const conversationInboxCategories: Array<{ key: ConversationInboxCategory; label: string }> =
  [
    { key: 'ALL', label: 'Todas' },
    { key: 'UNREAD', label: 'No leidos' },
    { key: 'ASSIGNED', label: 'Asignadas' },
    { key: 'PENDING', label: 'Pendientes' },
    { key: 'RESOLVED', label: 'Resueltas' },
    { key: 'ARCHIVED', label: 'Archivadas' },
  ]

export function translateConversationStatus(status: string | null | undefined) {
  const normalized = (status ?? '').toUpperCase()
  const labels: Record<string, string> = {
    UNREAD: 'No leido',
    IN_PROGRESS: 'En progreso',
    PENDING: 'Pendiente',
    RESOLVED: 'Resuelto',
    ARCHIVED: 'Archivado',
    OPEN: 'Abierto',
    CLOSED: 'Cerrado',
    ASSIGNED: 'Asignado',
    SENT: 'Enviado',
    FAILED: 'Fallido',
    DELIVERED: 'Entregado',
    READ: 'Leido',
    SIMULATED: 'Simulado',
    DRY_RUN: 'Simulacion',
  }

  return labels[normalized] ?? 'Sin clasificar'
}

export function getConversationStatusTone(status: string | null | undefined, unreadCount = 0) {
  const normalized = (status ?? '').toUpperCase()

  if (unreadCount > 0 || normalized === 'UNREAD') {
    return 'unread'
  }

  if (normalized === 'PENDING') {
    return 'pending'
  }

  if (normalized === 'RESOLVED' || normalized === 'CLOSED') {
    return 'resolved'
  }

  if (normalized === 'ARCHIVED') {
    return 'archived'
  }

  if (normalized === 'IN_PROGRESS' || normalized === 'OPEN' || normalized === 'ASSIGNED') {
    return 'progress'
  }

  return 'neutral'
}

export function inferConversationCustomerTag(
  conversation: ConversationSummaryResponse,
): ConversationCustomerTag {
  const text =
    `${conversation.customerName} ${conversation.customerPhone} ${conversation.lastMessagePreview ?? ''}`.toLowerCase()
  const unreadCount = conversation.unreadCount ?? 0

  if (text.includes('vip')) {
    return 'VIP'
  }

  if (unreadCount > 0 || text.includes('nuevo')) {
    return 'NEW'
  }

  if (
    conversation.prospectId ||
    text.includes('lead') ||
    text.includes('cotizar') ||
    text.includes('presupuesto')
  ) {
    return 'LEAD'
  }

  if (conversation.lastMessagePreview || conversation.customerName || conversation.customerPhone) {
    return 'CUSTOMER'
  }

  return 'UNKNOWN'
}

export function translateCustomerTag(tag: ConversationCustomerTag) {
  const labels: Record<ConversationCustomerTag, string> = {
    NEW: 'Nuevo',
    CUSTOMER: 'Cliente',
    VIP: 'VIP',
    LEAD: 'Lead',
    UNKNOWN: 'Sin clasificar',
  }

  return labels[tag]
}

export function matchesConversationCategory(
  conversation: ConversationSummaryResponse,
  category: ConversationInboxCategory,
) {
  const status = conversation.status.toUpperCase()

  if (category === 'UNREAD') {
    return conversation.unreadCount > 0 || status === 'UNREAD'
  }

  if (category === 'ASSIGNED') {
    return (
      Boolean(conversation.assignedUserId || conversation.assignedUserName) || status === 'ASSIGNED'
    )
  }

  if (category === 'PENDING') {
    return status === 'PENDING' || status === 'OPEN' || conversation.unreadCount > 0
  }

  if (category === 'RESOLVED') {
    return status === 'RESOLVED' || status === 'CLOSED'
  }

  if (category === 'ARCHIVED') {
    return status === 'ARCHIVED'
  }

  return true
}

export function getConversationInboxCounts(conversations: ConversationSummaryResponse[]) {
  return conversationInboxCategories.reduce<Record<ConversationInboxCategory, number>>(
    (counts, category) => {
      counts[category.key] = conversations.filter((conversation) =>
        matchesConversationCategory(conversation, category.key),
      ).length
      return counts
    },
    {
      ALL: 0,
      UNREAD: 0,
      ASSIGNED: 0,
      PENDING: 0,
      RESOLVED: 0,
      ARCHIVED: 0,
    },
  )
}

export function getConversationAssignees(conversations: ConversationSummaryResponse[]) {
  return Array.from(
    new Map(
      conversations
        .filter((conversation) => conversation.assignedUserId || conversation.assignedUserName)
        .map((conversation) => [
          conversation.assignedUserId ?? conversation.assignedUserName ?? 'sin-id',
          {
            id: conversation.assignedUserId ?? conversation.assignedUserName ?? 'sin-id',
            name: conversation.assignedUserName ?? 'Sin asignar',
          },
        ]),
    ).values(),
  ).sort((left, right) => left.name.localeCompare(right.name, 'es'))
}

export function filterConversations(
  conversations: ConversationSummaryResponse[],
  filters: ConversationInboxFilters,
) {
  const normalizedSearch = filters.search.trim().toLowerCase()

  return conversations.filter((conversation) => {
    if (!matchesConversationCategory(conversation, filters.category)) {
      return false
    }

    if (filters.status !== 'ALL') {
      const translatedStatus = translateConversationStatus(conversation.status).toLowerCase()
      if (
        conversation.status.toUpperCase() !== filters.status &&
        translatedStatus !== filters.status.toLowerCase()
      ) {
        return false
      }
    }

    if (filters.assignee !== 'ALL') {
      const assigneeKey =
        conversation.assignedUserId ?? conversation.assignedUserName ?? 'UNASSIGNED'
      if (filters.assignee === 'UNASSIGNED') {
        if (conversation.assignedUserId || conversation.assignedUserName) {
          return false
        }
      } else if (assigneeKey !== filters.assignee) {
        return false
      }
    }

    const customerTag = inferConversationCustomerTag(conversation)
    if (filters.tag !== 'ALL' && customerTag !== filters.tag) {
      return false
    }

    if (!normalizedSearch) {
      return true
    }

    const searchableText = [
      conversation.customerName,
      conversation.customerPhone,
      conversation.lastMessagePreview ?? '',
      conversation.assignedUserName ?? '',
      conversation.status,
      translateConversationStatus(conversation.status),
      translateCustomerTag(customerTag),
    ]
      .join(' ')
      .toLowerCase()

    return searchableText.includes(normalizedSearch)
  })
}

export function paginateConversations<T>(items: T[], page: number, pageSize: number) {
  const safePageSize = Math.max(1, pageSize)
  const totalPages = Math.max(1, Math.ceil(items.length / safePageSize))
  const currentPage = Math.min(Math.max(page, 0), totalPages - 1)
  const start = currentPage * safePageSize

  return {
    currentPage,
    totalPages,
    start,
    end: Math.min(start + safePageSize, items.length),
    items: items.slice(start, start + safePageSize),
  }
}
