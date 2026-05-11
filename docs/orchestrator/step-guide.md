# Orchestrator: Step 구현 가이드

## 개요

- `Orchestrator`는 `stepOrder` 순서로 정렬된 `Step` 시퀀스로 파이프라인을 실행합니다.
- IngestStep(0)과 DeliveryStep(99)을 포함한 모든 `Step`은 오케스트레이터 내부 `TreeMap`에 등록되며, 체크포인트 기반으로 재시도 시 건너뜁니다.

---

## 전체 파이프라인 구성

```
Discord Webhook
      ↓
 Controller
      ↓
 Orchestrator.run(pipelineId, DiscordPayload)
   ├── IngestStep     (stepOrder=0)   ← DiscordPayload → PipelineInput
   ├── LineSplitStep  (stepOrder=1)
   ├── SummaryStep    (stepOrder=2)
   └── DeliveryStep   (stepOrder=99)  ← Summary를 Discord로 전송
```

컨트롤러는 `orchestrator.run()`만 호출합니다.

---

## `stepOrder`의 역할

`stepOrder`는 **실행 순서**와 **재시작 위치**를 결정하는 정수 키입니다.

```
Orchestrator
  └── TreeMap<Integer, Step>  (stepOrder 오름차순 정렬)
        ├── 0  → IngestStep
        ├── 1  → Step A
        ├── 2  → Step B
        └── 99 → DeliveryStep
```

**최초 실행 (체크포인트 없음):**
- 재시작 위치: `INGEST_STEP_ORDER - 1 = -1`
- `tailMap(-1, false)` → `{0, 1, 2, ..., 99}` — IngestStep부터 DeliveryStep까지 순서대로 실행

**IngestStep(0) 성공 후 Step A(1) 실패, 재시도:**
- 마지막 체크포인트: stepOrder=0 (IngestStep)
- `tailMap(0, false)` → `{1, 2, ..., 99}` — IngestStep 건너뜀, Step A부터 재실행

**제약 조건:**
- `stepOrder` 값은 모든 Step 사이에서 **유일**해야 합니다.
- IngestStep은 반드시 stepOrder=0, DeliveryStep은 반드시 stepOrder=99이어야 합니다.
- 처리 Step의 stepOrder는 1~98 사이의 값을 사용합니다.
- 중간에 Step을 추가할 가능성이 있다면 간격을 크게 두는 것을 권장합니다. (예: 10, 20 설정 후 15 추가)

---

## Step 구현 예시

### IngestStep

Discord 웹훅 페이로드를 `PipelineInput`으로 변환합니다. 컨트롤러가 `orchestrator.run()`에 전달한 `initial`을 캐스팅합니다.

```java
// 컨트롤러가 orchestrator.run()에 initial로 전달하는 StepData
public record DiscordPayload(String text, String userId) implements StepData {}

// IngestStep 출력 — 이후 Step의 입력
public record PipelineInput(
        String text,
        String userId,
        String receivedAt
) implements StepData {}
```

```java
@Component
public class IngestStep implements Step {

    private final ObjectMapper objectMapper;

    public IngestStep(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Integer getStepOrder() {
        return 0;
    }

    @Override
    public StepData execute(String pipelineId, StepData input) {
        DiscordPayload payload = (DiscordPayload) input;
        return new PipelineInput(
                payload.text(),
                payload.userId(),
                LocalDateTime.now().toString()
        );
    }

    @Override
    public String serialize(StepData result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new NonRecoverableException("직렬화 실패", e);
        }
    }

    @Override
    public StepData deserialize(String json) {
        try {
            return objectMapper.readValue(json, PipelineInput.class);
        } catch (JsonProcessingException e) {
            throw new NonRecoverableException("역직렬화 실패", e);
        }
    }
}
```

---

### LineSplitStep (처리 단계 예시)

문서 본문을 줄 단위로 분리하고 빈 줄을 제거합니다.

```java
// LineSplitStep 입력 — IngestStep 출력 그대로 사용
// PipelineInput (위 참고)

// LineSplitStep 출력
public record SplitDocument(
        List<String> lines,
        int totalLines
) implements StepData {}
```

```java
@Component
public class LineSplitStep implements Step {

    private final ObjectMapper objectMapper;

    public LineSplitStep(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Integer getStepOrder() {
        return 1;
    }

    @Override
    public StepData execute(String pipelineId, StepData input) {
        PipelineInput pipelineInput = (PipelineInput) input;
        List<String> lines = Arrays.stream(pipelineInput.text().split("\\r?\\n"))
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .toList();
        return new SplitDocument(lines, lines.size());
    }

    @Override
    public String serialize(StepData result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new NonRecoverableException("직렬화 실패", e);
        }
    }

    @Override
    public StepData deserialize(String json) {
        try {
            return objectMapper.readValue(json, SplitDocument.class);
        } catch (JsonProcessingException e) {
            throw new NonRecoverableException("역직렬화 실패", e);
        }
    }
}
```

---

### DeliveryStep

