# Refactoring Plan

## Scope and principle
- 본 계획은 대규모 기능 변경이 아닌, 안전하고 되돌릴 수 있는 구조 개선을 단계적으로 수행한다.
- 우선순위 기준:
  1. 리스크 낮고 효과 큰 작업
  2. 중간 리스크 작업
  3. 구조 변경이 큰 작업
- 상세 진단 근거: `docs/refactoring/CURRENT_STATE.md`

## Priority 1 (Low risk, high impact)

## Task 1. Workspace 컨텍스트 해석 로직 공통화

### (a) 목적/문제
- 문제: `resolveWorkspaceId` 로직이 user 컨트롤러 4곳에 중복되어 정책 변경 시 누락 가능성이 높다.
- 해결: 공통 resolver 컴포넌트(예: `WorkspaceContextResolver`) 또는 공통 인터셉터를 도입한다.
- 효과: 파라미터/세션 해석 정책이 단일 지점으로 수렴되어 버그와 변경 비용 감소.

### (b) 변경 범위
- `user-was/controller/UserImportController.java`
- `user-was/controller/UserBillingController.java`
- `user-was/controller/UserDbConnectionController.java`
- `user-was/controller/UserGovernanceController.java`
- 신규: `user-was/.../web/WorkspaceContextResolver.java` (또는 동등 위치)

### (c) 단계별 작업 절차
1. 기존 중복 로직을 동일 동작으로 추출한 resolver 추가.
2. 컨트롤러 4곳에서 private 메서드 삭제 후 resolver 주입.
3. 예외 메시지/상태코드 호환성 확인(`workspace_id is required`).

### (d) 테스트/검증 방법
- 컨트롤러 단위/웹 MVC 테스트:
  - `workspace_id` 전달 시 성공
  - `tenant_id` fallback 성공
  - 세션 fallback 성공
  - 모두 없을 때 400

### (e) 롤백 방법
- resolver 사용 커밋 단위로 되돌리고, 기존 private 메서드 복구.
- DB 스키마 변경 없음, 롤백 비용 낮음.

### (f) 예상 리스크와 완화책
- 리스크: 요청 파라미터 우선순위 변경.
- 완화: 기존 우선순위(`workspace_id` -> `tenant_id` -> session)를 테스트로 고정.

## Task 2. 에러 응답 계약 통일 (admin/user)

### (a) 목적/문제
- 문제: admin/user의 ProblemDetail 구조가 달라 클라이언트 처리 복잡성 증가.
- 해결: 공통 에러 포맷 규칙(`type/title/detail/requestId`)을 정의하고 user-was 핸들러 반영.
- 효과: API 소비자 일관성 확보, 관측성(requestId) 개선.

### (b) 변경 범위
- `user-was/exception/UserWasExceptionHandler.java`
- `platform-common/common/web/RequestIdFilter.java` 활용 방식 정렬
- 문서: `docs/conventions/STYLE_GUIDE.md`

### (c) 단계별 작업 절차
1. 에러 응답 최소 필드 집합 정의.
2. user-was 핸들러에 requestId/type/title 매핑 추가.
3. 기존 사용자 영향(필드 추가) 검토 후 릴리즈 노트 반영.

### (d) 테스트/검증 방법
- `curl`/MockMvc로 admin/user 오류 응답 JSON 스키마 비교.
- requestId 헤더와 body 필드 일치 검증.

### (e) 롤백 방법
- 핸들러 단일 파일 롤백으로 기존 포맷 즉시 복귀 가능.

### (f) 예상 리스크와 완화책
- 리스크: 프론트가 기존 단순 포맷에 강결합.
- 완화: 필드 제거가 아니라 확장(추가) 방식으로 호환성 유지.

## Task 3. 로깅 기준 도입과 예외 경로 로그 보강

### (a) 목적/문제
- 문제: 실패 경로 로그 부족으로 장애 원인 추적이 어려움.
- 해결: 에러/보안 이벤트/비동기 작업 상태에 대한 최소 로그 포인트 정의 및 적용.
- 효과: 장애 분석 시간 단축, 운영 대응 품질 향상.

### (b) 변경 범위
- `admin-was/exception/GlobalExceptionHandler.java`
- `user-was/exception/UserWasExceptionHandler.java`
- `ImportWorkerService`, `UserImportProcessingService` catch 블록

### (c) 단계별 작업 절차
1. 로그 레벨 규칙 확정(DEBUG/INFO/WARN/ERROR).
2. 전역 예외 핸들러 및 비동기 처리 실패 지점에 구조화 로그 추가.
3. 민감정보 마스킹(비밀번호/토큰) 규칙 반영.

### (d) 테스트/검증 방법
- 실패 시나리오 실행 후 로그 필드(요청ID/작업ID/예외타입) 확인.

### (e) 롤백 방법
- 로깅 추가 커밋만 revert.

### (f) 예상 리스크와 완화책
- 리스크: 로그 과다.
- 완화: INFO 최소화, 디버그는 조건부/개발 프로파일만 활성화.

## Priority 2 (Medium risk)

## Task 4. 서비스 책임 분리 (JdbcTemplate SQL 분해)

### (a) 목적/문제
- 문제: 서비스가 SQL+비즈니스+워크플로우를 함께 처리해 변경 영향이 큼.
- 해결: SQL 접근을 Query/Command 컴포넌트(또는 repository adapter)로 분리.
- 효과: 서비스 단위 테스트 용이성 향상, 변경 충돌 감소.

### (b) 변경 범위
- `user-was/service/UserImportService.java`
- `user-was/service/UserBillingService.java`
- `admin-was/service/AdminUserManagementService.java`
- 신규 package 후보: `.../persistence` 또는 `.../repository/jdbc`

