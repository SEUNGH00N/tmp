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
mvn spring-boot:run
```

## Test
```powershell
mvn test
```

## Login
- Access: `http://localhost:8080`
- Default credentials: `admin` / `admin1234`
- Credentials are seeded by Flyway migrations (`V2__auth_user.sql`, `V3__seed_dummy_data.sql`)

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

## Seed data (V3)
- Users: `admin`, `operator`, `viewer`
- Sample jobs:
  - `10000000-0000-0000-0000-000000000001` (COMPLETED)
  - `10000000-0000-0000-0000-000000000002` (FAILED)

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

## Notes
- CSV/XLSX supported.
- Validation rules: `col_1` required, each cell max 255 chars.
- Partial success: invalid rows are logged to `error_log`, valid rows are saved.
- Original stitch screenshots are kept at `/mock/screens/*.png`.

## TODO

### Backend
- [ ] CSV / XLSX 데이터 추출 기능 구현 (`CsvParser`, `XlsxParser` 등 모듈 분리)
- [ ] 업로드 파일 기반 row 파싱 → `excel_data.payload_json` 저장 로직 추가
- [ ] Validation 로직 강화 (컬럼별 규칙, nullable, datatype validation)
- [ ] Import Job 상태 전환 로직 구현 (CREATED → PARSING → VALIDATING → LOADING → COMPLETED/FAILED)
- [ ] 대용량 업로드 처리 개선 (streaming 처리, chunk insert, batch insert 최적화)
- [ ] `processed_rows`, `success_count`, `fail_count` 업데이트 로직 구현
- [ ] error_log 기록 방식 개선 (error_code 표준화, row 원본 값 저장 여부 검토)
- [ ] `GET /api/v1/imports/{jobId}/rows` 실제 DB row 데이터 조회 구현
- [ ] `GET /api/v1/imports/{jobId}/errors` 페이징 조회 최적화 및 인덱스 추가 검토
- [ ] Tenant 관리 기능 추가 (`tenant` 테이블 생성 + `import_job.tenant_id` FK 적용)
- [ ] User 권한(Role) 기반 접근 제어 강화 (admin/operator/viewer 권한 분리)
- [ ] 업로드 파일 checksum 검증 및 중복 업로드 방지 로직 추가
- [ ] Job 이벤트/히스토리 테이블 추가 검토 (`import_job_event`)
- [ ] API 예외 처리 표준화 (ErrorResponse 구조 통일)
- [ ] API 문서화 추가 (Swagger/OpenAPI)

### Infra
- [ ] Docker compose 환경에서 postgres/redis healthcheck 추가
- [ ] WAS 이중화 구성 (App 인스턴스 2+ 대 + Load Balancer/Reverse Proxy)
- [ ] Stateless 아키텍처로 전환 (세션 제거/최소화, 토큰 기반 인증 + Redis는 캐시/락 용도로 분리)
- [ ] Redis 고도화 (세션/캐시/락 목적 분리, TTL 정책, eviction 정책 검토)
- [ ] Kafka 기반 비동기 Import 처리 구조 추가 (Producer/Consumer, retry, DLQ 설계)
- [ ] Import Job Queue 처리 구조 확립 (job 생성 → Kafka publish → worker 처리)
- [ ] Storage cleanup scheduler 구현 (`app.storage.retention-days` 기반 자동 삭제)
- [ ] Flyway migration 버전 정리 및 seed 데이터 구조 개선
- [ ] CI workflow 구성 (GitHub Actions: test + build)
- [ ] 운영 환경 분리 (dev/prod profile 분리 및 환경변수 기반 설정)
- [ ] 로그 파일 분리 및 rotation 설정 (logback/log4j2)

### Frontend
- [ ] UI에서 Tenant ID 입력 방식 개선 (로그인 기반 자동 세팅 or dropdown 선택)
- [ ] Dashboard 데이터 API 연동 (Recent Jobs 실시간 조회 및 Refresh 구현)
- [ ] Job 상세 페이지 API 연동 (`/imports/{jobId}`, `/errors`, `/rows`)
- [ ] 파일 업로드 진행률 표시(progress bar) 구현
- [ ] Validation error 표시 UI 개선 (row별 에러 highlight, 다운로드 지원 검토)
- [ ] Settings 페이지 API 연동 (`GET/PUT /api/v1/settings`)
- [ ] 로그인 세션 만료 처리 및 자동 redirect 구현
- [ ] UI 공통 컴포넌트화 (table, pagination, toast, modal)
