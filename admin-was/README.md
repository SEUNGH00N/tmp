# Admin WAS

## Scope
- 관리자 인증/인가
- 관리자 운영 API
- Import 관리 API
- Flyway 마이그레이션 소유

## Runtime
### Docker Gateway
- Admin Portal: `http://127.0.0.1:8081`

### Local Run
- Port: `8080`
- Base URL: `http://localhost:8080`

## Run
```powershell
cd .\admin-was
mvn spring-boot:run
```

## Auth APIs
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

인증 방식:
- Redis-backed `HttpSession`
- `Authorization: Bearer <accessToken>`

## Admin APIs
- `GET /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{userId}/status`
- `PUT /api/v1/admin/users/{userId}/roles`
- `GET /api/v1/admin/roles`

## Import APIs
- `POST /api/v1/imports` (`multipart`: `file`, `workspace_id`)
- `GET /api/v1/imports?workspace_id=...`
- `GET /api/v1/imports/{jobId}`
- `GET /api/v1/imports/{jobId}/rows`
- `GET /api/v1/imports/{jobId}/errors`

## Redis
- Session namespace: `admin-was:session`
- Token key: `auth:token:*`

## Notes
- 예외 처리에서 정적 리소스 누락은 `404 Not Found`로 반환
- Actuator health endpoint 활성화됨
