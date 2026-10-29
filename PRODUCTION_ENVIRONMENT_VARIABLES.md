# Production Environment Variables

This file lists every variable the deployed application reads. Values shown are
**sanitized placeholders**. Never commit real credentials.

Legend: **R** = required in production, **O** = optional/tuning, **S** = secret.

---

## 1. Render — backend service

Set these in Render → your service → **Environment**. `render.yaml` already
declares the keys; values marked `sync: false` must be entered in the dashboard.

| Variable | Req. | Example (placeholder) | Purpose / notes |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | R | `prod` | Enables `application-prod.properties`: Secure cookies, trusted client-IP handling, production log levels. |
| `PORT` | auto | *(Render sets it)* | Render injects the HTTP port. The app binds `${PORT:8080}`; never hardcode 8080 in production. |
| `DB_URL` | R, S | `jdbc:postgresql://ep-xxxx.region.aws.neon.tech/bhuvana_nursery?sslmode=require` | Neon JDBC URL. Keep `sslmode=require`. Prefer the **direct** (non-pooler) host for Flyway reliability. |
| `DB_USERNAME` | R, S | `neondb_owner` | Neon role. |
| `DB_PASSWORD` | R, S | `your-neon-password` | Neon password. |
| `ADMIN_USERNAME` | R (first boot) | `nurseryadmin` | Seeds the first administrator **only when `admin_users` is empty**. Ignored afterwards. |
| `ADMIN_PASSWORD` | R (first boot), S | `a-strong-unique-password` | Initial password. Ignored once the account exists. Remove from Render after the first successful login if you keep a secure offline copy. |
| `ADMIN_EMAIL` | R, S | `admin-inbox@example.com` | Inbox allowed to request resets. Backfills `admin_users.email` only while it is blank. Later changes are ignored (database wins) — see the deployment guide. |
| `SMTP_HOST` | R | `smtp.gmail.com` | Gmail SMTP host. Empty disables email and makes forgot-password return 503. |
| `SMTP_PORT` | R | `587` | STARTTLS port. |
| `SMTP_USERNAME` | R, S | `your-gmail-address@gmail.com` | Gmail account that sends the mail. |
| `SMTP_APP_PASSWORD` | R, S | `your-16-character-google-app-password` | Google **App Password**, not the normal Gmail password. Requires 2-Step Verification. |
| `MAIL_FROM` | R | `your-gmail-address@gmail.com` | Envelope/header sender. Keep identical to `SMTP_USERNAME` (Gmail rejects arbitrary From addresses). Defaults to `SMTP_USERNAME` if omitted. |
| `APP_BASE_URL` | R | `https://bhuvanursery.com` | Canonical public frontend URL. The reset link becomes `${APP_BASE_URL}/admin/reset-password?token=…`. No trailing slash required. |
| `IMAGE_STORAGE` | R | `r2` | `r2` in production. `local` writes to Render's ephemeral disk and is development-only. |
| `R2_ENDPOINT` | R, S | `https://<account-id>.r2.cloudflarestorage.com` | Cloudflare R2 S3 endpoint. |
| `R2_BUCKET` | R | `bhuvana-nursery-images` | Bucket name. |
| `R2_ACCESS_KEY_ID` | R, S | `your-r2-access-key-id` | R2 API token access key (Object Read & Write). |
| `R2_SECRET_ACCESS_KEY` | R, S | `your-r2-secret-access-key` | R2 secret. Server-side only. |
| `R2_PUBLIC_BASE_URL` | R | `https://images.bhuvanursery.com` | Public base URL of the bucket (r2.dev URL or custom domain). Stored image URLs are `${R2_PUBLIC_BASE_URL}/uploads/<uuid>.jpg`. |
| `R2_REGION` | O | `auto` | R2 signing region; `auto` is correct for R2. |
| `CORS_ALLOWED_ORIGINS` | O | *(empty)* | Only for split-origin deployments. Comma-separated exact origins, e.g. `https://bhuvanursery.com`. Never `*`. Leave empty with the recommended Netlify proxy. |
| `SECURE_COOKIES` | O | `true` | Overrides the prod default. Must be `true` on HTTPS. |
| `COOKIE_SAME_SITE` | O | `Lax` | `Lax` behind the same-origin Netlify proxy. Use `None` only for a cross-site API (requires HTTPS + CORS). |
| `TRUST_FORWARDED_FOR` | O | `true` | Prod default. Only keep true behind a trusted proxy. Uses Netlify's `X-Nf-Client-Connection-Ip`, otherwise the nearest `X-Forwarded-For` hop; feeds login/reset rate limiting. |
| `DB_POOL_SIZE` | O | `5` | Hikari maximum pool size. Keep small on Neon free/starter tiers. |
| `DB_POOL_MIN_IDLE` | O | `1` | Minimum idle connections. |
| `DB_CONNECTION_TIMEOUT_MS` | O | `10000` | Fail fast when the database is unreachable. |
| `DB_MAX_LIFETIME_MS` | O | `1800000` | 30 min; keeps connections fresh. |
| `DB_KEEPALIVE_MS` | O | `300000` | 5 min keepalive for quiet periods. |
| `DB_CONNECT_RETRIES` | O | `3` | Flyway connection retries during Neon wake-up. |
| `SESSION_TIMEOUT` | O | `8h` | Admin session lifetime. |
| `UPLOAD_DIRECTORY` | O | `./uploads` | Only used when `IMAGE_STORAGE=local`. |
| `FRONTEND_BASE_URL` | O | *(legacy)* | Older alias for `APP_BASE_URL`; `APP_BASE_URL` wins when both are set. |

