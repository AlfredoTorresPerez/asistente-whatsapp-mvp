import { describe, expect, it } from 'vitest'
import type { ConversationSummaryResponse } from '../../../services/api/types'
import {
  filterConversations,
  getConversationInboxCounts,
  inferConversationCustomerTag,
  paginateConversations,
  translateConversationStatus,
} from './conversationInbox'

function conversation(overrides: Partial<ConversationSummaryResponse>): ConversationSummaryResponse {
  return {
    id: 'conversation-1',
    customerName: 'Laura Gomez',
    customerPhone: '+56911111111',
    status: 'OPEN',
    unreadCount: 0,
    lastMessagePreview: 'Hola, quiero consultar por limpieza facial',
    lastMessageAt: '2026-06-05T12:00:00Z',
    channelType: 'WHATSAPP_WEB',
    assignedUserId: null,
    assignedUserName: null,
    prospectId: null,
    locationId: null,
    locationName: null,
    ...overrides,
  }
}

describe('conversation inbox helpers', () => {
  const conversations = [
    conversation({ id: 'unread', unreadCount: 2, status: 'OPEN', customerName: 'Laura Gomez' }),
    conversation({ id: 'assigned', assignedUserId: 'user-1', assignedUserName: 'Ana Lopez', status: 'IN_PROGRESS' }),
    conversation({ id: 'resolved', status: 'CLOSED', customerName: 'Carlos Martinez' }),
    conversation({ id: 'archived', status: 'ARCHIVED', customerName: 'Valentina Perez' }),
  ]

  it('translates technical statuses to Spanish labels', () => {
    expect(translateConversationStatus('UNREAD')).toBe('No leido')
    expect(translateConversationStatus('IN_PROGRESS')).toBe('En progreso')
    expect(translateConversationStatus('UNKNOWN_STATUS')).toBe('Sin clasificar')
  })

  it('computes inbox counters by category', () => {
    const counts = getConversationInboxCounts(conversations)

    expect(counts.ALL).toBe(4)
    expect(counts.UNREAD).toBe(1)
    expect(counts.ASSIGNED).toBe(1)
    expect(counts.RESOLVED).toBe(1)
    expect(counts.ARCHIVED).toBe(1)
  })

  it('filters by category, status, assignee, tag and search text', () => {
    const filtered = filterConversations(conversations, {
      category: 'ALL',
      status: 'IN_PROGRESS',
      assignee: 'user-1',
      tag: 'CUSTOMER',
      search: 'ana',
    })

    expect(filtered.map((item) => item.id)).toEqual(['assigned'])
  })

  it('infers customer tags and paginates results', () => {
    expect(inferConversationCustomerTag(conversation({ unreadCount: 1 }))).toBe('NEW')
    expect(inferConversationCustomerTag(conversation({ prospectId: 'prospect-1' }))).toBe('LEAD')

    const page = paginateConversations(conversations, 0, 2)
    expect(page.items.map((item) => item.id)).toEqual(['unread', 'assigned'])
    expect(page.totalPages).toBe(2)
  })
})
