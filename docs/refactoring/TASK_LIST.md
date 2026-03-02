# Refactoring Task List

# Task Rules
- 한 Task = 한 PR
- 기능 변경 금지 (동작 동일성 유지)
- 리팩토링만 수행 (구조/가독성/중복 제거/규칙 준수)
- 테스트 필수 (최소 회귀 테스트 또는 동작 동일성 검증)
- 규칙 위반 금지 (`README.md`, `docs/conventions/STYLE_GUIDE.md`, `docs/refactoring/STRATEGY.md` 준수)
- 롤백 가능해야 함 (커밋/PR 단위 revert 가능)

## Phase 1 - Low Risk

### RF-001
### Task Name
WorkspaceContextResolver 추출

### Phase
Phase 1 - Low Risk

### 목적
- `user-was` 컨트롤러의 `resolveWorkspaceId` 중복 제거 기반을 만든다.

### 작업 내용
1. `UserImportController`의 workspace 해석 로직을 기준 구현으로 선정한다.
2. 신규 `WorkspaceContextResolver` 클래스를 추가한다.
3. 기존 우선순위(`workspace_id > tenant_id > session`)를 그대로 구현한다.
4. 예외 메시지(`workspace_id is required`)를 기존과 동일하게 유지한다.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserImportController.java`
- 신규 `user-was/src/main/java/com/example/excelimport/userwas/web/WorkspaceContextResolver.java`

### 검증 방법
- resolver 단위 테스트 또는 controller 테스트로 4가지 케이스 검증:
  - `workspace_id` 전달
  - `tenant_id` fallback
  - session fallback
  - 누락 시 400

### 완료 조건
- 공통 resolver 클래스가 생성되어 있고 정책이 기존과 동일하다.
- 테스트에서 4개 케이스가 모두 통과한다.

### RF-002
### Task Name
User 컨트롤러 Resolver 적용 (1차)

### Phase
Phase 1 - Low Risk

### 목적
- 공통 resolver를 적용해 컨트롤러 중복 로직 제거를 시작한다.

### 작업 내용
1. `UserBillingController`에 resolver 주입.
2. private `resolveWorkspaceId` 메서드 제거.
3. 기존 endpoint 시그니처/응답 계약 유지.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserBillingController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/web/WorkspaceContextResolver.java`

### 검증 방법
- billing API 3개(`plan/subscription/summary`) 정상 응답 확인.
- workspace 누락 시 400 유지 확인.

### 완료 조건
- `UserBillingController`에 중복 resolver 메서드가 없다.
- 기존 API 응답 코드가 유지된다.

### RF-003
### Task Name
User 컨트롤러 Resolver 적용 (2차)

### Phase
Phase 1 - Low Risk

### 목적
- 남은 컨트롤러 2개에서 중복 로직 제거.

### 작업 내용
1. `UserDbConnectionController`에 resolver 적용.
2. `UserGovernanceController`에 resolver 적용.
3. 각 컨트롤러의 private resolver 메서드 제거.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserDbConnectionController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserGovernanceController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/web/WorkspaceContextResolver.java`

### 검증 방법
- 각 컨트롤러 API에서 `workspace_id`, `tenant_id`, session fallback 정상 동작 확인.

### 완료 조건
- user 컨트롤러 4곳 모두에서 resolver 중복 코드가 제거된다.

### RF-004
### Task Name
User 예외 응답 스키마 정렬

### Phase
Phase 1 - Low Risk

### 목적
- `admin-was`와 `user-was`의 ProblemDetail 응답 필드 차이를 축소한다.

### 작업 내용
1. `UserWasExceptionHandler`에 `type`, `title`, `requestId` 매핑 추가.
2. 기존 status/detail 유지(확장 방식).
3. 에러 응답 샘플을 문서(`README` 또는 refactoring docs)에 반영.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/exception/UserWasExceptionHandler.java`
- `README.md` 또는 `docs/refactoring/*` (필요 시)

### 검증 방법
- 동일 오류 시나리오에서 admin/user 응답 필드 비교.
- `X-Request-Id` 헤더와 body `requestId` 일치 확인.

### 완료 조건
- user 에러 응답에 `type/title/requestId`가 포함된다.
- 기존 클라이언트 호환성(기존 필드) 유지.

### RF-005
### Task Name
전역 예외 핸들러 로그 기준 적용

### Phase
Phase 1 - Low Risk

### 목적
- STYLE_GUIDE 로깅 규칙(D-2)을 전역 예외 경로에 적용한다.

### 작업 내용
1. `GlobalExceptionHandler`에 구조화 에러 로그 추가.
2. `UserWasExceptionHandler`에 구조화 에러 로그 추가.
3. 로그 필드에 `requestId`, `path`, `exceptionType` 포함.
4. 민감정보 출력 금지 확인.

