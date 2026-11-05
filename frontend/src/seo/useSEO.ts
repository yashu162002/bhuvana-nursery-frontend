/**
 * useSEO – imperatively updates <title>, <meta>, <link canonical>, and
 * Open-Graph / Twitter tags for every route.
 *
 * We do NOT use react-helmet or any third-party head manager so we don't add
 * a dependency. The hook is called once per route component on mount (and when
 * its arguments change) and cleans up on unmount.
 */

import { useEffect } from 'react'

export interface SEOMeta {
  title: string
  description: string
  canonical?: string
  keywords?: string
  ogTitle?: string
  ogDescription?: string
  ogImage?: string
  ogType?: 'website' | 'article' | 'product'
  twitterCard?: 'summary' | 'summary_large_image'
  /** Pass false for noindex pages (admin routes, 404). */
  robots?: string
}

// ---- helpers ----------------------------------------------------------------

function setMeta(name: string, content: string) {
  let el = document.querySelector<HTMLMetaElement>(`meta[name="${name}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('name', name)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function setProperty(property: string, content: string) {
  let el = document.querySelector<HTMLMetaElement>(`meta[property="${property}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('property', property)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function setCanonical(href: string) {
  let el = document.querySelector<HTMLLinkElement>('link[rel="canonical"]')
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', 'canonical')
    document.head.appendChild(el)
  }
  el.setAttribute('href', href)
}

// ---- hook -------------------------------------------------------------------

export function useSEO(meta: SEOMeta) {
  useEffect(() => {
    const prev = {
      title: document.title,
      description: document.querySelector<HTMLMetaElement>('meta[name="description"]')?.content ?? '',
    }

    // --- Title
    document.title = meta.title

    // --- Basic meta
    setMeta('description', meta.description)
    if (meta.keywords) setMeta('keywords', meta.keywords)
    setMeta('robots', meta.robots ?? 'index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1')

    // --- Canonical
    if (meta.canonical) setCanonical(meta.canonical)

    // --- Open Graph
    setProperty('og:title', meta.ogTitle ?? meta.title)
    setProperty('og:description', meta.ogDescription ?? meta.description)
    setProperty('og:type', meta.ogType ?? 'website')
    setProperty('og:site_name', 'Bhoovana Nursery')
    setProperty('og:locale', 'en_IN')
    if (meta.ogImage) setProperty('og:image', meta.ogImage)
    if (meta.canonical) setProperty('og:url', meta.canonical)

    // --- Twitter Card
    setMeta('twitter:card', meta.twitterCard ?? 'summary_large_image')
    setMeta('twitter:title', meta.ogTitle ?? meta.title)
    setMeta('twitter:description', meta.ogDescription ?? meta.description)
    if (meta.ogImage) setMeta('twitter:image', meta.ogImage)

    return () => {
      // Restore on unmount so flash of wrong title is minimal
      document.title = prev.title
    }
  }, [
    meta.title,
    meta.description,
    meta.canonical,
    meta.keywords,
    meta.ogTitle,
    meta.ogDescription,
    meta.ogImage,
    meta.ogType,
    meta.twitterCard,
    meta.robots,
  ])
}
