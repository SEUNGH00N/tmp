# User WAS

## Scope
- 사용자 업로드 API
- 잡 목록/상태/상세(행/에러) 조회
- 사용자 포털(`/user/*`)

## Runtime
- Port: `8081`
- Base URL: `http://localhost:8081`

## Run
```powershell
cd .\user-was
mvn spring-boot:run
```

## API
- `POST /api/v1/user/imports` (`multipart`: `workspace_id`, `file`)
  - 하위호환: `tenant_id` 허용
- `GET /api/v1/user/imports?workspace_id=...&page=0&size=20`
- `GET /api/v1/user/imports/{jobId}?workspace_id=...`
- `GET /api/v1/user/imports/{jobId}/rows?workspace_id=...&page=0&size=50`
- `GET /api/v1/user/imports/{jobId}/errors?workspace_id=...&page=0&size=50`

## Workspace Session API
- `POST /api/v1/user/imports/session/workspace?workspace_id=...`
- `GET /api/v1/user/imports/session/workspace`

## Import Dispatch Mode
- 설정: `app.user-import.dispatch-mode`
  - `sync` (기본): 내부 async worker 처리
  - `kafka`: Kafka producer adapter 지점 사용(현재 local fallback 가능)
- 설정: `app.user-import.kafka.local-fallback-enabled`

## Redis
- 세션 저장: Redis
- workspace 세션 컨텍스트를 Redis-backed Session으로 유지

## Front Pages
- `GET /` -> `/user/login.html`
- `GET /user/login.html`
- `GET /user/dashboard.html`
- `GET /user/job-detail.html?jobId=...`

## Current Validation Rules
- 상태 플로우: `CREATED -> PARSING -> VALIDATING -> LOADING -> COMPLETED/FAILED`
- 검증:
  - `col_1` 필수 (`IMP-VAL-001`)
  - 셀 길이 255 초과 금지 (`IMP-VAL-002`)
