# Refactoring Strategy

## 1. Goal
- 문서(`README.md`, `docs/conventions/STYLE_GUIDE.md`, `docs/refactoring/*.md`)에서 정의된 규칙을 실제 코드에 점진 적용한다.
- 기능 안정성을 해치지 않고, 작은 단위로 되돌릴 수 있는 방식으로 진행한다.

## 2. Document-to-code mismatch summary

### 2.1 Error response standard mismatch
- 규칙: STYLE_GUIDE C-1 (`ProblemDetail` + `type/title/status/detail/requestId`)
- 코드:
  - admin: `admin-was/.../GlobalExceptionHandler.java`는 `requestId/type/title` 포함
  - user: `user-was/.../UserWasExceptionHandler.java`는 간소 응답

### 2.2 Logging standard mismatch
- 규칙: STYLE_GUIDE D-2 (예외/비동기 실패 로그 필수)
- 코드:
  - `GlobalExceptionHandler`와 `UserWasExceptionHandler`에 예외 로그 없음
  - worker 실패 경로도 DB 이벤트 저장 위주, 애플리케이션 로그 부족

### 2.3 Exception handling rule mismatch
- 규칙: STYLE_GUIDE C-1 (`catch(Exception)` boundary 제한)
- 코드:
  - `UserImportService.readBytes/sha256Hex` 등 비-boundary에서도 광범위 catch 사용

### 2.4 Layer simplification gap
- 규칙: STYLE_GUIDE A-1/A-2 (service 오케스트레이션 중심)
- 코드:
  - `UserImportService`, `UserBillingService`, `AdminUserManagementService`가 SQL+정책+workflow를 모두 포함

### 2.5 Security baseline gap
- 규칙: STYLE_GUIDE G-2 보안 체크리스트
- 코드:
  - 평문 비밀번호 비교 (`AuthService`)
  - 기본 관리자 계정/비밀번호 seed (`V2__auth_user.sql`)
  - 기본 암호키 설정 (`user-was application.yml`)

## 3. 신규 코드 규칙 적용 방법
- 모든 신규 클래스/메서드는 STYLE_GUIDE `MUST`를 PR 체크리스트로 강제한다.
- PR 템플릿에 아래 항목을 추가한다:
  - 레이어 준수 확인 (`controller -> service -> repository/persistence`)
  - 예외 응답 규격 확인 (`requestId` 포함)
  - 민감정보 로그 노출 점검
  - 테스트 추가 여부
- 신규 API는 구현 전에 오류 응답 스키마를 먼저 정의한다.

## 4. 기존 코드 점진적 개선 방법
- Boy Scout Rule: 수정 파일에서만 규칙 위반을 함께 정리한다.
- 대형 파일은 한 번에 재작성하지 않고, "중복 제거 -> 책임 분리 -> 정책 통일" 순서로 쪼개서 처리한다.
- 공통화 대상은 먼저 어댑터/헬퍼로 추출하고, 이후 호출부를 단계 교체한다.

## 5. 대규모 수정 방지 전략
- 한 PR은 한 리팩토링 축만 다룬다.
  - 예: workspace resolver 공통화만 수행, 보안 개선은 별도 PR
- 기능 동작을 바꾸는 변경은 플래그 또는 dual-path 방식으로 도입한다.
- 구조 변경 전/후 API 응답 스냅샷 비교를 기본 검증으로 사용한다.

## 6. 기능 변경과 리팩토링 분리 원칙
- 리팩토링 PR:
  - 기능/스키마 의미 변경 금지
  - 동작 동일성 회귀 테스트 필수
- 기능 PR:
  - 구조 정리는 최소화, 필요한 경우 선행 리팩토링 PR을 먼저 머지
- 긴급 버그 수정:
  - 버그 fix PR과 후속 정리 PR을 분리 등록

## 7. Rollout sequence
1. Phase 1: 중복/응답표준/로깅 기준 정리
2. Phase 2: 서비스 책임 분리, 예외 상세화
3. Phase 3: 공통 파이프라인, 보안 강화

## 8. MUST refactoring rules (10)
1. 기능 변경과 리팩토링을 같은 PR에 넣지 않는다.
2. 한 PR은 한 리팩토링 목표만 다룬다.
3. 컨트롤러에서 `JdbcTemplate`/Repository 직접 호출을 금지한다.
4. `catch(Exception)`은 boundary 계층 외 사용하지 않는다.
5. 예외를 래핑할 때 원인 예외(`cause`)를 반드시 유지한다.
6. API 오류 응답은 `ProblemDetail` + `requestId`를 포함한다.
7. 민감정보(비밀번호, 토큰, secret)는 로그/응답에 남기지 않는다.
8. 중복 로직은 2곳 이상에서 발견되면 공통 컴포넌트로 추출한다.
9. 거대 클래스 분해는 동작 동일성 테스트를 먼저 고정한 후 수행한다.
10. 리팩토링으로 문서와 코드가 달라지면 같은 PR에서 문서를 갱신한다.

