# Admin-WAS 고도화 계획 (권한/사용자/향후 기능)

## 1. 목적
- 관리자 콘솔에서 운영자가 실제로 사용할 수 있는 권한 체계와 사용자 관리 기능을 단계적으로 완성한다.
- 현재 MVP 상태에서 운영 안정성, 보안성, 감사 가능성을 확보하는 것을 목표로 한다.

## 2. 우선순위 원칙
- P0: 보안/권한 우회 방지, 운영 중단 방지
- P1: 운영 효율(관리 UI/검색/필터), 감사 추적성
- P2: 고도화(자동화, 분석, 워크플로우 확장)

## 3. 권한 관리 메뉴 고도화

### 3.1 메뉴 구조 (정보구조)
- `권한 관리`
- `역할(Role) 관리`
- `권한 정책(Policy) 관리`
- `역할-권한 매핑`
- `권한 변경 이력`

### 3.2 P0 범위
- 역할 기본 세트 확정: `SUPER_ADMIN`, `ADMIN`, `OPERATOR`, `VIEWER`
- API 권한 매핑 표 확정 (endpoint + method + required role)
- 서버 측 강제 인가 적용 (UI 숨김만으로 통제하지 않음)
- 권한 없는 호출에 대한 표준 응답 정리 (RFC7807 + 내부 코드)

### 3.3 P1 범위
- 역할 생성/수정/비활성화 UI
- 역할별 권한 템플릿 기능 (예: ReadOnly, Operations)
- 역할 변경 사유 입력 + 감사 로그 저장
- 변경 Diff 화면 (변경 전/후 권한 비교)

### 3.4 P2 범위
- 정책 조건부 권한 (tenant 범위, 시간대 제한, IP 대역)
- 긴급 권한 부여(만료 시간 포함) 워크플로우
- 정책 시뮬레이터 (특정 사용자 기준 접근 가능 API 미리보기)

## 4. 사용자 관리 메뉴 고도화

### 4.1 메뉴 구조 (정보구조)
- `사용자 관리`
- `사용자 목록`
- `사용자 상세`
- `역할 할당`
- `계정 상태 관리`
- `접속/활동 이력`

### 4.2 P0 범위
- 사용자 목록: username/display_name/active/role/created_at
- 사용자 생성/활성-비활성 토글
- 초기 비밀번호 정책 및 강제 변경 플래그
- 잠금 계정 해제 기능

### 4.3 P1 범위
- 검색/필터/정렬/페이지네이션 강화
- 대량 작업: 역할 일괄 변경, 비활성 일괄 처리
- 상세 화면에 최근 활동 로그/최근 실패 로그인 노출
- CSV 내보내기 (운영 리포트)

### 4.4 P2 범위
- 사용자 그룹(팀) 기반 권한 부여
- 승인 기반 계정 프로비저닝 (요청/승인/반려)
- 외부 IdP 연동 대비 필드 확장 (OIDC subject 등)

## 5. 데이터 모델/백엔드 고도화 제안

### 5.1 신규 테이블 (권장)
- `role`
  - `id`, `name`, `description`, `active`, `created_at`
- `permission`
  - `id`, `resource`, `action`, `description`
- `role_permission`
  - `role_id`, `permission_id`, `created_at`
- `user_role`
  - `user_id`, `role_id`, `created_at`, `assigned_by`
- `audit_log`
  - `id`, `actor`, `target_type`, `target_id`, `action`, `before_json`, `after_json`, `created_at`

### 5.2 기존 `app_user` 확장 필드 (권장)
- `password_changed_at`
- `must_change_password`
- `failed_login_count`
- `locked_until`
- `last_login_at`

### 5.3 API 초안
- `GET /api/v1/admin/roles`
- `POST /api/v1/admin/roles`
- `PUT /api/v1/admin/roles/{roleId}`
- `GET /api/v1/admin/permissions`
- `PUT /api/v1/admin/roles/{roleId}/permissions`
- `GET /api/v1/admin/users`
- `POST /api/v1/admin/users`
- `PUT /api/v1/admin/users/{userId}/status`
- `PUT /api/v1/admin/users/{userId}/roles`
- `GET /api/v1/admin/audit-logs`

## 6. Admin UI 화면 제안

### 6.1 사용자 목록 화면
- 상단: 검색바(아이디/이름), 필터(active/role), 생성 버튼
- 테이블: Username, Display Name, Role, Active, Last Login, Actions
- 액션: 상세, 비활성화, 비밀번호 초기화

### 6.2 사용자 상세 화면
- 기본 정보 카드
- 역할 할당 카드
- 보안 상태 카드(잠금/실패횟수/마지막 로그인)
- 최근 활동/감사로그 탭

### 6.3 역할 관리 화면
- 좌측 역할 목록, 우측 권한 매트릭스(resource x action)
- 저장 시 변경 요약(추가/삭제 권한) 모달

## 7. 단계별 실행 계획 (WBS)

### Sprint A (P0)
- [ ] 권한 모델 DDL/Flyway 추가
- [ ] API 인가 미들웨어 적용
- [ ] 사용자 목록/상태 변경 API + UI
- [ ] 역할 조회/할당 API + UI
- [ ] 감사로그 최소 스키마 적재

### Sprint B (P1)
- [ ] 역할 CRUD 및 권한 매핑 UI 완성
- [ ] 사용자 검색/필터/정렬 고도화
- [ ] 대량 작업 기능
- [ ] 감사로그 조회 화면

### Sprint C (P2)
- [ ] 정책 기반 권한(조건부)
- [ ] 승인 워크플로우
- [ ] 권한 시뮬레이터

## 8. 수용 기준 (Acceptance Criteria)
- 권한 없는 사용자는 UI/API 모두에서 차단된다.
- 주요 관리자 행위는 모두 감사로그로 남고, 조회 가능하다.
- 사용자/역할 변경 시 운영자에게 변경 영향이 명확히 표시된다.
- 성능: 사용자 목록/역할 목록 API는 기본 페이지 기준 1초 이내 응답을 목표로 한다.

## 9. 리스크 및 대응
- 권한 매트릭스 복잡도 증가: 역할 템플릿/표준 역할로 시작
- 운영 실수 위험: 변경 사유 입력 + 변경 Diff + 롤백 절차 마련
- 감사로그 과다 적재: 보관 주기 및 인덱스 전략 선반영