### Sanitized Render `.env`-style example

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://ep-xxxx.region.aws.neon.tech/bhuvana_nursery?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=replace-me
ADMIN_USERNAME=nurseryadmin
ADMIN_PASSWORD=replace-me
ADMIN_EMAIL=admin-inbox@example.com
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-gmail-address@gmail.com
SMTP_APP_PASSWORD=replace-me
MAIL_FROM=your-gmail-address@gmail.com
APP_BASE_URL=https://bhuvanursery.com
IMAGE_STORAGE=r2
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
R2_BUCKET=bhuvana-nursery-images
R2_ACCESS_KEY_ID=replace-me
R2_SECRET_ACCESS_KEY=replace-me
R2_PUBLIC_BASE_URL=https://images.bhuvanursery.com
R2_REGION=auto
CORS_ALLOWED_ORIGINS=
```

---

## 2. Netlify — frontend build

| Variable | Req. | Example | Purpose |
|---|---|---|---|
| `VITE_API_BASE_URL` | O | `/api/v1` | Default is `/api/v1`. Leave at the default when Netlify proxies `/api` to Render (recommended). If set to an absolute URL, the API is cross-site: configure backend CORS and `COOKIE_SAME_SITE=None`. |
| `VITE_WHATSAPP_NUMBER` | O | `919740114246` | Public WhatsApp number (digits only, country code). |
| `VITE_PHONE_NUMBER` | O | `+919740114246` | Public telephone link. |
| `NODE_VERSION` | O | `22` | Declared in `netlify.toml`; Vite 8 needs Node 20.19+ or 22.12+. |

**Never** put database, SMTP, R2 or admin credentials in a `VITE_*` variable.
Vite inlines them into the public JavaScript bundle.

---

## 3. Local development (`backend/.env`)

Copy `backend/.env.example` to `backend/.env` and fill it in. Local defaults:
`IMAGE_STORAGE=local`, `SECURE_COOKIES=false`, `COOKIE_SAME_SITE=Lax`,
`TRUST_FORWARDED_FOR=false`, `APP_BASE_URL=http://localhost:5173`.

The frontend dev server uses `VITE_API_BASE_URL=/api/v1` and Vite proxies
`/api` and `/uploads` to `http://localhost:8080` (`frontend/vite.config.ts`).

---

## 4. Secret-handling rules

1. `.env` files are gitignored. `.env.example` contains placeholders only.
2. Rotate any credential that has ever been pasted into chat, a ticket, a log or
   a commit. A Gmail App Password can be revoked at
   `https://myaccount.google.com/apppasswords`.
3. Render/Netlify environment variables are write-only in their dashboards;
   prefer them over committed files.
4. Never log tokens, passwords, cookies, Authorization headers or full
   exception payloads. The application logs only token-free audit lines.
