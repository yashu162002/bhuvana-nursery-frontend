# ಭೂವನ ನರ್ಸರಿ · Bhuvana Nursery

Full-stack website for Bhuvana Nursery (Chitradurga, Karnataka): a Kannada-first
public catalogue with plant details, WhatsApp/phone contact, and a password-protected
admin area for plants, categories, hero slides and image uploads.

| Part | Stack |
|---|---|
| Frontend | React 19, TypeScript, Vite 8, Tailwind CSS 4, GSAP |
| Backend | Spring Boot 4.1.2, Java 21, Spring Security, JPA, Flyway |
| Database | PostgreSQL (Neon in production) |
| Images | Local filesystem in development; Cloudflare R2 in production |
| Email | Gmail SMTP (Google App Password) for admin password resets |
| Hosting | Netlify (frontend) + Render (Docker backend) |

## Repository layout

| Path | Contents |
|---|---|
| `backend/` | Spring Boot API, Flyway migrations, Dockerfile |
| `frontend/` | React + Vite single-page app (public site and admin area) |
| `netlify.toml`, `render.yaml` | Deployment configuration |

Both `backend/` and `frontend/` are ordinary directories tracked by this
repository. The frontend's standalone Git history (previously a nested
repository) was merged into this one, so `frontend/` no longer needs its own
remote to be deployed.

## Local development

Backend (from `backend/`):

```bash
cp .env.example .env      # fill in local DB/SMTP; never commit
./mvnw spring-boot:run    # Windows: mvnw.cmd spring-boot:run
```

Frontend (from `frontend/`):

```bash
npm ci
npm run dev               # http://localhost:5173
```

The Vite dev server proxies `/api` and `/uploads` to `http://localhost:8080`.
The app runs Flyway migrations on startup and seeds the first administrator from
`ADMIN_USERNAME` / `ADMIN_PASSWORD` / `ADMIN_EMAIL` only while `admin_users` is
empty.

## Tests

```bash
cd backend
./mvnw test               # auth, reset, CRUD, storage, CORS, migrations

cd ../frontend
npm run lint
npx tsc -b
npm run build
```

The backend test profile expects `TEST_DB_USERNAME` / `TEST_DB_PASSWORD` for a
disposable database; see `PRODUCTION_READINESS_REPORT.md` if the context fails
to start.

## Frontend tooling

The app is built with Vite's React + TypeScript template:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react)
  (Oxc) is enabled in `vite.config.ts`.
- The React Compiler is not enabled by default; see the
  [React Compiler guide](https://react.dev/learn/react-compiler/installation)
  before turning it on.
- `eslint.config.js` uses the recommended flat config. Type-aware rules can be
  enabled later with `tseslint.configs.recommendedTypeChecked`, and
  `eslint-plugin-react-x` / `eslint-plugin-react-dom` add React-specific checks.

## Deployment

Read these in order:

1. [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) — Neon → R2 → Gmail → Render →
   Netlify → Hostinger domain, with the existing-image migration and
   troubleshooting.
2. [PRODUCTION_ENVIRONMENT_VARIABLES.md](PRODUCTION_ENVIRONMENT_VARIABLES.md) —
   every variable, which are secrets, and sanitized examples.
3. [PRODUCTION_TEST_CHECKLIST.md](PRODUCTION_TEST_CHECKLIST.md) — go-live and
   post-deploy checks.
4. [PRODUCTION_READINESS_REPORT.md](PRODUCTION_READINESS_REPORT.md) — what was
   verified, what is blocked on external accounts, and known limitations.

Key files: `netlify.toml`, `render.yaml`, `backend/Dockerfile`,
`backend/.env.example`, `frontend/.env.production.example`.

## Security notes

- Never commit `.env`, real credentials, or secrets in `VITE_*` variables (Vite
  inlines them into the public bundle).
- The admin area uses BCrypt, server sessions, CSRF protection, rate limiting and
  single-use SHA-256-hashed reset tokens.
- Production must use HTTPS, `SECURE_COOKIES=true`, and Cloudflare R2
  (`IMAGE_STORAGE=r2`); Render's filesystem is ephemeral.
- Run a single backend instance: sessions, reset throttling and the
  session-revocation registry are in memory.
- If a credential is ever exposed, rotate it. A Gmail App Password can be revoked
  at <https://myaccount.google.com/apppasswords>.
