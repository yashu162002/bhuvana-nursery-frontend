import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { ScrollTrigger, prefersReducedMotion } from '../../lib/gsap'
import {
  acquireSmoothScroll,
  getLenis,
  releaseSmoothScroll,
} from '../../lib/smoothScroll'
import { useLanguage } from '../../context/languageContext'
import CustomCursor from './CustomCursor'
import ScrollProgress from './ScrollProgress'
import FloatingWhatsApp from './FloatingWhatsApp'

/**
 * Wraps every public page. Everything visual is scoped under `.lx`, so the
 * admin panel (which does not use this layout) is never affected.
 */
export default function PublicLayout() {
  const { language } = useLanguage()
  const { pathname, hash } = useLocation()

  useEffect(() => {
    acquireSmoothScroll()
    return releaseSmoothScroll
  }, [])

  // New page: start at the top (unless the link points at a #section).
  useEffect(() => {
    if (!hash) {
      window.scrollTo(0, 0)
      getLenis()?.scrollTo(0, { immediate: true })
    }
    const id = window.setTimeout(() => ScrollTrigger.refresh(), 120)
    return () => window.clearTimeout(id)
  }, [pathname, hash])

  // Custom fonts change text metrics - recalculate scroll positions once loaded.
  useEffect(() => {
    document.fonts?.ready.then(() => ScrollTrigger.refresh())
  }, [])

  return (
    <div
      className={`lx${prefersReducedMotion() ? '' : ' lx-motion'}`}
      data-lang={language}
    >
      <ScrollProgress />
      <CustomCursor />
      <Outlet />
      <FloatingWhatsApp />
    </div>
  )
}
