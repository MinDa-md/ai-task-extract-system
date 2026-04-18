---
name: tdd-run
description: |
  plan.md 기반으로 RED-GREEN-REFACTOR 사이클 실행. 인수는 "{feature} {milestone}" 형식 (예: "orchestrator M2").
  "tdd-run", "구현 시작", "plan 확인했어", "이제 구현해줘", "RED부터 시작", "테스트 작성해줘" 시 이 스킬을 사용.
  반드시 tdd-plan 스킬 실행 후 plan.md 검토가 완료된 상태에서 실행한다.
---

> **전제 조건**: `docs/{feature}/plan.md` 존재 및 사용자 검토 완료.
> **인수 형식**: `{feature} {milestone}` — 예: `orchestrator M2`

## STEP 0 — 플랜 로드 (교차 세션 시에만)

**같은 대화에서 tdd-plan이 실행됐으면 이 STEP 전체를 건너뛴다.**

별도 대화에서 tdd-run만 호출한 경우:
1. `mcp__plugin_serena_serena__check_onboarding_performed` 호출
   - 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation`
   - 미완료: 중단 — "먼저 `/tdd-plan {feature} {milestone}` 을 실행하세요."
2. `docs/{feature}/plan.md` Read. 없으면 중단.
3. `git log --oneline -1 -- src/main/` — plan.md 이후 src/main 변경이 있으면 사용자에게 알린다.

---

## STEP 1 — RED (실패 테스트 작성)

plan.md 테스트 케이스 전체를 Write 도구로 작성한다. **사용자 확인 없이 진행한다.**

- 메서드: Given-When-Then 패턴
- 메서드 위 한국어 주석 (plan.md "한국어 설명" 컬럼 사용)
- 클래스/메서드명: 영어 PascalCase/camelCase

**레벨별 필수 시나리오 — plan.md 테스트 표에 누락이 있으면 작성 전 사용자에게 알린다:**

| 레벨 | 필수 시나리오 |
|---|---|
| 단위 | 정상(happy path), 경계값(boundary), 예외/실패 |
| DataJpa | 저장, 조회, 예외(중복·미존재 등 DB 제약 위반) |
| 통합 | 성공 흐름, 실패/거부 흐름 |

**레벨 순서대로 개별 실행해 FAIL 확인한다** (단위 → DataJpa → 통합):
```
./gradlew test --tests "*{단위테스트클래스}" -q
./gradlew test --tests "*{DataJpaTestClass}" -q
./gradlew test --tests "*{통합테스트클래스}" -q
```
plan.md에 없는 레벨은 건너뛴다.

보고: "RED 완료. 실패 테스트: [목록]"

---

## STEP 2 — GREEN (최소 구현)

plan.md "생성/수정 파일" 순서대로 구현한다. **사용자 확인 없이 진행한다.**

- plan.md에 어노테이션/패키지 규칙이 명시됐으면 그대로 사용한다.
- plan.md에 없는 패턴은 `mcp__plugin_serena_serena__find_symbol` 로만 조회한다. (bash 금지)
- **탐색 제약**: `find_file`·`Glob`·`Grep` 으로 디렉토리 전체를 탐색하지 않는다. 탐색이 필요해지면 무조건 사용자에게 먼저 물어본다.
- 테스트를 통과하는 최소 구현만 작성. plan.md 범위 밖 추상화 금지.

**단위 → DataJpa → 통합 순으로 레벨별 실행한다:**
1. 해당 레벨 테스트 실행 (실패 원인 파악용 `-q` 없이):
   ```
   ./gradlew test --tests "*{해당레벨클래스}"
   ```
2. 통과 시 다음 레벨로 진행.
3. 실패 시 구현만 수정 (테스트 수정 금지). 레벨당 최대 2회 재시도.
4. 2회 후 잔존 실패 시 즉시 중단 — 다음 레벨로 넘기지 않는다.
   보고: "[N]개 테스트 실패. 실패 내용: [목록]. 확인 후 방향을 알려주세요."

보고: "GREEN 완료. [N]개 테스트 통과."

---

## STEP 3 — REFACTOR

테스트가 검증하는 동작을 변경하지 않는 범위만 정리한다.
동작·구조 변경은 다음 마일스톤 범위다.

GREEN에서 작성한 파일 대상으로 `mcp__plugin_serena_serena__find_symbol` 로 필요한 심볼만 조회한다.

허용: 중복 제거, 네이밍 개선, 매직 값 상수화, 불필요한 복잡도 제거  
금지: 기능 추가, 새 추상화, 테스트 구조 변경

**단위 → DataJpa → 통합 순으로 회귀 확인:**
```
./gradlew test --tests "*{해당레벨클래스}"
```

---

## 완료

보고:
```
구현 완료.
- RED: [N]개 테스트 작성
- GREEN: [N]개 통과
- REFACTOR: [변경 내용 또는 '변경 없음']
```
`/commit-commands:commit` 을 호출한다.
