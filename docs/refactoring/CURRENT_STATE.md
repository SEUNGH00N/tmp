# Current State Assessment

## 1) Repository structure and responsibilities

### Root summary
- `pom.xml`: Maven aggregator (`packaging=pom`) with 3 modules: `platform-common`, `admin-was`, `user-was`.
- `docker-compose.yml`: local runtime composition (`postgres`, `redis`, `admin/user was blue-green`, `gateway`).
- `deploy/k8s/base/*`: Kubernetes baseline manifests.
- `ERD.md`, `WBS.md`: product and data design docs.

### Module summary
- `platform-common`: shared web contract (`ApiResponse`), request correlation filter (`RequestIdFilter`).
- `admin-was`: admin auth/authorization, import ops, governance, Flyway owner.
- `user-was`: user-facing import/billing/db-connection/governance API + static user portal.

## 2) Languages, frameworks, build tools (evidence-based)

- Build: Maven multi-module (`pom.xml`).
- Language/runtime: Java 17 (`admin-was/pom.xml`, `user-was/pom.xml`, `platform-common/pom.xml`).
- Framework: Spring Boot 3.2.12 (`spring-boot-starter-parent` in each module POM).
- Data access:
  - `admin-was`: Spring Data JPA + JdbcTemplate mixed (`spring-boot-starter-data-jpa`, direct SQL in services).
  - `user-was`: JdbcTemplate 중심 (`spring-boot-starter-jdbc`).
- DB/cache/session: PostgreSQL, Redis, Spring Session Redis (`docker-compose.yml`, module `application.yml`).
- Migration: Flyway enabled in `admin-was`, disabled in `user-was`.
- Test: `spring-boot-starter-test`, 현재 테스트는 `admin-was` 통합 테스트 1건.

## 3) Architecture/layering pattern and dependency direction

### Observed layering
- `admin-was`:
  - Presentation: `controller/*`
  - Application/domain-ish services: `service/*`, `upload/*`, `auth/*`
  - Persistence: `repository/*` (JPA) + service 내부 `JdbcTemplate` SQL
  - Model: `entity/*`, `dto/*`
- `user-was`:
  - Presentation: `controller/*`
  - Service: `service/*`
  - Persistence: dedicated repository 계층 없이 service 내부 `JdbcTemplate` SQL
  - Model: `dto/*`

### Dependency direction status
- 모듈 간 방향은 비교적 단순함: `admin-was`, `user-was` -> `platform-common`.
- 레이어 규칙은 부분적으로만 지켜짐:
  - 컨트롤러 -> 서비스는 유지.
  - 서비스 -> 리포지토리(또는 JdbcTemplate)도 유지.
  - 다만 서비스가 SQL, 상태 전이, 이벤트 적재, 유효성, 저장소 정책까지 함께 수행하며 경계가 넓음.
  - `admin-was`/`user-was` 사이에 유사 비동기 처리 파이프라인이 중복 구현됨.

## 4) Code quality issues (symptom-based with concrete evidence)

## Issue A: 중복 로직 (workspace resolution, import processing pipeline)

### Symptom
- 동일 책임 코드가 여러 컨트롤러/서비스에 반복되어 수정 누락 위험 증가.

### Evidence
- 동일한 `WS_SESSION_KEY` + `resolveWorkspaceId` 구현이 4개 컨트롤러에 반복:
  - `user-was/src/main/java/com/example/excelimport/userwas/controller/UserImportController.java:29,106`
  - `user-was/src/main/java/com/example/excelimport/userwas/controller/UserBillingController.java:22,60`
  - `user-was/src/main/java/com/example/excelimport/userwas/controller/UserDbConnectionController.java:28,68`
  - `user-was/src/main/java/com/example/excelimport/userwas/controller/UserGovernanceController.java:26,69`
