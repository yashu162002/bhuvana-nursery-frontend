import { useRef } from 'react'
import { gsap, useGSAP } from '../../lib/gsap'
import { lockScroll, unlockScroll } from '../../lib/smoothScroll'
import { useLanguage } from '../../context/languageContext'

interface PreloaderProps {
  /** Fires while the curtain is lifting, so the hero can start animating. */
  onReveal: () => void
  onFinish: () => void
}

export default function Preloader({ onReveal, onFinish }: PreloaderProps) {
  const root = useRef<HTMLDivElement>(null)
  const { isKannada } = useLanguage()

  useGSAP(
    () => {
      const el = root.current
      if (!el) return

      lockScroll()

      const counter = { value: 0 }
      const number = el.querySelector<HTMLElement>('.lx-pre-num')
      const paths = Array.from(el.querySelectorAll<SVGPathElement>('.lx-pre-leaf path'))

      paths.forEach((path) => {
        const length = path.getTotalLength()
        gsap.set(path, { strokeDasharray: length, strokeDashoffset: length })
      })

      const tl = gsap.timeline({
        onComplete: () => {
          unlockScroll()
          onFinish()
        },
      })

      tl.fromTo(
        '.lx-pre-wi',
        { y: 0, yPercent: 125 },
        { y: 0, yPercent: 0, duration: 1.2, stagger: 0.14, ease: 'expo.out' },
        0.25,
      )
        .to(
          paths,
          { strokeDashoffset: 0, duration: 1.7, stagger: 0.18, ease: 'power2.inOut' },
          0.1,
        )
        .to(paths, { fillOpacity: 0.16, duration: 0.8, ease: 'power1.out' }, 1.5)
        .to(
          counter,
          {
            value: 100,
            duration: 2,
            ease: 'power2.inOut',
            onUpdate: () => {
              if (number) number.textContent = String(Math.round(counter.value)).padStart(3, '0')
            },
          },
          0.1,
        )
        .to('.lx-pre-fill', { scaleX: 1, duration: 2, ease: 'power2.inOut' }, 0.1)
        .to(
          '.lx-pre-content',
          { yPercent: -25, autoAlpha: 0, duration: 0.7, ease: 'power3.in' },
          2.3,
        )
        .to(
          el,
          {
            yPercent: -100,
            borderBottomLeftRadius: '50% 16vh',
            borderBottomRightRadius: '50% 16vh',
            duration: 1.25,
            ease: 'expo.inOut',
          },
          2.45,
        )
        .add(onReveal, 2.95)
    },
    { scope: root },
  )

  return (
    <div className="lx-pre" ref={root} role="status" aria-label="Loading">
      <div className="lx-pre-content">
        <svg
          className="lx-pre-leaf"
          viewBox="0 0 120 140"
          fill="currentColor"
          fillOpacity="0"
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M60 134 C60 104 60 74 60 40" />
          <path d="M60 80 C30 80 14 60 14 32 C42 32 60 50 60 80 Z" />
          <path d="M60 58 C60 30 78 12 106 12 C106 40 88 58 60 58 Z" />
        </svg>

        <div className="lx-pre-brand" aria-hidden="true">
          {(isKannada ? ['ಭೂವನ', 'ನರ್ಸರಿ'] : ['Bhoovana', 'Nursery']).map((word) => (
            <span className="lx-pre-w" key={word}>
              <span className="lx-pre-wi">{word}</span>
            </span>
          ))}
        </div>

        <div className="lx-pre-meter">
          <span className="lx-pre-line">
            <span className="lx-pre-fill" />
          </span>
          <span className="lx-pre-num">000</span>
        </div>
      </div>
    </div>
  )
}
