# Production Test Checklist

Use this before every production deploy and after every infrastructure change.
Do not mark an external integration as passed unless you actually exercised it.

Legend: `[x]` verified · `[ ]` not yet verified · `N/A` not applicable.

---

## 0. Automated checks (local, before deploy)

- [ ] `cd backend && ./mvnw test` - 85 tests, 0 failures
      (Windows: `mvnw.cmd test`).
- [ ] `cd frontend && npm run lint` — no errors.
- [ ] `cd frontend && npx tsc -b` — no type errors.
- [ ] `cd frontend && npm run build` — `dist/index.html` produced.
- [ ] `git grep`/secret scan: no real credentials in tracked files.
- [ ] `backend/.env` is untracked and ignored.
- [ ] `netlify.toml` has the real Render hostname (not the placeholder).
- [ ] `render.yaml` values are present in Render (no `sync: false` left blank).

Current audit results: backend **85/85**, frontend lint/type-check/build **pass**,
fresh Flyway schema **6/6**. See `PRODUCTION_READINESS_REPORT.md`.

---

## 1. Public website (custom domain)

- [ ] Homepage loads over HTTPS with no console errors.
- [ ] Kannada/English toggle works and the design is unchanged.
- [ ] Hero slideshow renders and advances; images are not broken.
- [ ] Catalogue shows all 14 plants (or the current count).
- [ ] Category filter changes the list and the “total plants” count.
- [ ] Search filters by English/Kannada/scientific name.
- [ ] Pagination works (next/previous, page indicator).
- [ ] Plant detail page opens from a card.
- [ ] Direct refresh of `/plants/curry-leaf` (or another slug) does not 404.
- [ ] Unknown route shows the styled 404 page.
- [ ] WhatsApp link opens `wa.me` with the correct prefilled message and the
      deployed plant URL (not localhost).
- [ ] Telephone link uses `tel:`.
- [ ] “Directions” and “Open in Google Maps” use the configured map link.
- [ ] Footer links and copyright render.
- [ ] Layout works on mobile width and desktop.
- [ ] Loading, empty and error states behave (stop the API and reload to see the
      error state, then restore).

---

## 2. Admin authentication

- [ ] `/admin/login` renders.
- [ ] Correct credentials sign in and redirect to `/admin/dashboard`.
- [ ] Wrong credentials return a generic “Invalid username or password.”
- [ ] Repeated wrong attempts return 429 (rate limit).
- [ ] Unauthenticated `/admin/dashboard` redirects to login.
- [ ] Unauthenticated `GET /api/v1/admin/plants` returns 401.
- [ ] Unauthenticated `POST /api/v1/admin/uploads` returns 401.
- [ ] Session survives a page refresh but not a backend restart.
- [ ] Logout returns to login and `/auth/me` then returns 401.
- [ ] `Set-Cookie` for `JSESSIONID` is `HttpOnly; Secure; SameSite=Lax` (or
      `None` only for split origins).
- [ ] `XSRF-TOKEN` is present and POSTs send `X-XSRF-TOKEN`.
- [ ] POST without a CSRF token returns 403.
- [ ] No admin credentials appear in browser storage, HTML or JS bundles.

---

## 3. Admin dashboard and CRUD

- [ ] Dashboard statistics match the database (plants, published/draft,
      categories, hero slides).
- [ ] Create a plant with all fields; it appears in the admin list.
- [ ] Upload a JPEG/PNG; preview shows; save persists.
- [ ] Reject a non-image and an image > 5 MB with a clear message.
- [ ] Reject an SVG/renamed executable.
- [ ] Replace a plant image; the old object is removed from R2 only after save.
- [ ] Publish/unpublish hides/shows the plant publicly.
- [ ] Archive hides the plant; restore brings it back.
- [ ] Permanent delete removes the plant and its unused image.
- [ ] Create/edit/publish a category.
- [ ] Unpublishing a category hides its plants publicly.
- [ ] Create/edit/toggle/delete a hero slide.
- [ ] Reorder hero slides and verify the public order.
- [ ] “Reset to defaults” works (test on a non-production/staging database).
- [ ] Validation errors are clear and do not leak stack traces.
- [ ] All admin mutations require an authenticated admin session.

---

## 4. Password reset (Gmail)

