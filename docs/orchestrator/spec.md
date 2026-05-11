## 아키텍처

오케스트레이터 패턴 구현

- `Orchestrator`는 `Step` 인터페이스에만 의존
- `Step` 구현체가 스프링 빈으로 등록되며 `StepOrder`가 클수록 먼저 실행
- Spring은 `Step` 구현체 빈들을 모아 `List<Step>`로 Orchestrator에 주입
- Step들은 순차 선형으로 실행되며 `StepData`가 단계 간 데이터를 전달

---

## 구현

### Pipeline Status

- `RUNNING`: 실행 중, 재시도 가능
- `SUCCESS`: 성공
- `FAILED`: 재시도 불가능

### StepData / Step 인터페이스

`StepData`: 단계 간 데이터 전달의 마커 인터페이스

`Step` 인터페이스:

| 메서드 | 설명 |
|---|---|
| `String getStepOrder()` | Step 고유 식별자 |
| `StepData execute(String pipelineId, StepData input)` | 실패는 예외로만 표현 |
| `String serialize(StepData result)` | 출력을 JSON으로 직렬화 |
| `StepData deserialize(String json)` | JSON을 구체 타입으로 역직렬화 |

예외 클래스: 
- `RecoverableException`: 재시도 가능한 예외
- `NonRecoverableException`: 재시도 불가능한 예외
- `ConcurrentPipelineException`: 이미 진행중인 파이프라인 예외 (동시성)

### StepProxy

`@Aspect` `StepProxy`가 `@Around("execution(* Step.execute(..))")` 로 모든 Step 실행을 가로챔

각 Step의 시작 / 완료(소요 시간 포함) / 실패를 로깅

## DB 구조

```mysql
CREATE TABLE `Pipeline`
(
    `id`              VARCHAR(36) NOT NULL,
    `created_at`      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `completed_at`    TIMESTAMP   NULL,
    `status`          ENUM('RUNNING', 'SUCCESS', 'FAILED', 'FINAL_FAILED') NOT NULL DEFAULT 'RUNNING',
    `run_count`       INT         NOT NULL
);

CREATE TABLE `Checkpoint`
(
    `id`          BIGINT       NOT NULL,
    `step_order`  INT          NOT NULL,
    `result`      TEXT         NULL,
    `created_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `pipeline_id` VARCHAR(36)  NOT NULL
);

ALTER TABLE `Pipeline`
    ADD CONSTRAINT `PK_PIPELINE` PRIMARY KEY (`id`);

ALTER TABLE `Checkpoint`
    ADD CONSTRAINT `PK_Step` PRIMARY KEY (`id`);

ALTER TABLE `Checkpoint`
    ADD CONSTRAINT `FK_Pipeline_TO_Step_1`
        FOREIGN KEY (`pipeline_id`) REFERENCES `Pipeline` (`id`);
```

- Checkpoint는 Step이 성공한 경우에만 생성
