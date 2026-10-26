import { useEffect, type RefObject } from 'react'
import { gsap, hasFinePointer, prefersReducedMotion } from './gsap'

/**
 * Pointer-driven polish for desktop mice:
 *   [data-magnetic] - buttons lean toward the cursor
 *   [data-tilt]     - cards tilt in 3D under the cursor
 */
export function useInteractions(root: RefObject<HTMLElement | null>): void {
  useEffect(() => {
    const scope = root.current
    if (!scope || !hasFinePointer() || prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      const cleanups: Array<() => void> = []

      scope.querySelectorAll<HTMLElement>('[data-magnetic]').forEach((button) => {
        const target = button.querySelector<HTMLElement>('.lx-btn-label') ?? button
        const x = gsap.quickTo(target, 'x', { duration: 0.6, ease: 'elastic.out(1, 0.5)' })
        const y = gsap.quickTo(target, 'y', { duration: 0.6, ease: 'elastic.out(1, 0.5)' })

        const move = (event: PointerEvent) => {
          const rect = button.getBoundingClientRect()
          x((event.clientX - (rect.left + rect.width / 2)) * 0.28)
          y((event.clientY - (rect.top + rect.height / 2)) * 0.34)
        }
        const leave = () => {
          x(0)
          y(0)
        }

        button.addEventListener('pointermove', move)
        button.addEventListener('pointerleave', leave)
        cleanups.push(() => {
          button.removeEventListener('pointermove', move)
          button.removeEventListener('pointerleave', leave)
        })
      })

      scope.querySelectorAll<HTMLElement>('[data-tilt]').forEach((card) => {
        gsap.set(card, { transformPerspective: 900 })
        const rotateX = gsap.quickTo(card, 'rotationX', { duration: 0.7, ease: 'power3' })
        const rotateY = gsap.quickTo(card, 'rotationY', { duration: 0.7, ease: 'power3' })

        const move = (event: PointerEvent) => {
          const rect = card.getBoundingClientRect()
          const px = (event.clientX - rect.left) / rect.width - 0.5
          const py = (event.clientY - rect.top) / rect.height - 0.5
          rotateY(px * 9)
          rotateX(py * -9)
          card.style.setProperty('--mx', `${(px + 0.5) * 100}%`)
          card.style.setProperty('--my', `${(py + 0.5) * 100}%`)
        }
        const leave = () => {
          rotateX(0)
          rotateY(0)
        }

        card.addEventListener('pointermove', move)
        card.addEventListener('pointerleave', leave)
        cleanups.push(() => {
          card.removeEventListener('pointermove', move)
          card.removeEventListener('pointerleave', leave)
        })
      })

      return () => cleanups.forEach((fn) => fn())
    }, scope)

    return () => ctx.revert()
  }, [root])
}
