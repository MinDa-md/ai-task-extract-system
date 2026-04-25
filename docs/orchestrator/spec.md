## 주제

- 채팅 플랫폼(Discord) 메시지 추출 및 분석 시스템
- AI API를 통해 사용자의 메시지 내용을 분석

## 아키텍처

오케스트레이터 패턴을 구현한다.

- `Orchestrator`는 `Phase` 인터페이스에만 의존한다 (DIP)
- `Phase` 구현체가 스프링 빈으로 등록되며, `@Order`로 실행 순서를 결정한다
- Spring은 `Phase` 구현체 빈들을 모아 `List<Phase>`로 Orchestrator에 주입한다
- Phase들은 순차 선형으로 실행된다
- `PhaseData`가 단계 간 데이터를 전달한다

---

## 구현 순서

### 1. PhaseData / Phase 인터페이스 설계

- `PhaseData` 인터페이스 — 단계 간 데이터 전달의 마커 계약
    - 각 Phase가 자신의 입출력에 맞는 구현체를 정의
    - 단계 간 타입 정합성은 Phase 담당자 간 계약 + 단위 테스트로 보장
- `Phase` 인터페이스
    - `execute(PhaseData input): PhaseData` — 실패는 예외로만 표현
    - `restore(String serializedData): PhaseData` — 재실행 시 이전 결과 역직렬화
    - 예외는 `RecoverableException` / `NonRecoverableException` 두 타입만 (개발자 계약, 컴파일 강제 아님)

### 2. 상태머신 정의

```
PENDING → RUNNING → SUCCESS
                  ↘ FAILED        (RecoverableException — 재실행 허용)
                  ↘ FINAL_FAILED  (NonRecoverableException — 재실행 차단)
```

- 상태는 Orchestrator가 관리한다
- 각 Phase 실행 전후로 상태를 전이시킨다
- 단계 완료 시마다 DB에 저장한다

#### 멱등성 및 재실행 로직

Orchestrator는 진입 시 `req_id`로 DB를 조회하고 분기한다:

| req_id 조회 결과 | 처리 |
|---|---|
| 없음 | 새 컨텍스트 생성 → 최초 실행 |
| SUCCESS | 저장된 결과 반환 (재실행 없음) |
| RUNNING | 중복 요청 거절 |
| FAILED | 이전 단계 결과 복원 → 실패 단계부터 재실행 |
| FINAL_FAILED | 재실행 차단, 원인 반환 |

재실행 시 Orchestrator는 `step_id`로 어느 단계부터 재개할지 파악하고, 복원 데이터 테이블에서 해당 Phase의 `restore()`를 호출해 PhaseData를 복원한다.

#### DB 저장 구조

**`pipeline_execution`** (req_id 단위, 단일 행)

| 컬럼 | 설명 |
|---|---|
| `req_id` | 요청 식별자 (PK) |
| `step_id` | 현재/마지막 실행 단계 식별자 |
| `step_status` | 해당 단계 상태 |
| `pipeline_status` | 전체 파이프라인 상태 |
| `created_at` | 생성 시각 |
| `completed_at` | 완료 시각 |

복원 데이터는 별도 테이블로 관리한다 (설계 예정).

### 3. Phase 구현체 stub + Orchestrator 뼈대

- 실제 로직 없이 흐름만 동작하도록 구현
- `run(String reqId, PhaseData initial)`: Phase 목록을 순서대로 실행, 반환된 PhaseData를 다음 Phase에 전달
- 구체적인 Phase 구현체는 목업(stub)으로 대체

### 4. 공통 실패 및 복구 전략

예외 타입으로 재실행 가능 여부를 분기한다.

| 예외 | 상태 전이 | 예시 |
|---|---|---|
| `RecoverableException` | FAILED (재실행 허용) | AI API 타임아웃, 503 |
| `NonRecoverableException` | FINAL_FAILED (재실행 차단) | 파싱 오류, 유효하지 않은 입력 |

- Orchestrator가 예외 타입을 보고 상태를 결정한다
- Phase는 예외를 던지는 것만 책임진다

### 5. 로깅 + 모니터링

- MDC로 요청 단위 추적
    - `Orchestrator.run()` 진입 시 `req_id` → MDC 등록
    - 모든 로그에 `req_id` 자동 포함
- 각 Phase의 시작 / 종료 / 소요 시간 / 최종 상태 기록
