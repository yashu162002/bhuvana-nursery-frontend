import { useEffect, useRef } from 'react'
import { Sprout } from 'lucide-react'
import { gsap, ScrollTrigger, prefersReducedMotion } from '../../lib/gsap'

interface MarqueeProps {
  items: string[]
}

/** Infinite ticker that speeds up (and reverses) with scroll velocity. */
export default function Marquee({ items }: MarqueeProps) {
  const root = useRef<HTMLDivElement>(null)
  const track = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (prefersReducedMotion() || !root.current || !track.current) return

    let direction = 1
    let boost = 0

    const ctx = gsap.context(() => {
      const tween = gsap.to(track.current, {
        xPercent: -50,
        duration: 38,
        ease: 'none',
        repeat: -1,
      })

      ScrollTrigger.create({
        trigger: root.current,
        start: 'top bottom',
        end: 'bottom top',
        onUpdate: (self) => {
          direction = self.direction
          boost = Math.min(Math.abs(self.getVelocity()) / 260, 6)
        },
      })

      const tick = () => {
        boost *= 0.94
        tween.timeScale(direction * (1 + boost))
      }
      gsap.ticker.add(tick)

      return () => gsap.ticker.remove(tick)
    }, root)

    return () => ctx.revert()
  }, [])

  const run = [...items, ...items]

  return (
    <div className="lx-marquee" ref={root} aria-hidden="true">
      <div className="lx-marquee-track" ref={track}>
        {[0, 1].map((copy) => (
          <div className="lx-marquee-group" key={copy}>
            {run.map((item, index) => (
              <span className="lx-marquee-item" key={`${copy}-${index}`}>
                {item}
                <Sprout size={26} strokeWidth={1.4} />
              </span>
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}
