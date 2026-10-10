# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

SauberZauber is currently **specification-only**: the repo contains no application code yet, only requirement documents under `requirements/`. There are no build, lint, or test commands yet — update this file with them once the backend/frontend projects are scaffolded.

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

## Planned architecture

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
