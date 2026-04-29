---
name: tdd-run
description: |
  .claude/docs/{feature}.md 기반으로 RED-GREEN-REFACTOR 사이클 실행.
  "tdd-run", "구현 시작", "plan 확인했어", "이제 구현해줘", "RED부터 시작", "테스트 작성해줘" 시 이 스킬을 사용.
  반드시 tdd-plan 스킬 실행 후 plan.md 검토가 완료된 상태에서 실행한다.
---

> **전제 조건**: `.claude/docs/{feature}.md` 존재 및 사용자 검토 완료.

## STEP 0 — 플랜 로드

1. `mcp__plugin_serena_serena__check_onboarding_performed` 호출
   - 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation`
   - 미완료: 중단 — "먼저 `/tdd-plan` 을 실행하세요."
2. `.claude/docs/{feature}.md` Read. 없으면 중단. (`{feature}`는 대화 컨텍스트에서 파악한다.)
3. `git log --oneline -3 -- src/main/` — 최근 src/main 변경 내역을 파악해 컨텍스트로 활용한다.

같은 대화에서 tdd-plan이 실행됐으면 1·2번을 건너뛴다.

---

## STEP 1 — RED (실패 테스트 작성)

`.claude/docs/{feature}.md`의 **단계별 할 일**·**제약**을 기반으로 테스트를 도출한다.

### 탐색 제약

- 코드베이스 탐색은 테스트 작성에 필요한 최소한의 컨텍스트(인터페이스, 컨벤션, 경로)만 수집하는 것으로 제한한다. 
- 구현 로직을 이해하려는 탐색은 하지 않는다.

### 테스트 레벨 결정

| 레벨 | 적합한 검증 대상 |
|---|---|
| 단위 | 비즈니스 로직, 실행 순서, 조건 분기 |
| `@DataJpaTest` | JPA 영속화, 레포지토리, DB 상태 전이 |
| `@SpringBootTest` | Security 필터, 다수 레이어가 얽혀 분리 불가한 경우 |

`@SpringBootTest`는 단위·DataJpaTest로 검증 불가할 때만 사용한다.

### 테스트 유효성 검증

테스트 후보 목록을 확인 표에 올리기 전, 각 항목을 아래 세 기준으로 평가한다. 하나라도 통과하지 못하면 수정하거나 제거한다.

| 기준 | 검증 질문 | 기준 미달 시 처리 |
|---|---|---|
| 결함 탐지력 | 이 테스트 없이 잘못된 구현이 빌드를 통과할 수 있는가? | 제거 |
| 실패 시나리오 | 이 테스트를 실패시키는 구체적인 입력·상태·순서를 한 줄로 기술할 수 있는가? | 특정 불가 시 제거 |
| 구현 독립성 | 테스트가 "어떻게 구현되는가"가 아닌 "무엇을 해야 하는가"를 검증하는가? | 외부 동작 기준으로 재작성 |

세 기준을 모두 통과한 테스트만 확인 표에 포함한다.

### 확인 후 작성

테스트 작성 전 보고 (클래스별 소제목 + 표):

**`{TestClassName}`**

| 레벨 | 메서드 | 검증 내용 |
|---|---|---|
| {단위\|DataJpa\|통합} | `{메서드명}` | **반드시** {한 줄 설명} |

> **검증 내용 칸은 생략 불가. 어떤 동작을 검증하는지 반드시 명시한다.**

클래스가 여러 개면 위 블록을 반복한다. 레벨 순서대로 그룹화한다 (단위 → DataJpa → 통합). 사용자 확인 후 작성한다.

### 작성 규칙

- 메서드: Given-When-Then 패턴
- 메서드 위 한국어 주석
- 클래스/메서드명: 영어 PascalCase/camelCase

### FAIL 확인

레벨 순서대로 개별 실행해 FAIL 확인한다 (단위 → DataJpa → 통합):
```
./gradlew test --tests "*{클래스명}" -q
```

보고: "RED 완료. 실패 테스트: [목록]"

---

## STEP 2 — GREEN (최소 구현)

### 코드 탐색

`.claude/docs/{feature}.md` `# 구현 범위` 섹션에 명시된 레이어·컴포넌트 범위로 탐색을 제한한다.
`mcp__plugin_serena_serena__find_symbol` 또는 `mcp__plugin_serena_serena__get_symbols_overview`로 관련 심볼만 핀포인트 조회한다.
`find_file`·`Glob`·`Grep`으로 디렉토리 전체를 스캔하지 않는다. 범위 밖 탐색이 필요해지면 사용자에게 먼저 물어본다.

### 구현 규칙

- 테스트를 통과하는 최소 구현만 작성. `.claude/docs/{feature}.md` 스코프 Out 항목 구현 금지.
- 기존 패턴은 Serena 조회 결과에서 파악한다.

### 레벨별 실행

단위 → DataJpa → 통합 순으로 진행한다:
1. 해당 레벨 테스트 실행:
   ```
   ./gradlew test --tests "*{클래스명}"
   ```
2. 통과 시 다음 레벨로 진행.
3. 실패 시 구현만 수정 (테스트 수정 금지). 레벨당 최대 2회 재시도.
4. 2회 후 잔존 실패 시 즉시 중단:
   보고: "[N]개 테스트 실패. 실패 내용: [목록]. 확인 후 방향을 알려주세요."

보고: "GREEN 완료. [N]개 테스트 통과."

---

## STEP 3 — REFACTOR

테스트가 검증하는 동작을 변경하지 않는 범위만 정리한다.

GREEN에서 작성한 파일 대상으로 `mcp__plugin_serena_serena__find_symbol`로 필요한 심볼만 조회한다.

허용: 중복 제거, 네이밍 개선, 매직 값 상수화, 불필요한 복잡도 제거
금지: 기능 추가, 새 추상화, 테스트 구조 변경

단위 → DataJpa → 통합 순으로 회귀 확인:
```
./gradlew test --tests "*{클래스명}"
```

보고: "REFACTOR 완료. [변경 내용 또는 '변경 없음']"

---

## 완료

보고:
```
구현 완료.
- RED: [N]개 테스트 작성
- GREEN: [N]개 통과
- REFACTOR: [변경 내용 또는 '변경 없음']
```

`/tdd-review` 로 구현 결과를 검토하거나 `/commit-commands:commit` 으로 커밋할 수 있습니다.