### (c) 단계별 작업 절차
1. 읽기 쿼리와 쓰기 커맨드 메서드를 분리(동작 동일 유지).
2. 서비스는 유스케이스 오케스트레이션만 담당하도록 정리.
3. SQL 문자열 상수/매퍼 중복 제거.

### (d) 테스트/검증 방법
- 기존 API 회귀 테스트.
- SQL 단위 검증(파라미터/결과 매핑) 테스트 추가.

### (e) 롤백 방법
- 클래스 단위 분리 전 상태로 revert 가능.
- DB 스키마는 변경하지 않음.

### (f) 예상 리스크와 완화책
- 리스크: 쿼리 파라미터 매핑 실수.
- 완화: 변경 전후 API 응답 스냅샷 비교 테스트.

## Task 5. 중복 인증 토큰 파싱/액터 식별 공통화

### (a) 목적/문제
- 문제: `extractBearerToken`/actor 추출 로직이 인터셉터/컨트롤러에 분산.
- 해결: 인증 컨텍스트 유틸 또는 필터/리졸버로 통합.
- 효과: 인증 관련 버그/정책 불일치 감소.

### (b) 변경 범위
- `admin-was/auth/AuthInterceptor.java`
- `admin-was/auth/AdminAuthorizationInterceptor.java`
- `admin-was/controller/AuthController.java`
- `admin-was/controller/AdminGovernanceController.java`

### (c) 단계별 작업 절차
1. 공통 토큰 파서 도입.
2. actor username 해석을 단일 서비스로 추출.
3. 각 컴포넌트의 중복 private 메서드 제거.

### (d) 테스트/검증 방법
- Bearer 헤더 유효/무효/누락 케이스 회귀.
- 세션+토큰 동시 존재 우선순위 검증.

### (e) 롤백 방법
- 공통화 커밋 revert 후 기존 private 로직 복원.

### (f) 예상 리스크와 완화책
- 리스크: 인증 실패/성공 조건 미세 변경.
- 완화: 기존 동작을 테스트로 먼저 고정 후 리팩토링.

## Priority 3 (Large structural change)

## Task 6. Import 처리 파이프라인 공통 코어화 (admin/user)

### (a) 목적/문제
- 문제: admin/user 비동기 import 처리의 상태 전이/이벤트/메트릭 로직이 중복.
- 해결: 공통 코어(예: `platform-common` 또는 공유 모듈) + 모듈별 adapter로 분리.
- 효과: 기능 확장(재시도/관측성/실패정책) 시 이중 수정 제거.

### (b) 변경 범위
- `admin-was/service/ImportWorkerService.java`
- `user-was/service/UserImportProcessingService.java`
- `admin-was/upload/*`, `user-was/service/*EventPublisher*`
- 신규 shared abstraction (pipeline, lifecycle hooks)

### (c) 단계별 작업 절차
1. 상태전이/이벤트/메트릭 공통 인터페이스 정의.
2. 모듈별 저장/조회 adapter 구현.
3. 기존 서비스를 wrapper로 축소해 점진 전환.
4. 안정화 후 중복 코드 제거.

### (d) 테스트/검증 방법
- 샘플 CSV/XLSX E2E 회귀 (성공/부분실패/실패).
- import_job/import_job_run/import_job_event/import_job_metric 정합성 검증.

### (e) 롤백 방법
- feature toggle(`dispatch-mode` 또는 새 플래그)로 old pipeline fallback.
- 전환 단계마다 독립 배포/롤백 가능하도록 작은 커밋 유지.

### (f) 예상 리스크와 완화책
- 리스크: 비동기 처리 순서/트랜잭션 경계 변경에 따른 데이터 불일치.
- 완화: 전환 중 shadow verification(구 파이프라인 결과 비교), 운영 전 부하 리허설.

## Task 7. 인증/시크릿 보안 경로 강화

### (a) 목적/문제
- 문제: 평문 비밀번호 비교/기본 자격증명/기본 암호키 노출.
- 해결: 비밀번호 해시(BCrypt), 기본 계정 제거/초기화 절차화, 암호키 필수 env 주입.
- 효과: 보안 사고 가능성 감소, 운영 감사 대응력 향상.

### (b) 변경 범위
- `admin-was/auth/AuthService.java`
- `admin-was/resources/db/migration/V2__auth_user.sql`
- `admin-was/resources/static/login.html`
- `user-was/resources/application.yml`
- `user-was/security/ConnectionSecretCrypto.java`

### (c) 단계별 작업 절차
1. 신규 해시 컬럼/마이그레이션 및 인증 로직 전환.
2. 시드 계정 전략 재정의(환경별 분리).
3. 기본 키/기본 비밀번호 제거, 시작 시 필수값 검증.

### (d) 테스트/검증 방법
- 로그인 성공/실패/잠금 관련 회귀.
- 환경변수 누락 시 부팅 실패 검증(의도적 fail-fast).

### (e) 롤백 방법
- 인증 전환은 feature flag 또는 dual-read(구/신 컬럼) 기간 운영 후 완전 전환.
- 긴급 시 구 인증 경로 임시 복구 가능하도록 단계적 릴리즈.

### (f) 예상 리스크와 완화책
- 리스크: 기존 계정 로그인 불가.
- 완화: 마이그레이션 스크립트와 운영 전 계정 리허설, 롤백 스크립트 동시 준비.

## Execution policy
- 각 Task는 독립 PR로 분리한다.
- 한 PR은 기능 변경과 구조 변경을 함께 넣지 않는다.
- 각 PR마다 테스트/검증/롤백 절차를 체크리스트로 첨부한다.

