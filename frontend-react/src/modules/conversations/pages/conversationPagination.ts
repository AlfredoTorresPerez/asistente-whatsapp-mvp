export const CONVERSATIONS_PAGE_SIZE = 10

export function dedupeById<T extends { id: string }>(items: T[]) {
  return Array.from(new Map(items.map((item) => [item.id, item])).values())
}

export function getTotalConversationPages(totalItems: number, pageSize = CONVERSATIONS_PAGE_SIZE) {
  return Math.max(1, Math.ceil(totalItems / pageSize))
}

export function clampConversationPage(page: number, totalPages: number) {
  return Math.min(Math.max(page, 0), Math.max(totalPages - 1, 0))
}

export function getConversationPageItems<T>(items: T[], page: number, pageSize = CONVERSATIONS_PAGE_SIZE) {
  const start = clampConversationPage(page, getTotalConversationPages(items.length, pageSize)) * pageSize
  return items.slice(start, start + pageSize)
}
