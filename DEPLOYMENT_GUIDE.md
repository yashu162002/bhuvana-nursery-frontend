# Bhuvana Nursery — Production Deployment Guide

Target architecture:

| Layer | Provider | Notes |
|---|---|---|
| Frontend | Netlify | React 19 + TypeScript + Vite 8; SPA redirects; `/api` proxy to Render |
| Backend | Render (Docker) | Spring Boot 4.1.2, Java 21, single instance |
| Database | Neon PostgreSQL | Flyway migrations V1–V6 run automatically on startup |
| Images | Cloudflare R2 | S3-compatible; backend uploads, public read |
| Email | Gmail SMTP | Google App Password; admin password-reset links |
| Domain | Hostinger | DNS points at Netlify; HTTPS via Netlify |

This guide assumes no prior production deployment. Follow the steps in order.

---

## 0. Before you start (pre-flight)

- [ ] Rotate the Gmail App Password that was exposed. Revoke it at
      `https://myaccount.google.com/apppasswords`, then generate a new one.
      Never reuse the old value.
- [ ] Decide the canonical frontend URL, for example
      `https://bhuvanursery.com` (apex) or `https://www.bhuvanursery.com`.
      It must be the URL used in `APP_BASE_URL` and in the Netlify primary
      domain.
- [ ] Make sure the frontend source is actually in the repository Netlify will
      build. **Current workspace caveat:** `frontend/` is a nested Git
      repository and is not tracked by the root repository. Choose one:
      - **A (recommended):** make `frontend/` part of the root repository
        (back up `frontend/.git`, remove it, then add/commit the folder), or
      - **B:** connect Netlify directly to the `frontend/` repository. Then copy
        the `[build]` section from `netlify.toml`, remove `base = "frontend"`,
        and keep `publish = "dist"`.
      Do not deploy a repository that does not contain the frontend files.
- [ ] Keep `backend/.env` local only. It is gitignored and must never be
      uploaded to Render or committed.
- [ ] Back up the current local database and `backend/uploads/` before changing
      anything (see §11).

No commits or pushes were made by this audit. Review `git status` before
committing.

---

## 1. Neon PostgreSQL

1. Create a Neon project and a database (for example `bhuvana_nursery`).
2. Copy the **direct** connection string (host without `-pooler`). Flyway runs
   schema migrations at startup and is most reliable over a direct connection.
3. Convert it to a JDBC URL and keep SSL:

   ```text
   jdbc:postgresql://<neon-host>/<database>?sslmode=require
   ```

4. Keep the role/password for `DB_USERNAME` / `DB_PASSWORD`.
5. Do **not** create tables manually. On first backend start Flyway applies:
   - `V1__create_plant_catalogue.sql`
   - `V2__seed_initial_catalogue.sql`
   - `V3__add_plant_image_url.sql`
   - `V4__create_hero_slides.sql`
   - `V5__update_hero_slides_to_current_six.sql`
   - `V6__create_admin_users_and_password_resets.sql`

   The fresh-schema test in this repository proves all six run in order
   (`FreshDatabaseMigrationTest`).

### Neon notes

- `sslmode=require` is mandatory.
- Keep `DB_POOL_SIZE` small (5 is the default here). Neon free/starter tiers
  have connection limits; a single Render instance with a 5-connection pool is
  sufficient.
- If you must use Neon's pooled endpoint, test Flyway carefully and consider
  `prepareThreshold=0` in the URL; a direct endpoint is recommended.
- Neon suspends idle computes. The first request after a pause can take a few
  seconds. Flyway retries (`DB_CONNECT_RETRIES=3`) and Hikari timeouts cover
  normal wake-ups.
- If the database is unreachable at startup, the app fails fast and Render marks
  the deploy unhealthy. It does not partially migrate: Flyway wraps each
  migration in a transaction and records it only on success.

### Backups and recovery

- Enable Neon's point-in-time restore / create a branch before risky changes.
- Manual logical backup (run from a trusted machine, never commit the dump):

  ```bash
  pg_dump "postgresql://<user>:<password>@<host>/<database>?sslmode=require" \
    --format=custom --file=bhuvana-$(date +%Y%m%d).dump
  ```

- Restore **to a new Neon branch/database first**, verify it, then switch the
  app's `DB_URL`. Never restore over the live database.
