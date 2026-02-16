# Excel Import MVP

## Prerequisites
- Java 17
- Maven 3.9+
- Docker Desktop

## Run infra (PowerShell)
```powershell
docker compose -f .\docker-compose.yml up -d
```

## Run app (PowerShell)
```powershell
cd .\admin-was
mvn spring-boot:run
```

## Multi-WAS structure
- Admin WAS (`pr/admin-was`): port `8080`
- User WAS (`pr/user-was`): port `8081`
- User WAS run:
```powershell
cd .\user-was
mvn spring-boot:run
```

## Workspace note
- `pr/src` is a legacy copy kept temporarily due IDE file locks during refactor.
- Active admin WAS source path is `pr/admin-was/src`.

## Test
```powershell
mvn test
```

## Login
- Access: `http://localhost:8080`
- Default credentials: `admin` / `admin1234`
- Default admin is seeded by `V2__auth_user.sql`
- Additional demo users are seeded by `V3__seed_dummy_data.sql`

## API
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`
- `POST /api/v1/imports` (multipart: `file`, `tenant_id`)
- `GET /api/v1/imports` (paged)
- `GET /api/v1/imports/{jobId}`
- `GET /api/v1/imports/{jobId}/errors?page=0&size=50`
- `GET /api/v1/imports/{jobId}/rows?page=0&size=50`
- `GET /api/v1/users?page=0&size=20`
- `GET /api/v1/settings`
- `PUT /api/v1/settings`

## Upload backend skeleton
- `ImportController` -> `ImportJobService.createJob(...)`
- `ImportJobService` -> `UserUploadFacade`
- `UserUploadFacade` orchestration flow:
  - `UploadGuard` (request/policy validation)
  - `UploadStoragePort` (file persistence; local FS adapter)
  - `ImportJobDispatchPort` (job creation + async dispatch)
- Current adapters:
  - `DefaultUploadGuard`
  - `LocalUploadStorageAdapter`
  - `JpaImportJobDispatchAdapter`
- Extension points prepared for production:
  - quota/tenant policy checks
  - antivirus/content signature checks
  - dedup/outbox/audit pipeline integration

## Seed data (V3)
- Users: `admin`, `operator`, `viewer`
- Sample jobs:
  - `10000000-0000-0000-0000-000000000001` (COMPLETED)
  - `10000000-0000-0000-0000-000000000002` (FAILED)

## Migration policy (admin-was)
- `V1__init.sql`: initial schema (`import_job`, `excel_data`, `error_log`, enum status)
- `V2__auth_user.sql`: `app_user` table + default admin seed
- `V3__seed_dummy_data.sql`: dev/demo seed data (users/jobs/rows/errors)
- `V4__import_job_status_to_varchar.sql`: enum -> `VARCHAR(32)` conversion for `import_job.status`
- `V5__migration_hygiene.sql`: status CHECK constraint + core index hygiene

### Seed policy
- `V2` is required baseline seed (login bootstrap).
- `V3` is demo/development seed. Keep out of production if strict clean data policy is required.
- If production should not contain demo data, split `V3` into profile/ops-managed seed flow.

## Storage policy
- Storage root: `./storage` (configurable via `app.storage.root`)
- Final path: `{root}/{tenantId}/{sha256_prefix}/{sha256}.{ext}`
- TTL property: `app.storage.retention-days` (cleanup job optional)

## Frontend pages
- `GET /` -> redirects to login page
- `GET /login.html`
- `GET /mock/dashboard.html`
- `GET /mock/job-list.html`
- `GET /mock/job-details.html`
- `GET /mock/settings.html`

## Frontend stack
- Vue: `Vue 3` CDN build (`https://unpkg.com/vue@3/dist/vue.global.prod.js`)
- Styling: Tailwind CSS CDN
- Auth/session: cookie-based session (`/api/v1/auth/*`)
- API binding: pages render API data directly in main content (no floating debug/live panel)

## Notes
- CSV/XLSX supported.
- Validation rules: `col_1` required, each cell max 255 chars.
- Partial success: invalid rows are logged to `error_log`, valid rows are saved.
- Original stitch screenshots are kept at `/mock/screens/*.png`.

## TODO (WBS)

### 1. Foundation Stabilization (Sprint 1)

