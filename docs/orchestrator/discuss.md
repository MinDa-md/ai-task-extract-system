## ADR-001: Step.execute() 시그니처

**결정**: `StepData execute(String pipelineId, StepData input)`

**이유**:
- 선형 파이프라인에서 공유 객체(Context)는 불필요하다. 매개변수-반환값으로 데이터 흐름이 충분히 표현된다
- 반환값이 있으면 각 Step의 입출력이 명시적으로 드러난다
- Orchestrator는 StepData 내부를 모른 채 그대로 다음 Step에 전달하면 된다
- Pipeline과 Step이 별도 엔티티로 분리되어 있어, Step 실행 시 어느 Pipeline에 속하는지 명시적으로 전달해야 한다
- StepProxy(ADR-009)가 로깅 시 pipelineId를 컨텍스트로 사용한다

**기각된 대안 1**: `execute(StepData): void` + 공유 컨텍스트 객체
- Step들이 공유 가변 객체에 데이터를 쓰는 방식은 암묵적 의존을 만든다
- Orchestrator가 컨텍스트 생명주기를 관리해야 하는 책임이 추가된다

**기각된 대안 2**: StepProxy가 `MDC.get("req_id")`로 pipelineId를 읽는 방식
- MDC는 스레드 로컬 기반으로 비동기 경계에서 전파가 보장되지 않는다
- 로깅 인프라를 비즈니스 키 전달 경로로 사용하는 것은 안티패턴이다

---

## ADR-002: StepData는 마커 인터페이스다. 타입 안전성은 테스트로 보완한다

**결정**: `StepData`는 내용이 없는 마커 인터페이스다. 각 Step은 자신이 받을 타입으로 직접 캐스팅한다. 단계 간 계약의 정합성은 단위 테스트로 보장한다.

**이유**:
- 파이프라인 단계 간 연결의 타입 정합성은 Java 타입 시스템으로 완전히 보장할 수 없다
- 제네릭을 써도 Orchestrator 루프에서 타입 소거로 동일한 런타임 문제가 발생한다. 복잡도 대비 실익이 없다
- "계약은 테스트가 보장한다"는 접근이 소규모 프로젝트에서 실용적이다

**기각된 대안 1**: 제네릭 `Step<I extends StepData, O extends StepData>`
- Step 내부는 안전해지지만 Orchestrator 루프(`List<Step<?,?>>`)에서 타입 소거로 동일한 런타임 캐스팅 필요

**기각된 대안 2**: TypedKey 기반 저장소
- 소규모 프로젝트에서 과도한 복잡도

---

## ADR-003: 비동기 처리 책임은 Step 내부에 있다

**결정**: Orchestrator는 항상 동기 인터페이스를 유지한다. 오래 걸리는 작업의 비동기 처리는 Step 내부에서 담당한다.

**이유**:
- Orchestrator가 비동기를 위임하려면 어떤 Step이 느린지 알아야 한다 — DIP 위반
- Step 내부에서 `CompletableFuture.join()` 등으로 완료를 기다리면 Orchestrator에게는 동기처럼 보인다

**기각된 대안**: Orchestrator가 비동기 위임 결정
- Orchestrator가 각 Step의 성능 특성을 알아야 하므로 Step 내부 지식에 의존하게 된다

---

## ADR-004: FAILED는 영구 실패를 나타낸다

**결정**: 단일 `FAILED` 상태로 영구 실패를 표현한다. 재시도 가능 여부는 `RUNNING` 상태가 담당한다.

- **RUNNING**: 일시적 실패 후 재시도 대기 상태
- **FAILED**: `NonRecoverableException` 발생 또는 runCount 한계 초과 시 전이. 이후 `run()` 호출 시 즉시 `NonRecoverableException` 발생

**이유**:
- 초기 설계의 `FAILED`(재실행 허용)와 `FINAL_FAILED`(재실행 차단) 구분은 `RUNNING`이 이미 "재시도 가능" 상태를 표현하므로 불필요하다
- 상태 수를 최소화하면 Orchestrator 진입 분기 조건이 단순해진다

---

## ADR-005: Step 인터페이스에서 onFailure()를 제거한다

