# SauberZauber

A small, self-hosted chore tracker for a two-person household.

SauberZauber records **who did which household task, and when**, so you can see at a glance how the housework is actually split and share it fairly. It is deliberately *not* a scheduler: tasks have no due dates and never "expire". They are simply things that keep coming back (laundry, taking out the trash, cleaning the bathroom …), and every time someone does one, it gets logged.

The app is built for exactly **one household with two users**. The UI is in German and works as an installable PWA on Android, iOS and desktop.

## Features

- **Tasks** – create, edit and delete recurring tasks with a title, an optional description and a Material Symbols icon (the icon search understands German terms).
- **Mark as done** – log a task as done by yourself *or* by the other person (e.g. "Sam took out the trash").
- **Statistics** – a separate page showing, for each task, how often each person did it.
- **Passkey login** – no passwords at all; sign-in uses WebAuthn passkeys only.
- **PWA** – installable on the home screen, mobile-first and responsive.

## Tech stack

| Part      | Technology                                                                   |
|-----------|------------------------------------------------------------------------------|
| Backend   | Java 25, Spring Boot 4, Spring Security 7 (WebAuthn), Flyway                 |
| Database  | PostgreSQL 18                                                                |
| Frontend  | Angular (standalone components), service worker, served by nginx            |
| Runtime   | Docker Compose: `db`, `backend`, `frontend`                                  |

nginx in the `frontend` container serves the Angular app and proxies `/api/`, the WebAuthn endpoints, `/enroll` and `/logout` to the backend, so everything runs on one origin. Only the frontend port (`APP_PORT`, default `8080`) is exposed.

## Quick start (local)

Requirements: Docker with Docker Compose.

```sh
git clone <repo-url> SauberZauber
cd SauberZauber
cp .env.example .env        # adjust if you like, defaults work for localhost
docker compose up --build
```

Then open <http://localhost:8080>.

### First login: enroll a passkey

There is no registration page. The users are defined in `APP_USERS` and created automatically on startup. For each user who has no passkey yet, the backend logs a **one-time enrollment link** (valid for 24 hours):

```sh
docker compose logs backend | grep Enrollment
```

Open the link for your user in the browser, register a passkey, and from then on sign in on the login page with "Mit Passkey anmelden". Hand the other link to the second person.

Once logged in, a user can register additional passkeys (e.g. for a second device) via Spring Security's page at `/webauthn/register`.

### Lost passkey

Set `APP_REENROLL` to the user name and restart the backend; a new enrollment link is logged on startup:

```sh
APP_REENROLL=alex docker compose up -d backend
docker compose logs backend | grep Enrollment
```

Remove the value again afterwards.

## Configuration

All configuration comes from environment variables in `.env` (see `.env.example`). No secrets belong in the repository.

| Variable            | Description                                                                                  | Example                          |
|---------------------|----------------------------------------------------------------------------------------------|----------------------------------|
| `POSTGRES_DB`       | Database name                                                                                | `sauberzauber`                   |
| `POSTGRES_USER`     | Database user                                                                                | `sauberzauber`                   |
| `POSTGRES_PASSWORD` | Database password – **change it**                                                            | `…`                              |
| `APP_USERS`         | Comma-separated user names (max. two)                                                        | `alex,sam`                       |
| `APP_RP_ID`         | WebAuthn relying party ID: the domain the app runs on                                        | `sauberzauber.example.de`        |
| `APP_ORIGIN`        | Exact origin the app is opened from (scheme + host + port)                                   | `https://sauberzauber.example.de`|
| `APP_PORT`          | Host port the app is published on                                                            | `8080`                           |
| `APP_REENROLL`      | Optional: user name that gets a fresh enrollment link on next startup                        | `alex`                           |

> Passkeys are bound to the domain. A passkey registered on `localhost` will not work on your production domain, so set `APP_RP_ID` and `APP_ORIGIN` correctly before enrolling.

## Production deployment

SauberZauber is meant to run behind an existing reverse proxy that terminates TLS (passkeys require HTTPS on anything other than `localhost`).

1. Point a domain to your server, e.g. `sauberzauber.example.de`.
2. Create `.env` from `.env.example` and set at least:
   ```sh
   POSTGRES_PASSWORD=<a strong password>
   APP_USERS=alex,sam
   APP_RP_ID=sauberzauber.example.de
   APP_ORIGIN=https://sauberzauber.example.de
   APP_PORT=8080
   ```
3. Start the stack: `docker compose up -d --build`
4. Configure your reverse proxy to forward the domain to `http://127.0.0.1:${APP_PORT}`. It must pass the `Host` (or `X-Forwarded-Host`), `X-Forwarded-Proto: https` and `X-Forwarded-For` headers, so the backend sets secure cookies and builds correct redirects. Example for nginx:
   ```nginx
   location / {
       proxy_pass http://127.0.0.1:8080;
       proxy_set_header Host $host;
       proxy_set_header X-Forwarded-Host $host;
       proxy_set_header X-Forwarded-Proto https;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
   }
   ```
5. Grab the enrollment links (`docker compose logs backend | grep Enrollment`) and register your passkeys.

Data lives in the Docker volume `db-data`; back it up (e.g. with `pg_dump`) if you care about your history.

## Development

### Frontend

```sh
cd Frontend
npm install
npm start        # dev server, proxies /api etc. to localhost:8080
npm test         # unit tests (vitest)
npm run build    # production build
```

For `npm start`, run the Docker stack (`docker compose up`) alongside it: the dev server on <http://localhost:4200> forwards the API and auth requests to the stack on `localhost:8080`. The backend only accepts passkey ceremonies from `APP_ORIGIN`, so sign in on <http://localhost:8080> first; cookies on `localhost` are shared across ports, so the dev server then uses that session.

The Material Symbols icon font is a self-hosted subset. After adding icons to `Frontend/src/app/tasks/icon-catalog.ts`, regenerate it with `npm run icons`.

### Backend

The backend tests need no local JDK; they run in a container and use Testcontainers for PostgreSQL:

```sh
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -v "$PWD/Backend":/app -v sauberzauber-m2:/root/.m2 -w /app \
  eclipse-temurin:25-jdk ./mvnw -B test
```

With a local JDK 25 you can also run `./mvnw test` inside `Backend/`.

When adding backend paths the browser calls directly, also add them to the proxy rules in `Frontend/nginx.conf` and `Frontend/proxy.conf.json`, and to the service worker exclusions in `Frontend/ngsw-config.json`.

## Project structure

```
Backend/          Spring Boot application (package de.sauberzauber.backend)
Frontend/         Angular app, nginx config, PWA assets
requirements/     Product description, feature specs (featues/) and UI design handoff (deisgn/)
docker-compose.yml
.env.example
```
