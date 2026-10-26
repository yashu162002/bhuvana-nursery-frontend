import { useEffect, useRef, useState } from 'react'
import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Check,
  MapPin,
} from 'lucide-react'
import { gsap, useGSAP, prefersReducedMotion } from '../../lib/gsap'
import { heroSlides, type HeroSlide } from '../../config/media'
import { getHeroSlides } from '../../api/adminHeroSlideApi'
import WordReveal from './WordReveal'

/** Seconds a photo is held still (drifting) before the next transition. */
const HOLD = 4.4
/** Seconds the cinematic transition itself takes. */
const TRANSITION = 2.3

/**
 * Four signature transitions that rotate continuously:
 *   wipe from the right -> rise from below -> iris from a corner -> wipe from the left.
 * x / y are the parallax offset (in %) the incoming photo starts from.
 */
const TRANSITIONS = [
  { from: 'inset(0% 0% 0% 100%)', to: 'inset(0% 0% 0% 0%)', x: 14, y: 0 },
  { from: 'inset(100% 0% 0% 0%)', to: 'inset(0% 0% 0% 0%)', x: 0, y: 14 },
  { from: 'circle(0% at 82% 78%)', to: 'circle(150% at 82% 78%)', x: -10, y: -6 },
  { from: 'inset(0% 100% 0% 0%)', to: 'inset(0% 0% 0% 0%)', x: -14, y: 0 },
]

interface HeroProps {
  kannada: boolean
  /** False while the preloader is still covering the page. */
  ready: boolean
  onExplore: () => void
  onDiscover: () => void
  directionsUrl: string
}

