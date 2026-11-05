/**
 * Central SEO configuration for Bhoovana Nursery.
 * All metadata, schema, and sitemap data lives here.
 */

export const SEO_SITE_URL = 'https://bhoovana.netlify.app'
export const SEO_SITE_NAME = 'Bhoovana Nursery'
export const SEO_SITE_NAME_KN = 'ಭೂವನ ನರ್ಸರಿ'
export const SEO_LOGO_URL = `${SEO_SITE_URL}/logo.png`
export const SEO_DEFAULT_OG_IMAGE = `${SEO_SITE_URL}/og-image.png`
export const SEO_PHONE = '+919740114246'
export const SEO_WHATSAPP = '919740114246'
export const SEO_LAT = 14.2658817
export const SEO_LNG = 76.43122
export const SEO_MAP_LINK = `https://www.google.com/maps?q=${SEO_LAT},${SEO_LNG}&z=17&hl=en`

export const SEO_ADDRESS = {
  streetAddress: 'Gonur, 4th mile near upper Bhadra Meldande pillar turuvanur road',
  addressLocality: 'Chitradurga',
  addressRegion: 'Karnataka',
  postalCode: '577517',
  addressCountry: 'IN',
}

export const SEO_OPENING_HOURS = [
  'Mo-Sa 08:00-18:00',
]

// ----- Page-level metadata -----

export const HOME_SEO = {
  title: 'Bhoovana Nursery – Best Plant Nursery in Chitradurga | Fruit, Agricultural & Tree Saplings',
  description:
    'Bhoovana Nursery (ಭೂವನ ನರ್ಸರಿ) in Chitradurga, Karnataka offers coconut, arecanut, mango, teak, fruit seedlings, and organic manure. Visit or call +91 97401 14246.',
  keywords:
    'Bhoovana Nursery, Bhuvana Nursery, ಭುವನ ನರ್ಸರಿ, nursery in Chitradurga, plant nursery Chitradurga, best nursery Chitradurga, fruit plants nursery Chitradurga, agricultural plants Chitradurga, coconut plants, arecanut plants, mango plants, teak plants, fruit seedlings, organic manure',
  canonical: SEO_SITE_URL,
  ogImage: SEO_DEFAULT_OG_IMAGE,
  ogType: 'website' as const,
}

export const PLANT_DETAIL_SEO = {
  // The category is intentionally not part of the title: the title already
  // carries the plant name, location, and brand, and the category is used in
  // the description and schema instead.
  titleTemplate: (plantNameEn: string, _categoryNameEn?: string) =>
    `${plantNameEn} Plant – Buy ${plantNameEn} Sapling in Chitradurga | Bhoovana Nursery`,
  descriptionTemplate: (
    plantNameEn: string,
    categoryNameEn: string,
    scientificName: string | null | undefined,
    descriptionEn: string | null | undefined,
  ) => {
    const sci = scientificName ? ` (${scientificName})` : ''
    const desc = descriptionEn
      ? ` ${descriptionEn.slice(0, 100).trim()}${descriptionEn.length > 100 ? '…' : ''}`
      : ''
    return `Buy ${plantNameEn}${sci} at Bhoovana Nursery, Chitradurga.${desc} ${categoryNameEn} available. Call +91 97401 14246 for prices and availability.`
  },
  canonical: (slug: string) => `${SEO_SITE_URL}/plants/${slug}`,
}

export const NOT_FOUND_SEO = {
  title: 'Page Not Found | Bhoovana Nursery',
  description: 'The page you are looking for does not exist. Return to Bhoovana Nursery home page.',
  canonical: SEO_SITE_URL,
}
