import { request } from '@playwright/test'
import type { APIRequestContext } from '@playwright/test'

const API_BASE_URL = 'http://localhost:8080/api/v1'
const HEALTH_ENDPOINT = '/actuator/health'

let apiContext: APIRequestContext | null = null

export async function getApiContext(): Promise<APIRequestContext> {
  if (!apiContext) {
    apiContext = await request.newContext({ baseURL: API_BASE_URL, timeout: 10000 })
  }
  return apiContext
}

export async function apiGet(path: string, token?: string) {
  const context = await getApiContext()
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`
  return context.get(path, { headers })
}

export async function apiPost(path: string, body: Record<string, unknown>, token?: string) {
  const context = await getApiContext()
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`
  return context.post(path, { data: body, headers })
}

export async function apiPatch(path: string, body: Record<string, unknown>, token?: string) {
  const context = await getApiContext()
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`
  return context.patch(path, { data: body, headers })
}

export async function expectApiHealthy() {
  try {
    const context = await getApiContext()
    const response = await context.get(HEALTH_ENDPOINT)
    if (response.status() !== 200) {
      return { healthy: false, status: response.status(), body: await response.text() }
    }
    const body = await response.json()
    return { healthy: body.status === 'UP', status: response.status(), body }
  } catch (error) {
    return { healthy: false, status: 0, body: null, error: String(error) }
  }
}

export async function disposeApiContext() {
  if (apiContext) {
    await apiContext.dispose()
    apiContext = null
  }
}
