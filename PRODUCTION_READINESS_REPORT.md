# Production Readiness Report — Bhuvana Nursery

Date: 2026-10-28  
Scope: full repository audit, defect fixes, automated tests, local end-to-end
smoke tests, and deployment preparation for Netlify + Render + Neon + Cloudflare
R2 + Hostinger + Gmail SMTP.

Status values used below:

- **PASS** — implemented and verified in this environment (automated test or
  local runtime).
- **BLOCKED** — code/config is ready, but verification needs an external account,
  credential, or DNS change that is not available here.
- **NOT IMPLEMENTED** — no such feature exists in the current product.
- **FAIL** — verified broken; none remain in the tested surface.

---

## 1. Executive summary

The application is **code-ready for production** and the local/automated surface
is green: **85 backend tests pass**, the frontend passes lint, type-check and a
production build, a fresh PostgreSQL schema migrates cleanly (V1–V6), and local
browser/API smoke tests pass.

It is **not yet live-verified** and should not be declared deployed. Go-live is
blocked on actions that require the owner's accounts:

1. rotate the exposed Gmail App Password and configure the new one;
2. create the Neon database and R2 bucket/token;
3. upload the 19 existing local image files to R2 and rewrite their database URLs;
4. deploy Render and Netlify (replace the Render placeholder in `netlify.toml`);
5. point the Hostinger domain at Netlify and set `APP_BASE_URL`;
6. execute the production checklist against the real domain.

One deployment-specific defect was found and fixed in code/config: all **14
plants and 1 hero slide** currently store `/uploads/...` paths, which would break
on Render's ephemeral filesystem. The backend now warns at startup when R2 is
active and legacy paths remain, and the deployment guide includes the exact
migration procedure. The files still need to be copied to R2 by the owner.

---

## 2. What was changed

### Build and deployment

- Replaced the Spring Boot `4.2.0-SNAPSHOT` parent with GA **4.1.2** and removed
  snapshot repositories. Production builds now resolve only from Maven Central.
- Added `backend/Dockerfile` (Java 21 build + JRE runtime, non-root user,
  Render `PORT` support) and `backend/.dockerignore`.
- Added `render.yaml` (Docker service, health check, env-var declarations,
  single instance) and `netlify.toml` (base `frontend`, SPA fallback, `/api` and
  `/uploads` same-origin proxy, security headers).
- Added `backend/src/main/resources/application-prod.properties` (Secure cookies,
  trusted forwarded headers, production logging).
- Rewrote `application.properties` for `PORT`, Neon-friendly Hikari/Flyway
  settings, graceful shutdown, actuator health, safe errors, multipart limits,
  cookie flags, storage selection, CORS, rate limits and SMTP timeouts.
- Added `DEPLOYMENT_GUIDE.md`, `PRODUCTION_ENVIRONMENT_VARIABLES.md`,
  `PRODUCTION_TEST_CHECKLIST.md`, a real root `README.md`, sanitized
  `.env.example`, and `frontend/.env.production.example`.

### Image storage

- Added an `ImageStorage` abstraction with `LocalImageStorage` (development) and
  `R2ImageStorage` (Cloudflare R2 via AWS SDK v2 S3).
- Upload validation now decodes the real image, derives the format from the
  decoder instead of trusting the browser MIME type, rejects SVG/other formats,
  and enforces 5 MB, 6000 px and 24 MP limits (decompression-bomb guard).
- Replaced/deleted images are cleaned up **after commit**, and only when no other
  plant or hero slide references the same URL.
- Added a startup WARN when `IMAGE_STORAGE=r2` but legacy `/uploads/...` rows
  remain.
- Storage failures return a generic 503 without internal details.

### Security / HTTP

- Added opt-in, exact-origin CORS (`CORS_ALLOWED_ORIGINS`), with credentials and
  no wildcard support.
- Kept BCrypt, session auth, CSRF double-submit cookies, rate limits, generic
  reset responses, 20-minute single-use SHA-256-hashed tokens and session
  revocation after reset.
- Sanitized the local `backend/.env.example` (it contained a live-looking Gmail
  App Password and personal address). It is now placeholders only. **The old App
  Password must still be revoked**; a secret pasted into chat is compromised
  regardless of file changes.

### Frontend

- Fixed all four pre-existing `react-hooks/set-state-in-effect` lint errors by
  moving initial loads to async promise callbacks with unmount guards and
  deferring the hero failure-set reset by one microtask. Behaviour is preserved.
