# Refactoring Implementation Plan

기준 문서:
- `README.md`
- `docs/conventions/STYLE_GUIDE.md`
- `docs/refactoring/CURRENT_STATE.md`
- `docs/refactoring/PLAN.md`

기준 코드 근거:
- `user-was/src/main/java/com/example/excelimport/userwas/controller/*`
- `user-was/src/main/java/com/example/excelimport/userwas/exception/UserWasExceptionHandler.java`
- `admin-was/src/main/java/com/example/excelimport/exception/GlobalExceptionHandler.java`
- `admin-was/src/main/java/com/example/excelimport/service/ImportWorkerService.java`
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportProcessingService.java`
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java`

## Phase 1 (Low Risk)

### Task 1. Workspace Context Resolver 공통화

목적:
- `resolveWorkspaceId` 중복 제거 및 정책 단일화.

변경 대상:
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserImportController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserBillingController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserDbConnectionController.java`
- `user-was/src/main/java/com/example/excelimport/userwas/controller/UserGovernanceController.java`
- 신규 `user-was/src/main/java/com/example/excelimport/userwas/web/WorkspaceContextResolver.java`

Step 1:
- 현재 4개 컨트롤러의 `resolveWorkspaceId` 로직을 공통 클래스로 추출한다.

Step 2:
- 컨트롤러에서 private 메서드를 제거하고 resolver 주입으로 교체한다.

Step 3:
- 에러 메시지/우선순위(`workspace_id > tenant_id > session`)를 기존과 동일하게 유지한다.

검증 방법:
- `workspace_id`, `tenant_id`, 세션 fallback, 누락(400) 케이스의 MockMvc 테스트 추가.

롤백 방법:
- resolver 도입 커밋 revert 후 기존 private 메서드 복원.

### Task 2. User/Admin 에러 응답 계약 통일

목적:
- README/STYLE_GUIDE의 ProblemDetail 표준을 user-was에도 동일하게 적용.

변경 대상:
- `user-was/src/main/java/com/example/excelimport/userwas/exception/UserWasExceptionHandler.java`
- (필요시) `platform-common/src/main/java/com/example/excelimport/common/web/RequestIdFilter.java`

Step 1:
- user-was 예외 응답에 `type`, `title`, `requestId`를 포함한다.

Step 2:
- admin-was와 user-was의 오류 JSON 스키마 차이를 문서화하고 동기화한다.

Step 3:
- 기존 클라이언트 호환성을 위해 필드 추가 방식으로 적용한다.

검증 방법:
- 동일 오류 케이스를 양 모듈에서 호출하여 응답 필드 비교.

롤백 방법:
- UserWasExceptionHandler 단일 파일 revert.

### Task 3. 예외/실패 로깅 최소 기준 도입

목적:
- 장애 추적성 확보 (`requestId`, `jobId`, `runId`, `runNo`).

변경 대상:
- `admin-was/src/main/java/com/example/excelimport/exception/GlobalExceptionHandler.java`
- `user-was/src/main/java/com/example/excelimport/userwas/exception/UserWasExceptionHandler.java`
- `admin-was/src/main/java/com/example/excelimport/service/ImportWorkerService.java`
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportProcessingService.java`

Step 1:
- 전역 예외 핸들러에 구조화 에러 로그 추가.

Step 2:
- 비동기 worker 실패 catch 블록에 식별자 포함 에러 로그 추가.

Step 3:
- 민감정보(비밀번호/토큰/secret) 마스킹 검증.

검증 방법:
- 의도적 실패 요청 생성 후 로그 필드와 마스킹 여부 확인.

롤백 방법:
- 로깅 추가 커밋 단위 revert.

## Phase 2 (Medium)

### Task 4. UserImportService 책임 분리 (Query/Command 분해)

목적:
- 거대 서비스 분해로 변경 영향도 축소, 테스트 용이성 향상.