- Never run `DROP`, `TRUNCATE`, `flyway clean` or destructive SQL against
  production.

---

## 2. Cloudflare R2 (plant and hero images)

Render's filesystem is ephemeral: every deploy replaces it. Production must use
R2, not local uploads.

1. Create an R2 bucket, for example `bhuvana-nursery-images`.
2. Enable public access:
   - simplest: the bucket's `r2.dev` public URL, or
   - recommended: a custom domain such as `images.bhuvanursery.com`.
3. Create an R2 API token with **Object Read & Write** for that bucket. Record
   the access key ID and secret once; Cloudflare will not show the secret again.
4. Collect:
   - `R2_ENDPOINT` = `https://<account-id>.r2.cloudflarestorage.com`
   - `R2_BUCKET`
   - `R2_ACCESS_KEY_ID`
   - `R2_SECRET_ACCESS_KEY`
   - `R2_PUBLIC_BASE_URL` = the public bucket/domain URL, **no trailing slash**
5. Set `IMAGE_STORAGE=r2` on Render.

### How the app uses R2

- The admin upload endpoint (`POST /api/v1/admin/uploads`) validates the file
  (JPEG/PNG only, max 5 MB, max 6000 px / 24 MP, decoded format checked), then
  stores `uploads/<uuid>.<ext>` in R2 and returns the absolute public URL.
- The URL is saved in `plants.image_url` / `hero_slides.image_url`.
- Replacing or deleting an image schedules deletion of the old object **after**
  the database commit, and only if no other plant or slide still references it.
- Credentials stay on the backend. No R2 secret is ever sent to the browser.
- R2 CORS is not required for public `<img>` rendering or backend-side uploads.
  If you later add direct browser-to-R2 uploads, configure CORS explicitly.

### Migrating the existing local images (required for this project)

The current database contains local paths (`/uploads/...`): all 14 plants and
1 hero slide. Those files are in `backend/uploads/` on this machine and will not
exist on Render. New uploads go to R2, but old rows must be migrated.

1. Back up the database.
2. Upload the existing files to R2, preserving their filenames, under the
   `uploads/` prefix. Either:
   - drag-and-drop into the bucket in the Cloudflare dashboard, or
   - with AWS CLI configured for R2:

     ```bash
     aws s3 cp backend/uploads/ \
       s3://bhuvana-nursery-images/uploads/ \
       --recursive \
       --endpoint-url https://<account-id>.r2.cloudflarestorage.com
     ```

3. Verify a few objects open publicly, for example
   `https://images.bhuvanursery.com/uploads/<filename>.jpg`.
4. Rewrite the stored URLs (replace the placeholder with your real public base,
   **without** a trailing slash):

   ```sql
   BEGIN;

   UPDATE plants
   SET image_url = 'https://images.bhuvanursery.com' || image_url,
       updated_at = now()
   WHERE image_url LIKE '/uploads/%';

   UPDATE hero_slides
   SET image_url = 'https://images.bhuvanursery.com' || image_url,
       updated_at = now()
   WHERE image_url LIKE '/uploads/%';

   COMMIT;
   ```

5. Verify no legacy paths remain:

   ```sql
   SELECT count(*) FROM plants       WHERE image_url LIKE '/uploads/%';
   SELECT count(*) FROM hero_slides  WHERE image_url LIKE '/uploads/%';
   ```

6. Restart the backend. With `IMAGE_STORAGE=r2`, startup logs a WARN if any
   legacy paths remain. Do not go live until that warning is gone.

The application never deletes local files automatically. Keep
`backend/uploads/` until the migration is verified.

---

## 3. Gmail SMTP (password reset email)

1. Enable 2-Step Verification on the sending Google account.
2. Create an App Password at `https://myaccount.google.com/apppasswords`.
   Use the 16-character value, not the normal account password.
3. Use the same address for:
   - `SMTP_USERNAME` (authenticated Gmail account)
   - `MAIL_FROM` (sender; Gmail rejects a different From without an alias)
   - `ADMIN_EMAIL` (the inbox that receives reset links)
4. Set `SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`.
5. Set `APP_BASE_URL` to the deployed frontend URL. The reset link is
   `${APP_BASE_URL}/admin/reset-password?token=…`; it must never be localhost.
