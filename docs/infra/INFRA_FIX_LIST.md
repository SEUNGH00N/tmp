# Infrastructure Structural Fix List

## Operating Rules
- This document tracks only structural infra issues.
- Each proposed task must be deliverable in one PR.
- Apply the same constraints as `docs/refactoring/TASK_LIST.md` (one task per PR, no feature change, rollbackable).
- If an item is already covered by an existing refactoring task, reference it instead of duplicating.

## Index (Summary)
- High: 4
- Medium: 6
- Low: 2

Issue counts by area:
- `docker-compose`: 7
- `dockerfile`: 3
- `gateway (nginx)`: 3
- `postgres`: 3
- `redis`: 3
- `admin-was`: 2
- `user-was`: 3
- `deploy/k8s`: 1

## Issues

### IF-001: Default DB credentials can leak to runtime
- Severity: High
- Category: INF-SEC
- Affected: docker-compose | postgres | was
- Evidence:
  - file: `docker-compose.yml` (`POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-excel}`)
  - file: `admin-was/src/main/resources/application.yml` (`spring.datasource.password` default)
  - file: `user-was/src/main/resources/application.yml` (`spring.datasource.password` default)
- Risk:
  - Unauthorized DB access if defaults are used beyond local dev.
- Fix (non-breaking):
  - Move to required `.env`/secret injection.
  - Remove sensitive defaults or fail fast when missing.
- Proposed PR Tasks:
  - IF-001-01: Add `.env.example` and README required env policy.
  - IF-001-02: Remove datasource password fallback defaults.
- Validation:
  - `docker compose config` does not resolve to weak default credentials.
  - Startup behavior is explicit when required vars are missing.

