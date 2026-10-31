import { useEffect, useState, useSyncExternalStore, type CSSProperties } from 'react'
import {
  getBackendSnapshot,
  retryBackendNow,
  startBackendProbe,
  subscribeBackend,
} from '../../api/backendReadiness'
import './CatalogueLoader.css'

/** Visual countdown max. NOT a timeout — backend readiness controls when it hides. */
const COUNTDOWN_SECONDS = 180

const MESSAGES = [
  'ನಿಮ್ಮ ತೋಟಕ್ಕಾಗಿ ಉತ್ತಮ ಸಸಿಗಳನ್ನು ಸಿದ್ಧಪಡಿಸುತ್ತಿದ್ದೇವೆ',
  'ಹಣ್ಣಿನ ಸಸಿಗಳು, ಕೃಷಿ ಸಸಿಗಳು, ಮರಗಳು — ಎಲ್ಲವೂ ನಿಮಗಾಗಿ ಕಾಯುತ್ತಿವೆ',
  'ನಮ್ಮ ಸಸ್ಯ ಲೋಕವನ್ನು ನಿಮ್ಮ ಮುಂದೆ ತರುತ್ತಿದ್ದೇವೆ 🌱',
]
const MESSAGE_SECONDS = 9

const LEAVES = [
  { x: '6%',  y: '12%', s: 44, d: 11, t: -2,  r: -24 },
  { x: '86%', y: '8%',  s: 34, d: 13, t: -6,  r: 28  },
  { x: '88%', y: '62%', s: 50, d: 15, t: -9,  r: -12 },
  { x: '4%',  y: '70%', s: 38, d: 12, t: -4,  r: 36  },
]

const SEEDS = [
  { x: '18%', d: 17, t: -3  },
  { x: '42%', d: 21, t: -11 },
  { x: '68%', d: 19, t: -6  },
  { x: '88%', d: 23, t: -14 },
]

function elapsedSince(startedAt: number): number {
  return Math.max(0, Math.floor((Date.now() - startedAt) / 1000))
}

