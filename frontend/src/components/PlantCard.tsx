import { ArrowUpRight, MessageCircle } from 'lucide-react'
import type { Plant } from '../features/plants/plantTypes'
import { assetUrl } from '../config/siteConfig'

type PlantCardProps = {
  plant: Plant
  language?: 'kn' | 'en'
  whatsappNumber?: string
}

export default function PlantCard({
  plant,
  language = 'kn',
  whatsappNumber = '',
}: PlantCardProps) {
  const isKannada = language === 'kn'
  const imageUrl = assetUrl(plant.imageUrl)

  const message = isKannada
    ? `ನಮಸ್ಕಾರ, ನನಗೆ ${plant.nameKn} (${plant.nameEn}) ಬಗ್ಗೆ ಮಾಹಿತಿ ಬೇಕು. ದಯವಿಟ್ಟು ಬೆಲೆ ಮತ್ತು ಪ್ರಸ್ತುತ ಲಭ್ಯತೆ ತಿಳಿಸಿ.`
    : `Hello, I would like to enquire about ${plant.nameEn}. Please share the current price and availability.`

  const whatsappUrl = whatsappNumber
    ? `https://wa.me/${whatsappNumber}?text=${encodeURIComponent(message)}`
    : null

  const priceLabel =
    plant.enquiryOnly || plant.price === null
      ? isKannada
        ? 'ಬೆಲೆಗಾಗಿ ಸಂಪರ್ಕಿಸಿ'
        : 'Enquire for price'
      : new Intl.NumberFormat(isKannada ? 'kn-IN' : 'en-IN', {
          style: 'currency',
          currency: 'INR',
          maximumFractionDigits: 2,
        }).format(plant.price)

  const availabilityLabel = {
    AVAILABLE: isKannada ? 'ಲಭ್ಯವಿದೆ' : 'Available',
    OUT_OF_STOCK: isKannada ? 'ಲಭ್ಯವಿಲ್ಲ' : 'Out of stock',
    UNKNOWN: isKannada ? 'ಲಭ್ಯತೆ ವಿಚಾರಿಸಿ' : 'Check availability',
  }[plant.availability]

  return (
    <article className="plant-card">
      <div className="plant-image">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={isKannada ? plant.nameKn : plant.nameEn}
            loading="lazy"
          />
        ) : (
          <div className="empty-state">
            {isKannada ? 'ಚಿತ್ರ ಲಭ್ಯವಿಲ್ಲ' : 'No image available'}
          </div>
        )}
        <span className="plant-tag">{availabilityLabel}</span>
      </div>

      <div className="plant-info">
        <span className="plant-english">{plant.nameEn}</span>
        <h3>{plant.nameKn}</h3>
        <p>{isKannada ? plant.descriptionKn : plant.descriptionEn}</p>

        <div className="plant-card-footer">
          <div>
            <span className="price-label">
              {isKannada ? 'ಬೆಲೆ' : 'PRICE'}
            </span>
            <strong>{priceLabel}</strong>
          </div>

          {whatsappUrl ? (
            <a
              className="plant-enquiry"
              href={whatsappUrl}
              target="_blank"
              rel="noreferrer"
              aria-label={`Enquire about ${plant.nameEn} on WhatsApp`}
            >
              <MessageCircle size={19} />
            </a>
          ) : (
            <a
              className="plant-enquiry"
              href="#contact"
              aria-label={`Contact the nursery about ${plant.nameEn}`}
            >
              <ArrowUpRight size={19} />
            </a>
          )}
        </div>
      </div>
    </article>
  )
}