### 변경 대상
- `admin-was/src/main/java/com/example/excelimport/exception/GlobalExceptionHandler.java`
- `user-was/src/main/java/com/example/excelimport/userwas/exception/UserWasExceptionHandler.java`

### 검증 방법
- 의도적 400/500 요청 후 로그 필드 확인.

### 완료 조건
- 양 모듈 전역 예외 경로에서 표준 필드 로그가 남는다.

### RF-006
### Task Name
비동기 Worker 실패 로그 기준 적용

### Phase
Phase 1 - Low Risk

### 목적
- 비동기 처리 실패 시 운영 추적성을 확보한다.

### 작업 내용
1. `ImportWorkerService` 실패 catch에 로그 추가.
2. `UserImportProcessingService` 실패 catch에 로그 추가.
3. `jobId`, `runId`, `runNo` 필드 포함.

### 변경 대상
- `admin-was/src/main/java/com/example/excelimport/service/ImportWorkerService.java`
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportProcessingService.java`

### 검증 방법
- 실패 케이스 실행 후 로그 필드 확인.

### 완료 조건
- worker 실패 경로 로그에 식별자 필드가 포함된다.

### RF-007
### Task Name
User API 성공 응답 requestId 정렬

### Phase
Phase 1 - Low Risk

### 목적
- user-was 성공 응답의 `meta.requestId`를 채워 request correlation을 일관화한다.

### 작업 내용
1. user 컨트롤러에서 `RequestIdFilter.REQUEST_ID_ATTR`를 읽는 공통 헬퍼를 정의한다.
2. `ApiResponse.success(data)` 호출을 `ApiResponse.success(data, requestId)`로 치환한다.
3. 기존 data/status/message 계약은 변경하지 않고 meta 필드만 보강한다.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserImportController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserBillingController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserDbConnectionController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserGovernanceController.java`

### 검증 방법
- user API 성공 응답에서 `meta.requestId` 존재 확인.
- `X-Request-Id` 헤더와 응답 body의 `meta.requestId` 값 일치 확인.

### 완료 조건
- user-was 성공 응답이 `ApiResponse.success(data, requestId)` 경로를 사용한다.
- 기존 endpoint 상태코드/응답 본문(data)은 유지된다.

## Phase 2 - Medium

### RF-101
### Task Name
UserImport 조회 Query 객체 분리

### Phase
Phase 2 - Medium

### 목적
- `UserImportService`의 조회 SQL 책임을 분리한다.

### 작업 내용
1. list/status/rows/errors 조회 SQL을 Query 클래스로 추출.
2. 결과 매핑 로직을 Query 객체로 이동.
3. 서비스는 Query 호출만 하도록 정리.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java`
- 신규 `user-was/src/main/java/com/example/excelimport/userwas/persistence/UserImportQueryRepository.java` (이름 확정 가능)

### 검증 방법
- 기존 조회 API 회귀 테스트.

### 완료 조건
- `UserImportService` 조회 SQL inline 코드가 제거된다.

### RF-102
### Task Name
UserImport 생성/이벤트 Command 객체 분리

### Phase
Phase 2 - Medium

### 목적
- 생성/이벤트/런 상태 업데이트 SQL을 Command 객체로 분리한다.

### 작업 내용
1. create 경로의 insert/update SQL을 Command 클래스로 추출.
2. dedup/run/event 저장 메서드 분리.
3. 서비스는 orchestration만 담당하도록 축소.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java`
- 신규 `user-was/src/main/java/com/example/excelimport/userwas/persistence/UserImportCommandRepository.java`

### 검증 방법
- import 생성 API 정상 동작 및 dedup 동작 회귀.

### 완료 조건
- `UserImportService`의 생성 관련 SQL inline 코드가 제거된다.

### RF-103
### Task Name
UserBilling SQL 접근 분리

### Phase
Phase 2 - Medium

### 목적
- `UserBillingService`의 SQL+정책 혼합도를 낮춘다.

