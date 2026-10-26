import { siteConfig } from '../config/siteConfig';
import type { Plant } from '../features/plants/plantTypes';

export function createPlantWhatsAppUrl(
  plant: Plant,
  kannada = true,
): string | null {
  const number = siteConfig.whatsappNumber.replace(/\D/g, '');

  // Never create a misleading link if the business number is missing.
  if (!number) {
    return null;
  }

  const plantName = kannada ? plant.nameKn : plant.nameEn;
  const pageUrl =
    `${window.location.origin}/plants/${encodeURIComponent(plant.slug)}`;

  const message = kannada
    ? [
        'ನಮಸ್ಕಾರ ಭೂವನ ನರ್ಸರಿ,',
        '',
        `ನನಗೆ ${plantName} ಬಗ್ಗೆ ಮಾಹಿತಿ ಬೇಕಾಗಿದೆ.`,
        'ದಯವಿಟ್ಟು ಪ್ರಸ್ತುತ ಬೆಲೆ ಮತ್ತು ಲಭ್ಯತೆಯನ್ನು ತಿಳಿಸಿ.',
        '',
        `ಸಸ್ಯದ ವಿವರ: ${pageUrl}`,
      ].join('\n')
    : [
        'Hello Bhoovana Nursery,',
        '',
        `I am interested in ${plantName}.`,
        'Please share the current price and availability.',
        '',
        `Plant details: ${pageUrl}`,
      ].join('\n');

  return `https://wa.me/${number}?text=${encodeURIComponent(message)}`;
}