6. Restart the backend and confirm startup logs show the reset email was
   backfilled (only when `admin_users.email` was blank):

   ```text
   Set the password-reset email for administrator 'nurseryadmin' from ADMIN_EMAIL.
   ```

   If `ADMIN_EMAIL` is empty and the account has no stored email, startup logs:

   ```text
   Password reset email is disabled for administrator(s) [...] ...
   ```

7. Send exactly one Forgot Password request from the deployed admin page and
   check Render logs for either:

   ```text
   Password reset email sent for administrator 'nurseryadmin'.
   ```

   or

   ```text
   Password reset email could not be sent: <safe reason>
   ```

   The API always answers generically (202) so it cannot be used to probe for
   accounts; the server log is the only reliable delivery signal.
8. Check Gmail Inbox, Spam and All Mail. Open the link and confirm the domain is
   the deployed frontend (not localhost), the token works once, the old password
   stops working and the new one signs in.

### Email troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Forgot password returns 503 | `SMTP_HOST` or `MAIL_FROM` empty | Set both, restart |
| 202 but no email, log says no admin email | `admin_users.email` empty and `ADMIN_EMAIL` empty | Set `ADMIN_EMAIL`, restart (backfills once) |
| 202 but no email, log says send failed | Wrong App Password / 2FA missing / Gmail blocked | Generate a new App Password; check Google account security alerts |
| Link points to localhost | `APP_BASE_URL` wrong | Set the canonical HTTPS frontend URL and restart |
| `ADMIN_EMAIL` changed but old address still receives | Email is already stored; backfill happens once | Update `admin_users.email` directly (see §6) |

Rate limiting: 3 requests per email and 10 per IP per hour (configurable via
`app.password-reset.rate-limit.*`). Tokens are 256-bit, stored only as SHA-256
hashes, expire in 20 minutes, are single-use, and invalidate earlier unused
links.

---

## 4. Render backend

### Option A — Blueprint (recommended)

1. Push the repository to GitHub (including `render.yaml` and `backend/`).
2. Render → **New** → **Blueprint** → select the repository.
3. Render reads `render.yaml`:
   - Docker context `backend`, Dockerfile `backend/Dockerfile`
   - health check `/actuator/health`
   - one instance
4. Fill every `sync: false` value from
   `PRODUCTION_ENVIRONMENT_VARIABLES.md`.
5. Deploy and watch the logs.

### Option B — manual Docker service

- New → Web Service → repository
- Runtime: **Docker**
- Dockerfile path: `backend/Dockerfile`
- Docker context: `backend`
- Health check path: `/actuator/health`
- Add the environment variables from the table.
- Do not set `PORT`; Render injects it and the app binds `${PORT:8080}`.

### Build/runtime facts

- Java 21 (Docker build stage and JRE runtime).
- Spring Boot **4.1.2 GA**; the previous `4.2.0-SNAPSHOT` parent and snapshot
  repositories were removed so production builds depend only on Maven Central.
- `mvn -DskipTests package` inside Docker; tests run locally/CI, not on Render.
- Flyway runs on startup; `ddl-auto=validate` means a schema mismatch fails the
  deploy instead of silently altering tables.
- Graceful shutdown is enabled.
- Health endpoint: `/actuator/health` (includes the database; details hidden).
  `/api/v1/health` is a lightweight public liveness endpoint.

### First boot checklist

- [ ] Flyway log: 6 migrations applied or already up to date.
- [ ] `Seeded initial administrator account '…'` (first boot only).
- [ ] `Image storage: Cloudflare R2 bucket '…'`.
- [ ] No `Password reset email is disabled…` warning.
- [ ] No legacy `/uploads/` warning (after image migration).
- [ ] Health check green.

### Admin account safety

- `AdminUserInitializer` only seeds while `admin_users` is empty. Restarts never
  overwrite the stored password.
- The first password comes from `ADMIN_PASSWORD`; use a strong unique value.
  After the first login, reset it through the Gmail flow and keep the new
  password only in a password manager.
- To change the reset email after it has been stored (backfill is one-time):

  ```sql
  UPDATE admin_users
  SET email = 'new-admin-inbox@example.com', updated_at = now()
  WHERE username = 'nurseryadmin';
  ```

- Never use a default/demo password in production.

### Scaling limitation

Sign-in sessions, session revocation and rate limits are in memory. Run
**one** Render instance. If you need horizontal scaling, first externalize
sessions and rate limiting (for example Redis); this is not implemented.

