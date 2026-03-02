# Framework Structural Fix List

## 운영 규칙
- 이 문서는 조기 발견된 구조적 프레임워크/아키텍처 문제만 관리한다.
- 각 항목은 1 PR 단위로 쪼개서 처리한다.
- `docs/refactoring/TASK_LIST.md` 규칙(한 Task=한 PR, 기능 변경 금지, 롤백 가능)을 동일 적용한다.
- 이미 `TASK_LIST`에 존재하는 작업은 중복 등록하지 않고 참조만 남긴다.

## Index (요약)
- Severity 요약:
  - High: 3
  - Medium: 6
  - Low: 1
- 모듈별 요약:
  - `user-was`: 9
  - `admin-was`: 7
  - `platform-common`: 2

## Issues

### FS-001: User API 성공 응답의 requestId 누락
- Severity: High
- Category: ARCH-09
- Affected Modules: user-was, platform-common
- Evidence:
  - file: `platform-common/src/main/java/com/example/excelimport/common/web/ApiResponse.java` (`success(data)`는 `meta.requestId=null`)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/controller/UserImportController.java` (`ApiResponse.success(data)` 사용)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/controller/UserBillingController.java` (`ApiResponse.success(...)` 사용)
  - file: `admin-was/src/main/java/com/example/excelimport/controller/ImportController.java` (admin은 `requestId(request)` 전달)
- Why it matters:
  - user-was 성공 응답은 request correlation이 비어 있어 장애 추적/로그 상관관계가 약해진다.
- Fix direction (non-breaking):
  - user-was 컨트롤러에서 `RequestIdFilter` attribute를 읽어 `ApiResponse.success(data, requestId)`를 사용한다.
  - 기존 payload 구조는 유지하고 `meta.requestId`만 채운다.
- Proposed Tasks:
  - NEW: `RF-007` (TASK_LIST 반영)
- Validation:
  - user API 성공 응답에서 `meta.requestId`가 비어있지 않은지 확인
  - `X-Request-Id` 헤더와 값 일치 확인
- Rollback:
  - 컨트롤러 변경 커밋 revert

### FS-002: UserImportService.create 트랜잭션 경계 부재
- Severity: High
- Category: ARCH-03
- Affected Modules: user-was
- Evidence:
  - file: `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java` (`create` 메서드에 `@Transactional` 없음)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java` (create 내부 다중 `jdbcTemplate.update`와 이벤트 publish 수행)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/service/UserGovernanceService.java` (동일 모듈 내 쓰기 메서드는 `@Transactional` 사용)
- Why it matters:
  - 중간 실패 시 부분 반영(잡 생성만 되고 run/event 누락 등) 가능성으로 데이터 정합성 리스크가 있다.
- Fix direction (non-breaking):
  - `create` 경로 DB 변경 구간을 명시적 트랜잭션 경계로 감싼다.
  - publish는 트랜잭션 이후 실행되도록 경계를 분리해 기존 비즈니스 결과를 유지한다.
- Proposed Tasks:
  - NEW: `RF-108` (TASK_LIST 반영)
- Validation:
  - 실패 주입 테스트로 DB 부분 반영 여부 확인
  - 성공 케이스에서 기존 row 생성 수/상태 동일성 확인
- Rollback:
  - 트랜잭션 어노테이션/분리 코드 revert

### FS-003: 운영 기본 자격증명/시크릿 기본값 위험
- Severity: High
- Category: ARCH-05
- Affected Modules: admin-was, user-was
- Evidence:
  - file: `admin-was/src/main/resources/db/migration/V2__auth_user.sql` (기본 admin 비밀번호 seed)
  - file: `admin-was/src/main/resources/static/login.html` (기본 계정 표시/프리필)
  - file: `admin-was/src/main/java/com/example/excelimport/auth/AuthService.java` (평문 비교)
  - file: `user-was/src/main/resources/application.yml` (`connection-secret-base64` 기본값)
- Why it matters:
  - 운영 환경 오배포 시 즉시 보안 사고로 이어질 수 있다.
- Fix direction (non-breaking):
  - 기존 계획대로 해시 전환 준비 + 기본값 제거 + 환경변수 강제 전략 적용.
- Proposed Tasks:
  - RF-205
  - RF-206
- Validation:
  - 기본값 제거 후 로그인/암복호화 경로 회귀 확인
- Rollback:
  - 전환 이전 커밋 revert 또는 dual-read fallback

### FS-004: 토큰 파싱 중복으로 인가 경로 유지보수 리스크
- Severity: Medium
- Category: ARCH-06
- Affected Modules: admin-was
- Evidence:
  - file: `admin-was/src/main/java/com/example/excelimport/auth/AuthInterceptor.java` (private `extractBearerToken`)
  - file: `admin-was/src/main/java/com/example/excelimport/auth/AdminAuthorizationInterceptor.java` (중복 파싱)
  - file: `admin-was/src/main/java/com/example/excelimport/controller/AuthController.java` (중복 파싱)
  - file: `admin-was/src/main/java/com/example/excelimport/controller/AdminGovernanceController.java` (중복 파싱)
- Why it matters:
  - 파싱 정책 변경 시 누락/불일치로 인증 우회 또는 오탐 가능성이 증가한다.
- Fix direction (non-breaking):
  - 공통 extractor 도입 후 호출부 치환.
- Proposed Tasks:
  - RF-104
  - RF-105
- Validation:
  - 401/403/정상 인증 회귀 테스트
- Rollback:
  - extractor 도입 PR revert

