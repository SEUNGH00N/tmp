# User WAS

## Scope
- 사용자 업로드 API
- 잡 목록/상세/행/에러 조회 API
- 사용자 포털(`/user/*`)

## Runtime
### Docker Gateway
- User Portal: `http://127.0.0.1:8080`

### Local Run
- Port: `8081`
- Base URL: `http://localhost:8081`

## Run
```powershell
cd .\user-was
mvn spring-boot:run
```

## User APIs
- `POST /api/v1/user/imports` (`multipart`: `workspace_id`, `file`)
- `GET /api/v1/user/imports?workspace_id=...`
- `GET /api/v1/user/imports/{jobId}?workspace_id=...`
- `GET /api/v1/user/imports/{jobId}/rows?workspace_id=...`
- `GET /api/v1/user/imports/{jobId}/errors?workspace_id=...`

## Billing APIs (V8)
- `GET /api/v1/user/billing/plan?workspace_id=...`
- `GET /api/v1/user/billing/subscription?workspace_id=...`
- `GET /api/v1/user/billing/summary?workspace_id=...`

## DB Connection APIs (V9)
- `POST /api/v1/user/db-connections?workspace_id=...`
- `GET /api/v1/user/db-connections?workspace_id=...`
- `POST /api/v1/user/db-connections/{connectionId}/test?workspace_id=...`

## Workspace Session APIs
- `POST /api/v1/user/imports/session/workspace?workspace_id=...`
- `GET /api/v1/user/imports/session/workspace`

## Redis
- Session namespace: `user-was:session`
- workspace context는 Redis-backed session으로 유지

## Notes
- 업로드 dedup (`import_dedup`) 적용
- run/event/metric 기반 비동기 관측 구조 적용
- Actuator health endpoint 활성화됨
