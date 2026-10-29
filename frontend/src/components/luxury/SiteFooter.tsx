import { useRef } from 'react'
import { ArrowUpRight } from 'lucide-react'
import { gsap, useGSAP, prefersReducedMotion } from '../../lib/gsap'
import { scrollToTarget } from '../../lib/smoothScroll'
import { useLanguage } from '../../context/languageContext'
import { siteConfig } from '../../config/siteConfig'

export default function SiteFooter() {
  const { isKannada } = useLanguage()
  const root = useRef<HTMLElement>(null)

  useGSAP(
    () => {
      if (prefersReducedMotion()) return
      gsap.fromTo(
        '.lx-foot-wi',
        { y: 0, yPercent: 105 },
        {
          y: 0,
          yPercent: 0,
          duration: 1.6,
          ease: 'expo.out',
          stagger: 0.12,
          scrollTrigger: { trigger: root.current, start: 'top 80%', once: true },
        },
      )
      gsap.fromTo(
        '.lx-foot-rule',
        { scaleX: 0 },
        {
          scaleX: 1,
          duration: 1.8,
          ease: 'expo.inOut',
          scrollTrigger: { trigger: root.current, start: 'top 85%', once: true },
        },
      )
    },
    { scope: root },
  )

  return (
    <footer className="lx-footer" ref={root}>
      <div className="lx-container">
        <div className="lx-foot-top">
          <div className="lx-foot-brand-wrap">
            <img src="/logo.png" alt="Bhoovana Nursery Logo" className="lx-foot-logo" />
            <p className="lx-foot-tag">
              {isKannada ? 'ಹಸಿರನ್ನು ಬೆಳೆಸಿ. ಭವಿಷ್ಯವನ್ನು ಬೆಳೆಸಿ.' : 'Grow green. Grow a better future.'}
            </p>
          </div>
          <button
            type="button"
            className="lx-foot-top-btn"
            data-magnetic
            onClick={() => scrollToTarget(0)}
          >
            {isKannada ? 'ಮೇಲಕ್ಕೆ' : 'Back to top'}
            <ArrowUpRight size={16} />
          </button>
        </div>

        <div className="lx-foot-rule" />

        <div className="lx-foot-wordmark" role="img" aria-label="Bhoovana Nursery">
          {(isKannada ? ['ಭೂವನ', 'ನರ್ಸರಿ'] : ['Bhoovana', 'Nursery']).map((word) => (
            <span className="lx-foot-w" key={word} aria-hidden="true">
              <span className="lx-foot-wi">{word}</span>
            </span>
          ))}
        </div>

        <div className="lx-foot-bottom">
          <span>
            © {new Date().getFullYear()} {siteConfig.name} ·{' '}
            {isKannada ? 'ಎಲ್ಲ ಹಕ್ಕುಗಳನ್ನು ಕಾಯ್ದಿರಿಸಲಾಗಿದೆ' : 'All rights reserved'}
          </span>
          <span className="lx-foot-dev-credit" aria-label="Designed and Developed by Yaश Tech">
            <span className="lx-foot-dev-line" aria-hidden="true" />
            <span className="lx-foot-dev-by">Designed &amp; Developed by</span>
            <span className="lx-foot-dev-brand">
              <span className="lx-foot-dev-name">
                Yaಶ Tech<sup className="lx-foot-dev-reg">®</sup>
              </span>
              <span className="lx-foot-dev-underline" aria-hidden="true" />
            </span>
          </span>
          <span>Chitradurga · Karnataka · India</span>
        </div>
      </div>
    </footer>
  )
}