### IF-002: Hardcoded connection secret key default in user-was
- Severity: High
- Category: INF-SEC
- Affected: was | user-was
- Evidence:
  - file: `user-was/src/main/resources/application.yml` (`app.security.connection-secret-base64` fixed value)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/security/ConnectionSecretCrypto.java` (`@Value` fallback secret)
- Risk:
  - Reused key across environments enables credential decryption if leaked.
- Fix (non-breaking):
  - Require env/secret injection; remove default secret.
  - Keep clear startup error if missing.
- Proposed PR Tasks:
  - IF-002-01: Remove default secret fallback and add startup guard.
  - IF-002-02: Document key rotation process.
- Validation:
  - App fails fast when secret is missing.
  - Existing encryption/decryption tests still pass with configured secret.

### IF-003: Redis persistence disabled while Redis stores sessions
- Severity: High
- Category: INF-REL
- Affected: docker-compose | redis | was
- Evidence:
  - file: `docker-compose.yml` (`redis-server --save "" --appendonly no`)
  - file: `admin-was/src/main/resources/application.yml` (`spring.session.store-type: redis`)
  - file: `user-was/src/main/resources/application.yml` (`spring.session.store-type: redis`)
- Risk:
  - Redis restart can wipe sessions and trigger mass logout.
- Fix (non-breaking):
  - Document whether session loss is acceptable.
  - Add optional AOF profile for safer runtime.
- Proposed PR Tasks:
  - IF-003-01: Document Redis persistence policy and outage impact.
  - IF-003-02: Add optional AOF-enabled compose profile.
- Validation:
  - Session behavior before/after Redis restart is tested and documented.

### IF-004: Missing gateway-to-WAS requestId propagation
- Severity: High
- Category: INF-OBS
- Affected: gateway | was
- Evidence:
  - file: `docker/nginx/nginx.conf` (no `X-Request-Id` forwarding)
  - file: `platform-common/src/main/java/com/example/excelimport/common/web/RequestIdFilter.java` (always generates new requestId)
- Risk:
  - Hard to correlate gateway logs and WAS logs during incidents.
- Fix (non-breaking):
  - Forward or generate requestId at gateway.
  - Reuse incoming requestId in WAS filter when present.
- Proposed PR Tasks:
  - IF-004-01: Add nginx `map` + `proxy_set_header X-Request-Id`.
  - IF-004-02: Update `RequestIdFilter` to prefer inbound header.
- Validation:
  - `curl -H "X-Request-Id: test-1"` gives consistent ID in gateway logs, response headers, and body.

### IF-005: Blue/green defined but routing fixed to green
- Severity: Medium
- Category: INF-REL
- Affected: docker-compose | gateway
- Evidence:
  - file: `docker-compose.yml` (blue and green services are both defined)
  - file: `docker/nginx/nginx.conf` (upstreams point only to `*-green`)
- Risk:
  - No real switch path; blue deployment cannot be traffic-validated.
- Fix (non-breaking):
  - Parameterize active color with green as default.
  - Add documented switch and rollback path.
- Proposed PR Tasks:
  - IF-005-01: Add active-color driven upstream routing.
  - IF-005-02: Add blue/green switch runbook.
- Validation:
  - Gateway reload switches upstream based on active color setting.

### IF-006: Gateway timeout/retry policy is incomplete
- Severity: Medium
- Category: INF-REL
- Affected: gateway
- Evidence:
  - file: `docker/nginx/nginx.conf` (`proxy_read_timeout` and `proxy_connect_timeout` only)
  - file: `docker/nginx/nginx.conf` (no `proxy_next_upstream`, `send_timeout`, `client_max_body_size`)
- Risk:
  - Unclear 502/504 behavior under upstream slowness/outage.
- Fix (non-breaking):
  - Add explicit timeout/retry/request-size policy.
- Proposed PR Tasks:
  - IF-006-01: Standardize nginx timeout/retry/body-size config.
- Validation:
  - Fault-injection tests reproduce expected timeout and status code behavior.

### IF-007: Runtime containers run as root
- Severity: Medium
- Category: INF-SEC
- Affected: dockerfile
- Evidence:
  - file: `docker/admin-was.Dockerfile` (no `USER`)
  - file: `docker/user-was.Dockerfile` (no `USER`)
- Risk:
  - Larger blast radius under container compromise.
- Fix (non-breaking):
  - Add app user and run with non-root UID/GID.
- Proposed PR Tasks:
  - IF-007-01: Non-root hardening for both Dockerfiles.
- Validation:
  - `docker exec <container> id` confirms non-root.
  - Healthchecks still pass.

### IF-008: No compose-level logging/resource guardrails
- Severity: Medium
- Category: INF-COST
- Affected: docker-compose
- Evidence:
  - file: `docker-compose.yml` (no logging rotation guidance or compose override template)
- Risk:
  - Disk growth from logs and unstable host resource usage.
- Fix (non-breaking):
  - Provide logging/resource guidelines and override template.
- Proposed PR Tasks:
  - IF-008-01: Add logging/resource guidance in docs.
  - IF-008-02: Add `docker-compose.override.example.yml`.
- Validation:
  - `docker inspect` confirms logging options when override is applied.

### IF-009: depends_on health ordering without app timeout/retry settings
- Severity: Medium
- Category: INF-REL
- Affected: docker-compose | was
- Evidence:
  - file: `docker-compose.yml` (`depends_on` with `condition: service_healthy`)
  - file: `admin-was/src/main/resources/application.yml` (no explicit DB/Redis timeout tuning)
  - file: `user-was/src/main/resources/application.yml` (no explicit DB/Redis timeout tuning)
- Risk:
  - Recovery from transient dependency outages is unpredictable.
- Fix (non-breaking):
  - Add explicit DB/Redis timeout values.
  - Add dependency restart recovery checks.
- Proposed PR Tasks:
  - IF-009-01: Add Spring datasource/redis timeout config.
  - IF-009-02: Add fault-injection smoke checks.
- Validation:
  - After Postgres/Redis restart, readiness returns in expected time window.

### IF-010: Postgres/Redis host ports always exposed
- Severity: Medium
- Category: INF-SEC
- Affected: docker-compose | postgres | redis
- Evidence:
  - file: `docker-compose.yml` (`5432:5432`, `6379:6379`)
- Risk:
  - Increased attack surface if compose is reused outside local environment.
- Fix (non-breaking):
  - Make DB/Redis host port exposure profile-based.
- Proposed PR Tasks:
  - IF-010-01: Add `dev-expose` profile for DB/Redis host ports.
- Validation:
  - Default profile does not expose DB/Redis host ports.
  - `dev-expose` profile preserves current local behavior.

### IF-011: Missing Postgres backup/restore runbook
- Severity: Low
- Category: INF-REL
- Affected: postgres | docs
- Evidence:
  - file: `README.md` (no backup/restore procedure)
  - file: `deploy/k8s/README.md` (deployment order only)
- Risk:
  - Slower recovery and unclear data-loss boundaries in incidents.
- Fix (non-breaking):
  - Add `pg_dump`/`pg_restore` runbook with verification steps.
- Proposed PR Tasks:
  - IF-011-01: Add `docs/infra/POSTGRES_BACKUP_RUNBOOK.md`.
- Validation:
  - Local dump/restore rehearsal passes.

### IF-012: Missing `.dockerignore` for build optimization
- Severity: Low
- Category: INF-COST
- Affected: dockerfile
- Evidence:
  - file: repository root (no `.dockerignore`)
  - file: `docker/admin-was.Dockerfile`, `docker/user-was.Dockerfile` (root context build)
- Risk:
  - Slower build and larger transfer context.
- Fix (non-breaking):
  - Add root `.dockerignore` to reduce context size.
- Proposed PR Tasks:
  - IF-012-01: Introduce root `.dockerignore`.
- Validation:
  - Compare Docker build context size before/after.
