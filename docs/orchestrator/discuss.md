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

## ADR-004: FAILED와 FINAL_FAILED를 구분한다

**결정**: 실패 상태를 `FAILED`(재실행 허용)와 `FINAL_FAILED`(재실행 차단) 두 가지로 나눈다.

**이유**:
- "재실행"은 애플리케이션 코드를 고치지 않아도 시간이 지나거나 외부 상황이 바뀌면 성공할 가능성이 있는 경우에만 허용한다
- `NonRecoverableException`(비즈니스 검증 실패, 런타임 버그 등)은 재실행해도 동일하게 실패하므로 차단이 맞다
- 재실행 가능 여부를 상태 자체에 인코딩하면 Orchestrator 진입 시 DB 조회 한 번으로 분기할 수 있다

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

**결정**: `Pipeline`은 `RUNNING / SUCCESS / FAILED / FINAL_FAILED` 상태를 `PipelineStatus` enum으로 직접 보유한다. Orchestrator 진입 및 종료 시 상태를 명시적으로 갱신하며, `pipeline.status`가 상태 판단의 단일 신뢰 소스다.

**이유**:
- 레코드 존재 여부와 플래그 조합으로 상태를 판별하는 방식(ADR-008)은 판단 로직을 읽는 사람이 데이터 조합을 머릿속으로 해석해야 하는 부담을 만든다
- 상태 값이 단일 컬럼에 명시적으로 기록되면 Orchestrator 진입 시 조회 한 번으로 분기할 수 있다

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

## ADR-014: 마지막 Step은 delivery 전용이다

**결정**: Step 목록의 마지막 Step은 delivery 전용으로 예약한다. 이전 Step의 결과를 사용자에게 전송하는 단일 책임을 가지며, `execute()`의 입력·출력 값은 의미 없다.

이미 완료된(`SUCCESS`) 파이프라인에 `run()`이 재호출될 때, Orchestrator는 루프 없이 마지막 Step만 직접 실행한다.

**이유**:
- delivery를 Step으로 통합하면 Orchestrator 루프를 단순한 순회 구조로 유지할 수 있다
- 완료된 파이프라인의 재호출 경로에서도 별도 분기 없이 마지막 Step 하나만 실행하면 된다

**제약**:
- Step 목록에 delivery Step은 반드시 하나이며 항상 마지막 위치여야 한다
- delivery Step의 `serialize()` / `deserialize()`는 호출되더라도 의미 있는 값을 반환할 필요가 없다
