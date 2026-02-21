# WBS (Single Developer)

기준 문서: `ERD.md`  
개발 인원: 1명  
목표: Workspace 중심 2.5계층 SaaS MVP 단계별 구현

## 1. Phase 1 (V7): Workspace/Account 전환

### 1.1 DB/Flyway
- [x] `workspace` 테이블 추가
- [x] `workspace_account` 테이블 추가
- [x] `workspace_membership` 테이블 추가
- [x] `import_job.workspace_id` 컬럼 추가
- [x] `tenant_id -> workspace_id` 이관 스크립트 적용
- [x] 상태/역할 CHECK 제약 정리

### 1.2 Backend
- [x] `workspace_id` 기반 import API 전환
- [x] owner/workspace 기본 생성 로직 반영
- [x] 세션 + Bearer 토큰 병행 인증 구조 반영(admin)
- [x] Redis 기반 세션 저장 반영(admin/user)

### 1.3 Frontend
- [x] user 포털 workspace 기준 파라미터 반영
- [x] admin 로그인 후 토큰 저장/전송 처리 반영

### 1.4 Test
- [x] 마이그레이션/통합 테스트 갱신
- [x] 루트 `mvn clean test` 통과

## 2. Phase 2 (V8): 과금/사용량 모델

### 2.1 DB/Flyway
- [x] `billing_plan`
- [x] `plan_feature`
- [x] `workspace_subscription`
- [x] `workspace_usage_daily`

### 2.2 Backend
- [x] 플랜 조회 API (`/api/v1/user/billing/plan`)
- [x] 구독 상태 조회 API (`/api/v1/user/billing/subscription`)
- [x] 업로드 quota 검사
- [x] 일별 usage 집계

### 2.3 Frontend
- [x] 플랜/사용량 UI
- [x] 한도 초과 UX

## 3. Phase 3 (V9): DB 연결 보안
- [x] `db_connection` + `secret_ref` 모델
- [x] 연결 등록/검증 API
- [x] 암호화/키버전 정책

## 4. Phase 4 (V10): 비동기 신뢰성/관측
- [x] `import_job_run`
- [x] `import_job_event`
- [x] `import_job_metric`
- [x] `import_dedup`
- [x] 이벤트 퍼블리셔 경계 분리(`sync/kafka` 모드 전환점)

## 5. Phase 5 (V11): 승인/거버넌스/플래그
- [x] `approval_request`
- [x] `approval_decision`
- [x] `workspace_feature_flag`
- [x] 정책별 승인 플로우

## 6. 공통 품질 체크
- [ ] README/ERD/WBS 동기화 유지
- [ ] 인덱스/쿼리 점검
- [ ] 운영 로그/에러 표준화
- [ ] 배포/롤백 체크리스트 정리


