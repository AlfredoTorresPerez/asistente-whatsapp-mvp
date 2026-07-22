// Database helper for E2E tests
// Backend integration tests use Testcontainers (src/test/resources/application-test.yml)
// For E2E, direct DB queries are still blocked (no exposed query endpoint).

const DB_ENABLED = false

export interface DbConnection {
  query: (sql: string, params?: Record<string, unknown>) => Promise<unknown[]>
  close: () => Promise<void>
}

export async function connectTestDatabase(): Promise<DbConnection | null> {
  if (!DB_ENABLED) return null
  return null
}

export async function cleanTestData(): Promise<{ ok: boolean; error?: string }> {
  if (!DB_ENABLED) return { ok: false, error: 'DB tests BLOCKED for E2E: no direct DB query endpoint exposed' }
  return { ok: true }
}

export async function findBookingByCustomerAndDate(): Promise<unknown | null> {
  return null
}

export async function getBookingStatus(): Promise<string | null> {
  return null
}

export async function assertBookingStatus(): Promise<boolean> {
  return false
}

export async function assertNoDuplicateBooking(): Promise<boolean> {
  return false
}

export async function assertBookingHistoryExists(): Promise<boolean> {
  return false
}

export function isDbAvailable(): boolean {
  return DB_ENABLED
}
