import { describe, expect, it } from 'vitest'
import {
  CONVERSATIONS_PAGE_SIZE,
  clampConversationPage,
  dedupeById,
  getConversationPageItems,
  getTotalConversationPages,
} from './conversationPagination'

describe('conversation pagination helpers', () => {
  const conversations = Array.from({ length: 19 }, (_, index) => ({ id: `conversation-${index}` }))

  it('keeps the visible conversation page at eight items', () => {
    expect(CONVERSATIONS_PAGE_SIZE).toBe(10)
    expect(getConversationPageItems(conversations, 0)).toHaveLength(10)
    expect(getConversationPageItems(conversations, 1)).toHaveLength(9)
  })

  it('deduplicates repeated conversations before rendering pages', () => {
    expect(dedupeById([{ id: 'a' }, { id: 'b' }, { id: 'a' }])).toEqual([{ id: 'a' }, { id: 'b' }])
  })

  it('clamps invalid pages to an available page', () => {
    expect(getTotalConversationPages(0)).toBe(1)
    expect(clampConversationPage(-1, 3)).toBe(0)
    expect(clampConversationPage(4, 3)).toBe(2)
  })
})
