export const siteConfig = {
  name: 'Bhoovana Nursery',
  nameKn: 'ಭೂವನ ನರ್ಸರಿ',
  nurseryName: 'Bhoovana Nursery',
  nurseryNameKn: 'ಭೂವನ ನರ್ಸರಿ',
  address:
    'Gonur, 4th mile near upper Bhadra Meldande pillar turuvanur road, Chitradurga Taluk and District, Karnataka 577517, India',
  apiBaseUrl:
    import.meta.env.VITE_API_BASE_URL || '/api/v1',
  whatsappNumber:
    import.meta.env.VITE_WHATSAPP_NUMBER || '919740114246',
  phoneNumber:
    import.meta.env.VITE_PHONE_NUMBER || '+919740114246',
  mapLink: 'https://www.google.com/maps?q=14.2658817,76.43122&z=17&hl=en',
};

/**
 * Origin that serves uploaded images, derived from VITE_API_BASE_URL.
 *
 * With the default same-origin `/api/v1` (Vite dev proxy, Netlify redirect)
 * this is empty, so `/uploads/...` keeps working unchanged. When the API lives
 * on its own origin the stored relative paths must be prefixed with that
 * origin, otherwise they would be requested from the frontend host.
 */
const apiOrigin = (() => {
  const base = siteConfig.apiBaseUrl;

  if (/^https?:\/\//i.test(base)) {
    try {
      return new URL(base).origin;
    } catch {
      return '';
    }
  }

  return '';
})();

/** Resolves a stored image path (`/uploads/x.jpg`, absolute URL) for rendering. */
export function assetUrl(path: string | null | undefined): string | null {
  if (!path) {
    return null;
  }

  if (/^(https?:|data:|blob:)/i.test(path)) {
    return path;
  }

  return `${apiOrigin}${path.startsWith('/') ? path : `/${path}`}`;
}
