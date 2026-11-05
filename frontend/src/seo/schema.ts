/**
 * LocalBusiness (Nursery) JSON-LD schema for Bhoovana Nursery.
 * Rendered as a <script type="application/ld+json"> on every public page.
 *
 * Schema types: LocalBusiness → GardenStore (most specific store type
 * available in schema.org that fits a plant/garden nursery).
 * See: https://schema.org/GardenStore
 */

import {
  SEO_SITE_URL,
  SEO_SITE_NAME,
  SEO_LOGO_URL,
  SEO_DEFAULT_OG_IMAGE,
  SEO_PHONE,
  SEO_LAT,
  SEO_LNG,
  SEO_ADDRESS,
  SEO_OPENING_HOURS,
  SEO_MAP_LINK,
} from './seoConfig'

export const LOCAL_BUSINESS_SCHEMA = {
  '@context': 'https://schema.org',
  '@type': ['GardenStore', 'LocalBusiness'],
  name: SEO_SITE_NAME,
  alternateName: ['Bhoovana Nursery', 'Bhuvana Nursery', 'ಭೂವನ ನರ್ಸರಿ', 'ಭುವನ ನರ್ಸರಿ'],
  url: SEO_SITE_URL,
  telephone: SEO_PHONE,
  logo: {
    '@type': 'ImageObject',
    url: SEO_LOGO_URL,
    width: 512,
    height: 512,
  },
  image: [SEO_DEFAULT_OG_IMAGE, SEO_LOGO_URL],
  description:
    'Bhoovana Nursery in Chitradurga, Karnataka offers a wide range of plants including coconut, arecanut, mango, teak, fruit seedlings, agricultural plants and organic manure.',
  address: {
    '@type': 'PostalAddress',
    streetAddress: SEO_ADDRESS.streetAddress,
    addressLocality: SEO_ADDRESS.addressLocality,
    addressRegion: SEO_ADDRESS.addressRegion,
    postalCode: SEO_ADDRESS.postalCode,
    addressCountry: SEO_ADDRESS.addressCountry,
  },
  geo: {
    '@type': 'GeoCoordinates',
    latitude: SEO_LAT,
    longitude: SEO_LNG,
  },
  openingHoursSpecification: SEO_OPENING_HOURS.map((spec) => {
    // spec format: 'Mo-Sa 08:00-18:00'
    const [days, hours] = spec.split(' ')
    const [opens, closes] = hours.split('-')
    return {
      '@type': 'OpeningHoursSpecification',
      dayOfWeek: days,
      opens,
      closes,
    }
  }),
  hasMap: SEO_MAP_LINK,
  priceRange: '₹',
  currenciesAccepted: 'INR',
  paymentAccepted: 'Cash, UPI',
  areaServed: {
    '@type': 'AdministrativeArea',
    name: 'Chitradurga, Karnataka, India',
  },
  sameAs: [],
}

/** Builds a Product schema for a plant detail page. */
export function buildPlantSchema(plant: {
  nameEn: string
  nameKn: string
  scientificName?: string | null
  descriptionEn?: string | null
  slug: string
  price?: number | null
  imageUrl?: string | null
  categoryNameEn?: string
  availability: 'AVAILABLE' | 'OUT_OF_STOCK' | 'UNKNOWN'
}) {
  const pageUrl = `${SEO_SITE_URL}/plants/${plant.slug}`
  const availabilityMap = {
    AVAILABLE: 'https://schema.org/InStock',
    OUT_OF_STOCK: 'https://schema.org/OutOfStock',
    UNKNOWN: 'https://schema.org/PreOrder',
  }

  return {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: plant.nameEn,
    alternateName: plant.nameKn,
    ...(plant.scientificName ? { disambiguatingDescription: plant.scientificName } : {}),
    description: plant.descriptionEn ?? `${plant.nameEn} plant available at Bhoovana Nursery, Chitradurga.`,
    url: pageUrl,
    ...(plant.imageUrl ? { image: plant.imageUrl } : {}),
    brand: {
      '@type': 'Brand',
      name: 'Bhoovana Nursery',
    },
    category: plant.categoryNameEn ?? 'Plants',
    offers: {
      '@type': 'Offer',
      url: pageUrl,
      priceCurrency: 'INR',
      ...(plant.price != null ? { price: plant.price } : {}),
      availability: availabilityMap[plant.availability],
      seller: {
        '@type': 'LocalBusiness',
        name: 'Bhoovana Nursery',
      },
    },
  }
}

/** Renders a schema object as a JSON-LD <script> and returns a cleanup fn. */
export function injectJsonLd(id: string, schema: object): () => void {
  let el = document.getElementById(id) as HTMLScriptElement | null
  if (!el) {
    el = document.createElement('script')
    el.id = id
    el.type = 'application/ld+json'
    document.head.appendChild(el)
  }
  el.textContent = JSON.stringify(schema)
  return () => {
    el?.remove()
  }
}
