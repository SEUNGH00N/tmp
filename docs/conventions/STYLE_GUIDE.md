# Code Style Guide

적용 범위: `platform-common`, `admin-was`, `user-was`

## 0. Rule levels
- `MUST`: 리뷰에서 반드시 지켜야 하는 규칙
- `SHOULD`: 합리적 사유가 없으면 지켜야 하는 권장 규칙

## A) Architecture and package rules

### A-1. Layer definitions
- `controller`: HTTP 요청/응답 변환, 입력 검증, 인증 컨텍스트 획득
- `service`: 유스케이스 오케스트레이션, 도메인 규칙
- `repository` or `persistence`: DB 접근(SQL/JPA), 외부 저장소 접근
- `domain` (`entity`, value object): 핵심 상태와 불변 규칙
- `dto`: API 경계 객체 (request/response)

### A-2. Dependency direction
- `MUST`: `controller -> service -> repository/persistence` 단방향을 유지한다.
- `MUST`: `controller`는 `JdbcTemplate`/JPA repository를 직접 참조하지 않는다.
- `MUST`: `service`는 HTTP 세부 타입(`HttpServletRequest`, `HttpSession`)을 직접 다루지 않는다.
- `SHOULD`: 모듈 간 공유는 `platform-common`을 통해 수행하고, `admin-was <-> user-was` 직접 참조는 금지한다.

### A-3. DTO / VO / Entity / Model boundaries
- `MUST`: `Entity`는 DB 영속 모델이며 API 응답에 직접 노출하지 않는다.
- `MUST`: API 입출력은 `dto`로만 표현한다.
- `SHOULD`: 단순 파라미터 묶음/불변 규칙은 VO(record)로 분리한다.
- `SHOULD`: DB row 매핑 전용 모델은 `persistence` 하위로 한정한다.

### A-4. Common util/helper policy
- `MUST`: 범용 `*Util`, `*Helper` 클래스 남발 금지.
- `MUST`: 공통 기능은 목적 중심으로 명명된 컴포넌트로 분리한다.
  - 예: `WorkspaceContextResolver`, `BearerTokenExtractor`
- `SHOULD`: 재사용 범위가 모듈 공통이면 `platform-common`으로 이동한다.

## B) Naming rules

### B-1. Conventions
- `MUST`: 클래스/레코드 `PascalCase`, 메서드/변수 `camelCase`, 상수 `UPPER_SNAKE_CASE`.
- `MUST`: 패키지는 소문자 도메인 중심 (`controller`, `service`, `repository`).
- `MUST`: boolean 메서드는 `is/has/can` 접두어 사용.

### B-2. Intent-revealing names
- `MUST`: 행위를 이름에 드러낸다.
  - 좋은 예: `validateUploadQuota`, `createImportApprovalRequest`
  - 나쁜 예: `process`, `handle`, `doWork`
- `MUST`: 금지 패턴
  - `Manager`, `Handler`, `Util`, `Helper`를 맥락 없이 사용하는 이름
  - `data`, `info`, `temp`, `obj` 같은 의미 없는 변수명
- `SHOULD`: CRUD 외 업무 의미가 있으면 도메인 동사 사용 (`dispatch`, `approve`, `reconcile`).

## C) Error and exception handling rules

### C-1. Exception model
- `MUST`: 모듈별 runtime exception은 공통 베이스(예: `ApiException`, `UserWasException`)를 사용한다.
- `MUST`: API 에러는 `ProblemDetail`로 반환하고 최소 필드를 통일한다.
  - `type`, `title`, `status`, `detail`, `requestId`
- `MUST`: 예외를 삼키지 않는다. 복구하지 못하면 원인 예외를 포함해 재던진다.
- `MUST`: `catch (Exception)`은 boundary 계층(비동기 worker entry, controller advice)에서만 허용한다.
- `SHOULD`: 도메인 에러 코드는 `IMP-*`, `AUTH-*` 등 prefix 체계를 문서화해 사용한다.

### C-2. Wrapping rules
- `MUST`: 원인 예외를 래핑할 때 `new XxxException(message, cause)` 패턴을 사용한다.
- `SHOULD`: 외부 예외 메시지를 그대로 사용자에 노출하지 않고 내부 로그와 분리한다.