### FS-005: admin/user 예외 응답 스키마 불일치
- Severity: Medium
- Category: ARCH-04
- Affected Modules: admin-was, user-was
- Evidence:
  - file: `admin-was/src/main/java/com/example/excelimport/exception/GlobalExceptionHandler.java` (`type/title/requestId` 세팅)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/exception/UserWasExceptionHandler.java` (간소 `ProblemDetail`)
- Why it matters:
  - 프론트/운영툴에서 에러 파싱 분기 증가, 문제 추적 비용 상승.
- Fix direction (non-breaking):
  - user-was도 동일 핵심 필드 채우기(추가 방식).
- Proposed Tasks:
  - RF-004
- Validation:
  - 동일 오류 입력으로 응답 JSON 필드 비교
- Rollback:
  - 핸들러 변경 revert

### FS-006: 서비스 계층 SQL inline 과다
- Severity: Medium
- Category: ARCH-08
- Affected Modules: user-was, admin-was
- Evidence:
  - file: `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java` (대량 inline SQL)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/service/UserBillingService.java` (쿼리/업서트 혼재)
  - file: `admin-was/src/main/java/com/example/excelimport/service/AdminUserManagementService.java` (JdbcTemplate 혼재)
- Why it matters:
  - 매핑 중복/수정 영향 확대/테스트 복잡도 증가.
- Fix direction (non-breaking):
  - Query/Command/persistence 계층으로 분리.
- Proposed Tasks:
  - RF-101
  - RF-102
  - RF-103
- Validation:
  - API 회귀 + SQL 매핑 테스트
- Rollback:
  - 추출 클래스 도입 커밋 revert

### FS-007: Workspace/Tenant Context 해석 중복 잔존
- Severity: Medium
- Category: ARCH-09
- Affected Modules: user-was
- Evidence:
  - file: `user-was/src/main/java/com/example/excelimport/userwas/controller/UserDbConnectionController.java` (private `resolveWorkspaceId` 유지)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/controller/UserGovernanceController.java` (private `resolveWorkspaceId` 유지)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/web/WorkspaceContextResolver.java` (공통 resolver 생성 완료)
- Why it matters:
  - 정책 변경 시 일부 컨트롤러 누락 가능성.
- Fix direction (non-breaking):
  - 남은 컨트롤러를 resolver로 치환.
- Proposed Tasks:
  - RF-003
- Validation:
  - 두 컨트롤러에 대해 fallback 순서/예외 메시지 회귀
- Rollback:
  - 컨트롤러 단위 revert

### FS-008: dispatch-mode 값 오설정 시 Bean 구성 실패 위험
- Severity: Medium
- Category: ARCH-02
- Affected Modules: admin-was, user-was
- Evidence:
  - file: `admin-was/src/main/java/com/example/excelimport/upload/LocalAsyncImportJobEventPublisher.java` (`sync`/`matchIfMissing`)
  - file: `admin-was/src/main/java/com/example/excelimport/upload/KafkaReadyImportJobEventPublisher.java` (`kafka` 조건)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/service/LocalAsyncUserImportEventPublisher.java` (`sync`/`matchIfMissing`)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/service/KafkaReadyUserImportEventPublisher.java` (`kafka` 조건)
  - file: `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java` (`UserImportEventPublisher` 주입 필수)
- Why it matters:
  - 허용되지 않은 값 설정 시 Publisher Bean 미생성으로 부팅 실패 가능.
- Fix direction (non-breaking):
  - dispatch-mode를 enum 기반 설정 검증으로 제한하고 startup validation 추가.
- Proposed Tasks:
  - NEW: FS-008-01 (추가 Task 제안, TASK_LIST 미반영)
- Validation:
  - `sync`, `kafka`, invalid 값으로 부팅 테스트
- Rollback:
  - 설정 검증 코드 revert

### FS-009: 테스트 커버리지 공백으로 구조 변경 리스크 상승
- Severity: Medium
- Category: ARCH-10
- Affected Modules: admin-was, user-was
- Evidence:
  - file: `admin-was/src/test/java/com/example/excelimport/ImportControllerIT.java` (admin 주요 테스트 1건)
  - file: `user-was/src/test/java/com/example/excelimport/userwas/web/WorkspaceContextResolverTest.java`
  - file: `user-was/src/test/java/com/example/excelimport/userwas/controller/UserBillingControllerTest.java`
  - 증상: auth/인가/async worker 핵심 경로 테스트 부재
- Why it matters:
  - 리팩토링 시 회귀 검출이 늦어지고 PR 리스크가 커진다.
- Fix direction (non-breaking):
  - 인증/인가, 예외 스키마, worker 실패 경로 회귀 테스트를 우선 추가.
- Proposed Tasks:
  - NEW: FS-009-01 (추가 Task 제안, TASK_LIST 미반영)
- Validation:
  - 모듈별 테스트 실행 + 실패 주입 테스트
- Rollback:
  - 테스트 코드만 분리 revert 가능

### FS-010: Docker 프로파일 선언 대비 리소스 분리 부재
- Severity: Low
- Category: ARCH-05
- Affected Modules: admin-was, user-was
- Evidence:
  - file: `docker-compose.yml` (`SPRING_PROFILES_ACTIVE: docker`)
  - file: `admin-was/src/main/resources/application.yml`, `user-was/src/main/resources/application.yml` (단일 파일 운용)
  - 증상: `application-docker.yml` 없음
- Why it matters:
  - 운영/개발 설정 차이가 늘어날수록 변경 충돌 가능성 증가.
- Fix direction (non-breaking):
  - `application-docker.yml`를 생성해 docker 전용 키만 분리하고 기본값은 유지.
- Proposed Tasks:
  - NEW: FS-010-01 (추가 Task 제안, TASK_LIST 미반영)
- Validation:
  - docker profile로 부팅 후 기존 동작 동일성 확인
- Rollback:
  - profile 파일 추가 커밋 revert