### 작업 내용
1. 요금제/구독/usage 조회 SQL을 Query 객체로 이동.
2. usage upsert SQL을 Command 객체로 이동.
3. 서비스는 계산/정책 판단 중심으로 정리.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserBillingService.java`
- 신규 `user-was/src/main/java/com/example/excelimport/userwas/persistence/UserBillingQueryRepository.java`
- 신규 `user-was/src/main/java/com/example/excelimport/userwas/persistence/UserBillingCommandRepository.java`

### 검증 방법
- billing API (`plan/subscription/summary`) 회귀 테스트.

### 완료 조건
- `UserBillingService`에서 직접 SQL 호출이 크게 축소된다.

### RF-104
### Task Name
Admin BearerTokenExtractor 도입

### Phase
Phase 2 - Medium

### 목적
- 인증 토큰 파싱 중복을 제거한다.

### 작업 내용
1. 공통 `BearerTokenExtractor` 추가.
2. `AuthInterceptor`, `AdminAuthorizationInterceptor`의 중복 파싱 코드 치환.

### 변경 대상
- 신규 `admin-was/src/main/java/com/example/excelimport/auth/BearerTokenExtractor.java`
- `admin-was/src/main/java/com/example/excelimport/auth/AuthInterceptor.java`
- `admin-was/src/main/java/com/example/excelimport/auth/AdminAuthorizationInterceptor.java`

### 검증 방법
- 인증/인가 인터셉터 동작(정상/401/403) 회귀.

### 완료 조건
- 인터셉터 내 중복 토큰 파싱 private 메서드가 제거된다.

### RF-105
### Task Name
Admin Controller 토큰 파싱 공통화

### Phase
Phase 2 - Medium

### 목적
- 컨트롤러 레벨 토큰 파싱 중복 제거.

### 작업 내용
1. `AuthController`, `AdminGovernanceController`에서 extractor 사용.
2. 중복 `extractBearerToken` private 메서드 제거.

### 변경 대상
- `admin-was/src/main/java/com/example/excelimport/controller/AuthController.java`
- `admin-was/src/main/java/com/example/excelimport/controller/AdminGovernanceController.java`
- `admin-was/src/main/java/com/example/excelimport/auth/BearerTokenExtractor.java`

### 검증 방법
- `/api/v1/auth/me`, governance actor 식별 경로 회귀.

### 완료 조건
- 두 컨트롤러의 중복 토큰 파싱 코드가 제거된다.

### RF-106
### Task Name
`catch(Exception)` 구체 예외로 축소 (UserImportService)

### Phase
Phase 2 - Medium

### 목적
- STYLE_GUIDE C-1 규칙 준수.

### 작업 내용
1. `readBytes`, `sha256Hex`에서 예외 타입을 구체화한다.
2. 원인 예외(`cause`)를 포함한 래핑 방식으로 전환한다.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java`

### 검증 방법
- 파일 읽기 실패/체크섬 실패 시 예외 상태코드 유지 확인.

### 완료 조건
- 해당 메서드에서 `catch(Exception)`이 제거된다.

### RF-107
### Task Name
`catch(Exception)` 구체 예외로 축소 (UserDbConnectionService)

### Phase
Phase 2 - Medium

### 목적
- DB connection test 경로의 예외 분류/추적성을 개선한다.

### 작업 내용
1. connection test의 광범위 catch를 SQL 관련 예외로 분리.
2. 사용자 메시지와 내부 원인 로그를 분리.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserDbConnectionService.java`

### 검증 방법
- 잘못된 호스트/인증 실패/타임아웃 케이스 검증.

### 완료 조건
- `UserDbConnectionService`에서 `catch(Exception)`이 제거된다.

### RF-108
### Task Name
UserImport 생성 트랜잭션 경계 명시화

### Phase
Phase 2 - Medium

### 목적
- `UserImportService.create`의 다중 DB 업데이트를 단일 트랜잭션 경계로 보호한다.

### 작업 내용
1. `create` 경로의 DB 변경 구간(잡 생성/run/event/dedup)을 트랜잭션 경계로 묶는다.
2. 외부 publish 호출 시점이 기존 동작과 충돌하지 않도록 경계를 분리/정리한다.
3. 예외 메시지/상태코드/기존 fallback 정책은 유지한다.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java`

### 검증 방법
- 실패 주입으로 부분 반영(중간 update만 반영) 여부 확인.
- 기존 import 생성 API 응답/상태코드 회귀 확인.

### 완료 조건
- `create` 경로에 명시적 트랜잭션 경계가 존재한다.
- 부분 반영 없이 롤백되는 것이 검증된다.

## Phase 3 - High

### RF-201
### Task Name
Import Pipeline 공통 인터페이스 정의

### Phase
Phase 3 - High

### 목적
- admin/user 중복 파이프라인 통합을 위한 비파괴 인터페이스를 먼저 마련한다.

### 작업 내용
1. 상태전이/이벤트/메트릭 lifecycle 인터페이스 추가.
2. 기존 worker에서 아직 동작 변경 없이 인터페이스 타입만 참조 가능하게 준비.

