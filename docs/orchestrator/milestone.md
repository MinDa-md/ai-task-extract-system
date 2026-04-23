# Milestone

## M1 — 인터페이스 + 상태머신 + DB 스키마

**목표**: Phase 실행 계약, 파이프라인 상태 전이 모델, DB 영속화 구조를 함께 확정한다.

### 산출물

- `PhaseData` (marker interface)
- `Phase` interface (`execute(PhaseData): PhaseData`, `restore(String): PhaseData`)
- `PipelineStatus` enum (`PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `FINAL_FAILED`)
- `pipeline_execution` 테이블 DDL
- 복원 데이터 테이블 DDL

### 완료 조건

- testcontainers 기반 DB 마이그레이션 통합 테스트 통과
- 상태 전이 규칙 단위 테스트 통과 (`PENDING → RUNNING → SUCCESS / FAILED / FINAL_FAILED`)

### 관련 ADR

ADR-001, ADR-002, ADR-004, ADR-006

--- 

## M2 — Orchestrator 뼈대 + Phase stub

**목표**: stub Phase들로 선형 실행 흐름이 동작하는 Orchestrator를 구현한다.

### 산출물

- `Orchestrator.run(String reqId, PhaseData initial)`
- stub Phase 구현체 2개 이상 (`@Order`로 순서 지정)

### 완료 조건

- stub Phase들을 순서대로 실행하는 통합 테스트 통과
- 각 Phase 실행 전후 DB 상태 전이 확인 테스트 통과

### 관련 ADR

ADR-001, ADR-003

---

## M3 — 실패 처리 및 재실행

**목표**: `RecoverableException` / `NonRecoverableException` 분기와 `req_id` 기반 재실행 로직을 구현한다.

### 산출물

- `RecoverableException`, `NonRecoverableException`
- Orchestrator `req_id` 조회·분기 로직 (없음 / `SUCCESS` / `RUNNING` / `FAILED` / `FINAL_FAILED`)
- `restore()` 호출을 통한 이전 단계 복원 흐름

### 완료 조건

- `FAILED` 상태에서 재실행 시 실패 단계부터 재개하는 통합 테스트 통과
- `FINAL_FAILED` 상태에서 재실행 차단 통합 테스트 통과
- `SUCCESS` 상태에서 저장된 결과 반환 테스트 통과

### 관련 ADR

ADR-004, ADR-005, ADR-006

---

## M4 — 로깅 + 모니터링

**목표**: `req_id` 기반 요청 추적과 Phase별 실행 지표를 기록한다.

### 산출물

- MDC 설정 (`req_id` 자동 포함, `Orchestrator.run()` 진입 시 등록)
- 각 Phase 시작 / 종료 / 소요 시간 / 최종 상태 로그

### 완료 조건

- 로그 출력에 `req_id` 포함 확인 (로그 캡처 검증) 테스트 통과
- Phase별 타이밍 로그 기록 확인 테스트 통과

### 관련 ADR

없음
