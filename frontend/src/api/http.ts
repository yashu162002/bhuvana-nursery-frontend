import axios from 'axios';

const http = axios.create({
  // In production the Netlify proxy makes every request same-origin, so
  // /api/v1 is always correct. VITE_API_BASE_URL is only needed if the
  // frontend ever calls the Render API directly (cross-site), which
  // requires additional CORS + SameSite=None backend configuration.
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  // Render free tier can take up to 50 s to wake from sleep on the first
  // request. 30 s covers most cold-start delays without hanging forever.
  timeout: 30000,
  withCredentials: true,
  // Axios reads XSRF-TOKEN cookie and sends it as X-XSRF-TOKEN header.
  // The backend SpaCsrfTokenRequestHandler accepts the raw cookie value
  // via xorHandler when the header is present.
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: {
    Accept: 'application/json',
  },
});

export default http;