**결정**: `Step` 인터페이스에 `onFailure()` 훅 메서드를 두지 않는다. 예외 처리, 재시도, 리소스 정리는 `execute()` 내부에서 담당한다.

**이유**:
- 파라미터 없는 `onFailure()`는 예외 컨텍스트에 접근할 수 없어 예외 포장, 조건부 재시도 등의 역할을 수행할 수 없다
- 리소스 정리는 `try-finally` 또는 `try-with-resources`로 충분히 처리된다
- 인터페이스 메서드는 호출자가 명확해야 한다. Orchestrator도 아니고 Step 자신이 내부에서 호출하는 메서드라면 인터페이스 계약에 올릴 이유가 없다

---

## ADR-006: Step 인터페이스에 serialize()/deserialize()를 추가한다

**결정**: `Step` 인터페이스에 두 메서드를 추가한다.
- `String serialize(StepData result)` — 출력을 JSON String으로 직렬화
- `StepData deserialize(String json)` — JSON String을 구체 타입으로 역직렬화

Orchestrator는 Step 성공 후 Checkpoint를 저장할 때 `step.serialize(result)`를 호출하고, 재시작 시 마지막 Checkpoint 복원에 `step.deserialize(json)`을 사용한다.

**이유**:
- StepData는 마커 인터페이스(ADR-002)이므로 Orchestrator가 구체 타입을 알 수 없다. 역직렬화 시 타입 정보가 없으면 ObjectMapper가 타입을 추론할 수 없다
- Step에 위임하면 관심사가 명확히 분리된다. 구체 타입 지식은 Step 안에 캡슐화된다

**기각된 대안 1**: Orchestrator에 ObjectMapper 주입
- 역직렬화 시 구체 타입 정보 부재로 타입 파라미터를 외부에서 전달해야 한다

**기각된 대안 2**: Step이 직접 DB를 조회해 복원
- 모든 Step에 DB 의존성이 생기며, DB 읽기 책임이 Orchestrator와 Step에 분산된다

**구현 제약**: `deserialize()` 구현 시 다음을 금지한다.
- `mapper.activateDefaultTyping(...)` — 폴리모픽 역직렬화로 DB 오염 시 RCE 가능
- `mapper.enableDefaultTyping(...)` — 위의 deprecated 전신, 동일하게 위험
- `mapper.readValue(json, Object.class)` — 타입 미지정으로 임의 클래스 인스턴스화 가능
- `ObjectInputStream` 기반 Java 네이티브 역직렬화

반드시 구체 타입으로만 역직렬화한다: `mapper.readValue(json, ConcreteStepData.class)`

---

## ADR-007: [폐기 — ADR-009로 대체]

AOP 기반 캐싱을 `@Aspect StepProxy`로 구현하는 방안. StepProxy가 Checkpoint INSERT와 캐시 조회를 담당했으나, 성공 후 INSERT 정책과 충돌하여 ADR-009로 대체됐다.

---

## ADR-008: [폐기 — ADR-010으로 대체]

`PipelineStatus` enum을 제거하고 행동 기반으로 상태를 판별하는 방안(summary 행 존재 = SUCCESS, final_failed 플래그 = FINAL_FAILED). 판단 로직을 읽는 사람이 데이터 조합을 머릿속으로 해석해야 하는 부담이 생겨 ADR-010으로 대체됐다.

---

## ADR-009: StepProxy는 로깅 전용 — Checkpoint INSERT는 Orchestrator 책임

**결정**: StepProxy에서 멱등성(캐싱) 로직을 제거한다. StepProxy는 start / complete+elapsed / failure 로그만 남긴다. Step 성공 후 `CheckpointRepository.save()` 호출은 Orchestrator가 직접 담당하며, result 직렬화는 `step.serialize(result)`를 사용한다 (ADR-006 유지).

**이유**:
- Checkpoint INSERT 시점(성공 후에만)과 조건을 Orchestrator가 제어해야 한다
- StepProxy(AOP)에서 INSERT하면 성공 여부 판별과 저장 시점을 분리할 수 없다

**기각된 대안**: ADR-007처럼 StepProxy에서 INSERT 유지
- Step 성공 후에만 INSERT하는 정책과 충돌한다

---

## ADR-010: Pipeline이 상태를 직접 소유한다

