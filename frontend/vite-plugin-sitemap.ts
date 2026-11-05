/**
 * vite-plugin-sitemap.ts
 *
 * Fetches published plant slugs from the production API at build time and
 * writes a complete sitemap.xml into the `dist/` directory.
 *
 * Usage: add to vite.config.ts plugins array.
 *
 * The plugin runs only during `vite build` (mode === 'build'). It is a no-op
 * during dev so local start-up is not slowed by network calls.
 *
 * Environment variable SITEMAP_API_URL can override the base URL used for
 * fetching plants (defaults to the production Render API).
 */

import type { Plugin } from 'vite'
import https from 'node:https'
import http from 'node:http'
import fs from 'node:fs'
import path from 'node:path'

const SITE_URL = 'https://bhoovana.netlify.app'
const API_BASE =
  process.env.SITEMAP_API_URL ??
  'https://bhuvana-nursery-api1.onrender.com/api/v1'

/** Minimal shape we care about from /plants response. */
interface PlantSummary {
  slug: string
  published?: boolean
}
interface PlantPage {
  content: PlantSummary[]
  totalPages: number
}

function fetchJson<T>(url: string): Promise<T> {
  return new Promise((resolve, reject) => {
    const mod = url.startsWith('https') ? https : http
    mod
      .get(url, (res) => {
        let body = ''
        res.on('data', (chunk: Buffer) => (body += chunk.toString()))
        res.on('end', () => {
          try {
            resolve(JSON.parse(body) as T)
          } catch {
            reject(new Error(`JSON parse error for ${url}`))
          }
        })
      })
      .on('error', reject)
  })
}

async function fetchAllSlugs(): Promise<string[]> {
  const slugs: string[] = []
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    try {
      const data = await fetchJson<PlantPage>(
        `${API_BASE}/plants?page=${page}&size=100`,
      )
      for (const p of data.content) {
        if (p.published !== false) slugs.push(p.slug)
      }
      totalPages = data.totalPages
      page++
    } catch {
      // If the API is unreachable during build (e.g., Render is cold),
      // emit the sitemap without plant detail pages rather than failing.
      console.warn('[sitemap] Could not fetch plant slugs – skipping plant URLs.')
      break
    }
  }

  return slugs
}

function buildSitemap(slugs: string[]): string {
  const today = new Date().toISOString().split('T')[0]

  const staticUrls = `
  <url>
    <loc>${SITE_URL}/</loc>
    <lastmod>${today}</lastmod>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>`.trim()

  const dynamicUrls = slugs
    .map(
      (slug) => `
  <url>
    <loc>${SITE_URL}/plants/${encodeURIComponent(slug)}</loc>
    <lastmod>${today}</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.8</priority>
  </url>`.trim(),
    )
    .join('\n  ')

  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">

  ${staticUrls}

  ${dynamicUrls}

</urlset>`
}

export function viteSitemap(): Plugin {
  return {
    name: 'vite-sitemap',
    apply: 'build',
    async closeBundle() {
      console.log('[sitemap] Generating sitemap.xml…')
      const slugs = await fetchAllSlugs()
      const xml = buildSitemap(slugs)

      // Write to dist/ (Vite's output directory)
      const outDir = path.resolve(process.cwd(), 'dist')
      fs.mkdirSync(outDir, { recursive: true })
      fs.writeFileSync(path.join(outDir, 'sitemap.xml'), xml, 'utf8')

      console.log(
        `[sitemap] sitemap.xml written with ${1 + slugs.length} URLs (1 static + ${slugs.length} plants).`,
      )
    },
  }
}