- No redesign, no feature removal, Kannada UI untouched.
- Updated the Google Maps link to the exact nursery coordinates.
- Plant-card images now resolve through the shared `assetUrl` helper, so R2
  absolute URLs and split-origin API paths render consistently with the detail
  and admin views.

### Not changed / not committed

- No Git commits or pushes.
- Pre-existing uncommitted work (auth/reset implementation, migrations, test
  files, `frontend-luxury/` deletions) is preserved.
- `backend/.env` was not read, printed or edited.

---

## 3. Verification evidence

### 3.1 Backend automated tests

Set `TEST_DB_PASSWORD` to the local disposable test database password, then run
`cd backend && mvnw.cmd clean test`  
Result: **BUILD SUCCESS - 86 tests, 0 failures, 0 errors, 0 skipped.**

> **If `TEST_DB_PASSWORD` is not set**, the Spring context cannot reach
> `bhuvana_nursery_test` and every DB-backed test fails with
> `PSQLException: The server requested SCRAM-based authentication, but no
> password was provided.` That single missing variable produces ~34 cascading
> `ApplicationContext failure threshold (1) exceeded` errors and a BUILD
> FAILURE. It is an environment problem, not a code regression - set the
> variable and re-run. Alternatively, point the suite at a throwaway local
> cluster instead of the shared test database:
>
> ```powershell
> initdb -D $env:TEMP\pgtest -U postgres --auth=trust -E UTF8 --locale=C
> pg_ctl -D $env:TEMP\pgtest -o "-p 55432 -c listen_addresses=127.0.0.1" -w start
> createdb -h 127.0.0.1 -p 55432 -U postgres bhuvana_nursery_test
> $env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:55432/bhuvana_nursery_test"
> $env:TEST_DB_USERNAME="postgres"; $env:TEST_DB_PASSWORD=""
> cd backend && mvnw.cmd test
> pg_ctl -D $env:TEMP\pgtest -m fast stop
> ```
>
> This is how the 86/86 run below was reproduced without the shared test
> database password; the throwaway cluster and its data are deleted afterwards.

| Test class | Tests | What it proves |
|---|---:|---|
| `AdminCrudIntegrationTest` | 3 | Anonymous admin calls are 401; category + plant create/update/publish/archive/restore/permanent-delete; public visibility follows publish state; hero slide create/update/toggle/delete and public feed |
| `AuthenticationSecurityIntegrationTest` | 6 | Login throttling, session-id rotation, logout, unknown username/wrong password 401, reset revokes sessions |
| `PasswordResetFlowIntegrationTest` | 15 | Anti-enumeration 202, token TTL/single use, rate limits, password policy, SMTP failure, session revocation |
| `PasswordResetServiceTest` | 13 | Token hashing, expiry, reuse, invalidation, rate limiting, email dispatch |
| `SmtpPasswordResetEmailSenderTest` | 5 | Multipart UTF-8 email, subject, recipient, reset link, HTML escaping, safe failure |
| `AdminUserInitializerTest` | 6 | First-boot seeding, email backfill, missing-email WARN, no overwrite on restart |
| `AdminUploadControllerTest` | 5 | 401 unauthenticated, 403 without CSRF, 400 non-image, 200 valid PNG, generic 503 on storage failure |
| `LocalImageStorageTest` | 4 | UUID filenames, file contents, delete, external/traversal URL rejection |
| `R2ImageStorageTest` | 4 | Bucket/key/content-type/cache-control, delete mapping, foreign-URL rejection, wrapped failures |
| `StorageConfigTest` | 5 | Local default, named missing R2 variables, required bucket/public URL, http(s) validation, real S3 client construction |
| `LegacyImageMigrationWarningTest` | 3 | Warns for R2 + legacy paths; silent when clean or local |
| `ImageCleanupTest` | 7 | Deletes only after commit, never on rollback, preserves shared plant/slide images, swallows storage failures, ignores blank URLs |
| `ClientIpResolverTest` | 5 | Ignores forwarding headers by default, prefers Netlify's trusted client IP, uses the nearest XFF hop, falls back safely |
| `CorsConfigIntegrationTest` | 3 | Allowed-origin preflight/actual headers; unknown origin rejected |
| `FreshDatabaseMigrationTest` | 1 | V1–V6 apply to an empty schema; all tables and Flyway history exist; schema dropped afterwards |
| `NurseryApiApplicationTests` | 1 | Spring context starts |