---

## 5. Netlify frontend

1. Push the frontend source (see pre-flight caveat).
2. Netlify → Add new site → Import from Git.
3. `netlify.toml` at the repository root configures:
   - base `frontend`
   - build `npm ci && npm run build`
   - publish `dist`
   - Node 22
   - SPA fallback (`/* → /index.html 200`)
   - `/api/*` and `/uploads/*` proxy to Render
   - basic security headers
4. The proxy destination in `netlify.toml` is pre-set to
   `https://bhuvana-nursery-api1.onrender.com`, the confirmed Render service
   URL. If the Render service is renamed, update both redirects by
   hand — Netlify does not substitute environment variables in redirect targets.
   Netlify reads configuration from both the base directory and the repository
   root, and a root-level `base` setting overrides the UI value, so the `/api`
   proxy applies either way. Leaving **Base directory** empty is still the
   simplest setup.
5. Leave `VITE_API_BASE_URL` unset (or `/api/v1`). The proxy keeps the browser
   on one origin, so the session and CSRF cookies stay first-party.
6. Deploy. Verify:
   - `/` loads;
   - refreshing `/plants/curry-leaf` does not 404;
   - admin login works;
   - `/api/v1/plants` returns JSON through the proxy.

### If the frontend is its own repository

Use the same `[build]` section without `base = "frontend"`:

```toml
[build]
  command = "npm ci && npm run build"
  publish = "dist"
```

Keep the same redirects and headers.

### Why the proxy instead of a cross-site API URL

A direct `https://site.netlify.app → https://api.onrender.com` setup makes the
session cookie third-party. Modern browsers block or partition third-party
cookies, which breaks login intermittently. The same-origin proxy avoids that.
If you still choose split origins, you must set on Render:

```text
CORS_ALLOWED_ORIGINS=https://your-site.netlify.app
COOKIE_SAME_SITE=None
SECURE_COOKIES=true
```

and use an exact origin (never `*`).

---

## 6. Hostinger domain + HTTPS

1. In Netlify: **Domain management → Add a domain** and enter the domain
   purchased at Hostinger.
2. Netlify shows the exact DNS records or nameservers to use. **Use exactly
   those values** — do not invent IPs or records.
3. At Hostinger, either:
   - change the domain's nameservers to the ones Netlify provides (simplest,
     Netlify DNS), or
   - keep Hostinger DNS and add the records Netlify displays (typically an
     apex/ALIAS or A record and a `www` CNAME; use the values Netlify shows).
4. Add both the apex and `www` in Netlify, choose one as the primary domain, and
   let Netlify redirect the other to it.
5. Wait for DNS verification. Netlify provisions a Let's Encrypt certificate
   automatically; enable **Force HTTPS**.
6. Set `APP_BASE_URL` on Render to the canonical HTTPS URL (for example
   `https://bhuvanursery.com`) and restart. The reset email link must use this
   domain.
7. Re-test login, image loading and the reset email from the custom domain.

If you use a custom R2 image domain (for example `images.bhuvanursery.com`),
add that record at Hostinger too, using the target Cloudflare provides.

---

## 7. Post-deploy verification

Run through `PRODUCTION_TEST_CHECKLIST.md`. Minimum go-live gates:

- [ ] Public homepage, catalogue, category filter, plant detail, WhatsApp/phone
      and map links work on the custom domain.
- [ ] Refreshing a nested route returns the app, not a 404.
- [ ] Admin login/logout works; unauthenticated admin API calls return 401.
- [ ] Create/edit/publish/archive/delete a plant; upload an image; confirm it
      persists in R2 and appears on the public site after redeploy.
- [ ] Category and hero-slide management work.
- [ ] Forgot password sends one email; link uses the custom domain; token works
      once; old password fails; new password works.
- [ ] `GET /actuator/health` returns UP; logs contain no secrets.
- [ ] No `/uploads/` legacy warning in Render logs.

---

## 8. Local development

Backend:

```bash
cd backend
cp .env.example .env      # fill in local DB/SMTP; never commit
./mvnw spring-boot:run    # Windows: mvnw.cmd spring-boot:run
```

Frontend:

```bash
cd frontend
npm ci
npm run dev               # http://localhost:5173
```