#### 1.1 Data model and migration hygiene
- [x] 1.1.1 Flyway migration 정리 (`V1~V5` 역할 문서화, seed/dev-only 분리 정책 수립)
- [x] 1.1.2 상태 컬럼 타입 전략 확정 (`VARCHAR(32)` + CHECK 제약) 및 엔티티 매핑 일치화
- [x] 1.1.3 인덱스 점검 (`import_job.created_at`, `error_log(job_id, created_at)`, `excel_data(job_id, created_at)`)

#### 1.2 Security and access control
- [ ] 1.2.1 Role 컬럼/테이블 설계 (`admin/operator/viewer`)
- [ ] 1.2.2 API 접근 제어 적용 (권한별 endpoint 허용 정책)
- [ ] 1.2.3 인증 실패/권한 실패 응답 표준화 (RFC7807 + 내부 에러코드)

#### 1.3 Core import correctness
- [ ] 1.3.1 import 상태 전환 규칙 검증 및 보강 (CREATED -> ... -> COMPLETED/FAILED)
- [ ] 1.3.2 `processed_rows/success_count/fail_count` 무결성 보장 (트랜잭션/예외 시나리오 포함)
- [ ] 1.3.3 validation/error_code 체계 정리 (필수값/길이/타입 규칙 + 코드 표준)

#### 1.4 Frontend operational readiness
- [ ] 1.4.1 로그인 세션 만료 UX 확정 (자동 redirect, 재로그인 후 복귀 경로)
- [ ] 1.4.2 Dashboard/JobDetail/Settings 실데이터 바인딩 QA (empty/error/loading 상태)
- [ ] 1.4.3 사이드바 네비 규칙 고정 (활성 메뉴, 경로 매핑, 접근 제약 페이지 처리)

### 2. Feature Completion (Sprint 2)

#### 2.1 Parsing and load pipeline hardening
- [ ] 2.1.1 CSV/XLSX 파서 모듈 분리 정리 (`CsvParser`, `XlsxParser`, 공통 인터페이스)
- [ ] 2.1.2 row 파싱 -> `excel_data.payload_json` 적재 경계조건 보강 (빈 파일/헤더/대량 행)
- [ ] 2.1.3 checksum 중복 업로드 정책 구현 (동일 파일 재업로드 시 처리 규칙)

#### 2.2 Tenant and domain expansion
- [ ] 2.2.1 `tenant` 테이블 추가 및 `import_job.tenant_id` FK 전환
- [ ] 2.2.2 Tenant 선택 UX 개선 (로그인 사용자 기본 tenant/드롭다운 선택)
- [ ] 2.2.3 tenant 단위 조회/권한 제한 적용 (`/imports`, `/users` 필터링 정책)

#### 2.3 API/UX quality
- [ ] 2.3.1 OpenAPI/Swagger 문서화 (인증, import, settings, users)
- [ ] 2.3.2 에러 상세 UX 개선 (row highlight, 다운로드 검토)
- [ ] 2.3.3 업로드 진행률 표시(progress bar + polling/stream 전략 선택)

### 3. Operability and Performance (Sprint 3)

#### 3.1 Runtime and infra quality
- [ ] 3.1.1 Docker compose healthcheck (postgres/redis/app)
- [ ] 3.1.2 환경 분리(dev/prod) 및 환경변수 기반 설정 표준화
- [ ] 3.1.3 CI 파이프라인 구성 (test/build + migration smoke test)
- [ ] 3.1.4 로그 분리/rotation 적용

#### 3.2 Data lifecycle
- [ ] 3.2.1 storage cleanup scheduler 구현 (`retention-days`)
- [ ] 3.2.2 정리 작업 안전장치 (dry-run, 보호 경로, 삭제 리포트)

#### 3.3 Performance hardening
- [ ] 3.3.1 대용량 업로드 streaming/chunk 전략 적용
- [ ] 3.3.2 batch insert 튜닝 및 병목 측정 (JPA batch vs native batch)
- [ ] 3.3.3 `/errors`, `/rows` 페이지네이션 성능 검증 (실데이터 기준)

### 4. Architecture Evolution (Backlog)

#### 4.1 Async architecture
- [ ] 4.1.1 Job queue 표준 플로우 정의 (create -> publish -> worker -> complete)
- [ ] 4.1.2 Kafka 도입 PoC (retry, DLQ, idempotency)

#### 4.2 Stateless and scale-out
- [ ] 4.2.1 세션 기반 인증에서 토큰 기반 인증 전환 계획 수립
- [ ] 4.2.2 Redis 역할 재정의 (cache/lock/session 분리)
- [ ] 4.2.3 App 다중 인스턴스 + reverse proxy 구성

