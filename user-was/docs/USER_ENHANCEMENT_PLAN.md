# User-WAS 고도화 전략

## 1. 목표
- 사용자 관점에서 업로드 성공률과 피드백 품질을 높인다.
- 업로드 이후 처리 상태를 신뢰 가능하게 제공한다.
- 대용량/오류 데이터에서도 안정적으로 동작하는 사용자용 WAS를 만든다.

## 2. 현재 상태 요약
- 업로드 API 및 파일 저장 동작 중
- 비동기 처리 파이프라인 동작 (`CREATED -> PARSING -> VALIDATING -> LOADING -> COMPLETED/FAILED`)
- 유효 행은 `excel_data`, 오류 행은 `error_log` 저장
- 상세 화면에서 상태/rows/errors 조회 가능

## 3. 우선순위 기준
- P0: 데이터 유실/권한 누수/운영 장애 방지
- P1: 사용자 경험 개선(가시성, 오류 안내, 재시도)
- P2: 고도화(자동화, 성능 최적화, 확장 기능)

## 4. 핵심 고도화 축

### 4.1 업로드 안정성
#### P0
- 파일 유효성 강화: 확장자 + MIME + 시그니처 체크
- tenant 경로 격리 검증 및 경로 우회 방지
- 업로드 요청 idempotency 키 도입 검토
- 업로드 실패 표준 에러코드 정리

#### P1
- 업로드 중복 감지(체크섬 + tenant)
- 동일 파일 재업로드 정책(무시/재처리/신규 job)
- 업로드 실패 시 재시도 UX 제공

#### P2
- 대용량 파일 chunk 업로드 전략
- 업로드 임시 스토리지 + finalize 워크플로우

### 4.2 파싱/검증 정확도
#### P0
- CSV/XLSX 파서 인터페이스 분리 (`Parser` 추상화)
- 헤더 유무/빈행 처리 규칙 확정
- 검증 규칙 카탈로그화(코드, 메시지, 심각도)

#### P1
- 컬럼 타입 추론(숫자/날짜/문자) 및 경고 레벨 도입
- 사용자 친화적 오류 메시지 매핑
- 검증 통계(오류 유형 TOP N) 제공

#### P2
- 사용자 정의 검증 규칙(컬럼별 정규식/범위)
- 검증 프로파일 저장/재사용

### 4.3 사용자 경험(Frontend)
#### P0
- 업로드/처리 상태 실시간성 개선(polling 표준화)
- empty/loading/error 상태 UI 일관화
- 상세 페이지에서 rows/errors 탭 분리 및 페이지네이션

#### P1
- 오류 행 다운로드(CSV)
- 성공/실패 요약 카드 + 추세 차트
- 최근 업로드 히스토리 필터링

#### P2
- 드래그&드롭 고도화(다중 파일 큐)
- 업로드 템플릿 추천/자동 매핑 UX

### 4.4 멀티테넌시/보안
#### P0
- 모든 조회 API에서 tenant 소유권 재검증
- tenant 간 데이터 접근 차단 테스트 케이스 추가
- 민감 로그 마스킹(파일명, 내부 경로 일부)

#### P1
- tenant quota(파일 크기/일일 건수) 정책
- 비정상 요청 rate-limit

#### P2
- tenant별 정책 커스터마이징(허용 확장자, 용량)

### 4.5 운영/관측성
#### P0
- 구조화 로그 도입(requestId, jobId, tenantId)
- 핵심 메트릭 수집(처리시간, 실패율, 오류율)
- 장애 시 상태 고정 방지(타임아웃/보상 처리)

#### P1
- 알림 연계(처리 실패 임계치 초과 시)
- 배치 성능 대시보드

#### P2
- SLA 기반 자동 스케일링 지표 설계

## 5. 데이터/스키마 확장 제안
- `import_job` 확장 필드
  - `original_filename`
  - `file_size`
  - `checksum`
  - `validation_summary_json`
- 신규 테이블
  - `import_job_event` (상태 전이/진행 이벤트)
  - `import_job_metric` (성능/통계 지표)
  - `import_dedup` (체크섬 기반 중복 정책)

## 6. API 고도화 초안
- `POST /api/v1/user/imports` (idempotency-key 지원)
- `GET /api/v1/user/imports/{jobId}/events`
- `GET /api/v1/user/imports/{jobId}/summary`
- `GET /api/v1/user/imports/{jobId}/errors/export`
- `POST /api/v1/user/imports/{jobId}/retry`

## 7. 단계별 WBS

### Sprint U1 (P0)
- [ ] 업로드/검증 에러코드 표준화
- [ ] 파서 모듈 분리 및 단위테스트 추가
- [ ] tenant 소유권 검증 테스트 보강
- [ ] 로그 필드 표준화(requestId/jobId/tenantId)

### Sprint U2 (P1)
- [ ] 중복 업로드 정책 구현
- [ ] 상세 UI 탭/페이지네이션 개선
- [ ] 오류 다운로드 API/UI 추가
- [ ] quota/rate-limit 초안 적용

### Sprint U3 (P2)
- [ ] chunk 업로드 PoC
- [ ] 사용자 정의 검증 규칙
- [ ] 이벤트/메트릭 기반 운영 대시보드

## 8. 수용 기준 (Acceptance Criteria)
- tenant 경계를 넘는 조회가 기술적으로 차단된다.
- 처리 실패 시 원인(검증/시스템)을 사용자와 운영자가 구분할 수 있다.
- 100MB 급 파일 처리에서도 서비스가 중단되지 않는다.
- 동일 파일 재업로드 정책이 문서/코드/UX에서 일치한다.

## 9. 리스크 및 대응
- 과도한 검증으로 처리 지연: 규칙 레벨(필수/권장) 분리
- 대용량 시 메모리 압박: 스트리밍 파싱, 배치 insert, 백프레셔
- 운영 복잡도 증가: 이벤트/로그 스키마 표준 선반영