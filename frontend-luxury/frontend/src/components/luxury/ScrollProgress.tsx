import { useRef } from 'react'
import { gsap, useGSAP, prefersReducedMotion } from '../../lib/gsap'

export default function ScrollProgress() {
  const bar = useRef<HTMLDivElement>(null)

  useGSAP(() => {
    if (prefersReducedMotion()) return
    gsap.fromTo(
      bar.current,
      { scaleX: 0 },
      {
        scaleX: 1,
        ease: 'none',
        scrollTrigger: { start: 0, end: 'max', scrub: 0.3 },
      },
    )
  })

  return <div className="lx-progress" ref={bar} aria-hidden="true" />
}
