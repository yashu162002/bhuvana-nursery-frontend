import { useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Menu, Phone, X } from 'lucide-react'
import { gsap, ScrollTrigger, useGSAP, prefersReducedMotion } from '../../lib/gsap'
import { lockScroll, unlockScroll, scrollToTarget } from '../../lib/smoothScroll'
import { useLanguage } from '../../context/languageContext'
import { siteConfig } from '../../config/siteConfig'

const NAV = [
  { id: 'home', kn: 'ಮುಖಪುಟ', en: 'Home' },
  { id: 'categories', kn: 'ವಿಭಾಗಗಳು', en: 'Categories' },
  { id: 'plants', kn: 'ಸಸ್ಯಗಳು', en: 'Plants' },
  { id: 'about', kn: 'ನಮ್ಮ ಬಗ್ಗೆ', en: 'About' },
  { id: 'contact', kn: 'ಸಂಪರ್ಕಿಸಿ', en: 'Contact' },
]

const MENU_CLOSED = 'circle(0% at calc(100% - 46px) 46px)'
const MENU_OPEN = 'circle(150% at calc(100% - 46px) 46px)'

interface SiteHeaderProps {
  /** Always show the solid (scrolled) look, e.g. on inner pages. */
  solid?: boolean
  /** Hold the entrance animation until the preloader has lifted. */
  ready?: boolean
}

