# Infra Task List

## Task Rules
- One task equals one PR.
- No feature change; refactor or hardening only.
- Rollback must be possible (`git revert` or config toggle rollback).
- Each task must include validation commands and expected results.

## Phase 1 - Low Risk

### IF-001
### Task Name
Add `.env.example` and required env policy

### Phase
Phase 1 - Low Risk

### Purpose
- Prevent accidental use of weak default credentials.

### Work Steps
1. Add root `.env.example`.
2. Document required env vars in `README.md`.
3. Document dev-only fallback risk and policy.

### Change Scope
- `README.md`
- new `.env.example`
- `docs/infra/INFRA_RULES.md`

### Validation
- `docker compose config`

### Done Criteria
- Required env contract is explicit and reproducible.

### IF-002
### Task Name
Add nginx requestId propagation policy

### Phase
Phase 1 - Low Risk

### Purpose
- Ensure gateway and WAS log correlation.

### Work Steps
1. Add requestId map/forwarding in `nginx.conf`.
2. Keep inbound requestId when provided.
3. Add requestId to access log format.

### Change Scope
- `docker/nginx/nginx.conf`

### Validation
- `curl -H "X-Request-Id: test-1" ...`

### Done Criteria
- Same requestId is observable in gateway and WAS outputs.

### IF-003
### Task Name
Run WAS containers as non-root

### Phase
Phase 1 - Low Risk

### Purpose
- Reduce runtime privilege risk.

### Work Steps
1. Add app user/group in both Dockerfiles.
2. Adjust `/app` permissions.
3. Switch runtime to `USER` non-root.

### Change Scope
- `docker/admin-was.Dockerfile`
- `docker/user-was.Dockerfile`

### Validation
- `docker compose up -d --build`
- `docker exec <id> id`

### Done Criteria
- Both WAS containers run as non-root and stay healthy.

### IF-004
### Task Name
Introduce root `.dockerignore`

### Phase
Phase 1 - Low Risk

### Purpose
- Improve build speed and cache efficiency.

### Work Steps
1. Add root `.dockerignore`.
2. Exclude build artifacts and VCS noise.
3. Compare Docker build context size before/after.

### Change Scope
- new `.dockerignore`

### Validation
- `docker build -f docker/admin-was.Dockerfile .`

### Done Criteria
- Context size is reduced and build remains successful.

## Phase 2 - Medium

### IF-101
### Task Name
Document Redis persistence policy and optional AOF profile

### Phase
Phase 2 - Medium

### Purpose
- Clarify session-loss risk and provide safer runtime option.

### Work Steps
1. Document current non-persistent Redis policy.
2. Add optional AOF compose profile/override example.
3. Add outage impact notes.

### Change Scope
- `docker-compose.yml` (or override template)
- `README.md`
- `docs/infra/INFRA_RULES.md`

### Validation
- Restart Redis and compare session behavior.

### Done Criteria
- Policy and optional persistence path are documented.

### IF-102
### Task Name
Standardize nginx timeout/retry/body-size

### Phase
Phase 2 - Medium

### Purpose
- Make 502/504 behavior predictable under faults.

### Work Steps
1. Define connect/read/send timeout values.
2. Add `proxy_next_upstream` strategy.
3. Align `client_max_body_size` with upload constraints.

### Change Scope
- `docker/nginx/nginx.conf`

### Validation
- Fault injection: upstream delay/outage tests.

### Done Criteria
- Timeout and retry behavior matches documented policy.

### IF-103
### Task Name
Add compose logging and resource guardrail templates

### Phase
Phase 2 - Medium

### Purpose
- Control log growth and host resource pressure.

### Work Steps
1. Add `docker-compose.override.example.yml`.
2. Add logging rotation examples.
3. Document recommended CPU/memory limits.

### Change Scope
- new `docker-compose.override.example.yml`
- `README.md`
- `docs/infra/INFRA_RULES.md`

### Validation
- `docker inspect` shows expected logging options.

### Done Criteria
- Team has a reusable guardrail template.

### IF-104
### Task Name
Add explicit Spring DB/Redis timeout settings

### Phase
Phase 2 - Medium

### Purpose
- Improve recovery behavior after dependency restarts.

### Work Steps
1. Add Hikari timeout settings.
2. Add Redis timeout settings.
3. Keep existing API behavior unchanged.

### Change Scope
- `admin-was/src/main/resources/application.yml`
- `user-was/src/main/resources/application.yml`

### Validation
- Restart Postgres/Redis and verify readiness recovery.

### Done Criteria
- Timeout settings are explicit and tested.

## Phase 3 - High

### IF-201
### Task Name
Remove sensitive fallback defaults (fail-fast)

### Phase
Phase 3 - High

### Purpose
- Eliminate startup with weak default secrets.

### Work Steps
1. Remove datasource password fallback defaults.
2. Remove connection secret fallback defaults.
3. Add explicit startup validation/failure behavior.

### Change Scope
- `admin-was/src/main/resources/application.yml`
- `user-was/src/main/resources/application.yml`
- `user-was/src/main/java/com/example/excelimport/userwas/security/ConnectionSecretCrypto.java`

### Validation
- Missing required env leads to clear startup failure.
- With env set, existing flows still work.

### Done Criteria
- No sensitive default fallback remains.

### IF-202
### Task Name
Enable active color switch for blue/green routing

### Phase
Phase 3 - High

### Purpose
- Make rollout/rollback possible by config + reload.

### Work Steps
1. Parameterize active upstream color.
2. Keep default color as green.
3. Document switch and rollback runbook.

### Change Scope
- `docker/nginx/nginx.conf`
- `docker-compose.yml`
- `docs/infra/INFRA_TEST_PLAN.md`

### Validation
- Gateway reload switches active color as expected.

### Done Criteria
- Blue/green switch path is reproducible and documented.

### IF-203
### Task Name
Add Postgres backup and restore runbook

### Phase
Phase 3 - High

### Purpose
- Standardize data recovery procedure.

### Work Steps
1. Add `pg_dump` and `pg_restore` commands.
2. Add restore verification queries.
3. Link runbook from `README.md`.

### Change Scope
- new `docs/infra/POSTGRES_BACKUP_RUNBOOK.md`
- `README.md`

### Validation
- Local dump/restore rehearsal succeeds.

### Done Criteria
- Backup/restore runbook is complete and usable.
