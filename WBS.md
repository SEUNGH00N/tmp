# WBS (Single Developer)

기준 문서: `ERD.md`  
개발 인원: 1명(본인)  
목표: Workspace 중심 2.5계층 SaaS MVP를 `V7~V11` 단계로 구현

---

## 0. 원칙
- 한 번에 한 스프린트만 완료 후 다음 단계 진행
- 각 버전마다 `DDL -> 백엔드 API -> 화면 반영 -> 테스트` 순서 고정
- 병렬 작업 최소화, 배포 가능 상태를 매 단계 유지

---

## 1. Phase 1 (V7): Workspace/Account 전환

### 1.1 DB/Flyway
- [ ] `workspace` 테이블 추가
- [ ] `workspace_account` 테이블 추가
- [ ] `workspace_membership` 테이블 추가 (UK: workspace_id + account_id)
- [ ] `import_job.workspace_id` 컬럼 추가
- [ ] `import_job.tenant_id -> workspace_id` 데이터 이관 스크립트 작성
- [ ] 상태 CHECK 제약 표준화(필요 컬럼)

### 1.2 Backend
- [ ] 로그인/세션에서 workspace 컨텍스트 처리
- [ ] import API의 tenant 기반 로직을 workspace 기반으로 전환
- [ ] owner 기본 생성/연결 로직 구현

### 1.3 Frontend
- [ ] 사용자 화면에 workspace 표시
- [ ] 업로드/잡 목록 API 파라미터 workspace 기준으로 정리

### 1.4 Test
- [ ] 마이그레이션 통합 테스트
- [ ] workspace 스코프 데이터 접근 테스트

---

## 2. Phase 2 (V8): 과금/사용량 모델

### 2.1 DB/Flyway
- [ ] `billing_plan` 생성
- [ ] `plan_feature` 생성 (typed limit 구조)
- [ ] `workspace_subscription` 생성
- [ ] `workspace_usage_daily` 생성 (UK: workspace_id + usage_day)

### 2.2 Backend
- [ ] 플랜 조회 API
- [ ] 구독 상태 조회 API
- [ ] 업로드 시 quota 검사 로직
- [ ] 일 단위 usage 집계 배치/서비스 구현

### 2.3 Frontend
- [ ] 현재 플랜/한도/사용량 UI 추가
- [ ] 한도 초과 시 사용자 안내 메시지 처리

### 2.4 Test
- [ ] quota enforcement 테스트
- [ ] usage 집계 정확성 테스트

---

## 3. Phase 3 (V9): DB 연결 보안

### 3.1 DB/Flyway
- [ ] `db_connection` 테이블 생성
- [ ] `secret_ref` 중심 컬럼 설계 반영
- [ ] MVP fallback(`password_enc`, `key_version`) 반영

### 3.2 Backend
- [ ] DB 연결 등록/수정/비활성 API
- [ ] secret_ref 기반 연결 정보 조회 로직 구현
- [ ] 암호화 저장/복호화 사용 경계 분리

### 3.3 Frontend
- [ ] DB 연결 관리 화면(등록/테스트/비활성)

### 3.4 Test
- [ ] 비밀정보 마스킹 테스트
- [ ] 연결 테스트 API 통합 테스트

---

## 4. Phase 4 (V10): 비동기 신뢰성/관측성

### 4.1 DB/Flyway
- [ ] `import_job_run` 생성 (UK: job_id + run_no)
- [ ] `import_job_event` 생성
- [ ] `import_job_metric` 생성
- [ ] `import_dedup` 생성 (UK: workspace_id + checksum + file_size)

### 4.2 Backend
- [ ] 업로드 중복 검사(dedup) 적용
- [ ] 재시도(run_no 증가) 처리 구현
- [ ] 워커 실행 이력/에러 기록 저장
- [ ] 이벤트/메트릭 적재 구현

### 4.3 Frontend
- [ ] Job 상세에 run/event/metric 탭 추가
- [ ] 실패 원인 및 재시도 이력 표시

### 4.4 Test
- [ ] 중복 업로드 idempotency 테스트
- [ ] 재처리/재시도 시나리오 테스트

---

## 5. Phase 5 (V11): 승인/거버넌스/롤아웃

### 5.1 DB/Flyway
- [ ] `approval_request` 생성
- [ ] `approval_decision` 생성
- [ ] `workspace_feature_flag` 생성 (UK: workspace_id + feature_code)

### 5.2 Backend
- [ ] OWNER/PLATFORM_ADMIN/DUAL_APPROVAL 정책 처리
- [ ] 승인 요청/결정 API 구현
- [ ] feature flag 기반 기능 활성화 제어 구현

### 5.3 Frontend
- [ ] 승인 요청/결정 UI 구현
- [ ] 워크스페이스별 기능 플래그 관리 UI 추가

### 5.4 Test
- [ ] 정책별 승인 플로우 테스트
- [ ] feature flag on/off 회귀 테스트

---

## 6. 공통 품질 항목 (매 Phase 반복)
- [ ] API 문서 동기화(README, endpoint spec)
- [ ] 마이그레이션 롤백/재실행 검증
- [ ] 주요 쿼리 인덱스 점검
- [ ] 운영 로그/에러 핸들링 검증
- [ ] 데모 시나리오 업데이트

---

## 7. 완료 정의 (Definition of Done)
- [ ] Flyway 적용 성공 및 기존 데이터 호환 확인
- [ ] 핵심 API 정상 동작(수동 + 자동 테스트)
- [ ] UI에서 실제 사용자 시나리오 1회 완료 가능
- [ ] README/ERD/WBS 최신 상태 유지
- [ ] 다음 Phase 진입 전 기술부채 TODO 명시