export default function Hero({
  kannada,
  ready,
  onExplore,
  onDiscover,
  directionsUrl,
}: HeroProps) {
  const root = useRef<HTMLElement>(null)
  const slideEls = useRef<(HTMLDivElement | null)[]>([])

  const [slides, setSlides] = useState<HeroSlide[]>(heroSlides)
  const [current, setCurrent] = useState(0)
  const [failed, setFailed] = useState<ReadonlySet<number>>(() => new Set())

  // Imperative slideshow state lives in refs so GSAP callbacks never go stale.
  const currentRef = useRef(0)
  const busy = useRef(false)
  const failedRef = useRef<ReadonlySet<number>>(failed)
  const timer = useRef<gsap.core.Tween | null>(null)
  const transitionCount = useRef(0)
  const goRef = useRef<(target: number) => void>(() => {})
  const startRef = useRef<() => void>(() => {})
  const startedRef = useRef(false)

  const motion = !prefersReducedMotion()
  const total = slides.length

  useEffect(() => {
    let active = true
    getHeroSlides()
      .then((data) => {
        if (!active) return
        if (Array.isArray(data) && data.length > 0) {
          setSlides(
            data.map((item) => ({
              src: item.imageUrl,
              kn: item.titleKn?.trim() || item.altText?.trim() || 'ಹಸಿರು ಸಸ್ಯಲೋಕ',
              en: item.titleEn?.trim() || item.altText?.trim() || 'Bhuvana Nursery',
            })),
          )
        }
      })
      .catch(() => {
        // Fall back to the default hero slides
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    failedRef.current = failed
  }, [failed])

  // Preload every photo; anything that fails to load is skipped for good.
  useEffect(() => {
    setFailed(new Set())
    slides.forEach((slide, index) => {
      const image = new Image()
      image.onerror = () => setFailed((previous) => new Set(previous).add(index))
      image.src = slide.src
    })
  }, [slides])

  /* ---- slideshow engine ------------------------------------ */
  useGSAP(
    (_context, contextSafe) => {
      const safe = contextSafe as <T extends (...args: never[]) => unknown>(fn: T) => T
      const slideElements = slideEls.current.filter(Boolean) as HTMLDivElement[]
      if (!slideElements.length) return

      const imageOf = (slide: HTMLElement) => slide.querySelector('.lx-hs-img') as HTMLElement

      gsap.set(slideElements, { autoAlpha: 0, zIndex: 1 })
      gsap.set(slideElements[0], { autoAlpha: 1, zIndex: 2, clipPath: 'inset(0% 0% 0% 0%)' })
      gsap.set(imageOf(slideElements[0]), { scale: motion ? 1.4 : 1.06 })

      const pickNext = (from: number, step: number) => {
        for (let i = 1; i <= total; i += 1) {
          const index = (from + step * i + total * 4) % total
          if (!failedRef.current.has(index)) return index
        }
        return from
      }

      const validCount = () => total - failedRef.current.size

      const drift = (image: HTMLElement) => {
        gsap.killTweensOf(image)
        gsap.to(image, { scale: 1.04, duration: HOLD + TRANSITION + 2, ease: 'none' })
      }

      const fills = () =>
        Array.from(root.current?.querySelectorAll<HTMLElement>('.lx-hs-fill') ?? [])

      const runProgress = (target: number) => {
        fills().forEach((fill, index) => {
          gsap.killTweensOf(fill)
          gsap.set(fill, { scaleX: index < target ? 1 : 0 })
        })
        const active = fills()[target]
        if (active && motion) {
          gsap.to(active, { scaleX: 1, duration: TRANSITION + HOLD, ease: 'none' })
        } else if (active) {
          gsap.set(active, { scaleX: 1 })
        }
      }

      const schedule = () => {
        timer.current?.kill()
        if (!motion || validCount() < 2) return
        timer.current = gsap.delayedCall(HOLD, () => {
          goRef.current(pickNext(currentRef.current, 1))
        })
      }

      const go = safe((target: number) => {
        if (busy.current || target === currentRef.current) return

        timer.current?.kill()

        const from = currentRef.current
        const incoming = slideElements[target]
        const outgoing = slideElements[from]
        if (!incoming || !outgoing) return

        const incomingImage = imageOf(incoming)
        const outgoingImage = imageOf(outgoing)

        currentRef.current = target
        setCurrent(target)
        runProgress(target)

        if (!motion) {
          gsap.set(outgoing, { autoAlpha: 0 })
          gsap.set(incoming, { autoAlpha: 1, clipPath: 'inset(0% 0% 0% 0%)' })
          return
        }

        busy.current = true
        const transition = TRANSITIONS[transitionCount.current % TRANSITIONS.length]
        transitionCount.current += 1

        gsap.killTweensOf([incomingImage, outgoingImage])
        gsap.set(incoming, { autoAlpha: 1, zIndex: 3, clipPath: transition.from })
        gsap.set(incomingImage, { scale: 1.45, xPercent: transition.x, yPercent: transition.y })

        gsap
          .timeline({
            onComplete: () => {
              gsap.set(outgoing, { autoAlpha: 0, zIndex: 1 })
              gsap.set(incoming, { zIndex: 2 })
              busy.current = false
              drift(incomingImage)
              schedule()
            },
          })
          .to(incoming, { clipPath: transition.to, duration: TRANSITION - 0.4, ease: 'expo.inOut' }, 0)
          .to(
            incomingImage,
            { scale: 1.1, xPercent: 0, yPercent: 0, duration: TRANSITION, ease: 'power3.out' },
            0,
          )
          .to(
            outgoingImage,
            {
              xPercent: -transition.x * 0.55,
              yPercent: -transition.y * 0.55,
              scale: 1.2,
              duration: TRANSITION - 0.4,
              ease: 'expo.inOut',
            },
            0,
          )
      })

      goRef.current = go

      // Called once the preloader lifts.
      startRef.current = safe(() => {
        if (startedRef.current) {
          schedule()
          return
        }
        startedRef.current = true

        const first = imageOf(slideElements[0])
        gsap.fromTo(
          first,
          { scale: motion ? 1.4 : 1.06 },
          {
            scale: 1.06,
            duration: motion ? 3.2 : 0,
            ease: 'power3.out',
            onComplete: () => drift(first),
          },
        )
        runProgress(0)

        if (failedRef.current.has(0)) {
          goRef.current(pickNext(0, 1))
        } else {
          schedule()
        }
      })

      if (ready) {
        startRef.current()
      }
    },
    { scope: root, dependencies: [slides] },
  )

  /* ---- intro choreography, plays when the page is ready --------------- */
  useGSAP(
    () => {
      if (!ready) return
      startRef.current()

      if (!motion) return

      gsap
        .timeline({ defaults: { ease: 'expo.out' } })
        .fromTo('.lx-hero-eyebrow', { autoAlpha: 0, y: 26 }, { autoAlpha: 1, y: 0, duration: 1.3 }, 0.35)
        .fromTo('.lx-hero-sub', { autoAlpha: 0, y: 34 }, { autoAlpha: 1, y: 0, duration: 1.4 }, 1.0)
        .fromTo('.lx-hero-actions > *', { autoAlpha: 0, y: 34 }, { autoAlpha: 1, y: 0, duration: 1.3, stagger: 0.12 }, 1.2)
        .fromTo('.lx-hero-note', { autoAlpha: 0 }, { autoAlpha: 1, duration: 1.4 }, 1.6)
        .fromTo('.lx-hs-ui', { autoAlpha: 0, y: 50 }, { autoAlpha: 1, y: 0, duration: 1.4 }, 1.3)
        .fromTo('.lx-hero-vert', { autoAlpha: 0, x: 24 }, { autoAlpha: 1, x: 0, duration: 1.4 }, 1.5)
        .fromTo('.lx-hero-frame', { autoAlpha: 0, scale: 1.04 }, { autoAlpha: 1, scale: 1, duration: 2 }, 0.2)
    },
    { scope: root, dependencies: [ready] },
  )

  /* ---- caption swap on every slide change ----------------------------- */
  useGSAP(
    () => {
      if (!ready || !motion) return
      gsap.fromTo(
        '.lx-hs-cap-i',
        { yPercent: 115 },
        { yPercent: 0, duration: 1.1, ease: 'expo.out', delay: 0.55, stagger: 0.08 },
      )
    },
    { scope: root, dependencies: [current, ready, kannada, slides] },
  )

  // Gentle scroll-away: the whole hero drifts and dims as you leave it.
  useGSAP(
    () => {
      if (!motion || !root.current) return
      gsap.to('.lx-hs', {
        yPercent: 14,
        ease: 'none',
        scrollTrigger: { trigger: root.current, start: 'top top', end: 'bottom top', scrub: true },
      })
      gsap.to('.lx-hero-inner', {
        yPercent: -10,
        autoAlpha: 0.15,
        ease: 'none',
        scrollTrigger: { trigger: root.current, start: 'top top', end: '80% top', scrub: true },
      })
    },
    { scope: root },
  )

  function step(direction: 1 | -1) {
    for (let i = 1; i <= total; i += 1) {
      const index = (currentRef.current + direction * i + total * 4) % total
      if (!failedRef.current.has(index)) {
        goRef.current(index)
        return
      }
    }
  }

  const visibleSlides = slides
    .map((slide, index) => ({ slide, index }))
    .filter(({ index }) => !failed.has(index))
  const position = Math.max(
    0,
    visibleSlides.findIndex(({ index }) => index === current),
  )
  const caption = slides[current] ?? slides[0]

  const headline = kannada
    ? [
        { text: 'ನಿಮ್ಮ ತೋಟದ' },
        { text: 'ಹಸಿರು ಕನಸುಗಳಿಗೆ' },
        { text: 'ಭೂವನ	 ನರ್ಸರಿ.', accent: true },
      ]
    : [
        { text: 'Grow your' },
        { text: 'own little' },
        { text: 'paradise.', accent: true },
      ]

  return (
    <section className="lx-hero" id="home" ref={root}>
      <div className="lx-hs" aria-hidden="true">
        {slides.map((slide, index) => (
          <div
            className="lx-hs-slide"
            key={slide.src + index}
            ref={(element) => {
              slideEls.current[index] = element
            }}
          >
            <img
              className="lx-hs-img"
              src={slide.src}
              alt=""
              decoding="async"
              fetchPriority={index === 0 ? 'high' : 'auto'}
            />
          </div>
        ))}
        <div className="lx-hs-shade" />
        <div className="lx-hs-grain" />
      </div>

      <div className="lx-hero-frame" aria-hidden="true" />

      <div className="lx-hero-inner">
        <div className="lx-hero-copy">
          <div className="lx-eyebrow lx-hero-eyebrow">
            <span className="lx-eyebrow-line" />
            {kannada ? 'ಪ್ರಕೃತಿಯೊಂದಿಗೆ ಬೆಳೆಯಿರಿ' : 'Grow with nature'}
          </div>

          <WordReveal
            as="h1"
            className="lx-hero-title"
            lines={headline}
            play={ready}
            delay={0.25}
            stagger={0.1}
          />

          <p className="lx-hero-sub">
            {kannada
              ? 'ನಿಮ್ಮ ಕೃಷಿ ಭೂಮಿ, ತೋಟ ಮತ್ತು ಮನೆಯ ಹಸಿರಿಗಾಗಿ ವಿವಿಧ ಸಸಿಗಳ ಬಗ್ಗೆ ತಿಳಿಯಿರಿ. ನಿಮ್ಮ ಅಗತ್ಯಕ್ಕೆ ತಕ್ಕ ಸಸಿಗಳನ್ನು ಆಯ್ಕೆ ಮಾಡಲು ನಮ್ಮನ್ನು ಸಂಪರ್ಕಿಸಿ.'
              : 'Explore plants for your farm, garden and home. Get in touch with us to discover the right plants for your green space.'}
          </p>

          <div className="lx-hero-actions">
            <button type="button" className="lx-btn lx-btn--gold" data-magnetic onClick={onExplore}>
              <span className="lx-btn-label">
                {kannada ? 'ಸಸ್ಯಗಳನ್ನು ವೀಕ್ಷಿಸಿ' : 'Explore plants'}
                <ArrowRight size={18} />
              </span>
            </button>

            <a
              className="lx-btn lx-btn--ghost"
              data-magnetic
              href={directionsUrl}
              target="_blank"
              rel="noreferrer"
            >
              <span className="lx-btn-label">
                <MapPin size={17} />
                {kannada ? 'ನರ್ಸರಿಗೆ ದಾರಿ' : 'Get directions'}
                <ArrowUpRight size={15} />
              </span>
            </a>
          </div>

          <div className="lx-hero-note">
            <span className="lx-hero-note-icon">
              <Check size={14} />
            </span>
            {kannada
              ? 'ಸಸಿಗಳ ಬೆಲೆ ಮತ್ತು ಲಭ್ಯತೆಗಾಗಿ ನೇರವಾಗಿ ವಿಚಾರಿಸಿ'
              : 'Contact us directly for plant prices and availability'}
          </div>
        </div>
      </div>

      <div className="lx-hero-vert" aria-hidden="true">
        Bhuvana Nursery &nbsp;·&nbsp; Chitradurga, Karnataka
      </div>

      <div className="lx-hs-ui">
        <div className="lx-hs-meta">
          <span className="lx-hs-count">
            <b>{String(position + 1).padStart(2, '0')}</b>
            <i />
            {String(visibleSlides.length).padStart(2, '0')}
          </span>
          <span className="lx-hs-cap" aria-live="off">
            <span className="lx-hs-cap-m">
              <span className="lx-hs-cap-i">{caption ? (kannada ? caption.kn : caption.en) : ''}</span>
            </span>
          </span>
        </div>

        <div className="lx-hs-bars" role="tablist" aria-label="Hero photographs">
          {slides.map((slide, index) => (
            <button
              type="button"
              key={slide.src + index}
              role="tab"
              aria-selected={index === current}
              aria-label={`Photo ${index + 1}`}
              className={`lx-hs-bar${failed.has(index) ? ' is-hidden' : ''}`}
              onClick={() => goRef.current(index)}
            >
              <span className="lx-hs-fill" />
            </button>
          ))}
        </div>

        <div className="lx-hs-controls">
          <button type="button" className="lx-round" aria-label="Previous photo" onClick={() => step(-1)}>
            <ArrowLeft size={18} />
          </button>
          <button type="button" className="lx-round" aria-label="Next photo" onClick={() => step(1)}>
            <ArrowRight size={18} />
          </button>
          <a
            className="lx-scroll-hint"
            href="#categories"
            onClick={(event) => {
              event.preventDefault()
              onDiscover()
            }}
            aria-label={kannada ? 'ಮುಂದೆ ಅನ್ವೇಷಿಸಿ' : 'Discover more'}
          >
            <ArrowDown size={17} />
          </a>
        </div>
      </div>
    </section>
  )
}