export default function SiteHeader({ solid = false, ready = true }: SiteHeaderProps) {
  const { isKannada, toggle } = useLanguage()
  const { pathname } = useLocation()
  const isHome = pathname === '/'

  const root = useRef<HTMLDivElement>(null)
  const menu = useRef<HTMLDivElement>(null)
  const menuMounted = useRef(false)
  const openRef = useRef(false)

  const [scrolled, setScrolled] = useState(false)
  const [hidden, setHidden] = useState(false)
  const [open, setOpen] = useState(false)

  /* ---- scrolled / hide-on-scroll-down behaviour ---------------------- */
  useGSAP(
    () => {
      ScrollTrigger.create({
        start: 0,
        end: 'max',
        onUpdate: (self) => {
          const y = self.scroll()
          setScrolled(y > 70)
          setHidden(self.direction === 1 && y > 520 && !openRef.current)
        },
      })
    },
    { scope: root },
  )

  /* ---- entrance ------------------------------------------------------ */
  useGSAP(
    () => {
      if (!ready || prefersReducedMotion()) return
      gsap.fromTo(
        '.lx-header',
        { yPercent: -100, autoAlpha: 0 },
        { yPercent: 0, autoAlpha: 1, duration: 1.4, ease: 'expo.out', delay: 0.2 },
      )
      gsap.fromTo(
        '.lx-nav-link',
        { y: -14, autoAlpha: 0 },
        { y: 0, autoAlpha: 1, duration: 1, ease: 'expo.out', stagger: 0.07, delay: 0.55 },
      )
    },
    { scope: root, dependencies: [ready] },
  )

  /* ---- full-screen menu --------------------------------------------- */
  useGSAP(
    () => {
      openRef.current = open
      const el = menu.current
      if (!el) return

      if (!menuMounted.current) {
        menuMounted.current = true
        return
      }

      if (open) {
        lockScroll()
        gsap
          .timeline()
          .set(el, { autoAlpha: 1 })
          .fromTo(el, { clipPath: MENU_CLOSED }, { clipPath: MENU_OPEN, duration: 1.1, ease: 'expo.inOut' })
          .fromTo(
            '.lx-menu-i',
            { y: 0, yPercent: 115 },
            { y: 0, yPercent: 0, duration: 1, ease: 'expo.out', stagger: 0.07 },
            0.4,
          )
          .fromTo(
            '.lx-menu-foot',
            { autoAlpha: 0, y: 20 },
            { autoAlpha: 1, y: 0, duration: 0.9, ease: 'expo.out' },
            0.85,
          )
      } else {
        unlockScroll()
        gsap.to(el, {
          clipPath: MENU_CLOSED,
          duration: 0.8,
          ease: 'expo.inOut',
          onComplete: () => {
            gsap.set(el, { autoAlpha: 0 })
          },
        })
      }
    },
    { scope: root, dependencies: [open] },
  )

  function handleNav(event: React.MouseEvent, id: string) {
    setOpen(false)
    if (!isHome) return // let <Link> navigate to "/#id"
    event.preventDefault()
    // Close the menu first on mobile, then glide to the section.
    window.setTimeout(() => scrollToTarget(`#${id}`), open ? 450 : 0)
  }

  const phone = siteConfig.phoneNumber.replace(/[^\d+]/g, '')
  const cls = [
    'lx-header',
    scrolled || solid ? 'is-solid' : '',
    hidden && !open ? 'is-hidden' : '',
    open ? 'is-open' : '',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div className="lx-header-wrap" ref={root}>
    <header className={cls}>
      <div className="lx-header-bar">
        <Link
          className="lx-brand"
          to="/"
          aria-label="Bhoovana Nursery home"
          onClick={(event) => {
            setOpen(false)
            if (isHome) {
              event.preventDefault()
              scrollToTarget(0)
            }
          }}
        >
          <span className="lx-brand-mark">
            <img src="/logo.png" alt="Bhoovana Nursery Logo" className="lx-brand-logo-img" />
          </span>
          <span className="lx-brand-copy">
            <strong>{isKannada ? 'ಭೂವನ ನರ್ಸರಿ' : 'Bhoovana Nursery'}</strong>
            <small>{isKannada ? 'BHOOVANA NURSERY' : 'ಭೂವನ ನರ್ಸರಿ'}</small>
          </span>
        </Link>

        <nav className="lx-nav" aria-label="Primary">
          {NAV.map((item) => (
            <Link
              key={item.id}
              className="lx-nav-link"
              to={{ pathname: '/', hash: `#${item.id}` }}
              onClick={(event) => handleNav(event, item.id)}
            >
              <span>{isKannada ? item.kn : item.en}</span>
            </Link>
          ))}
        </nav>

        <div className="lx-header-actions">
          <button
            type="button"
            className="lx-lang"
            onClick={toggle}
            aria-label="Switch website language"
          >
            <span className={isKannada ? 'is-on' : ''}>ಕನ್ನಡ</span>
            <i />
            <span className={!isKannada ? 'is-on' : ''}>EN</span>
          </button>

          <Link className="lx-admin-link" to="/admin/login">
            Admin
          </Link>

          <button
            type="button"
            className="lx-burger"
            onClick={() => setOpen((value) => !value)}
            aria-label={open ? 'Close menu' : 'Open menu'}
            aria-expanded={open}
          >
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>
    </header>

      <div className="lx-menu" ref={menu} aria-hidden={!open}>
        <nav className="lx-menu-nav" aria-label="Mobile">
          {NAV.map((item, index) => (
            <Link
              key={item.id}
              className="lx-menu-link"
              to={{ pathname: '/', hash: `#${item.id}` }}
              onClick={(event) => handleNav(event, item.id)}
              tabIndex={open ? 0 : -1}
            >
              <span className="lx-menu-n">{String(index + 1).padStart(2, '0')}</span>
              <span className="lx-menu-m">
                <span className="lx-menu-i">{isKannada ? item.kn : item.en}</span>
              </span>
            </Link>
          ))}
        </nav>

        <div className="lx-menu-foot">
          {phone && (
            <a href={`tel:${phone}`} tabIndex={open ? 0 : -1}>
              <Phone size={16} /> {siteConfig.phoneNumber}
            </a>
          )}
          <Link to="/admin/login" onClick={() => setOpen(false)} tabIndex={open ? 0 : -1}>
            Admin
          </Link>
        </div>
      </div>
    </div>
  )
}
