# Excel Import Platform

## Overview
`admin-was` + `user-was` 분리 구조의 Excel Import SaaS MVP 프로젝트입니다.

- `admin-was`: 인증/권한/관리 화면/마이그레이션 소유
- `user-was`: 사용자 업로드/잡 조회/에러 조회
- 공용 DB: PostgreSQL
- 공용 세션 스토어: Redis

## Modules
- `admin-was`
- `user-was`
- `platform-common`

## Runtime Ports
- Admin WAS: `http://localhost:8080`
- User WAS: `http://localhost:8081`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

## Current Architecture
- 인증: `HttpSession + Bearer Token` 병행 지원
- 세션 저장: Redis (`spring-session-data-redis`)
- 업로드 처리: `Event Publisher` 경유 구조
  - 기본: `sync` 모드(내부 async worker 호출)
  - 확장: `kafka` 모드(현재 producer adapter 지점만 준비)
- 테넌트 컨텍스트: `workspace_id` 기준
- 과금/사용량(V8):
  - `billing_plan`, `plan_feature`, `workspace_subscription`, `workspace_usage_daily`
  - 업로드 quota 검사(월 업로드 횟수/최대 파일 크기)
  - 사용자 포털에서 플랜/사용량 요약 조회
- DB 연결 보안(V9):
  - `db_connection` 테이블
  - `password_enc` + `key_version` 저장 정책
  - 사용자 연결 등록/조회/연결 테스트 API
- 비동기 신뢰성/관측(V10):
  - `import_job_run`, `import_job_event`, `import_job_metric`, `import_dedup`
  - 업로드 중복 파일 dedup 처리
  - run 단위 상태/에러/메트릭 적재

## Run
1. Infra
```powershell
docker compose -f .\docker-compose.yml up -d
```
2. Admin WAS
```powershell
cd .\admin-was
mvn spring-boot:run
```
3. User WAS
```powershell
cd .\user-was
mvn spring-boot:run
```

## Test
```powershell
cd .\
mvn clean test
```

## Docs
- `ERD.md`: As-Is / To-Be ERD
- `WBS.md`: 단일 개발자 기준 실행 WBS
- `admin-was/README.md`: Admin 상세
- `user-was/README.md`: User 상세

## Next Focus
- Admin: RBAC 확장(권한 단위 API 인가), 감사로그 고도화
- User: 플랜/사용량 제한(`usage_daily`) 반영
- Common: Kafka producer/consumer 실제 연결 및 DLQ 전략
