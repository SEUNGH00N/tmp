# Admin WAS

## Scope
- 관리자 인증/인가
- Import 관리 API
- 사용자/설정 관리 API
- Flyway 마이그레이션 관리

## Runtime
- Port: `8080`
- Base URL: `http://localhost:8080`

## Run
```powershell
cd .\admin-was
mvn spring-boot:run
```

## Auth

### Login
- `POST /api/v1/auth/login`
- Body: `{"username":"admin","password":"admin1234"}`
- Response: `username`, `tokenType`, `accessToken`

### Me
- `GET /api/v1/auth/me`
- 인증 방식:
  - 세션 쿠키
  - `Authorization: Bearer <accessToken>`

### Logout
- `POST /api/v1/auth/logout`
- 세션/토큰 동시 무효화

## Import API
- `POST /api/v1/imports` (`multipart`: `file`, `workspace_id`)
  - 하위호환: `tenant_id` 허용
- `GET /api/v1/imports?workspace_id=...`
- `GET /api/v1/imports/{jobId}`
- `GET /api/v1/imports/{jobId}/rows?page=0&size=50`
- `GET /api/v1/imports/{jobId}/errors?page=0&size=50`

## Admin API
- `GET /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{userId}/status`
- `PUT /api/v1/admin/users/{userId}/roles`
- `GET /api/v1/admin/roles`

## Import Dispatch Mode
- 설정: `app.import.dispatch-mode`
  - `sync` (기본): 내부 async worker 처리
  - `kafka`: Kafka producer adapter 지점 사용(현재 local fallback 가능)
- 설정: `app.import.kafka.local-fallback-enabled`

## Async Reliability & Observability (V10)
- `import_job_run`으로 재시도/실행 이력 관리
- `import_job_event`로 상태 이벤트 적재
- `import_job_metric`으로 처리량 메트릭 적재
- `import_dedup`으로 동일 파일 중복 업로드 감지

## Redis
- 세션 저장: Redis
- 인증 토큰 저장: Redis (`auth:token:*`)

## Front Pages
- `GET /` -> `/login.html`
- `GET /login.html`
- `GET /mock/dashboard.html`
- `GET /mock/admin-users.html`
- `GET /mock/admin-roles.html`