### 변경 대상
- 신규 `platform-common/src/main/java/com/example/excelimport/common/imports/*` (경로 확정 가능)
- `admin-was/src/main/java/com/example/excelimport/service/ImportWorkerService.java`
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportProcessingService.java`

### 검증 방법
- 컴파일/기존 테스트 통과.

### 완료 조건
- 공통 인터페이스가 생성되고 기존 동작은 동일하다.

### RF-202
### Task Name
Admin Worker Adapter 도입

### Phase
Phase 3 - High

### 목적
- admin 쪽 파이프라인을 adapter 구조로 분리해 공통화 전환 리스크를 낮춘다.

### 작업 내용
1. `ImportWorkerService`의 DB 이벤트/메트릭 기록 부분을 adapter로 추출.
2. worker 본문은 orchestration 중심으로 축소.

### 변경 대상
- `admin-was/src/main/java/com/example/excelimport/service/ImportWorkerService.java`
- 신규 `admin-was/src/main/java/com/example/excelimport/service/imports/*`

### 검증 방법
- admin import 성공/실패 회귀, 이벤트/메트릭 row 생성 확인.

### 완료 조건
- worker 내부의 DB 기록 코드가 adapter로 분리된다.

### RF-203
### Task Name
User Worker Adapter 도입

### Phase
Phase 3 - High

### 목적
- user 쪽 파이프라인도 같은 형태로 맞춰 공통화 가능 상태를 만든다.

### 작업 내용
1. `UserImportProcessingService`의 상태/이벤트/메트릭 업데이트 로직을 adapter로 추출.
2. 서비스는 파싱/검증 흐름 중심으로 유지.

### 변경 대상
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportProcessingService.java`
- 신규 `user-was/src/main/java/com/example/excelimport/userwas/service/imports/*`

### 검증 방법
- user import 성공/실패 회귀.

### 완료 조건
- user worker 내부 DB 업데이트 코드가 adapter로 분리된다.

### RF-204
### Task Name
Import Pipeline 전환 토글 경로 정리

### Phase
Phase 3 - High

### 목적
- 고위험 구조 전환의 즉시 롤백 경로를 확보한다.

### 작업 내용
1. dispatch mode(기존 `sync`/`kafka`) 기준으로 old/new 경로 선택 지점을 명확히 분리.
2. 기본값은 기존 경로 유지.
3. 롤백 절차를 문서에 추가.

### 변경 대상
- `admin-was/src/main/resources/application.yml`
- `user-was/src/main/resources/application.yml`
- 관련 publisher/worker wiring 클래스
- `docs/refactoring/IMPLEMENTATION_PLAN.md` (필요 시)

### 검증 방법
- 토글별 부팅/요청 처리 경로 확인.

### 완료 조건
- 구성값 변경만으로 기존 경로로 복귀 가능하다.

### RF-205
### Task Name
Auth 해시 전환 준비(비파괴)

### Phase
Phase 3 - High

### 목적
- 보안 강화 전환을 기능 중단 없이 준비한다.

### 작업 내용
1. 비밀번호 해시 컬럼 추가 마이그레이션 작성.
2. `AuthService`를 dual-read 구조(기존/신규)로 준비.
3. 기본 시드 계정 처리 방식을 문서화한다.

### 변경 대상
- `admin-was/src/main/resources/db/migration/*`
- `admin-was/src/main/java/com/example/excelimport/auth/AuthService.java`
- `docs/refactoring/IMPLEMENTATION_PLAN.md` 또는 `docs/refactoring/STRATEGY.md`

### 검증 방법
- 기존 계정 로그인 회귀.
- 신규 해시 경로 단위 테스트 추가.

### 완료 조건
- 해시 전환 준비가 완료되고 기존 로그인 동작이 유지된다.

### RF-206
### Task Name
기본 자격증명/기본 시크릿 제거 준비(비파괴)

### Phase
Phase 3 - High

### 목적
- 기본 자격증명/기본 키 노출 리스크 제거를 위한 준비 작업 수행.

### 작업 내용
1. `login.html` 기본 계정 표시/프리필 제거.
2. `user-was` connection secret 기본값 제거 전략을 문서화.
3. 환경변수 누락 경고 로그 또는 startup check(비차단) 추가.

### 변경 대상
- `admin-was/src/main/resources/static/login.html`
- `user-was/src/main/resources/application.yml`
- `user-was/src/main/java/com/example/excelimport/userwas/security/ConnectionSecretCrypto.java`
- `docs/refactoring/STRATEGY.md` (필요 시)

### 검증 방법
- 로그인 페이지 동작 확인.
- 환경변수 누락 시 경고/체크 동작 확인.

### 완료 조건
- 기본 자격증명 노출이 제거되고 운영 전환 체크 경로가 문서화된다.