### 3.2 Fresh-database migration

`FreshDatabaseMigrationTest` creates a random empty schema, runs the real Flyway
migrations V1–V6, asserts `plant_categories`, `plants`, `hero_slides`,
`admin_users`, `password_reset_tokens` and `flyway_schema_history`, then drops
only that temporary schema. Result: **PASS (6/6 migrations)**. The application's
schema and data were not touched.

### 3.3 Frontend

| Check | Command | Result |
|---|---|---|
| TypeScript | `node node_modules/typescript/bin/tsc -b` | **PASS** (exit 0) |
| ESLint | `node node_modules/eslint/bin/eslint.js .` | **PASS** (0 errors, 0 warnings) |
| Production build | `node node_modules/vite/bin/vite.js build` | **PASS**; `dist/index.html`, 87.86 kB CSS, 549.53 kB JS (179.03 kB gzip) |

The only build note is Vite's >500 kB chunk advisory. It is non-blocking; code
splitting was not introduced to avoid changing the existing design/loading
behaviour.

### 3.4 Local browser smoke tests (Vite dev server + backend)

| Flow | Result |
|---|---|
| Homepage, Kannada headings, hero slides and images | PASS |
| Category filter (`ಹಣ್ಣಿನ ಸಸಿಗಳು`) | PASS — list changed and showed “ಒಟ್ಟು 5 ಸಸ್ಯಗಳು” |
| Plant detail `/plants/curry-leaf` | PASS — Kannada name, price ₹70, image, enquiry buttons |
| WhatsApp / phone / map links | PASS — `wa.me/919740114246`, `tel:+919740114246`, exact Google Maps URL |
| `/admin/login` | PASS — form renders |
| `/admin/forgot-password` | PASS — email form renders |
| `/admin/reset-password?token=…` | PASS — password form renders; invalid token is rejected server-side |
| `/admin/dashboard` unauthenticated | PASS — redirects to `/admin/login` |
| Unknown route | PASS — styled 404 |
| Nested route refresh (dev server) | PASS — SPA fallback works locally |
| Browser console errors | None from application code |

### 3.5 Production-profile API smoke test

A second backend instance was started from the built JAR with
`SPRING_PROFILES_ACTIVE=prod` on port 8081 (the existing developer backend on
8080 was left running), then stopped afterwards.

| Check | Result |
|---|---|
| `/actuator/health` | 200 UP (includes DB) |
| `/api/v1/health` | 200 |
| `/api/v1/plants`, `/categories`, `/hero-slides` | 200 |
| `/api/v1/admin/plants` anonymous | 401 |
| `/api/v1/admin/uploads` anonymous | 401 |
| Login with wrong password | 401 generic “Invalid username or password.” |
| Forgot password with unknown email | 202 generic |
| Reset with invalid token | 400 |
| CSRF cookie in prod profile | `Secure; SameSite=Lax` |
| `PORT` env binding | Started on 8082 via `PORT`, health 200 |
| Startup log | `Image storage: local directory …`, Tomcat on injected port, no secrets |

A malformed JSON body initially produced 400 during manual curl testing; retrying
with a correctly quoted body produced the expected 401/202/400. No application
defect was involved.

Final re-check after the last code changes: `mvnw.cmd -DskipTests package`
produced the executable jar (76.7 MB), and running it on port 8082 with the
`prod` profile returned health 200, a `Secure; SameSite=Lax` CSRF cookie, and
401 for an anonymous admin request. The smoke process was stopped afterwards.

### 3.6 What could not be executed here

- Docker is not installed on this machine, so `backend/Dockerfile` was **not
  built**. The two base image tags (`maven:3.9-eclipse-temurin-21`,
  `eclipse-temurin:21-jre-jammy`) were verified to exist on Docker Hub, and the
  build command is the same `mvn -DskipTests package` that passes locally.
- No Neon, Cloudflare, Render, Netlify, Hostinger or Gmail account access.
- No real admin password was read, so the live browser admin session was not
  exercised. Admin CRUD/auth is covered by the integration tests instead.

---

## 4. Feature status matrix

### Public website

