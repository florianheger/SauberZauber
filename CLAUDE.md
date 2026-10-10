# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

All features from `requirements/` are implemented: login (`Backend/.../auth`, `Frontend/src/app/auth`), tasks and completions (`Backend/.../task`, `Frontend/src/app/tasks`), statistics (`Frontend/src/app/stats`), PWA (service worker, manifest, icons).

- **UI design source of truth:** `requirements/deisgn/` (Claude Design handoff; note the spelling). `README.md` has the tokens, sizes and behaviour; `SauberZauber App.dc.html` is the clickable prototype. Recreate changes from there pixel-accurately; tokens live in `Frontend/src/styles.scss` (`--p1`/`--p2` = first/second household member, `--me`/`--other` follow `data-me` on the shell).

- `Backend/` — Spring Boot (Maven, Java 25), package `de.sauberzauber.backend`
- `Frontend/` — Angular (standalone, vitest), served by nginx in Docker; nginx proxies `/api/` to the backend
- `docker-compose.yml` — `db` (Postgres 18), `backend`, `frontend` (only port exposed: `APP_PORT`, default `8080`)

Commands:
- Run everything: `cp .env.example .env && docker compose up --build` → http://localhost:8080
- Enrollment links (first passkey per user): `docker compose logs backend | grep Enrollment`
- Backend tests (no local JDK needed; integration tests use Testcontainers via the Docker socket): `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -v "$PWD/Backend":/app -v sauberzauber-m2:/root/.m2 -w /app eclipse-temurin:25-jdk ./mvnw -B test`
- New backend paths that the browser calls directly must also be added to the proxy rules in `Frontend/nginx.conf` and `Frontend/proxy.conf.json`
- Frontend: `cd Frontend && npm start` (proxies `/api` to `localhost:8080`), `npm test`, `npm run build`
- Icons: the Material Symbols font is a self-hosted subset (`Frontend/public/fonts/`). After adding icons to `Frontend/src/app/tasks/icon-catalog.ts` (`ICON_CATALOG` or `UI_ICONS`), run `cd Frontend && npm run icons`
- Service worker: backend navigations (`/enroll`, `/webauthn/**`, `/login/webauthn`, `/logout`, `/api/**`) are excluded in `Frontend/ngsw-config.json` `navigationUrls`; keep that list in sync with the nginx proxy rules

Requirements:
- `requirements/description.md` — overall purpose and tech stack
- `requirements/featues/*.md` — one file per feature (note: the directory name is spelled `featues`)

Treat these documents as the source of truth; read the relevant feature file before implementing or changing a feature.

## What the app does

A household chore tracker for exactly **one household with two users**. It records who did which task when and how often, so housework can be shared fairly. It is explicitly **not** a scheduler: tasks have no due dates and are always repeating. Do not add multi-household, multi-tenant, or >2-user functionality.

Features:
- **Tasks**: create/edit/delete; title, optional description, Material Symbols icon (icon search must work with German terms).
- **Task done**: a user can mark a task done for themself *or* for the other user.
- **Statistics**: separate page showing, per task, how many times each user did it.
- **Login**: passkey-only (see below).

## Architecture

- **Deployment**: everything runs via `docker compose`, served over HTTPS on a real public domain. Angular and the API share one origin behind a reverse proxy, so no CORS configuration.
  - TLS is terminated by the server's existing reverse proxy, which forwards to `APP_PORT`. It must send `Host` (or `X-Forwarded-Host`), `X-Forwarded-Proto: https` and `X-Forwarded-For`; nginx passes them on and Spring then sets `Secure` cookies and correct redirects.
  - In production `APP_RP_ID` is the domain and `APP_ORIGIN` the exact `https://` origin; passkeys registered for one origin (e.g. localhost) do not work on another.
- **Backend**: Spring Boot 4 / Spring Security 7.x, PostgreSQL (persistent volume), Flyway migrations.
- **Frontend**: Angular, mobile-first, responsive for phone and desktop, installable as a PWA on Android and iOS. The UI is entirely in German (all user-facing text, labels, and messages).

## Authentication (security-critical)

The app is internet-facing, so authentication must use Spring Security's built-in mechanisms. The **only** permitted custom security code is the one-time enrollment link; WebAuthn ceremonies, sessions, CSRF, and logout must all come from Spring Security.

- Passkey (WebAuthn) login only, via `spring-security-webauthn` (`http.webAuthn(...)` with `rpId`/`allowedOrigins` from env). Endpoints: `POST /webauthn/authenticate/options`, `POST /login/webauthn`. Users may have multiple passkeys.
- Persist credentials with `JdbcPublicKeyCredentialUserEntityRepository` + `JdbcUserCredentialRepository`; their tables are created via Flyway.
- Session-based auth; CSRF via `CookieCsrfTokenRepository.withHttpOnlyFalse()` with a plain `CsrfTokenRequestAttributeHandler`, Angular sends `X-XSRF-TOKEN`. Do **not** use `csrf.spa()`: Spring's default `/webauthn/register` page sends a masked token in that header, which `spa()` rejects with 403 (covered by `SecurityConfigTest`).
- Form login, HTTP basic, one-time-token login, and any password mechanism must be disabled.
- No registration UI. Users come from env `APP_USERS` (e.g. `alex,sam`) and are created idempotently on startup.
- **Enrollment**: on startup, each user without a credential gets a 32-byte `SecureRandom` base64url token; only its SHA-256 hash + 24h expiry is stored in `enrollment_token` (old tokens invalidated), and the link `${APP_ORIGIN}/enroll?token=…` is logged. `GET /enroll` does a constant-time hash compare + expiry check, deletes the token, creates an authenticated session (save context, change session ID), and redirects to Spring's `/webauthn/register`. Invalid/expired/reused → 403. `APP_REENROLL=<user>` forces a new link on next startup.
- Angular: single "Sign in with passkey" login page, route guard on `GET /api/me` (401 → login), logout via `POST /logout`.

Required tests: token generation/validation/expiry, enroll endpoint (valid, expired, reused), user bootstrap idempotency, rejection of password/basic login.

## Configuration

No secrets in the repo; all config comes from env vars (`APP_USERS`, `APP_RP_ID`, `APP_ORIGIN`, `APP_REENROLL`, DB credentials). Keep things minimal — no admin UI.

## Git

- Keep commit messages short (one line, in the style of the existing history, e.g. `add task-done feature`).
- Never mention Claude in commits or PRs: no `Co-Authored-By` trailer, no "Generated with Claude Code" line, no other attribution.
