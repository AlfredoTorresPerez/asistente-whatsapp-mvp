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

export async function findBookingByCustomerAndDate(_customerPhone: string, _date: string): Promise<unknown | null> {
  return null
}

export async function getBookingStatus(_bookingId: string): Promise<string | null> {
  return null
}

export async function assertBookingStatus(_bookingId: string, _expectedStatus: string): Promise<boolean> {
  return false
}

export async function assertNoDuplicateBooking(_customerId: string, _professionalId: string, _startsAt: string): Promise<boolean> {
  return false
}

export async function assertBookingHistoryExists(_bookingId: string): Promise<boolean> {
  return false
}

export function isDbAvailable(): boolean {
  return DB_ENABLED
}
