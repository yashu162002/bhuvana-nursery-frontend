import { useRef } from 'react'
import { MessageCircle } from 'lucide-react'
import { ScrollTrigger, useGSAP } from '../../lib/gsap'
import { siteConfig } from '../../config/siteConfig'

export default function FloatingWhatsApp() {
  const link = useRef<HTMLAnchorElement>(null)
  const number = siteConfig.whatsappNumber.replace(/\D/g, '')

  useGSAP(() => {
    ScrollTrigger.create({
      start: 520,
      end: 'max',
      onToggle: (self) => link.current?.classList.toggle('is-shown', self.isActive),
    })
  })

  if (!number) return null

  return (
    <a
      ref={link}
      className="lx-wa"
      href={`https://wa.me/${number}`}
      target="_blank"
      rel="noreferrer"
      aria-label="Chat on WhatsApp"
    >
      <span className="lx-wa-pulse" />
      <MessageCircle size={24} />
    </a>
  )
}
