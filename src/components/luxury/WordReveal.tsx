import { useRef, type ElementType } from 'react'
import { gsap, useGSAP, prefersReducedMotion } from '../../lib/gsap'

export interface RevealLine {
  text: string
  /** Rendered in the accent (italic / gold) style. */
  accent?: boolean
}

interface WordRevealProps {
  lines: RevealLine[]
  as?: ElementType
  className?: string
  /** Hold the animation until this becomes true (e.g. after the preloader). */
  play?: boolean
  /** Start when scrolled into view instead of immediately. */
  scroll?: boolean
  delay?: number
  stagger?: number
}

/**
 * Masked word-by-word headline reveal.
 *
 * It deliberately splits by WORD, not by character: Kannada letters combine
 * into conjuncts and vowel signs, and splitting inside a word would break
 * the glyph shaping.
 */
export default function WordReveal({
  lines,
  as: Tag = 'h2',
  className = '',
  play = true,
  scroll = false,
  delay = 0,
  stagger = 0.075,
}: WordRevealProps) {
  const ref = useRef<HTMLElement>(null)
  const signature = lines.map((line) => line.text).join('|')
  const Component = Tag as 'h2'

  useGSAP(
    () => {
      if (prefersReducedMotion() || !play || !ref.current) return

      const words = ref.current.querySelectorAll('.lx-wi')

      gsap.fromTo(
        words,
        { y: 0, yPercent: 140, rotate: 5 },
        {
          y: 0,
          yPercent: 0,
          rotate: 0,
          duration: 1.4,
          ease: 'expo.out',
          stagger,
          delay,
          scrollTrigger: scroll
            ? { trigger: ref.current, start: 'top 88%', once: true }
            : undefined,
        },
      )
    },
    {
      scope: ref,
      dependencies: [play, scroll, delay, stagger, signature],
      revertOnUpdate: true,
    },
  )

  return (
    <Component
      ref={ref as never}
      className={`lx-split ${className}`.trim()}
      aria-label={lines.map((line) => line.text).join(' ')}
    >
      {lines.map((line, lineIndex) => (
        <span
          className={`lx-line${line.accent ? ' is-accent' : ''}`}
          key={lineIndex}
          aria-hidden="true"
        >
          {line.text.split(' ').map((word, wordIndex) => (
            <span key={wordIndex}>
              <span className="lx-w">
                <span className="lx-wi">{word}</span>
              </span>{' '}
            </span>
          ))}
        </span>
      ))}
    </Component>
  )
}
