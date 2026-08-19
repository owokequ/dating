# Owoke: portfolio notes

This document is a short, evidence-based guide for presenting Owoke in a resume
or technical interview. Keep claims specific to the codebase; do not invent user
or performance metrics that have not been measured in a deployed environment.

## One-line description

Built a Java 21 / Spring Boot microservice dating planner with a React client,
secure cookie-based authentication, Kafka-based integration and a production-like
Docker deployment.

## Resume bullets

Choose two or three bullets that match the role; keep the links when the resume
format permits them.

- Designed a seven-service Spring Boot system with database-per-service ownership
  and versioned Kafka contracts; used transactional outbox and consumer inbox
  patterns to make asynchronous communication retriable and idempotent.
- Implemented an edge-security model with RS256 JWT/JWKS validation, HttpOnly
  cookies, CSRF protection, CORS allow-listing and Redis-backed authentication
  rate limiting; browser JavaScript does not handle bearer tokens.
- Protected date-proposal mutations from network retries through persisted
  idempotency keys and request fingerprints, then covered critical flows with
  Testcontainers integration tests.
- Automated repeatable checks in GitHub Actions: Maven verification, Liquibase
  migrations, architecture tests, frontend lint/typecheck/tests/build, event
  schema validation, Compose rendering and Docker image builds.
- Packaged a production-like stack with Caddy-managed TLS, per-service health
  probes, Prometheus/Grafana and an external Cloudflare Worker that performs
  debounced availability monitoring and Telegram recovery notifications.

## Technical story to tell

### Problem and boundary

Owoke coordinates a couple's plan from discovery to a mutually agreed date. The
interesting part is not rendering cards: a proposal can be submitted more than
once, delivery can fail after the database commit, or one downstream service can
be temporarily unavailable. The system treats those as normal conditions.

### Consistency model

The service writes its business change and an outbox record in the same database
transaction. A dispatcher publishes the record to Kafka later. Consumers first
record the event ID in their inbox, so a redelivery does not repeat the side
effect. Retryable Kafka listeners send exhausted failures to a durable failed
message store. This is **at-least-once delivery with idempotent consumers**, not
an incorrect promise of exactly-once delivery.

Useful code paths:

- [DateProposalService](../services/dating-service/src/main/java/com/dating/owoke/dating/dateproposal/service/DateProposalService.java)
- [IdempotencyService](../services/dating-service/src/main/java/com/dating/owoke/dating/shared/idempotency/service/IdempotencyService.java)
- [OutboxDispatcher](../services/dating-service/src/main/java/com/dating/owoke/dating/shared/messaging/service/OutboxDispatcher.java)
- [DateProposalCommandListener](../services/dating-service/src/main/java/com/dating/owoke/dating/dateproposal/messaging/listener/DateProposalCommandListener.java)

### Security model

The browser stores neither access nor refresh tokens in JavaScript. The Gateway
extracts the token from an HttpOnly cookie, validates JWT issuer/audience against
Identity's JWKS endpoint and forwards authenticated requests. Stateful mutations
also need the readable `XSRF-TOKEN` cookie echoed as `X-XSRF-TOKEN`; this protects
the cookie-authenticated browser flow against cross-site request forgery.

This model has a trade-off: cookies require deliberate CSRF and CORS design,
which the Gateway owns centrally. It reduces the blast radius of an XSS bug
compared with exposing bearer tokens to browser storage.

### Operational model

Local development supports both hybrid processes and a full Compose environment.
Production confines application services and data stores to the Docker network;
only Caddy exposes ports 80/443. Spring Actuator feeds Prometheus, Grafana is
preconfigured, and an external Cloudflare Worker checks availability so that an
outage of the main host can still be detected.

## Questions an interviewer may ask

| Question | Honest short answer |
|---|---|
| Why microservices for a portfolio project? | To practice data ownership and failure boundaries. I would start a commercial product as a modular monolith unless independent deployment/scaling or team boundaries justified splitting it. |
| Why not exactly-once Kafka? | End-to-end exactly-once is costly and does not cover external side effects. I chose at-least-once delivery plus inbox deduplication and idempotent handlers. |
| Why cookies if the API is stateless? | JWT verification stays stateless on the server, while HttpOnly cookies keep tokens out of JavaScript. CSRF protection is necessary because browsers attach cookies automatically. |
| How would you scale it? | Move Kafka and PostgreSQL to managed, multi-node offerings; keep secrets in a manager; introduce replicas and load balancing based on measured bottlenecks. The included one-host Compose deployment is production-like, not highly available. |
| What would you measure next? | Request latency/error rate, consumer lag, outbox age, retry/DLT volume, database saturation and user journey completion. No unmeasured SLA or throughput claim should be made. |

## Before sharing the repository

- Add a real, redacted product screenshot or 30–60 second walkthrough video near
  the top of the README. Do not use placeholder UI images.
- Keep the CI badge green and ensure the repository visibility matches the links
  in the resume.
- Decide on a license with the repository owner before adding one; a license is a
  legal choice, not a cosmetic README change.
- If a public demo is enabled, use disposable demo accounts and production
  secrets outside the repository. Never commit `.env.local` or real bot/OIDC/JWT
  credentials.
