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

Services own separate PostgreSQL databases and never read each other's tables.
Cross-service facts are propagated through versioned Kafka events. Kafka is not
used between controller, service and repository layers inside one application.

## Requirements

- JDK 21 (the project also compiles on the installed JDK 25)
- Docker Desktop
- Node.js 22+ for the web client

## Local infrastructure

```powershell
docker-compose -f infra/compose.yaml up -d
docker-compose -f infra/compose.yaml ps
```

| Component | Local address |
|---|---|
| Identity PostgreSQL | `localhost:15432/owoke_identity` |
| Dating PostgreSQL | `localhost:15433/owoke_dating` |
| Notification PostgreSQL | `localhost:15434/owoke_notification` |
| Places PostgreSQL | `localhost:15435/owoke_places` |
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

Configuration keys are documented in `.env.example`. Spring Boot does not load
that file automatically; configure variables in the IDE or shell. Never commit
real bot tokens, OIDC secrets, signing keys or provider API keys.

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
