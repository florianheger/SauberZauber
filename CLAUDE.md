# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

The core skeleton exists; features from `requirements/` are not implemented yet. Currently there is only a smoke-test endpoint `GET /api/hello` shown on the Angular start page.

- `Backend/` — Spring Boot (Maven, Java 25), package `de.sauberzauber.backend`
- `Frontend/` — Angular (standalone, vitest), served by nginx in Docker; nginx proxies `/api/` to the backend
- `docker-compose.yml` — `db` (Postgres 18), `backend`, `frontend` (only port exposed: `8080`)

Commands:
- Run everything: `cp .env.example .env && docker compose up --build` → http://localhost:8080
- Backend tests (no local JDK needed): `docker run --rm -v "$PWD/Backend":/app -w /app eclipse-temurin:25-jdk ./mvnw -B test`
- Frontend: `cd Frontend && npm start` (proxies `/api` to `localhost:8080`), `npm test`, `npm run build`

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
- **Backend**: Spring Boot 4 / Spring Security 7.x, PostgreSQL (persistent volume), Flyway migrations.
- **Frontend**: Angular, mobile-first, responsive for phone and desktop, installable as a PWA on Android and iOS. The UI is entirely in German (all user-facing text, labels, and messages).

## Authentication (security-critical)

The app is internet-facing, so authentication must use Spring Security's built-in mechanisms. The **only** permitted custom security code is the one-time enrollment link; WebAuthn ceremonies, sessions, CSRF, and logout must all come from Spring Security.

- Passkey (WebAuthn) login only, via `spring-security-webauthn` (`http.webAuthn(...)` with `rpId`/`allowedOrigins` from env). Endpoints: `POST /webauthn/authenticate/options`, `POST /login/webauthn`. Users may have multiple passkeys.
- Persist credentials with `JdbcPublicKeyCredentialUserEntityRepository` + `JdbcUserCredentialRepository`; their tables are created via Flyway.
- Session-based auth; CSRF via `CookieCsrfTokenRepository.withHttpOnlyFalse()`, Angular sends `X-XSRF-TOKEN`.
- Form login, HTTP basic, one-time-token login, and any password mechanism must be disabled.
- No registration UI. Users come from env `APP_USERS` (e.g. `florian,ronja`) and are created idempotently on startup.
- **Enrollment**: on startup, each user without a credential gets a 32-byte `SecureRandom` base64url token; only its SHA-256 hash + 24h expiry is stored in `enrollment_token` (old tokens invalidated), and the link `${APP_ORIGIN}/enroll?token=…` is logged. `GET /enroll` does a constant-time hash compare + expiry check, deletes the token, creates an authenticated session (save context, change session ID), and redirects to Spring's `/webauthn/register`. Invalid/expired/reused → 403. `APP_REENROLL=<user>` forces a new link on next startup.
- Angular: single "Sign in with passkey" login page, route guard on `GET /api/me` (401 → login), logout via `POST /logout`.

Required tests: token generation/validation/expiry, enroll endpoint (valid, expired, reused), user bootstrap idempotency, rejection of password/basic login.

## Configuration

No secrets in the repo; all config comes from env vars (`APP_USERS`, `APP_RP_ID`, `APP_ORIGIN`, `APP_REENROLL`, DB credentials). Keep things minimal — no admin UI.

## Git

- Keep commit messages short (one line, in the style of the existing history, e.g. `add task-done feature`).
- Never mention Claude in commits or PRs: no `Co-Authored-By` trailer, no "Generated with Claude Code" line, no other attribution.