The Vite dev server proxies `/api` and `/uploads` to `http://localhost:8080`
(`frontend/vite.config.ts`). Local defaults use `IMAGE_STORAGE=local`, HTTP
cookies and `APP_BASE_URL=http://localhost:5173`.

Useful checks:

```bash
cd backend
./mvnw test

cd ../frontend
npm run lint
npx tsc -b
npm run build
```

---

## 9. Operations

- **Logs:** Render → Logs. Look for Flyway, admin seeding, storage, and
  password-reset lines. The app never logs raw tokens or credentials.
- **Health:** point an uptime monitor at `/actuator/health`; it includes the
  database. `/api/v1/health` is a cheap liveness check.
- **Restarts:** Render restarts on deploy/crash. Sessions are lost on restart
  (in-memory), which is expected.
- **Secrets:** rotate the Gmail App Password and R2 keys if exposed. Update
  Render env vars; no code change needed.
- **Dependencies:** update Spring Boot/Vite deliberately and run the full test
  suite. Do not reintroduce snapshot repositories.
- **Backups:** Neon PITR/branches + periodic `pg_dump`; keep R2 versioning or a
  copy of the bucket if available.
- **Costs/limits:** Render free instances sleep; Neon free tier suspends;
  Gmail has daily sending limits; R2 has storage/operation costs. Monitor them.

---

## 10. Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| Login succeeds but `/auth/me` is 401 | Cookies blocked/third-party, wrong SameSite/Secure, or CORS | Use the Netlify same-origin proxy; `SECURE_COOKIES=true`, `COOKIE_SAME_SITE=Lax`; clear old cookies |
| POST returns 403 | CSRF token missing/stale | Reload the page; the SPA fetches `/auth/csrf`; ensure the proxy forwards `X-XSRF-TOKEN` and cookies |
| Forgot password returns 503 | SMTP not configured | Set `SMTP_HOST` and `MAIL_FROM`; restart |
| 202 but no email | Admin email missing/mismatch or SMTP failure | Check Render logs; see §3 |
| Reset link points to localhost | `APP_BASE_URL` not set | Set the HTTPS frontend URL; restart |
| Images 404 after deploy | Local storage on Render, or legacy `/uploads/` rows | `IMAGE_STORAGE=r2`; migrate existing files/URLs (§2) |
| Refresh on `/admin/...` 404 | SPA redirect missing | Use `netlify.toml` SPA fallback; redeploy |
| CORS error | Split origins without exact CORS config | Prefer proxy; otherwise set `CORS_ALLOWED_ORIGINS` + `COOKIE_SAME_SITE=None` |
| Flyway fails on startup | Wrong Neon URL/SSL, pooled endpoint, or unreachable DB | Use direct host + `sslmode=require`; check Neon status |
| Render build fails | Snapshot repo/Java mismatch | Use the provided Dockerfile (Java 21, Maven Central only) |
| Render sleeps and first request is slow | Free instance | Expected; upgrade or keep warm |

---

## 11. Security notes and known limitations

Implemented:

- BCrypt password hashing; admin APIs require `ROLE_ADMIN`.
- Session-based auth with session-fixation protection and logout revocation.
- CSRF double-submit cookie; Secure/HttpOnly/SameSite configuration.
- Opt-in, exact-origin CORS; no wildcard with credentials.
- Login and reset rate limiting; generic anti-enumeration responses.
- Single-use, hashed, expiring reset tokens; sessions revoked after reset.
- Upload validation by actual image decoding, size/dimension limits, UUID names,
  JPEG/PNG only; R2 credentials server-side.
- Health endpoint without sensitive details; no stack traces/messages in errors.
- No secrets in tracked frontend code; `.env` ignored; example sanitized.

Known limitations / manual follow-ups:

- Single backend instance only (in-memory sessions/rate limits).
- No category delete endpoint/UI (categories are published/unpublished; plants
  reference them). Report if you want guarded deletion added.
- No contact form; contact is WhatsApp/phone/map links (existing design).
- No 2FA for admin login.
- Gmail App Password is a long-lived credential; rotate on exposure.
- Legacy local images must be migrated to R2 manually (§2).
- Netlify proxy target and domain DNS are account-specific manual steps.
- Live Gmail/R2/Neon/Render/Netlify/domain verification was **not** possible in
  this environment; see `PRODUCTION_READINESS_REPORT.md` for exact status.
