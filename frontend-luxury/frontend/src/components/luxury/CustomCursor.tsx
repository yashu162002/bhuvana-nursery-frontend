import { useEffect, useRef } from 'react'
import { gsap, hasFinePointer, prefersReducedMotion } from '../../lib/gsap'

/** A soft follower ring + dot for mouse users. Touch devices never see it. */
export default function CustomCursor() {
  const ring = useRef<HTMLDivElement>(null)
  const dot = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!hasFinePointer() || prefersReducedMotion()) return
    const ringEl = ring.current
    const dotEl = dot.current
    if (!ringEl || !dotEl) return

    const ctx = gsap.context(() => {
      gsap.set([ringEl, dotEl], { xPercent: -50, yPercent: -50 })
      const ringX = gsap.quickTo(ringEl, 'x', { duration: 0.55, ease: 'power3' })
      const ringY = gsap.quickTo(ringEl, 'y', { duration: 0.55, ease: 'power3' })
      const dotX = gsap.quickTo(dotEl, 'x', { duration: 0.12, ease: 'power3' })
      const dotY = gsap.quickTo(dotEl, 'y', { duration: 0.12, ease: 'power3' })

      const move = (event: PointerEvent) => {
        ringEl.classList.add('is-visible')
        dotEl.classList.add('is-visible')
        ringX(event.clientX)
        ringY(event.clientY)
        dotX(event.clientX)
        dotY(event.clientY)
      }

      const over = (event: PointerEvent) => {
        const hot = (event.target as HTMLElement | null)?.closest(
          'a, button, input, [data-cursor]',
        )
        ringEl.classList.toggle('is-active', Boolean(hot))
      }

      const leave = () => {
        ringEl.classList.remove('is-visible')
        dotEl.classList.remove('is-visible')
      }

      window.addEventListener('pointermove', move, { passive: true })
      window.addEventListener('pointerover', over, { passive: true })
      document.addEventListener('pointerleave', leave)

      return () => {
        window.removeEventListener('pointermove', move)
        window.removeEventListener('pointerover', over)
        document.removeEventListener('pointerleave', leave)
      }
    })

    return () => ctx.revert()
  }, [])

  return (
    <>
      <div className="lx-cursor-ring" ref={ring} aria-hidden="true" />
      <div className="lx-cursor-dot" ref={dot} aria-hidden="true" />
    </>
  )
}
