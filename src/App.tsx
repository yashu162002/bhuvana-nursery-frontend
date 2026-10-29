import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  ArrowRight,
  ArrowUpRight,
  Check,
  Flower2,
  Leaf,
  MapPin,
  MessageCircle,
  Phone,
  Sprout,
  Trees,
} from 'lucide-react'
import PlantCatalogue from './features/plants/PlantCatalogue'
import { siteConfig } from './config/siteConfig'
import { aboutImages, categoryImages, statementImage } from './config/media'
import { gsap, useGSAP, prefersReducedMotion } from './lib/gsap'
import { scrollToTarget } from './lib/smoothScroll'
import { useInteractions } from './lib/useInteractions'
import { useLanguage } from './context/languageContext'
import Hero from './components/luxury/Hero'
import Marquee from './components/luxury/Marquee'
import Preloader from './components/luxury/Preloader'
import SiteHeader from './components/luxury/SiteHeader'
import SiteFooter from './components/luxury/SiteFooter'
import WordReveal from './components/luxury/WordReveal'

const WHATSAPP_NUMBER = siteConfig.whatsappNumber.replace(/\D/g, '')
const PHONE_NUMBER = siteConfig.phoneNumber
const MAP_LINK = siteConfig.mapLink

const categories = [
  { name: 'ಹಣ್ಣಿನ ಸಸಿಗಳು', english: 'Fruit plants', icon: Flower2, category: 'fruit', slug: 'fruit-plants' },
  { name: 'ಕೃಷಿ ಸಸಿಗಳು', english: 'Agricultural plants', icon: Sprout, category: 'agriculture', slug: 'agricultural-plants' },
  { name: 'ಮರಗಳು', english: 'Trees & shade', icon: Trees, category: 'trees', slug: 'trees' },
  { name: 'ನೈಸರ್ಗಿಕ ಗೊಬ್ಬರ', english: 'Natural manure', icon: Leaf, category: 'manure', slug: 'natural-manure' },
]

const INTRO_KEY = 'bhoovana-intro-seen'

function introAlreadySeen(): boolean {
  try {
    return window.sessionStorage.getItem(INTRO_KEY) === '1'
  } catch {
    return false
  }
}

