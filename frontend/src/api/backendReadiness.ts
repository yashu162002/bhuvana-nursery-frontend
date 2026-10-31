import { isAxiosError } from 'axios'
import http from './http'

/**
 * Backend readiness probe (Render cold-start handling).
 *
 * One module-level singleton loop, safe under React Strict Mode.
 * Calling startBackendProbe() many times still runs exactly one
 * request at a time. The loop stops the moment the backend answers.
 *
 * Probe endpoint: GET /categories — public, read-only, touches the DB,
 * so a successful JSON array response proves API + DB are both awake.
 * Goes through the existing axios instance (same baseURL / Netlify proxy).
 */

export type BackendStatus = 'waiting' | 'ready'

export interface BackendSnapshot {
  status: BackendStatus
  /** Date.now() when the probe started; the visual countdown derives from it. */
  startedAt: number
  /** Number of probe attempts made so far. */
  attempts: number
}

const PROBE_PATH = '/categories'
const REQUEST_TIMEOUT_MS = 20_000
const RETRY_MIN_MS = 2_000
const RETRY_MAX_MS = 3_000
/** Consecutive 500s tolerated (DB may still be waking) before we stop blocking. */
const MAX_CONSECUTIVE_500 = 4

/** Statuses meaning "server is not up yet" — keep waiting. */
const COLD_START_STATUSES = new Set([408, 425, 429, 502, 503, 504])

let snapshot: BackendSnapshot = { status: 'waiting', startedAt: 0, attempts: 0 }
let started = false
let retryTimer: number | undefined
let abort: AbortController | undefined
let consecutive500 = 0
/** Bumped whenever retryBackendNow() replaces the running loop. */
let generation = 0
const listeners = new Set<() => void>()

function publish(next: Partial<BackendSnapshot>) {
  snapshot = { ...snapshot, ...next }
  listeners.forEach((l) => l())
}

function markReady() {
  window.clearTimeout(retryTimer)
  retryTimer = undefined
  abort = undefined
  publish({ status: 'ready' })
}

/** true = backend is up; false = keep retrying. */
async function probeOnce(): Promise<boolean> {
  abort = new AbortController()
  try {
    const response = await http.get<unknown>(PROBE_PATH, {
      signal: abort.signal,
      timeout: REQUEST_TIMEOUT_MS,
      headers: { 'Cache-Control': 'no-cache' },
    })
    // A cold-start HTML interstitial can arrive as 200; only a real JSON array counts.
    return Array.isArray(response.data)
  } catch (error) {
    if (!isAxiosError(error)) return false
    if (!error.response) return false

    const status = error.response.status
    if (COLD_START_STATUSES.has(status)) return false

    if (status === 500) {
      consecutive500 += 1
      return consecutive500 >= MAX_CONSECUTIVE_500
    }

    // 401/403/404/other 4xx: server is awake, let the app's own error handling take over.
    return true
  }
}

function nextDelay(attempt: number): number {
  return Math.min(RETRY_MAX_MS, RETRY_MIN_MS + (attempt - 1) * 1_000)
}

async function loop() {
  const mine = ++generation
  const ok = await probeOnce()
  if (mine !== generation) return
  publish({ attempts: snapshot.attempts + 1 })
  if (ok) {
    markReady()
    return
  }
  retryTimer = window.setTimeout(() => void loop(), nextDelay(snapshot.attempts))
}

/** Idempotent — safe to call from effects (Strict Mode double-invoke, remounts). */
export function startBackendProbe(): void {
  if (started) return
  started = true
  publish({ startedAt: Date.now() })
  void loop()
}

/** Cancel any in-flight request and probe again immediately (retry button). */
export function retryBackendNow(): void {
  if (!started || snapshot.status === 'ready') return
  window.clearTimeout(retryTimer)
  retryTimer = undefined
  abort?.abort()
  void loop()
}

export function subscribeBackend(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function getBackendSnapshot(): BackendSnapshot {
  return snapshot
}