**결정**: `Pipeline`은 `RUNNING / SUCCESS / FAILED` 세 가지 상태를 `PipelineStatus` enum으로 직접 보유한다. Orchestrator 진입 및 종료 시 상태를 명시적으로 갱신하며, `pipeline.status`가 상태 판단의 단일 신뢰 소스다.

| 상태 | 의미 |
|---|---|
| `RUNNING` | 실행 중이거나 회복 가능한 실패 후 재시도 대기 |
| `SUCCESS` | 파이프라인 정상 완료 |
| `FAILED` | runCount 한계 초과 또는 `NonRecoverableException`으로 영구 실패 |

**이유**:
- 레코드 존재 여부와 플래그 조합으로 상태를 판별하는 방식(ADR-008)은 판단 로직을 읽는 사람이 데이터 조합을 머릿속으로 해석해야 하는 부담을 만든다
- 상태 값이 단일 컬럼에 명시적으로 기록되면 Orchestrator 진입 시 조회 한 번으로 분기할 수 있다
- ADR-004 초기 설계의 `FINAL_FAILED`는 `RUNNING`이 이미 재시도 가능 상태를 표현하므로 제거했다

**기각된 대안**: 행동 기반 판별 (ADR-008)
- `summary` 행 존재 여부, `final_failed` 플래그 등 여러 데이터를 조합해야 하므로 가독성이 낮다
- 판단 로직이 여러 곳에 흩어질 위험이 있다

---

## ADR-011: 실행 단위와 저장 단위 명칭을 구분한다

**결정**: 실행 단위와 DB 저장 단위의 명칭을 아래와 같이 분리한다.

| 역할 | 명칭 |
|------|------|
| 실행 인터페이스 | `Step` |
| 실행 데이터 | `StepData` |
| DB 성공 기록 | `Checkpoint` |

**이유**:
- 기존 `PhaseExecution`은 실행 흐름(`Phase`)과 DB 기록이라는 두 역할을 이름 하나에 담고 있었다
- 실행 책임과 저장 책임이 명칭 수준에서 혼재되어, DB 저장 구조가 실행 흐름 설계에 영향을 주는 SRP 붕괴가 발생했다
- 명칭을 분리하면 각 타입의 역할 경계가 코드를 읽는 시점에 명확해진다

**기각된 대안**: `Phase`, `PhaseData`, `PhaseExecution` 유지
- `PhaseExecution`이 실행 단위인지 DB 레코드인지 이름만으로 구분할 수 없다

---

## ADR-012: Checkpoint는 성공 시에만 insert-only로 기록한다

**결정**: Checkpoint는 Step 실행 성공 시에만 INSERT한다. UPDATE, DELETE는 없다. 재시작 시 마지막 Checkpoint 이후 Step부터 실행한다.

**이유**:
- Checkpoint의 유일한 목적은 "어디서 재시작할 것인가"를 결정하는 것이다
- 실패한 Step은 기록하지 않으므로 Checkpoint가 존재하는 것은 곧 해당 Step이 성공했다는 의미다
- insert-only 구조로 인해 갱신 전략을 고민할 필요가 없다

**기각된 대안**: 실패 포함 전체 실행 이력 기록
- 재시작 기준 계산이 복잡해지고, Checkpoint 조회 시 성공 여부 필터링이 필요해진다

---

## ADR-013: Summary는 결과 저장 전용이다. Orchestrator는 Summary를 알지 못한다

**결정**: Summary는 파이프라인 전체 결과를 집계해 저장하는 전용 테이블이다. Orchestrator는 Summary를 직접 참조하거나 저장하지 않는다. 상태 판단에 Summary를 사용하지 않는다.

**이유**:
- Summary가 상태 판단에 관여하면 상태 판단 로직이 `pipeline.status`와 Summary에 분산되어 ADR-010의 단일 신뢰 소스 원칙과 충돌한다
- Orchestrator의 책임은 Step 실행 순서 관리에 한정한다. Summary 저장은 파이프라인 완료 이후 별도 관심사다

**기각된 대안**: Summary 존재 여부로 SUCCESS 판별 (ADR-008)
- 상태 판단 로직이 분산되어 ADR-010의 단일 신뢰 소스 원칙과 충돌한다

---

