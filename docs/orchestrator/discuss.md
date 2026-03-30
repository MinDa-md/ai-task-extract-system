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

## ADR-006: Phase 인터페이스에 restore()를 추가한다

**결정**: `PhaseData restore(String serializedData)`를 `Phase` 인터페이스에 추가한다. 재실행 시 Orchestrator는 DB에서 JSON을 읽어 해당 Phase의 `restore()`를 호출하고, Phase가 자신의 구체 타입으로 역직렬화한다.

**이유**:
- 직렬화/역직렬화는 구체 PhaseData 타입을 알아야 하므로, 그 지식을 가진 Phase가 담당하는 것이 자연스럽다
- 별도의 복원 계층을 두면 그 계층이 도메인 구체 클래스에 의존하게 되어 역의존이 발생한다
- DB 읽기(Orchestrator)와 역직렬화(Phase)를 분리하면서 구체 타입 지식은 Phase 안에 캡슐화된다

**기각된 대안**: Phase가 직접 DB를 조회해 복원
- 모든 Phase에 DB 의존성이 생기며, DB 읽기 책임이 Orchestrator와 Phase에 분산된다
