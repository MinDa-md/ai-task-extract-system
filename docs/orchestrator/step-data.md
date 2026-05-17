# StepData 목록

파이프라인 흐름: `DiscordPayload` → Ingest → `RawChatLog` → Filter → `AnonymizedChatLog` → AI → `AiSummary` → DB →
`SavedSummary` → Delivery → `DeliveryResult`

## 공통 DTO

| 클래스명        | 필드                                                                    |
|-------------|-----------------------------------------------------------------------|
| PipelineDto | `requestKey: String`, `channelId: String`                             |
| SummaryDto  | `meetingTime: String`, `location: String`, `participantsInfo: String` |

## ingest

| 클래스명           | 구분         | 필드                       |
|----------------|------------|--------------------------|
| DiscordPayload | 초기 입력 (외부) | `chatLogs: List<String>` |
| RawChatLog     | Output     | `rawText: String`        |

## filter

| 클래스명              | 구분     | 필드                                                        |
|-------------------|--------|-----------------------------------------------------------|
| AnonymizedChatLog | Output | `filteredText: String`, `dictionary: Map<String, String>` |

## ai

| 클래스명      | 구분     | 필드                                                       |
|-----------|--------|----------------------------------------------------------|
| AiSummary | Output | `summary: SummaryDto`, `dictionary: Map<String, String>` |

## db

| 클래스명         | 구분     | 필드                                                       |
|--------------|--------|----------------------------------------------------------|
| SavedSummary | Output | `summary: SummaryDto`, `dictionary: Map<String, String>` |

## delivery

| 클래스명           | 구분     | 필드              |
|----------------|--------|-----------------|
| DeliveryResult | Output | `sent: boolean` |

## 참고

- `PipelineDto`는 step data가 아닌 `execute(PipelineDto context, I input)` 파라미터로 모든 단계에 전달된다. step data 클래스는 도메인 변환 결과만 담는다.
- `dictionary`(`Map<String, String>`)는 Filter → DB까지 raw Map으로 전달, JSON 직렬화는 DbStep 내부에서 수행.
- `DiscordPayload`는 Orchestrator.run()에 전달되는 초기 StepData다. Input 클래스가 제거됐으며, 각 Step은 이전 Step의 Output을 직접 입력으로 받는다.
- 각 Step의 입출력 타입은 `Step<I, O>` 선언으로 명시된다.