변경 대상:
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java`
- 신규 `user-was/src/main/java/com/example/excelimport/userwas/persistence/*`

Step 1:
- 조회 SQL과 변경 SQL을 Query/Command 클래스로 분리.

Step 2:
- 서비스는 워크플로우 오케스트레이션만 남기고 직접 SQL 호출을 축소.

Step 3:
- 기존 API 계약과 DB 스키마는 유지한다.

검증 방법:
- import 생성/조회/rows/errors API 회귀 테스트.

롤백 방법:
- Query/Command 추출 PR revert.

### Task 5. 인증 토큰 파싱/사용자 식별 공통화

목적:
- `extractBearerToken` 중복 제거, 인증 정책 일관성 확보.

변경 대상:
- `admin-was/src/main/java/com/example/excelimport/auth/AuthInterceptor.java`
- `admin-was/src/main/java/com/example/excelimport/auth/AdminAuthorizationInterceptor.java`
- `admin-was/src/main/java/com/example/excelimport/controller/AuthController.java`
- `admin-was/src/main/java/com/example/excelimport/controller/AdminGovernanceController.java`
- 신규 `admin-was/src/main/java/com/example/excelimport/auth/BearerTokenExtractor.java`

Step 1:
- 공통 BearerTokenExtractor 추가.

Step 2:
- 중복 private 메서드를 extractor 호출로 치환.

Step 3:
- 세션 우선/토큰 fallback 정책을 테스트로 고정.

검증 방법:
- 인증 성공/실패/권한부족 회귀 테스트.

롤백 방법:
- extractor 도입 PR revert.

### Task 6. catch(Exception) 범위 축소

목적:
- STYLE_GUIDE의 예외 처리 규칙(`catch(Exception)` 제한) 준수.

변경 대상:
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java` (`readBytes`, `sha256Hex`)
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserDbConnectionService.java`

Step 1:
- `IOException`, `NoSuchAlgorithmException`, `SQLException` 등 구체 예외로 분기.

Step 2:
- 원인 예외를 포함한 도메인 예외 래핑으로 전환.

Step 3:
- 사용자 노출 메시지와 내부 원인 로그를 분리.

검증 방법:
- 실패 주입 테스트(파일 읽기 실패, DB connection 실패).

롤백 방법:
- 메서드 단위 revert.

## Phase 3 (High)

### Task 7. Import 비동기 파이프라인 공통 코어화

목적:
- admin/user 중복 파이프라인 제거.

변경 대상:
- `admin-was/src/main/java/com/example/excelimport/service/ImportWorkerService.java`
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportProcessingService.java`
- `admin-was/src/main/java/com/example/excelimport/upload/*`
- `user-was/src/main/java/com/example/excelimport/userwas/service/*EventPublisher*`

Step 1:
- 공통 lifecycle 인터페이스(상태전이/이벤트/메트릭) 설계.

Step 2:
- 모듈별 persistence adapter 구현.

Step 3:
- 단계적 전환(feature flag) 후 중복 코드 제거.

검증 방법:
- CSV/XLSX 성공/부분실패/실패 E2E.
- `import_job`, `import_job_run`, `import_job_event`, `import_job_metric` 정합성 확인.

롤백 방법:
- 플래그 기반으로 기존 worker 경로로 즉시 복귀.

### Task 8. 인증/시크릿 보안 강화

목적:
- 평문 인증/기본 자격증명/기본 암호키 리스크 제거.

변경 대상:
- `admin-was/src/main/java/com/example/excelimport/auth/AuthService.java`
- `admin-was/src/main/resources/db/migration/V2__auth_user.sql`
- `admin-was/src/main/resources/static/login.html`
- `user-was/src/main/resources/application.yml`
- `user-was/src/main/java/com/example/excelimport/userwas/security/ConnectionSecretCrypto.java`

Step 1:
- 비밀번호 해시(BCrypt) 기반 인증으로 전환.

Step 2:
- 기본 계정/기본 비밀번호/기본 키 제거 및 환경변수 강제.

Step 3:
- 마이그레이션 및 운영 전환 절차(dual-read 기간) 적용.

검증 방법:
- 로그인/토큰/세션 회귀 + 환경변수 누락 fail-fast 검증.

롤백 방법:
- 인증 전환 직전 스냅샷 기준으로 DB/코드 동시 롤백.

