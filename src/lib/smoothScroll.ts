import Lenis from 'lenis'
import 'lenis/dist/lenis.css'
import { gsap, ScrollTrigger, prefersReducedMotion } from './gsap'

let lenis: Lenis | null = null
let tick: ((time: number) => void) | null = null
let users = 0
let locked = false

/** Start (or share) the single Lenis instance. Call release when done. */
export function acquireSmoothScroll(): void {
  users += 1
  if (lenis || prefersReducedMotion()) return

  lenis = new Lenis({
    lerp: 0.085,
    wheelMultiplier: 0.95,
    smoothWheel: true,
  })
  lenis.on('scroll', ScrollTrigger.update)

  tick = (time: number) => lenis?.raf(time * 1000)
  gsap.ticker.add(tick)
  gsap.ticker.lagSmoothing(0)

  if (locked) lenis.stop()
}

export function releaseSmoothScroll(): void {
  users = Math.max(0, users - 1)
  if (users > 0 || !lenis) return

  if (tick) gsap.ticker.remove(tick)
  lenis.destroy()
  lenis = null
  tick = null
  gsap.ticker.lagSmoothing(500, 33)
}

export function getLenis(): Lenis | null {
  return lenis
}

/** Freeze page scroll (preloader, mobile menu). */
export function lockScroll(): void {
  locked = true
  lenis?.stop()
  document.documentElement.classList.add('lx-locked')
}

export function unlockScroll(): void {
  locked = false
  lenis?.start()
  document.documentElement.classList.remove('lx-locked')
}

interface ScrollOptions {
  offset?: number
  immediate?: boolean
  duration?: number
}

export function scrollToTarget(
  target: string | HTMLElement | number,
  { offset = 0, immediate = false, duration = 1.7 }: ScrollOptions = {},
): void {
  if (lenis) {
    lenis.scrollTo(target, {
      offset,
      immediate,
      duration,
      easing: (t: number) => (t === 1 ? 1 : 1 - Math.pow(2, -10 * t)),
    })
    return
  }

  if (typeof target === 'number') {
    window.scrollTo({ top: target + offset, behavior: immediate ? 'auto' : 'smooth' })
    return
  }

  const element =
    typeof target === 'string' ? document.querySelector(target) : target
  element?.scrollIntoView({ behavior: immediate ? 'auto' : 'smooth' })
}
