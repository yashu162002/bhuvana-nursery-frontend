import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Leaf, MessageCircle } from 'lucide-react';
import { getPlantBySlug } from '../../api/plantApi';
import { siteConfig } from '../../config/siteConfig';
import { createPlantWhatsAppUrl } from '../../utils/whatsapp';
import type { Plant } from './plantTypes';
import { gsap, useGSAP, prefersReducedMotion } from '../../lib/gsap';
import SiteHeader from '../../components/luxury/SiteHeader';
import SiteFooter from '../../components/luxury/SiteFooter';

function Shell({ children }: { children: ReactNode }) {
  return (
    <>
      <SiteHeader solid />
      {children}
      <SiteFooter />
    </>
  );
}

interface PlantDetailPageProps {
  kannada?: boolean;
}

export default function PlantDetailPage({
  kannada = true,
}: PlantDetailPageProps) {
  const { slug } = useParams<{ slug: string }>();
  const [plant, setPlant] = useState<Plant | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const page = useRef<HTMLElement>(null);

  useEffect(() => {
    let cancelled = false;

    if (!slug) {
      setError(kannada ? 'ಸಸ್ಯದ ವಿವರಗಳು ಲಭ್ಯವಿಲ್ಲ.' : 'Plant not found.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError('');
    setPlant(null);

    getPlantBySlug(slug)
      .then((data) => {
        if (!cancelled) setPlant(data);
      })
      .catch((requestError: unknown) => {
        if (!cancelled) {
          const status =
            typeof requestError === 'object' &&
            requestError !== null &&
            'response' in requestError
              ? (requestError as {
                  response?: { status?: number };
                }).response?.status
              : undefined;

          setError(
            status === 404
              ? kannada
                ? 'ಈ ಸಸ್ಯದ ವಿವರಗಳು ಕಂಡುಬಂದಿಲ್ಲ.'
                : 'This plant could not be found.'
              : kannada
                ? 'ಸಸ್ಯದ ವಿವರಗಳನ್ನು ಲೋಡ್ ಮಾಡಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ.'
                : 'Could not load plant details. Please try again.',
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [slug, kannada]);

  useGSAP(
    () => {
      if (!plant || prefersReducedMotion()) return;

      gsap
        .timeline({ defaults: { ease: 'expo.out' } })
        .fromTo('.plant-back-link', { autoAlpha: 0, x: -24 }, { autoAlpha: 1, x: 0, duration: 1 }, 0.1)
        .fromTo(
          '.plant-detail-image',
          { clipPath: 'inset(100% 0% 0% 0% round 28px)' },
          { clipPath: 'inset(0% 0% 0% 0% round 28px)', duration: 1.7, ease: 'expo.inOut' },
          0.1,
        )
        .fromTo(
          '.plant-detail-image img, .plant-detail-placeholder',
          { scale: 1.35 },
          { scale: 1, duration: 2.2 },
          0.1,
        )
        .fromTo(
          '.plant-detail-info > *',
          { autoAlpha: 0, y: 46 },
          { autoAlpha: 1, y: 0, duration: 1.2, stagger: 0.09 },
          0.55,
        );
    },
    { scope: page, dependencies: [plant?.id] },
  );

  if (loading) {
    return (
      <Shell>
        <main className="plant-detail-state" role="status">
          <span className="catalogue-spinner" />
          {kannada ? 'ಸಸ್ಯದ ವಿವರಗಳನ್ನು ಲೋಡ್ ಮಾಡಲಾಗುತ್ತಿದೆ...' : 'Loading plant details...'}
        </main>
      </Shell>
    );
  }

  if (error || !plant) {
    return (
      <Shell>
        <main className="plant-detail-state">
          <p>{error || (kannada ? 'ಸಸ್ಯ ಕಂಡುಬಂದಿಲ್ಲ.' : 'Plant not found.')}</p>
          <Link to="/#plants" className="plant-back-link">
            <ArrowLeft size={18} />
            {kannada ? 'ಸಸ್ಯಗಳ ಪಟ್ಟಿಗೆ ಹಿಂತಿರುಗಿ' : 'Back to plants'}
          </Link>
        </main>
      </Shell>
    );
  }

  const whatsappUrl = createPlantWhatsAppUrl(plant, kannada);

  const description = kannada
    ? plant.descriptionKn
    : plant.descriptionEn;

  const availabilityLabel =
    plant.availability === 'AVAILABLE'
      ? kannada ? 'ಲಭ್ಯವಿದೆ' : 'Available'
      : plant.availability === 'OUT_OF_STOCK'
        ? kannada ? 'ಪ್ರಸ್ತುತ ಲಭ್ಯವಿಲ್ಲ' : 'Out of stock'
        : kannada ? 'ಲಭ್ಯತೆ ದೃಢಪಡಿಸಿಕೊಳ್ಳಿ' : 'Availability to be confirmed';

  return (
    <Shell>
    <main className="plant-detail-page" ref={page}>
      <div className="plant-detail-container">
        <Link to="/#plants" className="plant-back-link">
          <ArrowLeft size={18} />
          {kannada ? 'ಎಲ್ಲಾ ಸಸ್ಯಗಳು' : 'All plants'}
        </Link>

        <div className="plant-detail-layout">
          <div className="plant-detail-image">
            {plant.imageUrl ? (
              <img
                src={plant.imageUrl}
                alt={kannada ? plant.nameKn : plant.nameEn}
              />
            ) : (
              <div className="plant-detail-placeholder">
                <Leaf size={64} />
                <span>
                  {kannada ? 'ಚಿತ್ರ ಶೀಘ್ರದಲ್ಲೇ ಲಭ್ಯ' : 'Image coming soon'}
                </span>
              </div>
            )}
          </div>

          <section className="plant-detail-info">
            <span className="plant-detail-category">
              {kannada ? plant.categoryNameKn : plant.categoryNameEn}
            </span>

            <h1>{kannada ? plant.nameKn : plant.nameEn}</h1>

            {plant.scientificName && (
              <p className="plant-detail-scientific">
                {plant.scientificName}
              </p>
            )}

            <p className="plant-detail-availability">
              {availabilityLabel}
            </p>

            <div className="plant-detail-price">
              {plant.price !== null && plant.price !== undefined
                ? `₹${plant.price.toLocaleString('en-IN')}`
                : kannada
                  ? 'ಬೆಲೆಗಾಗಿ ಸಂಪರ್ಕಿಸಿ'
                  : 'Price on enquiry'}
            </div>

            {description && (
              <div className="plant-detail-description">
                <h2>{kannada ? 'ಸಸ್ಯದ ವಿವರಗಳು' : 'About this plant'}</h2>
                <p>{description}</p>
              </div>
            )}

            <p className="plant-detail-note">
              {kannada
                ? 'ಬೆಲೆ, ಗಾತ್ರ ಮತ್ತು ಪ್ರಸ್ತುತ ಲಭ್ಯತೆ ಬದಲಾಗಬಹುದು. ಖರೀದಿಸುವ ಮೊದಲು ನರ್ಸರಿಯೊಂದಿಗೆ ದೃಢಪಡಿಸಿಕೊಳ್ಳಿ.'
                : 'Price, size and current availability may vary. Please confirm with the nursery before purchasing.'}
            </p>

            {whatsappUrl ? (
              <a
                className="plant-whatsapp-button"
                href={whatsappUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                <MessageCircle size={20} />
                {kannada ? 'WhatsApp ಮೂಲಕ ವಿಚಾರಿಸಿ' : 'Enquire on WhatsApp'}
              </a>
            ) : (
              <p className="plant-contact-warning">
                {kannada
                  ? 'WhatsApp ಸಂಪರ್ಕ ಸಂಖ್ಯೆ ಇನ್ನೂ ಹೊಂದಿಸಲಾಗಿಲ್ಲ. ದಯವಿಟ್ಟು ನಂತರ ಪ್ರಯತ್ನಿಸಿ.'
                  : 'WhatsApp contact is not configured yet. Please try again later.'}
              </p>
            )}

            {siteConfig.phoneNumber && (
              <a
                className="plant-call-link"
                href={`tel:${siteConfig.phoneNumber.replace(/[^\d+]/g, '')}`}
              >
                {kannada ? 'ದೂರವಾಣಿ ಕರೆ ಮಾಡಿ' : 'Call the nursery'}
              </a>
            )}
          </section>
        </div>
      </div>
    </main>
    </Shell>
  );
}
