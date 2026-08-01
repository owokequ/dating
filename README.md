# Owoke

Owoke is a production-oriented dating planner built as a Java 21 / Spring Boot 4.1
microservice system with a React web client. It is a regular website, not a
Telegram Mini App. Telegram is used for optional OIDC login and notifications.

## Services

| Module | Port | Responsibility |
|---|---:|---|
| `services/api-gateway` | 8080 | Public API routing and edge security |
| `services/identity-service` | 8081 | Accounts, credentials and tokens |
| `services/dating-service` | 8082 | Couples, invitations and dates |
| `services/notification-service` | 8083 | In-app, Telegram and email delivery |
| `services/places-service` | 8084 | Kazan place catalog and providers |
| `services/media-service` | 8085 | Uploaded and provider image metadata |
| `services/events-service` | 8086 | KudaGo events, occurrences and moderation |

Services own separate PostgreSQL databases and never read each other's tables.
Cross-service facts are propagated through versioned Kafka events. Kafka is not
used between controller, service and repository layers inside one application.

## Requirements

- JDK 21 (the project also compiles on the installed JDK 25)
- Docker Desktop
- Node.js 22+ for the web client

## Local infrastructure

The recommended entry point is the Windows development launcher:

```powershell
copy .env.local.example .env.local
.\dev.cmd up
```

`up` starts PostgreSQL, Redis, Kafka and Mailpit in Docker, then starts the Java
services and Vite as separately managed local processes. It waits for every
readiness endpoint before reporting success. The terminal remains the
supervisor: `Ctrl+C` stops only its Java/Node process trees and leaves the
infrastructure warm for a faster next start.

```powershell
.\dev.cmd status
.\dev.cmd logs
.\dev.cmd logs -Follow
.\dev.cmd down
```

`down` stops both launcher processes and Owoke containers but deliberately
preserves containers and named volumes. It never uses `down -v`.
On `up`, Compose may remove obsolete orphan containers left by renamed services;
their named volumes are still preserved.

For a fully containerized local run:

```powershell
.\dev.cmd docker
.\dev.cmd logs -Follow
.\dev.cmd down
```

This mode merges `infra/compose.yaml` with `infra/compose.full.yaml`, builds all
applications and exposes the website at `http://localhost:5173`. Hybrid mode is
preferred while coding because IDE debugging and incremental Java/Vite rebuilds
are faster.

The underlying infrastructure-only commands remain available:

```powershell
docker compose -f infra/compose.yaml up -d --wait
docker compose -f infra/compose.yaml ps
```

| Component | Local address |
|---|---|
| Identity PostgreSQL | `localhost:15432/owoke_identity` |
| Dating PostgreSQL | `localhost:15433/owoke_dating` |
| Notification PostgreSQL | `localhost:15434/owoke_notification` |
| Places PostgreSQL | `localhost:15435/owoke_places` |
| Media PostgreSQL | `localhost:15436/owoke_media` |
| Events PostgreSQL | `localhost:15437/owoke_events` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |
| Mailpit SMTP / UI | `localhost:1025` / `http://localhost:8025` |

The previous development volumes are intentionally not removed by the new
Compose project. Never run `down -v` unless local data can be discarded.

## Build

```powershell
.\mvnw.cmd clean verify
```

Run an individual service with `-pl`, for example:

```powershell
.\mvnw.cmd -pl services/identity-service spring-boot:run
```

Run the regular React website in another terminal:

```powershell
cd web-app
npm ci
npm run dev
```

Vite serves `http://localhost:5173` and proxies `/api` to the API Gateway on
port `8080`. Production serves the built `web-app/dist` and `/api` under one
HTTPS origin. Browser JavaScript never receives access or refresh tokens;
requests use HttpOnly cookies plus the readable `XSRF-TOKEN` cookie.

Frontend verification:

```powershell
cd web-app
npm run lint
npm test
npm run build
```

Configuration keys are documented in `.env.local.example`. The launcher loads
`.env.local` into its own process and all children inherit those variables.
Existing shell variables take precedence. Spring Boot itself does not load env
files automatically. Never commit real bot tokens, OIDC secrets, signing keys
or provider API keys.

### KudaGo places and events

KudaGo does not require an API key. Local configuration enables a manual import
of 30 Kazan places and an event catalog covering the next 90 days. The event
scheduler refreshes the catalog every six hours; both imports can also be
started from the corresponding admin pages:

```text
/admin/places
/admin/events
```

Imported place cards stay in `DRAFT` until an administrator publishes them.
Events with a valid venue become public automatically; incomplete events remain
drafts. Public cards retain a direct source link required by the KudaGo license.
2GIS remains available as an optional disabled provider.

### Gmail SMTP for external testing

Mailpit remains the safe local default. To deliver real messages through a
personal Gmail account, enable Google 2-Step Verification, create a dedicated
App Password named `Owoke local`, and replace only the mail block in the ignored
`.env.local` file:

```dotenv
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-complete-address@gmail.com
MAIL_PASSWORD=your-16-character-app-password-without-spaces
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_CONNECTION_TIMEOUT=5000
MAIL_SMTP_READ_TIMEOUT=5000
MAIL_SMTP_WRITE_TIMEOUT=5000
NOTIFICATION_FROM_EMAIL=your-complete-address@gmail.com
MAIL_HEALTH_ENABLED=true
```

Restart Owoke after changing the file. The App Password is an application
secret: do not paste it into source code, commit it, or reuse the normal Google
Account password. For public production traffic, use a transactional email
provider or Google Workspace SMTP relay instead of a personal Gmail mailbox.

## Package convention

Business code is grouped by feature and then by responsibility:

```text
feature/
  controller/
  dto/
  service/
  repository/
  domain/
  mapper/
  exception/
shared/
  configuration/
  security/
  messaging/
  exception/
```

REST DTOs, Kafka event payloads and JPA entities are separate types. There is no
shared Java domain module between services; event contracts live under
`contracts/events` as JSON Schema documents.

## Production-like deployment

1. Copy `.env.production.example` to a secret file outside the repository and
   replace every placeholder. Generate a dedicated RSA key pair for JWT signing.
2. Copy `infra/redis/users.acl.example` outside the repository, replace its
   password and point `REDIS_ACL_FILE` to the absolute file path.
3. Point `OWOKE_DOMAIN` DNS records to the VPS and start the stack:

```bash
docker compose --env-file /opt/owoke/secrets/production.env \
  -f infra/compose.prod.yaml up -d --build
```

Caddy obtains and renews TLS certificates automatically. Only ports 80/443 are
published; databases, Kafka, Redis, Prometheus, Grafana and Java services remain
inside the Docker network. Access Grafana for administration through an SSH
tunnel rather than exposing it publicly.

After deployment, register the Telegram webhook at the ordinary HTTPS endpoint
`https://<domain>/api/v1/telegram/webhook` and send the configured
`TELEGRAM_WEBHOOK_SECRET` as Telegram's `secret_token`. The bot buttons link to
the website; no Telegram Mini App API is used.

The bundled single-node Kafka listener is private to the one-host Docker network.
When Kafka moves off-host or to a managed cluster, set the clients and broker to
SASL/TLS before opening network access. One VPS, one Kafka broker and local
PostgreSQL/Redis are production-like, not highly available; schedule encrypted
volume snapshots and periodic restore drills before accepting real user data.
The concrete PostgreSQL backup and isolated restore procedure is documented in
`infra/backup/README.md`.