- 비동기 import 실행 로직(상태 전이/이벤트/메트릭/실패 처리)이 두 서비스에 거의 동일:
  - `admin-was/src/main/java/com/example/excelimport/service/ImportWorkerService.java:55-121`
  - `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportProcessingService.java:40-108`

## Issue B: 거대 서비스 + 책임 혼합 (Service가 SQL/비즈니스/조립을 동시 수행)

### Symptom
- 단일 클래스가 너무 많은 use-case를 다뤄 변경 영향 범위가 큼.

### Evidence
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java` (321 lines):
  - 생성/중복체크/저장/이벤트/거버넌스/조회/워크스페이스 생성까지 포함 (`49-279`).
- `user-was/src/main/java/com/example/excelimport/userwas/service/UserBillingService.java`:
  - 쿼리/요금제 판정/집계/업서트가 단일 클래스에 결합 (`24-173`).
- `admin-was/src/main/java/com/example/excelimport/service/AdminUserManagementService.java`:
  - 조회/역할 매핑/상태 변경/집계 로직 동시 보유 (파일 302 lines).

## Issue C: 예외 처리 정책 불일치 + 광범위 catch

### Symptom
- 동일 도메인 에러가 모듈별로 다른 형태/정보량으로 응답됨.
- `catch (Exception)` 다수로 원인 분류/복구 전략이 약함.

### Evidence
- admin/user 예외 응답 계약 불일치:
  - admin: `GlobalExceptionHandler`가 `requestId`, `type/title` 포함 ProblemDetail 생성
    - `admin-was/src/main/java/com/example/excelimport/exception/GlobalExceptionHandler.java:23-67`
  - user: `UserWasExceptionHandler`는 status/detail만 반환
    - `user-was/src/main/java/com/example/excelimport/userwas/exception/UserWasExceptionHandler.java:13-20`
- 광범위 `catch (Exception)`:
  - `admin-was/src/main/java/com/example/excelimport/service/ImportWorkerService.java:114`
  - `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportProcessingService.java:100`
  - `user-was/src/main/java/com/example/excelimport/userwas/service/UserImportService.java:303,317`

## Issue D: 보안 기본값/인증 처리 취약 지점

### Symptom
- 개발 편의 설정이 운영 안전장치 없이 남아 있음.

### Evidence
- 평문 비밀번호 비교:
  - `admin-was/src/main/java/com/example/excelimport/auth/AuthService.java:18-21`
- 초기 관리자 계정 평문 시드:
  - `admin-was/src/main/resources/db/migration/V2__auth_user.sql:10-15`
- 로그인 페이지 기본 자격증명 노출:
  - `admin-was/src/main/resources/static/login.html:47,56`
- user-was 암호화 키 기본값 하드코딩:
  - `user-was/src/main/resources/application.yml:27`
  - `user-was/src/main/java/com/example/excelimport/userwas/security/ConnectionSecretCrypto.java:25`

## Issue E: 로깅 가이드/적용 부재

### Symptom
- 에러 경로에서 로그가 거의 없어 운영 추적성이 낮음.

### Evidence
- 실제 `Logger` 사용은 Kafka-ready publisher 2곳 중심:
  - `admin-was/src/main/java/com/example/excelimport/upload/KafkaReadyImportJobEventPublisher.java:14,28`
  - `user-was/src/main/java/com/example/excelimport/userwas/service/KafkaReadyUserImportEventPublisher.java:13,29`
- 전역 예외 핸들러에서 예외 로그 미기록:
  - `admin-was/src/main/java/com/example/excelimport/exception/GlobalExceptionHandler.java:49-52`

## Issue F: 테스트 범위 부족

### Symptom
- 핵심 경로 대비 자동화 테스트가 매우 제한적.

### Evidence
- 테스트 클래스 1개만 존재:
  - `admin-was/src/test/java/com/example/excelimport/ImportControllerIT.java`
- `user-was/src/test/java` 부재.
- 인증/인가/거버넌스/DB connection/security 관련 테스트 확인 불가.