## D) Logging rules

### D-1. Level baseline
- `ERROR`: 요청 실패/데이터 손상 가능/복구 실패
- `WARN`: 비정상 입력, fallback 사용
- `INFO`: 상태 전이 시작/완료, 중요한 운영 이벤트
- `DEBUG`: SQL 파라미터, 상세 추적 (개발 환경 중심)

### D-2. Mandatory logging policy
- `MUST`: 전역 예외 핸들러에서 `requestId`, `path`, `exceptionType`를 포함해 로그를 남긴다.
- `MUST`: 비동기 import 실패 시 `jobId`, `runId`, `runNo`를 로그 필드로 남긴다.
- `MUST`: 비밀번호/토큰/secret 값은 절대 로그에 남기지 않는다.
- `SHOULD`: 구조화 로그(JSON 또는 key=value)를 우선 사용한다.

### D-3. Correlation ID
- `MUST`: `RequestIdFilter`의 `X-Request-Id`를 모든 API 응답과 에러 로그에서 사용한다.

## E) Testing rules

### E-1. Unit / integration scope
- `MUST`: 서비스 레벨 신규 유스케이스는 최소 1개 단위 테스트를 추가한다.
- `MUST`: API 엔드포인트 추가/변경 시 MockMvc 또는 통합 테스트를 추가한다.
- `MUST`: 버그 수정은 재현 테스트와 함께 머지한다.

### E-2. Naming / structure
- `MUST`: 테스트 메서드는 `given_when_then` 또는 `should...` 패턴을 따른다.
  - 예: `shouldReturn400WhenWorkspaceIdMissing`
- `SHOULD`: Arrange-Act-Assert 블록을 유지한다.

### E-3. Practical target
- 현재 상태: `admin-was` 테스트 1건, `user-was` 테스트 부재.
- `SHOULD`: 커버리지 수치보다 우선 대상 중심으로 확장한다.
  - 우선 1: 인증/인가 경로
  - 우선 2: import 처리 성공/실패 경로
  - 우선 3: billing/quota, governance 승인 경로

## F) Formatter / linter policy

### F-1. Current status
- 저장소에 강제 formatter/linter 설정 파일이 없다.

### F-2. Gradual adoption
- `MUST`: IDE 자동 포맷만으로 끝내지 말고 팀 공통 도구를 도입한다.
- `SHOULD`: 점진 도입 순서
  1. `spotless + google-java-format` (format)
  2. `checkstyle` 최소 규칙 (naming/import/order)
  3. CI에서 changed-file 우선 검사
- `SHOULD`: 기존 코드 전체 일괄 포맷보다, 변경 파일부터 적용한다.

## G) PR and review rules

### G-1. PR size and commit
- `MUST`: PR은 400 LOC 내외를 목표로 분할한다(문서/테스트 제외).
- `MUST`: 리팩토링 PR은 기능 변경 PR과 분리한다.
- `MUST`: 커밋 메시지는 의도를 드러내는 동사형을 사용한다.
  - 예: `refactor(user-was): extract workspace resolver`
  - 예: `test(admin-was): add auth unauthorized cases`

### G-2. Review checklist
- `MUST`: 아래 항목을 PR 본문 체크리스트로 포함한다.
  - 보안: 민감정보/권한 우회/기본 자격증명 포함 여부
  - 성능: N+1/비효율 쿼리/불필요한 동기 I/O
  - 테스트: 신규/변경 경로 테스트 존재 여부
  - 문서: API/규칙/운영 절차 문서 반영 여부

## H) Directory and README composition

### H-1. Documentation layout
- `README.md`: 프로젝트 개요, 실행 방법, 문서 인덱스, 핵심 규칙 링크
- `docs/refactoring/CURRENT_STATE.md`: 현재 구조/품질 진단
- `docs/refactoring/PLAN.md`: 리팩토링 우선순위 실행 계획
- `docs/conventions/STYLE_GUIDE.md`: 본 규칙 문서

### H-2. Maintenance rule
- `MUST`: 구조/규칙 변경 PR은 README 문서 인덱스를 함께 갱신한다.
- `SHOULD`: 규칙 예외가 필요한 경우 PR 설명에 명시하고 후속 정리 이슈를 등록한다.