function format(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/**
 * Inline backend-wake-up loader shown inside the PlantCatalogue section.
 * Starts the probe, shows the countdown, and calls onReady() the moment
 * the backend responds — which triggers a normal catalogue fetch.
 */
export default function CatalogueLoader({ onReady }: { onReady: () => void }) {
  const backend = useSyncExternalStore(subscribeBackend, getBackendSnapshot)

  // Start the singleton probe (idempotent — safe to call multiple times).
  useEffect(() => {
    startBackendProbe()
  }, [])

  // Fire onReady the moment the backend becomes available.
  useEffect(() => {
    if (backend.status === 'ready') onReady()
  }, [backend.status, onReady])

  const [elapsed, setElapsed] = useState(() => elapsedSince(backend.startedAt))
  const [ringOffset] = useState(() => Math.max(0, (Date.now() - backend.startedAt) / 1000))

  // Countdown tick — stops when backend is ready.
  useEffect(() => {
    if (backend.status === 'ready') return
    const timer = window.setInterval(
      () => setElapsed(elapsedSince(backend.startedAt)),
      500,
    )
    return () => window.clearInterval(timer)
  }, [backend.status, backend.startedAt])

  const remaining = Math.max(0, COUNTDOWN_SECONDS - elapsed)
  const overtime = remaining === 0
  const message = MESSAGES[Math.floor(elapsed / MESSAGE_SECONDS) % MESSAGES.length]
  const spoken = Math.ceil(remaining / 30) * 30

  return (
    <div
      className={`cl-loader${overtime ? ' cl-overtime' : ''}`}
      role="status"
      aria-label="ಸಸ್ಯ ಸಂಗ್ರಹ ಲೋಡ್ ಆಗುತ್ತಿದೆ"
    >
      {/* Hidden SVG symbol reused by leaves */}
      <svg width="0" height="0" className="cl-defs" aria-hidden="true" focusable="false">
        <defs>
          <symbol id="cl-leaf" viewBox="0 0 120 140">
            <path d="M60 80 C30 80 14 60 14 32 C42 32 60 50 60 80 Z" />
            <path d="M60 58 C60 30 78 12 106 12 C106 40 88 58 60 58 Z" />
            <path
              d="M60 134 C60 104 60 74 60 40"
              fill="none"
              stroke="currentColor"
              strokeWidth="3"
              strokeLinecap="round"
            />
          </symbol>
        </defs>
      </svg>

      {/* Decorative background */}
      <div className="cl-bg" aria-hidden="true">
        <span className="cl-glow cl-glow--a" />
        <span className="cl-glow cl-glow--b" />
        {LEAVES.map((leaf, i) => (
          <svg
            key={i}
            className="cl-leaf"
            style={{
              '--x': leaf.x,
              '--y': leaf.y,
              '--s': `${leaf.s}px`,
              '--d': `${leaf.d}s`,
              '--t': `${leaf.t}s`,
              '--r': `${leaf.r}deg`,
            } as CSSProperties}
          >
            <use href="#cl-leaf" />
          </svg>
        ))}
        {SEEDS.map((seed, i) => (
          <span
            key={i}
            className="cl-seed"
            style={{ '--x': seed.x, '--d': `${seed.d}s`, '--t': `${seed.t}s` } as CSSProperties}
          />
        ))}
      </div>

      {/* Card */}
      <div className="cl-card">
        <img
          className="cl-logo"
          src="/logo.png"
          alt="ಭೂವನ ನರ್ಸರಿ — Bhoovana Nursery"
          width={512}
          height={341}
          decoding="async"
        />

        <h2 className="cl-title">ನಿಮಗಾಗಿ ಸಸ್ಯಗಳನ್ನು ಸಿದ್ಧಪಡಿಸಲಾಗುತ್ತಿದೆ</h2>
        <p className="cl-sub">ದಯವಿಟ್ಟು ಸ್ವಲ್ಪ ಸಮಯ ಕಾಯಿರಿ...</p>

        {/* SVG countdown ring */}
        <div className="cl-dial" aria-hidden="true">
          <svg className="cl-ring" viewBox="0 0 200 200">
            <circle className="cl-ring-track" cx="100" cy="100" r="88" pathLength="100" />
            <circle
              className="cl-ring-fill"
              cx="100"
              cy="100"
              r="88"
              pathLength="100"
              style={{
                animationDuration: `${COUNTDOWN_SECONDS}s`,
                animationDelay: `-${ringOffset.toFixed(2)}s`,
              }}
            />
            <circle className="cl-ring-wait" cx="100" cy="100" r="88" pathLength="100" />
          </svg>
          <span className="cl-pulse" />
          <div className="cl-dial-inner">
            <span className="cl-time">{format(remaining)}</span>
            <span className="cl-time-label">
              {overtime ? 'ಇನ್ನೂ ಸ್ವಲ್ಪ ಕಾಯಿರಿ' : 'ಗರಿಷ್ಠ ಸಮಯ'}
            </span>
          </div>
        </div>

        {/* Rotating message */}
        <p className="cl-message" key={overtime ? 'late' : message}>
          <svg className="cl-message-icon" aria-hidden="true">
            <use href="#cl-leaf" />
          </svg>
          {overtime
            ? 'ಸಸ್ಯ ಲೋಕ ಸಿದ್ಧವಾಗುತ್ತಿದೆ... ಇನ್ನೂ ಸ್ವಲ್ಪ ಸಮಯ ಕಾಯಿರಿ 🌱'
            : message}
        </p>

        {/* Progress bar sweep */}
        <span className="cl-bar" aria-hidden="true" />

        {/* Retry button (overtime only) */}
        {overtime && (
          <button type="button" className="cl-retry" onClick={retryBackendNow}>
            ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ
          </button>
        )}

        {/* Screen-reader polite announcement */}
        <span className="cl-sr" aria-live="polite">
          {overtime
            ? 'Still preparing the nursery. Please keep this page open.'
            : `Preparing plants for you. About ${spoken} seconds at most.`}
        </span>
      </div>
    </div>
  )
}
