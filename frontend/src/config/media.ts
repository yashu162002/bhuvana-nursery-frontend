/**
 * All decorative photography for the public site lives here.
 *
 * To use your OWN nursery photos (recommended - real photos always look best):
 *   1. Copy them into  frontend/public/hero/  (e.g. nursery-1.jpg)
 *   2. Change `src` below to '/hero/nursery-1.jpg'
 *
 * Landscape photos, at least 2000px wide, work best for the hero.
 *
 * If a remote image ever fails to load, the slideshow automatically skips it,
 * so the site never shows a broken picture.
 */

const unsplash = (id: string, width = 2400, quality = 80) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}&q=${quality}`

export interface HeroSlide {
  src: string
  kn: string
  en: string
}

export const heroSlides: HeroSlide[] = [
  {
    src: unsplash('photo-1441974231531-c6227db76b6e'),
    kn: 'ಹಸಿರು ಛತ್ರ',
    en: 'The green canopy',
  },
  {
    src: unsplash('photo-1416879595882-3373a0480b5b'),
    kn: 'ಹೊಸ ಚಿಗುರು',
    en: 'New beginnings',
  },
  {
    src: unsplash('photo-1448375240586-882707db888b'),
    kn: 'ಬೆಳಕಿನ ಕಾನನ',
    en: 'Woods of light',
  },
  {
    src: unsplash('photo-1530836369250-ef72a3f5cda8'),
    kn: 'ಕಾಳಜಿಯಿಂದ ಬೆಳೆದದ್ದು',
    en: 'Grown with care',
  },
  {
    src: unsplash('photo-1466692476868-aef1dfb1e735'),
    kn: 'ನಿಮ್ಮ ತೋಟದ ಹಾದಿ',
    en: 'The path to your garden',
  },
  {
    src: unsplash('photo-1463936575829-25148e1db1b8'),
    kn: 'ಸಸ್ಯ ಸೊಬಗು',
    en: 'Botanical beauty',
  },
]

/** Background photos for the four category cards (keys match App.tsx). */
export const categoryImages: Record<string, string> = {
  fruit: unsplash('photo-1464965911861-746a04b4bca6', 1000, 75),
  agriculture: unsplash('photo-1500382017468-9049fed747ef', 1000, 75),
  trees: unsplash('photo-1448375240586-882707db888b', 1000, 75),
  manure: unsplash('photo-1416879595882-3373a0480b5b', 1000, 75),
}

export const statementImage = unsplash('photo-1441974231531-c6227db76b6e', 2200, 70)

export const aboutImages = {
  main: unsplash('photo-1416879595882-3373a0480b5b', 1100, 80),
  accent: unsplash('photo-1530836369250-ef72a3f5cda8', 700, 80),
}