`pipelineId`로 Summary를 직접 조회해 Discord로 전송합니다. `input`은 사용하지 않습니다.

```java
// DeliveryStep 출력
public record DeliveryResult(boolean sent) implements StepData {}
```

```java
@Component
public class DeliveryStep implements Step {

    private final SummaryRepository summaryRepository;
    private final DiscordClient discordClient;
    private final ObjectMapper objectMapper;

    public DeliveryStep(SummaryRepository summaryRepository,
                        DiscordClient discordClient,
                        ObjectMapper objectMapper) {
        this.summaryRepository = summaryRepository;
        this.discordClient = discordClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Integer getStepOrder() {
        return 99;
    }

    @Override
    public StepData execute(String pipelineId, StepData input) {
        Summary summary = summaryRepository.findByPipelineId(pipelineId);
        discordClient.send(summary.toMessage());
        return new DeliveryResult(true);
    }

    @Override
    public String serialize(StepData result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new NonRecoverableException("직렬화 실패", e);
        }
    }

    @Override
    public StepData deserialize(String json) {
        try {
            return objectMapper.readValue(json, DeliveryResult.class);
        } catch (JsonProcessingException e) {
            throw new NonRecoverableException("역직렬화 실패", e);
        }
    }
}
```

---

## 전체 실행 흐름

```
Controller.run(pipelineId, DiscordPayload)
  │
  └── Orchestrator.run(pipelineId, DiscordPayload)
        ├── loadPipeline()          — Pipeline 행 조회 또는 생성
        ├── incrementRunCount()     — 실행 전 저장 (runCount=1)
        ├── loadCheckpoint()        — 체크포인트 없음 → stepOrder=-1 에서 시작
        │
        ├── IngestStep (stepOrder=0)
        │     execute(DiscordPayload) → PipelineInput
        │     Checkpoint(stepOrder=0) 저장
        │
        ├── LineSplitStep (stepOrder=1)
        │     execute(PipelineInput) → SplitDocument
        │     Checkpoint(stepOrder=1) 저장
        │
        ├── SummaryStep (stepOrder=2)
        │     execute(SplitDocument) → Summary (저장)
        │     Checkpoint(stepOrder=2) 저장
        │
        ├── DeliveryStep (stepOrder=99)
        │     execute(Summary) → DeliveryResult
        │     Checkpoint(stepOrder=99) 저장
        │
        └── pipeline.markCompleted()  → status=SUCCESS
```

---

## 실패 후 재시도 흐름

IngestStep(stepOrder=0) 실패 시:

```
1회차 실행 (runCount=1):
  IngestStep → 예외 발생 (Checkpoint 없음)
    └── Orchestrator: pipeline.status=RUNNING 유지

재시도: Controller.run(pipelineId, DiscordPayload) 재호출

2회차 실행:
  loadCheckpoint()    → 체크포인트 없음 → stepOrder=-1 에서 시작
  tailMap(-1, false)  → {0, 1, ..., 99}
  IngestStep → 처음부터 재실행
```

IngestStep 성공 후 LineSplitStep(stepOrder=1) 실패 시:

```
1회차 실행 (runCount=1):
  IngestStep    → Checkpoint(stepOrder=0) 저장 ✓
  LineSplitStep → 예외 발생

재시도: Controller.run(pipelineId, DiscordPayload) 재호출

2회차 실행:
  loadCheckpoint()   → Checkpoint(stepOrder=0) 조회
  tailMap(0, false)  → {1, 2, ..., 99}
  IngestStep ← 건너뜀
  LineSplitStep → 재실행 (Checkpoint에서 PipelineInput 역직렬화해 입력으로 사용)
```

runCount 한계(3) 도달 시:

```
3회차 실패 후 4회차 호출:
  loadPipeline()       → runCount=3
  isRunCountExceeded() → true
    └── pipeline.markFailed() → status=FAILED, 저장
  isFailed()           → true
    └── NonRecoverableException 발생, 재시도 중단
```

---

## 주요 제약 사항

| 규칙 | 이유 |
|---|---|
| `getStepOrder()`는 Step마다 유일한 값을 반환해야 한다 | `TreeMap` 키이므로 중복 시 기존 Step이 덮어써진다 |
| IngestStep은 stepOrder=0, DeliveryStep은 stepOrder=99이어야 한다 | Orchestrator의 `INGEST_STEP_ORDER`, `DELIVERY_STEP_ORDER` 상수와 일치해야 한다 |
| `serialize`와 `deserialize`는 서로 역함수여야 한다 | 역직렬화된 값이 재시도 시 다음 `execute` 호출에 직접 전달된다 |
| `execute`에서 `input`을 기대하는 구체 타입으로 캐스팅한다 | 타입 계약이 위치 기반이며 컴파일러가 강제하지 않는다 |
| 영구 실패 시 `NonRecoverableException`을 던진다 | Orchestrator가 이를 받아 파이프라인을 FAILED로 표시하고 재시도를 중단한다 |
