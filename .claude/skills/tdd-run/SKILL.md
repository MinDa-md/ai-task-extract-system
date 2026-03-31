---
name: tdd-run
description: |
  plan.md 기반으로 RED-GREEN-REFACTOR 사이클 실행. 인수는 "{feature} {milestone}" 형식 (예: "orchestrator M2").
  "tdd-run", "구현 시작", "plan 확인했어", "이제 구현해줘", "RED부터 시작", "테스트 작성해줘" 시 이 스킬을 사용.
  반드시 tdd-plan 스킬 실행 후 plan.md 검토가 완료된 상태에서 실행한다.
---

> **전제 조건**: `docs/{feature}/plan.md` 가 존재하고 사용자가 검토 완료한 상태여야 한다.
> **인수 형식**: `{feature} {milestone}` — 예: `orchestrator M2`

## STEP 0 — 플랜 로드 (교차 세션 시에만 실행)

**이 STEP은 같은 대화에서 tdd-plan이 이미 실행된 경우 전부 건너뛴다.**
plan.md 내용이 현재 컨텍스트에 있으면 재읽기가 불필요하다.

같은 대화에서 tdd-plan이 실행되지 않은 경우(별도 대화에서 tdd-run만 호출):
1. `mcp__plugin_serena_serena__check_onboarding_performed` 호출
   - 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation` 호출
   - 미완료: 사용자에게 알린다 — 먼저 `/tdd-plan {feature} {milestone}` 을 실행해야 한다
2. Read 도구로 `docs/{feature}/plan.md` 를 읽는다.
   파일이 없으면 중단: "plan.md 가 없습니다. 먼저 `/tdd-plan {feature} {milestone}` 을 실행해 주세요."
3. `git log --oneline -1 -- src/main/` 으로 마지막 src/main 커밋을 확인한다.
   커밋 메시지가 plan.md의 `{milestone}` 이후 작업으로 보이면 사용자에게 알린다:
   "plan.md 생성 후 src/main 변경이 감지됐습니다. plan.md가 현재 코드베이스와 일치하는지 확인해 주세요."

plan.md 에서 다음을 추출한다:
- 생성/수정할 파일 목록과 경로
- 테스트 클래스명, 메서드명, 검증 내용
- 테스트 인프라 구조 (@TestConfiguration, stub 등)
- 주요 설계 결정사항

---

## STEP 1 — RED (실패 테스트 작성)

plan.md 의 테스트 케이스를 그대로 Write 도구로 작성한다. **사용자 확인 없이 진행한다** (plan.md 검토에서 이미 합의됨).

테스트 작성 규칙:
- 메서드는 Given-When-Then 패턴
- 각 테스트 메서드 바로 위에 한국어 주석 (plan.md 의 "한국어 설명" 컬럼 사용)
- 클래스/메서드명은 영어 PascalCase/camelCase

테스트를 실행해 RED를 확인한다 (plan.md에서 추출한 실제 클래스명으로 대체):
```
./gradlew test --tests "*실제테스트클래스명" -q
```

예상되는 RED 결과 (둘 다 정상):
- 구현체 미존재: `BUILD FAILED` (컴파일 오류)
- 구현체 존재하나 로직 미완성: 테스트 런타임 실패

사용자에게 보고한다:
"RED 완료. 실패 테스트: [목록] — 모두 의도한 실패입니다."

---

## STEP 2 — GREEN (최소 구현)

plan.md 의 "생성/수정 파일" 섹션을 순서대로 구현한다. **사용자 확인 없이 진행한다**.

기존 코드 패턴이 필요한 경우 (bash 사용 금지):
- plan.md에 어노테이션/패키지 규칙이 명시됐으면 그대로 사용한다
- plan.md에 없는 패턴은 `mcp__plugin_serena_serena__find_symbol` 로 기존 유사 심볼만 직접 조회한다

구현 원칙:
- plan.md 의 메서드 시그니처와 로직 흐름을 따름
- 테스트를 통과하는 최소한의 코드만 작성
- plan.md 에 없는 추상화/기능 추가 금지

테스트 실행 (실패 원인 파악을 위해 `-q` 없이):
```
./gradlew test --tests "*실제테스트클래스명"
```

실패 잔존 시 구현만 수정한다 (테스트 수정 금지). **테스트 재실행을 최대 2회까지 반복한다** (재실행 1회 = 1회 차감).
2회 후에도 실패 시 중단하고 사용자에게 보고:
"[N]개 테스트가 2회 재실행 후에도 실패합니다. 실패 내용: [목록]. 확인 후 방향을 알려주세요."

사용자에게 보고한다:
"GREEN 완료. [N]개 테스트 통과."

---

## STEP 3 — REFACTOR

REFACTOR는 GREEN에서 통과한 테스트가 검증하는 **동작을 변경하지 않는** 범위의 정리만 수행한다.
동작이나 구조가 바뀌는 개선은 REFACTOR가 아니라 다음 마일스톤의 범위다.

GREEN에서 작성한 파일들을 대상으로, `mcp__plugin_serena_serena__find_symbol` 로 정리가 필요한 심볼만 바로 읽는다.
GREEN에서 이미 심볼 구조를 파악했으므로 `get_symbols_overview` 재조회는 불필요하다.

허용: 중복 제거, 네이밍 개선, 매직 넘버/문자열 상수화, 불필요한 복잡도 제거
금지: 기능 추가, 새 추상화 도입, 테스트 구조 변경, 동작에 영향을 주는 모든 변경

테스트 재실행해 회귀 없음 확인:
```
./gradlew test --tests "*실제테스트클래스명"
```

---

## 완료

사용자에게 보고한다:
"구현 완료.
- RED: [N]개 테스트 작성
- GREEN: [N]개 통과
- REFACTOR: [변경 내용 또는 '변경 없음']"

`/commit-commands:commit` 을 호출한다.
