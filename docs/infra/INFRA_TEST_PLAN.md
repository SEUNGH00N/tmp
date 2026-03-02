# Infrastructure Test Plan

## 1. Goal
- Verify hardening changes do not break functional behavior.
- Provide repeatable command-based checks for local and CI.

## 2. Preconditions
- Working directory: `c:\00_excel_db\pr`
- Docker Desktop or Docker Engine running
- Required env vars are set when using `.env`

## 3. Compose Boot and Health Validation
### 3.1 Boot stack
```powershell
docker compose up -d --build
docker compose ps
```
Expected:
- `postgres`, `redis`, `admin-was-green`, `user-was-green`, `gateway` are `running` or `healthy`.

### 3.2 Readiness endpoints
```powershell
curl -sS http://127.0.0.1:8080/actuator/health/readiness
curl -sS http://127.0.0.1:8081/actuator/health/readiness
```
Expected:
- Both return `UP`.

## 4. Fault Injection Tests
### 4.1 Redis restart
```powershell
docker compose stop redis
Start-Sleep -Seconds 10
docker compose start redis
Start-Sleep -Seconds 10
docker compose ps
```
Validate:
- WAS services do not stay in crash loop.
- API recovery after Redis comes back.

### 4.2 Postgres restart
```powershell
docker compose restart postgres
Start-Sleep -Seconds 15
docker compose ps
curl -sS http://127.0.0.1:8081/actuator/health/readiness
```
Validate:
- Readiness recovers in bounded time.
- DB-backed APIs recover without manual restart.

### 4.3 Single WAS restart behind gateway
```powershell
docker compose restart user-was-green
Start-Sleep -Seconds 10
curl -i http://127.0.0.1:8080/api/v1/user/imports/session/workspace
```
Validate:
- Gateway path recovers.
- No prolonged 502/504 window.

## 5. Gateway Timeout and 504 Verification
### 5.1 Baseline latency
```powershell
curl -w "`nconnect=%{time_connect} total=%{time_total} code=%{http_code}`n" -o NUL -sS http://127.0.0.1:8080/actuator/health/readiness
```
Validate:
- Capture baseline response timing before hardening.

### 5.2 Upstream outage behavior
```powershell
docker compose stop user-was-green
curl -i --max-time 20 http://127.0.0.1:8080/
docker compose start user-was-green
```
Validate:
- Observed 5xx and timeout timing match nginx policy.

## 6. Blue/Green Switch Validation
Precondition: active color switch support is implemented.
```powershell
docker exec -it $(docker compose ps -q gateway) nginx -s reload
curl -i http://127.0.0.1:8080/actuator/health/readiness
```
Validate:
- Upstream switches without restarting full stack.
- Rollback path restores previous color quickly.

## 7. Observability Validation (requestId)
```powershell
curl -i -H "X-Request-Id: infra-test-001" http://127.0.0.1:8081/api/v1/auth/me
```
Validate:
- Response header and body `meta.requestId` are consistent.
- Same requestId is traceable in gateway and WAS logs.

## 8. CI Pipeline Suggestions
- Job 1: `docker compose config` sanity check (required env guard)
- Job 2: `docker compose up -d --build` + health polling
- Job 3: fault injection smoke (`redis restart`, `postgres restart`)
- Job 4: requestId propagation e2e check

Suggested script entrypoints:
```powershell
./scripts/infra/compose-health-check.ps1
./scripts/infra/fault-injection-smoke.ps1
./scripts/infra/requestid-e2e.ps1
```

## 9. Cleanup
```powershell
docker compose down
# if full data reset is needed:
# docker compose down -v
```