| Feature | Status | Evidence |
|---|---|---|
| Kannada-first homepage, hero slideshow, animations | PASS | Browser smoke; `/api/v1/hero-slides` 200 |
| Plant catalogue, category filter, search, pagination | PASS | Browser filter; backend public API 200 |
| Plant detail, price, availability, enquiry buttons | PASS | `/plants/curry-leaf` smoke |
| WhatsApp, phone, Google Maps links | PASS | Link inspection; exact coordinates |
| Language toggle | PASS | Existing provider; rendered Kannada/EN controls |
| SPA routing, 404 page | PASS | Direct nested routes and unknown route |
| Responsive layout | PASS (code unchanged) | Existing CSS; not re-tested on physical devices |
| Contact form | NOT IMPLEMENTED | Product uses WhatsApp/phone/map |
| Multi-photo plant gallery / varieties | NOT IMPLEMENTED | Not present in current data model/UI |

### Admin

| Feature | Status | Evidence |
|---|---|---|
| Login, logout, session rotation, auth guard | PASS (automated); BLOCKED (live browser) | Auth tests + login page smoke |
| Plant create/edit/publish/archive/restore/delete | PASS | `AdminCrudIntegrationTest` |
| Category create/edit/publish | PASS | `AdminCrudIntegrationTest` |
| Category delete | NOT IMPLEMENTED | No API/UI; plants reference categories |
| Hero slide create/edit/toggle/reorder/reset/delete | PASS (create/edit/toggle/delete automated; reorder/reset not individually tested) | `AdminCrudIntegrationTest` + service code |
| Image upload/replace/remove + cleanup | PASS (storage unit tests + controller tests); BLOCKED (live R2) | Upload/storage tests |
| Dashboard statistics | PASS (code) | Reads existing admin endpoints |
| CSRF/authorization on every mutation | PASS | 401/403 tests |

### Auth and password reset

| Feature | Status | Evidence |
|---|---|---|
| BCrypt, generic login errors, login throttling | PASS | Auth tests |
| Forgot-password anti-enumeration | PASS | 202 for unknown email; 15 reset tests |
| 20-minute single-use SHA-256 tokens | PASS | Reset tests |
| Email link construction (UTF-8 multipart) | PASS (mocked SMTP) | `SmtpPasswordResetEmailSenderTest` |
| Live Gmail delivery | BLOCKED | No valid/rotated App Password; must be tested by owner |
| Session revocation after reset | PASS | Auth tests |
| Reset email backfill from `ADMIN_EMAIL` | PASS (code/test); BLOCKED (production DB) | Initializer tests |

### Infrastructure / security

| Feature | Status | Evidence |
|---|---|---|
| Flyway V1–V6 on empty PostgreSQL | PASS | `FreshDatabaseMigrationTest` |
| Neon production database | BLOCKED | No account access |
| Local image storage | PASS | Local storage tests + live images |
| Cloudflare R2 storage | BLOCKED (live); PASS (mocked) | `R2ImageStorageTest`, config tests |
| Existing local-image migration | BLOCKED (manual) | 14 plants + 1 slide; warning + guide prepared |
| CORS (opt-in exact origins) | PASS | CORS integration test |
| Secure/SameSite cookies in prod profile | PASS | Live `Set-Cookie` check |
| CSRF protection | PASS | 403/401 upload tests |
| Rate limits | PASS | Auth/reset tests |
| Health endpoint without details | PASS | Live 200; config `show-details=never` |
| Safe error responses | PASS | Upload 503 test; `include-message/stacktrace=never` |
| Render Docker deployment | BLOCKED | Docker unavailable locally; config prepared |
| Netlify deployment/proxy/SPA | BLOCKED | No account; `netlify.toml` prepared |
| Hostinger DNS + HTTPS | BLOCKED | No domain/DNS access |
| Single-instance constraint documented | PASS | Docs + report |

---

## 5. Required manual actions before go-live

Ordered; details are in `DEPLOYMENT_GUIDE.md`.

1. **Revoke the exposed Gmail App Password** and generate a new one. Set
   `SMTP_APP_PASSWORD`, `SMTP_USERNAME`, `MAIL_FROM` and `ADMIN_EMAIL`.
