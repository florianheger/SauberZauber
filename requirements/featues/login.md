# Feature: Login
Users must be able to log in securely using a passkey. Login via password or OTP is not available.

There is no registration in the frontend. The users are created automatically on the first `docker compose up` (see Enrollment).

The app will be published to the internet, so it is essential to use Spring Security instead of self-developed security functionality. The only custom security code allowed is the minimal one-time enrollment link described below; everything else (WebAuthn ceremonies, sessions, CSRF, logout) must come from Spring Security.

## Stack
- Backend: Spring Boot 4 / Spring Security 7.x, `org.springframework.security:spring-security-webauthn` (version via Boot BOM)
- Frontend: Angular, native `navigator.credentials` (optionally `@simplewebauthn/browser`)
- DB: PostgreSQL via docker compose (persistent volume), Flyway migrations
- Deployment: docker compose, served behind HTTPS on the real domain

## Requirements
- Only the users from env `APP_USERS` (e.g. `florian,ronja`) exist; they are created idempotently on startup.
- Login only via passkey (`POST /webauthn/authenticate/options`, `POST /login/webauthn`).
- Users can hold multiple passkeys.

## Spring Security config
- `http.webAuthn(w -> w.rpName(...).rpId(${APP_RP_ID}).allowedOrigins(${APP_ORIGIN}))`
- Use `JdbcPublicKeyCredentialUserEntityRepository` + `JdbcUserCredentialRepository` (create the required tables via Flyway).
- Session-based auth; CSRF with `CookieCsrfTokenRepository.withHttpOnlyFalse()`; Angular sends `X-XSRF-TOKEN`.
- Disable form login, HTTP basic, one-time-token login, and any password-based mechanism.
- Angular and API served from the same origin (reverse proxy), so no CORS.

## Enrollment (first passkey per user)
Passkeys are created on the user's device, so each user enrolls once via a one-time link. This link is only for registering a passkey and is not a login method.

1. On startup, for each user in `APP_USERS` with **no** registered credential:
   - generate a token (32 bytes `SecureRandom`, base64url), store **only the SHA-256 hash** + expiry (24h) in `enrollment_token`
   - log: `Enrollment for <user>: ${APP_ORIGIN}/enroll?token=<token>`
   - regenerate on each startup while the user has no credential (invalidate old tokens)
2. `GET /enroll?token=...`:
   - validate hash (constant-time compare) + expiry, then delete the token (single use)
   - create an authenticated session for that user (`HttpSessionSecurityContextRepository.saveContext`, change session ID)
   - redirect to `/webauthn/register` (Spring's default registration page)
3. Invalid/expired/reused token → 403.
4. Recovery: env `APP_REENROLL=<user>` forces a new enrollment link on the next startup, even if credentials exist.

## Angular
- Login page with a single "Sign in with passkey" button (also support conditional UI/autofill if simple).
- Route guard based on `GET /api/me` (401 → login page).
- Logout via `POST /logout`.

## Constraints
- No secrets in the repo; all config via env (`APP_USERS`, `APP_RP_ID`, `APP_ORIGIN`, DB creds).
- Tests: token generation/validation/expiry, enroll endpoint (valid, expired, reused), user bootstrap idempotency, password/basic login rejected.
- Keep it minimal; no admin UI.