function App() {
  const { isKannada } = useLanguage()
  const { hash } = useLocation()
  const root = useRef<HTMLDivElement>(null)

  // Cinematic intro: once per browser session, skipped for #links / reduced motion.
  const [showIntro] = useState(
    () => !prefersReducedMotion() && !introAlreadySeen() && !window.location.hash,
  )
  const [revealed, setRevealed] = useState(!showIntro)
  const [introGone, setIntroGone] = useState(!showIntro)
  // Set when a category card is clicked; PlantCatalogue applies the filter.
  const [categoryRequest, setCategoryRequest] = useState<
    { slug: string; nonce: number } | undefined
  >()
  const ready = revealed

  useInteractions(root)

  // Arriving from another page via /#plants etc.
  useEffect(() => {
    if (!hash || !ready) return
    const id = window.setTimeout(() => scrollToTarget(hash, { immediate: true }), 350)
    return () => window.clearTimeout(id)
  }, [hash, ready])

  /* ---- scroll-driven choreography ------------------------------------ */
  useGSAP(
    () => {
      if (prefersReducedMotion()) return

      // Fade + rise
      gsap.utils.toArray<HTMLElement>('[data-reveal]').forEach((el) => {
        const kind = el.dataset.reveal
        const offset =
          kind === 'left' ? { x: -80 } : kind === 'right' ? { x: 80 } : kind === 'scale' ? { scale: 0.9 } : { y: 56 }
        gsap.fromTo(
          el,
          { autoAlpha: 0, ...offset },
          {
            autoAlpha: 1,
            x: 0,
            y: 0,
            scale: 1,
            duration: 1.4,
            ease: 'expo.out',
            delay: Number(el.dataset.delay ?? 0),
            scrollTrigger: { trigger: el, start: 'top 88%', once: true },
          },
        )
      })

      // Children entering one after another
      gsap.utils.toArray<HTMLElement>('[data-stagger]').forEach((group) => {
        gsap.fromTo(
          group.children,
          { autoAlpha: 0, y: 90 },
          {
            autoAlpha: 1,
            y: 0,
            duration: 1.3,
            ease: 'expo.out',
            stagger: 0.13,
            scrollTrigger: { trigger: group, start: 'top 85%', once: true },
          },
        )
      })

      // Photographs unveiled with a curtain wipe
      gsap.utils.toArray<HTMLElement>('[data-clip]').forEach((el) => {
        gsap.fromTo(
          el,
          { clipPath: 'inset(100% 0% 0% 0% round 28px)' },
          {
            clipPath: 'inset(0% 0% 0% 0% round 28px)',
            duration: 1.8,
            ease: 'expo.inOut',
            scrollTrigger: { trigger: el, start: 'top 85%', once: true },
          },
        )
      })

      // Depth: images drift slower than the page
      gsap.utils.toArray<HTMLElement>('[data-parallax]').forEach((el) => {
        const speed = Number(el.dataset.parallax) || 0.12
        gsap.fromTo(
          el,
          { yPercent: -speed * 100 },
          {
            yPercent: speed * 100,
            ease: 'none',
            scrollTrigger: {
              trigger: el.parentElement,
              start: 'top bottom',
              end: 'bottom top',
              scrub: true,
            },
          },
        )
      })

      // Statement band: the photo gently zooms as you pass
      gsap.fromTo(
        '.lx-statement-bg img',
        { scale: 1.25 },
        {
          scale: 1,
          ease: 'none',
          scrollTrigger: {
            trigger: '.lx-statement',
            start: 'top bottom',
            end: 'bottom top',
            scrub: true,
          },
        },
      )

      // Contact panel: floating leaves at different depths
      gsap.utils.toArray<HTMLElement>('.lx-leaf').forEach((leaf, index) => {
        gsap.to(leaf, {
          yPercent: (index % 2 ? -1 : 1) * (40 + index * 18),
          rotate: (index % 2 ? -1 : 1) * 24,
          ease: 'none',
          scrollTrigger: {
            trigger: '.lx-contact',
            start: 'top bottom',
            end: 'bottom top',
            scrub: 1.2,
          },
        })
      })

      // Rotating badge
      gsap.to('.lx-badge-ring', {
        rotation: 360,
        duration: 28,
        repeat: -1,
        ease: 'none',
        transformOrigin: '50% 50%',
      })

      // Contact lines pulse in
      gsap.fromTo(
        '.lx-point',
        { autoAlpha: 0, x: -30 },
        {
          autoAlpha: 1,
          x: 0,
          duration: 1.1,
          ease: 'expo.out',
          stagger: 0.12,
          scrollTrigger: { trigger: '.lx-points', start: 'top 88%', once: true },
        },
      )
    },
    { scope: root },
  )

  function finishIntro() {
    try {
      window.sessionStorage.setItem(INTRO_KEY, '1')
    } catch {
      /* ignore */
    }
    setIntroGone(true)
  }

  const goTo = (selector: string) => scrollToTarget(selector, { offset: -10 })

  function browseCategory(slug: string) {
    // Changing the key remounts the catalogue with the chosen filter, so
    // clicking the same card twice starts from a clean, filtered state.
    setCategoryRequest((previous) => ({
      slug,
      nonce: (previous?.nonce ?? 0) + 1,
    }))
    goTo('#plants')
  }

  const marqueeItems = isKannada
    ? categories.map((c) => c.name)
    : categories.map((c) => c.english)

  return (
    <div className="lx-page" ref={root}>
      {showIntro && !introGone && (
        <Preloader onReveal={() => setRevealed(true)} onFinish={finishIntro} />
      )}

      <SiteHeader ready={ready} />

      <main>
        <Hero
          kannada={isKannada}
          ready={ready}
          onExplore={() => goTo('#plants')}
          onDiscover={() => goTo('#categories')}
          directionsUrl={MAP_LINK}
        />

        <Marquee items={marqueeItems} />

        {/* ---------------------------------------------------------- categories */}
        <section className="lx-section lx-categories" id="categories">
          <div className="lx-container">
            <div className="lx-sec-head">
              <div>
                <span className="lx-eyebrow" data-reveal>
                  <span className="lx-eyebrow-line" />
                  {isKannada ? 'ನಮ್ಮ ಸಂಗ್ರಹ' : 'Our collection'}
                </span>
                <WordReveal
                  as="h2"
                  className="lx-h2 lx-font-collection"
                  scroll
                  lines={
                    isKannada
                      ? [{ text: 'ನಿಮ್ಮ ಅಗತ್ಯಕ್ಕೆ' }, { text: 'ತಕ್ಕ ಸಸಿಗಳು', accent: true }]
                      : [{ text: 'Find your' }, { text: 'kind of green', accent: true }]
                  }
                />
              </div>
              <p className="lx-lead" data-reveal data-delay="0.2">
                {isKannada
                  ? 'ಹಣ್ಣಿನ ಸಸಿಗಳಿಂದ ಕೃಷಿ ಸಸಿಗಳವರೆಗೆ — ವಿವಿಧ ಆಯ್ಕೆಗಳನ್ನು ಅನ್ವೇಷಿಸಿ.'
                  : 'From fruit plants to agricultural varieties, explore different options.'}
              </p>
            </div>

            <div className="lx-cat-grid" data-stagger>
              {categories.map((category, index) => {
                const Icon = category.icon
                return (
                  <button
                    type="button"
                    className="lx-cat"
                    data-tilt
                    key={category.category}
                    onClick={() => browseCategory(category.slug)}
                  >
                    <span
                      className="lx-cat-media"
                      style={{ backgroundImage: `url(${categoryImages[category.category]})` }}
                    />
                    <span className="lx-cat-glow" />
                    <span className="lx-cat-num">{String(index + 1).padStart(2, '0')}</span>
                    <span className="lx-cat-icon">
                      <Icon size={24} strokeWidth={1.4} />
                    </span>
                    <span className="lx-cat-body">
                      <span className="lx-cat-title">{isKannada ? category.name : category.english}</span>
                      <span className="lx-cat-sub">{isKannada ? category.english : category.name}</span>
                    </span>
                    <span className="lx-cat-arrow">
                      <ArrowUpRight size={20} />
                    </span>
                  </button>
                )
              })}
            </div>
          </div>
        </section>

        {/* ---------------------------------------------------------- statement */}
        <section className="lx-statement" aria-label={isKannada ? 'ಹಸಿರೇ ಬದುಕು' : 'Life in green'}>
          <div className="lx-statement-bg">
            <img src={statementImage} alt="" loading="lazy" />
          </div>
          <div className="lx-statement-inner">
            <span className="lx-eyebrow lx-eyebrow--light" data-reveal>
              <span className="lx-eyebrow-line" />
              {isKannada ? 'ಪ್ರಕೃತಿ · ಬೆಳವಣಿಗೆ · ಭವಿಷ್ಯ' : 'Nature · Growth · Future'}
            </span>
            <WordReveal
              as="h2"
              className="lx-statement-title lx-font-statement"
              scroll
              stagger={0.12}
              lines={
                isKannada
                  ? [{ text: 'ಹಸಿರೇ' }, { text: 'ಬದುಕು.', accent: true }]
                  : [{ text: 'Life' }, { text: 'in green.', accent: true }]
              }
            />
          </div>
        </section>

        {/* ---------------------------------------------------------- catalogue */}
        <div className="lx-catalogue-wrap">
          <PlantCatalogue
            key={categoryRequest?.nonce ?? 0}
            kannada={isKannada}
            initialCategory={categoryRequest?.slug}
          />
        </div>

        {/* ---------------------------------------------------------- about */}
        <section className="lx-section lx-about" id="about">
          <div className="lx-container lx-about-grid">
            <div className="lx-about-media">
              <div className="lx-about-main" data-clip>
                <img src={aboutImages.main} alt="Young plants and seedlings in a garden" loading="lazy" data-parallax="0.1" />
              </div>
              <div className="lx-about-accent" data-reveal="scale" data-delay="0.2">
                <img src={aboutImages.accent} alt="" loading="lazy" />
              </div>

              <div className="lx-badge" aria-hidden="true">
                <svg className="lx-badge-ring" viewBox="0 0 200 200">
                  <defs>
                    <path id="lx-badge-path" d="M100,100 m-78,0 a78,78 0 1,1 156,0 a78,78 0 1,1 -156,0" />
                  </defs>
                  <text>
                    <textPath href="#lx-badge-path" startOffset="0">
                      BHOOVANA NURSERY · CHITRADURGA · GROW WITH NATURE ·
                    </textPath>
                  </text>
                </svg>
                <Leaf className="lx-badge-leaf" size={30} strokeWidth={1.3} />
              </div>
            </div>

            <div className="lx-about-copy">
              <span className="lx-eyebrow" data-reveal>
                <span className="lx-eyebrow-line" />
                {isKannada ? 'ಭೂವನ ನರ್ಸರಿಯ ಪರಿಚಯ' : 'A little about us'}
              </span>
              <WordReveal
                as="h2"
                className="lx-h2 lx-font-about"
                scroll
                lines={
                  isKannada
                    ? [{ text: 'ಪ್ರತಿ ಸಸಿಯಲ್ಲೂ' }, { text: 'ಹೊಸ ಭವಿಷ್ಯ.', accent: true }]
                    : [{ text: 'Every plant holds' }, { text: 'a new beginning.', accent: true }]
                }
              />
              <p className="lx-body" data-reveal data-delay="0.15">
                {isKannada
                  ? 'ನಿಮ್ಮ ತೋಟಕ್ಕೆ ಅಥವಾ ಕೃಷಿ ಭೂಮಿಗೆ ಬೇಕಾದ ಸಸಿಗಳನ್ನು ಹುಡುಕುತ್ತಿದ್ದೀರಾ? ಭೂವನ ನರ್ಸರಿಯಲ್ಲಿ ಲಭ್ಯವಿರುವ ಸಸಿಗಳ ವಿಧಗಳು, ಬೆಲೆ ಮತ್ತು ಪ್ರಸ್ತುತ ಲಭ್ಯತೆ ಕುರಿತು ನಮ್ಮನ್ನು ಸಂಪರ್ಕಿಸಿ.'
                  : 'Looking for plants for your garden or farm? Contact Bhoovana Nursery to learn about available plant varieties, prices and current stock.'}
              </p>

              <div className="lx-points">
                {[
                  isKannada ? 'ವಿವಿಧ ಸಸ್ಯಗಳ ಆಯ್ಕೆಗಳು' : 'A variety of plant options',
                  isKannada ? 'ನೇರ ಬೆಲೆ ವಿಚಾರಣೆ' : 'Direct price enquiries',
                  isKannada ? 'ಸಸಿಗಳ ಲಭ್ಯತೆ ಮಾಹಿತಿ' : 'Availability information',
                ].map((text) => (
                  <div className="lx-point" key={text}>
                    <span className="lx-point-icon">
                      <Check size={15} />
                    </span>
                    {text}
                  </div>
                ))}
              </div>

              <button type="button" className="lx-btn lx-btn--forest" data-magnetic onClick={() => goTo('#contact')}>
                <span className="lx-btn-label">
                  {isKannada ? 'ನಮ್ಮನ್ನು ಸಂಪರ್ಕಿಸಿ' : 'Get in touch'}
                  <ArrowRight size={18} />
                </span>
              </button>
            </div>
          </div>
        </section>

        {/* ---------------------------------------------------------- contact */}
        <section className="lx-section lx-contact" id="contact">
          <svg className="lx-leaf lx-leaf--a" viewBox="0 0 120 140" aria-hidden="true">
            <path d="M60 80 C30 80 14 60 14 32 C42 32 60 50 60 80 Z M60 134 C60 104 60 74 60 40" />
          </svg>
          <svg className="lx-leaf lx-leaf--b" viewBox="0 0 120 140" aria-hidden="true">
            <path d="M60 58 C60 30 78 12 106 12 C106 40 88 58 60 58 Z M60 134 C60 104 60 74 60 40" />
          </svg>
          <svg className="lx-leaf lx-leaf--c" viewBox="0 0 120 140" aria-hidden="true">
            <path d="M60 80 C30 80 14 60 14 32 C42 32 60 50 60 80 Z" />
          </svg>

          <div className="lx-container lx-contact-grid">
            <div className="lx-contact-copy">
              <span className="lx-eyebrow lx-eyebrow--light" data-reveal>
                <span className="lx-eyebrow-line" />
                {isKannada ? 'ನಿಮ್ಮ ಮುಂದಿನ ಹೆಜ್ಜೆ' : 'Let’s connect'}
              </span>
              <WordReveal
                as="h2"
                className="lx-h2 lx-h2--light lx-font-contact"
                scroll
                lines={
                  isKannada
                    ? [{ text: 'ನಿಮ್ಮ ಹಸಿರು ಪ್ರಯಾಣ' }, { text: 'ಇಲ್ಲಿಂದ ಆರಂಭವಾಗಲಿ.', accent: true }]
                    : [{ text: 'Let your green journey' }, { text: 'begin here.', accent: true }]
                }
              />
              <p className="lx-body lx-body--light" data-reveal data-delay="0.15">
                {isKannada
                  ? 'ಸಸಿಗಳ ಬೆಲೆ, ವಿಧಗಳು ಮತ್ತು ಲಭ್ಯತೆಯ ಬಗ್ಗೆ ತಿಳಿಯಲು ನಮ್ಮನ್ನು ಸಂಪರ್ಕಿಸಿ ಅಥವಾ ನರ್ಸರಿಗೆ ಭೇಟಿ ನೀಡಿ.'
                  : 'Contact us about plant prices, varieties and availability, or visit our nursery.'}
              </p>

              <div className="lx-contact-actions" data-reveal data-delay="0.25">
                {WHATSAPP_NUMBER ? (
                  <a
                    className="lx-btn lx-btn--gold"
                    data-magnetic
                    href={`https://wa.me/${WHATSAPP_NUMBER}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <span className="lx-btn-label">
                      <MessageCircle size={18} />
                      WhatsApp
                    </span>
                  </a>
                ) : (
                  <span className="lx-note">
                    {isKannada ? 'WhatsApp ಸಂಖ್ಯೆ ಶೀಘ್ರದಲ್ಲೇ ಸೇರಿಸಲಾಗುತ್ತದೆ' : 'WhatsApp contact will be added soon'}
                  </span>
                )}

                {PHONE_NUMBER && (
                  <a className="lx-btn lx-btn--ghost" data-magnetic href={`tel:${PHONE_NUMBER}`}>
                    <span className="lx-btn-label">
                      <Phone size={18} />
                      {isKannada ? 'ಕರೆ ಮಾಡಿ' : 'Call us'}
                    </span>
                  </a>
                )}

                <a className="lx-btn lx-btn--ghost" data-magnetic href={MAP_LINK} target="_blank" rel="noreferrer">
                  <span className="lx-btn-label">
                    <MapPin size={18} />
                    {isKannada ? 'ದಾರಿ ಪಡೆಯಿರಿ' : 'Directions'}
                  </span>
                </a>
              </div>
            </div>

            <aside className="lx-contact-card" data-reveal="right">
              <span className="lx-contact-pin">
                <MapPin size={22} />
              </span>
              <span className="lx-label">{isKannada ? 'ನಮ್ಮ ವಿಳಾಸ' : 'Our location'}</span>
              <h3 className="lx-font-contact">{isKannada ? siteConfig.nurseryNameKn : siteConfig.nurseryName}</h3>
              <p>{siteConfig.address}</p>
              <a className="lx-map-link" href={MAP_LINK} target="_blank" rel="noreferrer">
                {isKannada ? 'Google Maps ನಲ್ಲಿ ತೆರೆಯಿರಿ' : 'Open in Google Maps'}
                <ArrowUpRight size={16} />
              </a>

              <div className="lx-contact-lines">
                {PHONE_NUMBER && (
                  <a href={`tel:${PHONE_NUMBER}`}>
                    <Phone size={17} />
                    {PHONE_NUMBER}
                  </a>
                )}
                {WHATSAPP_NUMBER && (
                  <a href={`https://wa.me/${WHATSAPP_NUMBER}`} target="_blank" rel="noreferrer">
                    <MessageCircle size={17} />
                    WhatsApp: {PHONE_NUMBER || WHATSAPP_NUMBER}
                  </a>
                )}
              </div>
            </aside>
          </div>
        </section>
      </main>

      <SiteFooter />
    </div>
  )
}

export default App