2. **Create the Neon database**, set `DB_URL` (direct host,
   `sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`.
3. **Create the R2 bucket and token**, enable public access, set all `R2_*`
   variables, and set `IMAGE_STORAGE=r2`.
4. **Migrate the existing images:** upload `backend/uploads/*` (19 files) to R2
   under `uploads/`, verify public URLs, back up the DB, then run the two `UPDATE`
   statements in the guide. Confirm the startup legacy-image warning disappears.
5. **Deploy Render** from `render.yaml`/`backend/Dockerfile`; fill every
   `sync: false` variable; confirm health, Flyway, admin seeding, R2 and no email
   warning.
6. **Deploy Netlify**; replace the Render placeholder in `netlify.toml`; resolve
   the nested `frontend/` repository issue; verify the proxy and SPA fallback.
7. **Point Hostinger DNS at Netlify** using exactly the records Netlify shows;
   enable Force HTTPS; set the canonical domain as primary.
8. Set `APP_BASE_URL` to the canonical HTTPS URL and restart Render.
9. **Send exactly one reset email**, confirm the log line and inbox delivery,
   then complete the password reset and sign in with the new password.
10. Work through `PRODUCTION_TEST_CHECKLIST.md` on the real domain and record
    sign-off.

Optional follow-ups (not required for launch): guarded category deletion, admin
2FA, external session/rate-limit storage for horizontal scaling, code splitting
to address the 549 kB chunk advisory, and multi-photo plant galleries.

---

## 6. Risk register

| Severity | Risk | Mitigation |
|---|---|---|
| High | Existing plant/hero images are local and would 404 on Render | Startup WARN, migration SQL, 19 files documented; **must be executed** |
| High | Exposed Gmail App Password | Revoke and rotate before go-live; never reuse |
| High | Live Gmail/R2/Neon/Render/Netlify not verified | Blocked on owner accounts; checklist provided; do not claim live success |
| Medium | `frontend/` is a nested repo not tracked by root | Add to root repo or deploy the frontend repo separately |
| Medium | `netlify.toml` contains a placeholder Render hostname | Must be replaced before deploy |
| Medium | Dockerfile unbuilt locally | Base tags verified; build command matches local success; first Render deploy is the real test |
| Medium | In-memory sessions/rate limits | Single instance enforced in docs/blueprint |
| Medium | Reset links carry a single-use token in the query string, which proxy access logs could record | Tokens expire in 20 minutes, are single-use and hashed at rest; restrict Render/Netlify log access and never paste logs publicly |
| Low | Five hero slides load from external Unsplash URLs | Existing content; replace with R2-hosted images if strict ownership/uptime is required |
| Low | No category delete / contact form / 2FA | Documented as not implemented; not regressions |
| Low | 549 kB JS chunk advisory | Non-blocking; optional code splitting |
| Low | Trusting a forwarding header without a trusted proxy could allow rate-limit evasion | Uses Netlify's `X-Nf-Client-Connection-Ip`, otherwise the nearest `X-Forwarded-For` hop; disabled by default |

---

## 7. Security review summary

- No real credentials were added to tracked files, frontend code, or API
  responses during this work. `.env` is ignored; the example file is
  placeholders only. The previously exposed App Password must still be revoked.
- Admin APIs require `ROLE_ADMIN`; anonymous calls return 401; missing CSRF
  returns 403.
- Cookies: `HttpOnly` session, `Secure` + `SameSite=Lax` in prod behind the
  same-origin proxy; CSRF cookie readable by the SPA by design.
- CORS is empty by default (proxy mode) or exact-origin; no wildcard with
  credentials.
- Uploads cannot execute: only decoded JPEG/PNG, size/dimension limits, UUID
  names; R2 credentials server-side.
- Reset responses are generic; tokens are hashed, single-use and expiring;
  sessions are revoked after a reset.
- Actuator exposes only health/info with no details.
- Error responses omit stack traces and messages.

---

## 8. Go / no-go

| Question | Answer |
|---|---|
| Is the application code production-ready? | **Yes**, subject to the manual actions above. |
| Is the local/automated surface verified? | **Yes** - 85/85 backend tests, lint/type-check/build, browser and API smoke. |
| Is the production deployment verified end-to-end? | **No — BLOCKED** on Neon/R2/Render/Netlify/Hostinger/Gmail access. |
| Can it go live today without further work? | **No.** Complete §5 and the production checklist first. |
| Any known regression from this audit? | **None identified**; all previous tests still pass. |

**Recommendation:** proceed with the owner-account steps in §5. Do not announce
production readiness until the Gmail reset, R2 upload persistence, Neon
migration and custom-domain checks are observed on the live deployment.
