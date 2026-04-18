## ADR-001: Phase.execute()는 PhaseData를 반환한다

**결정**: `PhaseData execute(PhaseData input)`

**이유**:
- 선형 파이프라인에서 공유 객체(Context)는 불필요하다. 매개변수-반환값으로 데이터 흐름이 충분히 표현된다
- 반환값이 있으면 각 Phase의 입출력이 명시적으로 드러난다
- Orchestrator는 PhaseData 내부를 모른 채 그대로 다음 Phase에 전달하면 된다

**기각된 대안**: `execute(PhaseData): void` + 공유 컨텍스트 객체
- Phase들이 공유 가변 객체에 데이터를 쓰는 방식은 암묵적 의존을 만든다
- Orchestrator가 컨텍스트 생명주기를 관리해야 하는 책임이 추가된다

---

## ADR-002: PhaseData는 마커 인터페이스다. 타입 안전성은 테스트로 보완한다

**결정**: `PhaseData`는 내용이 없는 마커 인터페이스다. 각 Phase는 자신이 받을 타입으로 직접 캐스팅한다. 단계 간 계약의 정합성은 단위 테스트로 보장한다.

**이유**:
- 파이프라인 단계 간 연결의 타입 정합성은 Java 타입 시스템으로 완전히 보장할 수 없다
- 제네릭을 써도 Orchestrator 루프에서 타입 소거로 동일한 런타임 문제가 발생한다. 복잡도 대비 실익이 없다
- 덕타이핑 언어처럼 "계약은 테스트가 보장한다"는 접근이 소규모 프로젝트에서 실용적이다

**기각된 대안 1**: 제네릭 `Phase<I extends PhaseData, O extends PhaseData>`
- Phase 내부는 안전해지지만 Orchestrator 루프(`List<Phase<?,?>>`)에서 타입 소거로 동일한 런타임 캐스팅 필요

**기각된 대안 2**: TypedKey 기반 저장소
- 소규모 프로젝트에서 과도한 복잡도

---

## ADR-003: 비동기 처리 책임은 Phase 내부에 있다

**결정**: Orchestrator는 항상 동기 인터페이스를 유지한다. 오래 걸리는 작업의 비동기 처리는 Phase 내부에서 담당한다.

**이유**:
- Orchestrator가 비동기를 위임하려면 어떤 Phase가 느린지 알아야 한다 — DIP 위반
- Phase 내부에서 `CompletableFuture.join()` 등으로 완료를 기다리면 Orchestrator에게는 동기처럼 보인다

**기각된 대안**: Orchestrator가 비동기 위임 결정
- Orchestrator가 각 Phase의 성능 특성을 알아야 하므로 Phase 내부 지식에 의존하게 된다

---

## ADR-004: FAILED와 FINAL_FAILED를 구분한다

**결정**: 실패 상태를 `FAILED`(재실행 허용)와 `FINAL_FAILED`(재실행 차단) 두 가지로 나눈다.

**이유**:
- "재실행"은 애플리케이션 코드를 고치지 않아도 시간이 지나거나 외부 상황이 바뀌면 성공할 가능성이 있는 경우에만 허용한다
- `NonRecoverableException`(비즈니스 검증 실패, 런타임 버그 등)은 재실행해도 동일하게 실패하므로 차단이 맞다
- 재실행 가능 여부를 상태 자체에 인코딩하면 Orchestrator 진입 시 DB 조회 한 번으로 분기할 수 있다

---

## ADR-005: Phase 인터페이스에서 onFailure()를 제거한다

**결정**: `Phase` 인터페이스에 `onFailure()` 훅 메서드를 두지 않는다. 예외 처리, 재시도, 리소스 정리는 `execute()` 내부에서 담당한다.

**이유**:
- 파라미터 없는 onFailure()는 예외 컨텍스트에 접근할 수 없어 예외 포장, 조건부 재시도 등의 역할을 수행할 수 없다
- 리소스 정리는 `try-finally` 또는 `try-with-resources`로 충분히 처리된다
- 인터페이스 메서드는 호출자가 명확해야 한다. Orchestrator도 아니고 Phase 자신이 내부에서 호출하는 메서드라면 인터페이스 계약에 올릴 이유가 없다

---

## ADR-007: 멱등성 로직을 @Aspect PhaseProxy로 주입한다

**결정**: Phase 실행 캐싱(멱등성)을 `@Aspect` `PhaseProxy`로 구현한다. `@Around("execution(* Phase.execute(..))")`로 모든 Phase 실행을 가로채, pipelineId+stepId 조합으로 캐시 히트 여부를 확인한 뒤 반환하거나 실행 후 저장한다.

**이유**:
- Orchestrator 루프와 Phase 구현체 어디에도 멱등성 코드가 없어 단일 책임이 유지된다
- Phase를 추가할 때 캐싱 로직을 신경 쓸 필요가 없다 (OCP)
- `@Order` 기반 Phase 빈 순서가 AOP 인터셉터와 독립적으로 유지된다

**기각된 대안**: Orchestrator 루프 내부에서 직접 캐시 확인
- Orchestrator가 PhaseExecution 저장소에 직접 의존하게 되어 책임이 비대해진다
- Phase가 추가될 때마다 Orchestrator 로직이 영향을 받는다

---

## ADR-006: Phase 인터페이스에 serialize()/deserialize()를 추가한다

**결정**: `Phase` 인터페이스에 두 메서드를 추가한다.
- `String serialize(PhaseData result)` — 출력을 JSON String으로 직렬화
- `PhaseData deserialize(String json)` — JSON String을 구체 타입으로 역직렬화

PhaseProxy는 phase 결과를 저장할 때 `phase.serialize()`를 호출하고, 캐시 히트 시 `phase.deserialize()`를 호출한다. Orchestrator의 `restoreLastResult`도 `phases.last().deserialize()`를 사용한다.

**이유**:
- PhaseData는 마커 인터페이스(ADR-002)이므로 PhaseProxy나 Orchestrator가 구체 타입을 알 수 없다. 역직렬화 시 타입 정보가 없으면 ObjectMapper가 타입을 추론할 수 없다
- 직렬화도 같은 이유로 Phase에 위임하면 PhaseProxy에 ObjectMapper 의존성이 사라지고 관심사가 명확히 분리된다
- DB 읽기/쓰기(PhaseProxy, Orchestrator)와 타입 변환(Phase)을 분리하면서 구체 타입 지식은 Phase 안에 캡슐화된다

**기각된 대안 1**: PhaseProxy에 ObjectMapper 주입
- 직렬화는 가능하나 역직렬화 시 구체 타입 정보 부재로 타입 파라미터를 외부에서 전달해야 한다

**기각된 대안 2**: Phase가 직접 DB를 조회해 복원
- 모든 Phase에 DB 의존성이 생기며, DB 읽기 책임이 Orchestrator와 Phase에 분산된다