## ADR-014: 첫 번째와 마지막 Step은 경계 Step으로 예약한다

**결정**: Step 목록의 첫 번째(stepOrder=0)와 마지막(stepOrder=99) 위치는 각각 ingest와 delivery 전용 경계 Step으로 예약한다. 두 경계 Step은 Orchestrator 내부 TreeMap에 등록되며, 처리 Step과 동일한 체크포인트 기반 재시도 로직을 따른다.

- **IngestStep(stepOrder=0)**: 컨트롤러가 전달한 외부 입력을 파이프라인 내부 StepData로 변환한다
- **DeliveryStep(stepOrder=99)**: 파이프라인 결과를 외부로 전송한다. `execute()` 입력은 무시하고 Repository에서 직접 조회한다

`loadCheckpoint()`가 체크포인트 없을 때 `INGEST_STEP_ORDER - 1 = -1`을 반환하므로, `tailMap(-1, false)`는 IngestStep(0)을 포함한 모든 Step을 실행한다. IngestStep 성공 후 체크포인트(stepOrder=0)가 저장되면, 재시도 시 `tailMap(0, false)`로 IngestStep을 자동으로 건너뛴다.

이미 완료된(`SUCCESS`) 파이프라인에 `run()`이 재호출될 때, Orchestrator는 루프 없이 DeliveryStep만 직접 실행한다.

**이유**:
- 두 경계 Step을 내부 TreeMap에 통합하면 컨트롤러는 `orchestrator.run()`만 호출하면 된다. 경계 Step 실행 책임이 Orchestrator 외부로 분산되지 않는다
- `tailMap(-1, false)` 시작점 설계로, IngestStep의 첫 실행 포함과 재시도 시 스킵이 별도 분기 없이 처리된다. 처리 Step의 체크포인트 재시작 로직과 동일하다

**기각된 대안**: IngestStep / DeliveryStep을 Orchestrator 외부에서 호출
- 컨트롤러가 경계 Step 실행 순서와 완료 조건을 직접 관리해야 한다
- 파이프라인 재시도 시 IngestStep 중복 실행 방지 로직이 Orchestrator 외부에 필요해진다

**제약**:
- IngestStep은 stepOrder=0, DeliveryStep은 stepOrder=99로 고정이며, 처리 Step의 stepOrder는 1~98 사이여야 한다
- DeliveryStep의 `serialize()` / `deserialize()`는 호출되더라도 의미 있는 값을 반환할 필요가 없다

---

## ADR-015: Orchestrator는 at-least-once 실행을 보장한다. delivery step의 멱등성은 Step 책임이다

**결정**: Orchestrator는 step의 exactly-once 실행을 보장하지 않는다. delivery step 개발자는 외부 발송 서비스의 idempotency key를 사용해 중복 발송을 직접 방어해야 한다.

**이유**:
- `step.execute()`가 정상 반환해도 `checkpointRepository.save()`가 실패하면 `pipeline.status`는 `RUNNING`으로 남는다. retry 시 delivery step이 재실행되어 중복 발송이 발생한다
- `step.execute()`는 외부 네트워크 호출을 포함하므로 checkpoint 저장과 원자적으로 묶을 수 없다 (ADR-003)
- DB 기반 중복 방지(별도 테이블 기록)는 checkpoint 저장 실패를 유발한 DB 장애 시 동일하게 실패한다. DB에 의존하는 어떤 방식도 DB 전체 장애에 대해 근본적 한계를 가진다
- 외부 발송 서비스가 idempotency key를 지원하면 `pipelineId`를 key로 전달해 서비스 레벨에서 중복을 차단할 수 있다. 이 책임은 외부 서비스를 직접 아는 delivery step 개발자에게 있다

**제약**:
- delivery step 개발자는 사용하는 외부 발송 서비스의 idempotency key 지원 여부를 반드시 확인해야 한다
- 외부 서비스가 idempotency key를 지원하지 않는 경우, 아래 두 전략 중 하나를 명시적으로 선택하고 트레이드오프를 문서화해야 한다
  - at-least-once (기본 동작): 중복 발송 가능, 미발송 없음
  - at-most-once: 발송 전 `delivery_records` 테이블에 기록 후 발송. 중복 발송 없음, DB 장애 시 미발송 가능