- [ ] Request a reset with the configured admin email.
- [ ] API returns a generic 202 regardless of whether the email exists.
- [ ] Render logs show `Password reset email sent for administrator '…'`.
- [ ] Email arrives in Inbox/Spam/All Mail.
- [ ] Link uses the custom HTTPS domain, never localhost.
- [ ] Link opens `/admin/reset-password?token=…` and the form renders.
- [ ] Expired token is rejected.
- [ ] Invalid/random token is rejected.
- [ ] Reusing a used token is rejected.
- [ ] Successful reset signs out existing sessions.
- [ ] Old password fails; new password signs in.
- [ ] A second request invalidates the first unused link.
- [ ] Per-email/per-IP limits return 429 after the configured budget.
- [ ] SMTP failure is logged safely and the API response stays generic.
- [ ] No token, password or link appears in logs.

---

## 5. Images and R2

- [ ] `IMAGE_STORAGE=r2` on Render.
- [ ] New uploads return an absolute `https://…` URL.
- [ ] The object exists in the bucket under `uploads/<uuid>.<ext>`.
- [ ] The URL loads publicly in a private/incognito window.
- [ ] The plant/hero image renders on the custom domain.
- [ ] Replacing an image leaves no orphaned old object (or it is cleaned up).
- [ ] Deleting a plant/slide does not delete an image still used elsewhere.
- [ ] Upload failure shows a generic 503 and does not crash the app.
- [ ] `backend/uploads/` legacy paths were migrated; startup has no
      “still reference local /uploads/” warning.
- [ ] R2 secret/access keys never appear in frontend JS or API responses.

---

## 6. Database (Neon)

- [ ] `DB_URL` uses the Neon host and `sslmode=require`.
- [ ] Flyway logs show V1–V6 applied (or “up to date”).
- [ ] `flyway_schema_history` exists; all six rows are `success`.
- [ ] Admin user exists once; password is not reset on redeploy.
- [ ] `admin_users.email` is populated (not NULL).
- [ ] Plants/categories/hero slides persist across a Render restart.
- [ ] Creating/updating data is visible after a redeploy.
- [ ] No manual table creation was needed.
- [ ] A backup/branch exists before any schema change.
- [ ] Restore was tested on a separate branch (not production).

---

## 7. Security

- [ ] `/actuator/health` returns status only; no details, env or beans.
- [ ] Other actuator endpoints are not exposed.
- [ ] Admin APIs return 401/403, never data, without a session.
- [ ] CORS is empty (proxy mode) or an exact origin; never `*`.
- [ ] `TRUST_FORWARDED_FOR` is only true behind Render/Netlify.
- [ ] No default/demo admin password.
- [ ] No secrets in Git, logs, API responses or `VITE_*` variables.
- [ ] Exposed Gmail App Password revoked and replaced.
- [ ] Security headers present (`X-Frame-Options`, `nosniff`, HSTS).
- [ ] Error responses do not include stack traces or SQL.
- [ ] Uploaded files cannot execute (JPEG/PNG only, decoded).
- [ ] Session/CSRF cookies are Secure and appropriately SameSite.

---

## 8. Deployment configuration

- [ ] Netlify build uses Node 22 and `npm ci && npm run build`.
- [ ] `publish` points at `dist`.
- [ ] SPA fallback is active.
- [ ] `/api/*` proxy points at the real Render service.
- [ ] `/uploads/*` proxy points at the real Render service (legacy paths).
- [ ] Render Docker build succeeds; Java 21 runtime.
- [ ] `PORT` is injected by Render; no hardcoded port.
- [ ] Health check path `/actuator/health` is green.
- [ ] One Render instance (in-memory sessions/rate limits).
- [ ] Render env vars match `PRODUCTION_ENVIRONMENT_VARIABLES.md`.
- [ ] `APP_BASE_URL` matches the canonical domain.
- [ ] DNS records are exactly those provided by Netlify.
- [ ] Apex and `www` both resolve; one redirects to the primary.
- [ ] Force HTTPS is on; certificate is valid.

---

## 9. Rollback drill

- [ ] Know the previous Render deploy and how to redeploy it.
- [ ] Know the Neon branch/PITR restore procedure.
- [ ] Know how to revert a bad Netlify deploy.
- [ ] R2 objects are versioned or backed up if needed.
- [ ] `APP_BASE_URL`, CORS and DNS changes are documented.

---

## 10. Sign-off

| Area | Verified by | Date | Notes |
|---|---|---|---|
| Automated tests | | | |
| Public site | | | |
| Admin CRUD | | | |
| Password reset / Gmail | | | |
| R2 uploads | | | |
| Neon migrations | | | |
| Domain / HTTPS | | | |
