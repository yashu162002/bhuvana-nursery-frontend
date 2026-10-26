import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Leaf, Search } from 'lucide-react'
import { getCategories, getPlants } from '../../api/plantApi'
import { createPlantWhatsAppUrl } from '../../utils/whatsapp'
import { gsap, ScrollTrigger, useGSAP, prefersReducedMotion } from '../../lib/gsap'
import WordReveal from '../../components/luxury/WordReveal'
import type {
  Plant,
  PlantCategory,
  PlantPage,
} from './plantTypes'

const emptyPage: PlantPage = {
  content: [],
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
}

interface PlantCatalogueProps {
  kannada?: boolean
}

function availabilityLabel(
  availability: Plant['availability'],
  kannada: boolean,
): string {
  if (availability === 'AVAILABLE') {
    return kannada ? 'ಲಭ್ಯವಿದೆ' : 'Available'
  }

  if (availability === 'OUT_OF_STOCK') {
    return kannada ? 'ಪ್ರಸ್ತುತ ಲಭ್ಯವಿಲ್ಲ' : 'Out of stock'
  }

  return kannada ? 'ವಿವರಗಳಿಗೆ ಸಂಪರ್ಕಿಸಿ' : 'Contact to confirm'
}

export default function PlantCatalogue({
  kannada = true,
}: PlantCatalogueProps) {
  const [categories, setCategories] = useState<PlantCategory[]>([])
  const [catalogue, setCatalogue] = useState<PlantPage>(emptyPage)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retryCount, setRetryCount] = useState(0)
  const section = useRef<HTMLElement>(null)

  // Cards glide in one after another each time a new page of plants arrives.
  useGSAP(
    () => {
      if (prefersReducedMotion() || loading) return

      const cards = gsap.utils.toArray<HTMLElement>('.catalogue-card')
      if (!cards.length) return

      gsap.set(cards, { autoAlpha: 0, y: 80, scale: 0.97 })
      ScrollTrigger.batch(cards, {
        start: 'top 92%',
        once: true,
        onEnter: (batch) =>
          gsap.to(batch, {
            autoAlpha: 1,
            y: 0,
            scale: 1,
            duration: 1.2,
            ease: 'expo.out',
            stagger: 0.1,
            overwrite: true,
          }),
      })
    },
    {
      scope: section,
      dependencies: [catalogue.content, loading],
      revertOnUpdate: true,
    },
  )

  useEffect(() => {
    let cancelled = false

    getCategories()
      .then((data) => {
        if (!cancelled) setCategories(data)
      })
      .catch(() => {
        if (!cancelled) {
          setError(
            kannada
              ? 'ವರ್ಗಗಳನ್ನು ಲೋಡ್ ಮಾಡಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ.'
              : 'Could not load plant categories.',
          )
        }
      })

    return () => {
      cancelled = true
    }
  }, [kannada, retryCount])

  useEffect(() => {
    let cancelled = false

    setLoading(true)
    setError('')

    getPlants({
      page,
      size: 12,
      search: search || undefined,
      category: category || undefined,
    })
      .then((data) => {
        if (!cancelled) setCatalogue(data)
      })
      .catch(() => {
        if (!cancelled) {
          setError(
            kannada
              ? 'ಸಸ್ಯಗಳ ವಿವರಗಳನ್ನು ಪಡೆಯಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ. ಸರ್ವರ್ ಚಾಲನೆಯಲ್ಲಿದೆಯೇ ಎಂದು ಪರಿಶೀಲಿಸಿ.'
              : 'Could not load plants. Please check that the server is running.',
          )
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [page, search, category, kannada, retryCount])

  function submitSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function selectCategory(value: string) {
    setCategory(value)
    setPage(0)
  }

  return (
    <section className="catalogue-section" id="plants" ref={section}>
      <div className="catalogue-heading">
        <span className="catalogue-eyebrow">
          <Leaf size={16} />
          {kannada ? 'ನಮ್ಮ ಸಸ್ಯ ಸಂಗ್ರಹ' : 'OUR PLANT COLLECTION'}
        </span>

        <WordReveal
          as="h2"
          className="lx-h2 lx-font-catalogue"
          scroll
          lines={
            kannada
              ? [{ text: 'ನಿಮ್ಮ ತೋಟಕ್ಕೆ ಸೂಕ್ತವಾದ' }, { text: 'ಸಸ್ಯಗಳನ್ನು ಹುಡುಕಿ', accent: true }]
              : [{ text: 'Find the right plants' }, { text: 'for your garden', accent: true }]
          }
        />

        <p>
          {kannada
            ? 'ಸಸ್ಯಗಳ ವಿವರಗಳನ್ನು ನೋಡಿ. ಬೆಲೆ ಮತ್ತು ಪ್ರಸ್ತುತ ಲಭ್ಯತೆಗಾಗಿ ನಮ್ಮನ್ನು ಸಂಪರ್ಕಿಸಿ.'
            : 'Explore our plant catalogue. Contact us to confirm current prices and availability.'}
        </p>
      </div>

      <form className="catalogue-search" onSubmit={submitSearch}>
        <Search size={20} aria-hidden="true" />

        <input
          type="search"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          placeholder={
            kannada
              ? 'ಸಸ್ಯದ ಹೆಸರನ್ನು ಹುಡುಕಿ...'
              : 'Search plant names...'
          }
          aria-label={kannada ? 'ಸಸ್ಯ ಹುಡುಕಾಟ' : 'Search plants'}
        />

        <button type="submit">
          {kannada ? 'ಹುಡುಕಿ' : 'Search'}
        </button>
      </form>

      <div className="catalogue-filters" aria-label="Plant categories">
        <button
          type="button"
          className={!category ? 'active' : ''}
          onClick={() => selectCategory('')}
        >
          {kannada ? 'ಎಲ್ಲಾ ಸಸ್ಯಗಳು' : 'All plants'}
        </button>

        {categories.map((item) => (
          <button
            type="button"
            key={item.id}
            className={category === item.slug ? 'active' : ''}
            onClick={() => selectCategory(item.slug)}
          >
            {kannada ? item.nameKn : item.nameEn}
          </button>
        ))}
      </div>

      {error && (
        <div className="catalogue-message catalogue-error" role="alert">
          <p>{error}</p>
          <button
            type="button"
            onClick={() => setRetryCount((count) => count + 1)}
          >
            {kannada ? 'ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ' : 'Retry'}
          </button>
        </div>
      )}

      {loading && (
        <div className="catalogue-message" role="status">
          <span className="catalogue-spinner" />
          {kannada ? 'ಸಸ್ಯಗಳನ್ನು ಲೋಡ್ ಮಾಡಲಾಗುತ್ತಿದೆ...' : 'Loading plants...'}
        </div>
      )}

      {!loading && !error && catalogue.content.length === 0 && (
        <div className="catalogue-message">
          {kannada
            ? 'ಈ ಹುಡುಕಾಟಕ್ಕೆ ಸಸ್ಯಗಳು ಕಂಡುಬಂದಿಲ್ಲ.'
            : 'No plants found for this search.'}
        </div>
      )}

      {!loading && !error && catalogue.content.length > 0 && (
        <>
          <div className="catalogue-grid">
            {catalogue.content.map((plant) => (
              <article className="catalogue-card" key={plant.id}>
                <div className="catalogue-image">
                  {plant.imageUrl ? (
                    <img
                      src={plant.imageUrl}
                      alt={kannada ? plant.nameKn : plant.nameEn}
                      loading="lazy"
                    />
                  ) : (
                    <div className="catalogue-image-placeholder">
                      <Leaf size={44} />
                      <span>
                        {kannada ? 'ಸಸ್ಯದ ಚಿತ್ರ ಶೀಘ್ರದಲ್ಲೇ' : 'Plant image coming soon'}
                      </span>
                    </div>
                  )}

                  {plant.featured && (
                    <span className="catalogue-featured">
                      {kannada ? 'ವಿಶೇಷ' : 'Featured'}
                    </span>
                  )}
                </div>

                <div className="catalogue-card-body">
                  <span className="catalogue-category">
                    {kannada
                      ? plant.categoryNameKn
                      : plant.categoryNameEn}
                  </span>

                  <h3>
                    <Link to={`/plants/${encodeURIComponent(plant.slug)}`}>
                      {kannada ? plant.nameKn : plant.nameEn}
                    </Link>
                  </h3>

                  {plant.scientificName && (
                    <p className="catalogue-scientific">
                      {plant.scientificName}
                    </p>
                  )}

                  {(kannada
                    ? plant.descriptionKn
                    : plant.descriptionEn) && (
                    <p className="catalogue-description">
                      {kannada
                        ? plant.descriptionKn
                        : plant.descriptionEn}
                    </p>
                  )}

                  <span
                    className={`catalogue-availability status-${plant.availability.toLowerCase()}`}
                  >
                    {availabilityLabel(plant.availability, kannada)}
                  </span>

                  <div className="catalogue-card-footer">
                    <div className="catalogue-price">
                      {plant.price !== null && plant.price !== undefined
                        ? `₹${plant.price.toLocaleString('en-IN')}`
                        : kannada
                          ? 'ಬೆಲೆ ವಿಚಾರಿಸಿ'
                          : 'Price on enquiry'}
                    </div>

                    <div className="catalogue-card-actions">
                      <Link
                        className="catalogue-details-link"
                        to={`/plants/${encodeURIComponent(plant.slug)}`}
                      >
                        {kannada ? 'ವಿವರಗಳು' : 'Details'}
                      </Link>

                      {createPlantWhatsAppUrl(plant, kannada) ? (
                        <a
                          className="catalogue-enquire"
                          href={createPlantWhatsAppUrl(plant, kannada)!}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {kannada ? 'ವಿಚಾರಿಸಿ' : 'Enquire'}
                        </a>
                      ) : (
                        <Link
                          className="catalogue-enquire"
                          to={`/plants/${encodeURIComponent(plant.slug)}`}
                        >
                          {kannada ? 'ವಿಚಾರಿಸಿ' : 'Enquire'}
                        </Link>
                      )}
                    </div>
                  </div>
                </div>
              </article>
            ))}
          </div>

          <div className="catalogue-pagination">
            <span>
              {kannada
                ? `ಒಟ್ಟು ${catalogue.totalElements} ಸಸ್ಯಗಳು`
                : `${catalogue.totalElements} plants total`}
            </span>

            <div>
              <button
                type="button"
                aria-label="Previous page"
                disabled={catalogue.first || loading}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                <ChevronLeft size={18} />
                {kannada ? 'ಹಿಂದೆ' : 'Previous'}
              </button>

              <span>
                {catalogue.totalPages === 0
                  ? '0 / 0'
                  : `${catalogue.page + 1} / ${catalogue.totalPages}`}
              </span>

              <button
                type="button"
                aria-label="Next page"
                disabled={catalogue.last || loading}
                onClick={() => setPage((current) => current + 1)}
              >
                {kannada ? 'ಮುಂದೆ' : 'Next'}
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
        </>
      )}
    </section>
  )
}
