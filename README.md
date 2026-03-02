# Excel Import Platform

## Overview
`admin-was` + `user-was` + `platform-common` 멀티모듈 기반 Excel Import SaaS MVP 프로젝트입니다.

- `admin-was`: 관리자 인증/인가, 운영 API, 마이그레이션 소유
- `user-was`: 사용자 업로드/조회 포털 API
- 공용 인프라: PostgreSQL, Redis

## Modules
- `platform-common`
- `admin-was`
- `user-was`

## Runtime Ports
### Docker Gateway (권장)
- User Portal: `http://127.0.0.1:8080`
- Admin Portal: `http://127.0.0.1:8081`
- PostgreSQL: `127.0.0.1:5432`
- Redis: `127.0.0.1:6379`

### Local Spring Boot Direct Run
- Admin WAS: `http://localhost:8080`
- User WAS: `http://localhost:8081`

## Environment Variables (Docker Compose)
- `docker-compose.yml` currently has fallback defaults for DB credentials.
- Team policy: use `.env` values explicitly and do not rely on fallback defaults.

Setup:
```powershell
copy .env.example .env
```

Required variables:
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

Validation:
```powershell
docker compose --env-file .env config
```

## Current Deployment Baseline
- `docker-compose.yml` 구성:
  - `gateway` (nginx reverse proxy)
  - `admin-was-green`, `user-was-green` (현재 active)
  - `admin-was-blue`, `user-was-blue` (정의는 유지, 현재 미사용)
  - `postgres`, `redis`
- 앱 헬스체크:
  - `/actuator/health/readiness`
  - `/actuator/health/liveness`

## Redis Usage
- Session Store: `spring-session-data-redis`
- Admin Token Store: `auth:token:*`
- Session namespace 분리:
  - admin: `admin-was:session`
  - user: `user-was:session`

## Run
```powershell
cd .\
docker compose up -d --build
```

## Test
```powershell
cd .\
mvn clean test
```

## Kubernetes Preparation
- `deploy/k8s/base`에 base manifest 추가됨
- readiness/liveness probe + rolling update 전략 반영
- Docker/K8s 환경변수 키 정렬

## Docs
- `ERD.md`
- `WBS.md`
- `admin-was/README.md`
- `user-was/README.md`
- `docs/refactoring/CURRENT_STATE.md`
- `docs/refactoring/PLAN.md`
- `docs/conventions/STYLE_GUIDE.md`

## Troubleshooting
- `localhost`에서 IPv4/IPv6 라우팅이 섞이면 정적 리소스가 간헐 실패할 수 있음
- 포털 접근은 `127.0.0.1` 사용 권장

## Repository Architecture Snapshot (2026-03-02)
- 빌드: Maven 멀티모듈 (`platform-common`, `admin-was`, `user-was`)
- 공통 모듈: `platform-common` (`ApiResponse`, `RequestIdFilter`)
- `admin-was`: controller/service/repository(entity) + 일부 JdbcTemplate SQL
- `user-was`: controller/service + JdbcTemplate 중심
- 인프라: PostgreSQL, Redis, Spring Session Redis

## Refactoring and Convention Baseline
- 현재 상태 진단: `docs/refactoring/CURRENT_STATE.md`
- 실행 계획: `docs/refactoring/PLAN.md`
- 코드 규칙(MUST/SHOULD): `docs/conventions/STYLE_GUIDE.md`

## Team Working Rules (Quick)
- 모든 변경은 작고 되돌릴 수 있게 진행한다.
- 리팩토링과 기능 변경은 같은 PR에 섞지 않는다.
- API 에러 응답은 ProblemDetail 표준을 사용한다.
- 컨트롤러는 DB 접근(JdbcTemplate/JPA)을 직접 하지 않는다.
- 민감정보(비밀번호/토큰/시크릿)는 로그/응답에 노출하지 않는다.
