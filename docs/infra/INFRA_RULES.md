# Infrastructure Rules

## 0. Working Rules
- MUST: One PR must contain one infra change unit.
- MUST: Keep behavior unchanged; only improve security, reliability, observability, or cost control.
- MUST: Every change must include rollback steps (`git revert` or config toggle rollback).
- MUST: PR must include local reproducible validation commands.
- SHOULD: Separate env settings by environment using `.env` and secrets.

## 1. Docker and Image Rules
- MUST: Runtime containers run as non-root (`USER`).
- MUST: Healthcheck command tools (`curl` or equivalent) must exist in the image.
- MUST: Maintain `.dockerignore` to reduce build context.
- SHOULD: Keep multi-stage build and small runtime image.
- SHOULD: Evaluate read-only root filesystem and tmpfs where possible.

## 2. Docker Compose Rules
- MUST: No sensitive defaults in compose (DB password, keys, tokens).
- MUST: Keep a tracked `.env.example` and load real values from untracked `.env`.
- MUST: Each service defines restart policy.
- MUST: Stateful data path (Postgres) must be persisted.
- MUST: Define logging policy (rotation or centralized collection).
- SHOULD: Do not expose DB/Redis host ports by default.
- SHOULD: Provide CPU/memory guidance and override examples.

Compose required vars (current baseline):
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

## 3. Postgres Rules
- MUST: Credentials come from secrets or `.env`, not hardcoded defaults.
- MUST: Migration ownership and execution path are documented.
- MUST: Backup and restore runbook is documented.
- SHOULD: Hikari pool and timeout values are explicit per environment.

## 4. Redis Rules
- MUST: If Redis stores sessions, persistence policy must be explicitly documented.
- MUST: Do not expose Redis without auth controls in non-local environments.
- SHOULD: Set maxmemory and eviction policy intentionally.
- SHOULD: Evaluate managed Redis or replication/Sentinel for production.

## 5. Gateway Rules
- MUST: Standardize `X-Request-Id` and `X-Forwarded-*` propagation.
- MUST: Define `proxy_connect_timeout`, `proxy_read_timeout`, and `send_timeout`.
- MUST: Define upstream failure behavior (`proxy_next_upstream` or explicit fail-fast).
- SHOULD: Document blue/green switch and rollback path.
- SHOULD: Include requestId, upstream status, and latency in access logs.

## 6. Spring WAS Rules
- MUST: Keep readiness/liveness and define graceful shutdown (`server.shutdown`, `spring.lifecycle.*`).
- MUST: DB/Redis timeout settings must be explicit.
- MUST: No default secrets in app configuration.
- SHOULD: Define retry/backoff strategy for external dependency failures.
- SHOULD: Keep requestId consistent across response and logs.
