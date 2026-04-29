---
name: tdd-plan
description: |
  TDD 구현 전 온보딩 및 구현 계획(plan.md) 수립.
  "계획", "plan", "설계 먼저", "구현 전에 확인", "어떻게 구현될지", "tdd-plan", "M2 계획", "구현 예측" 시 이 스킬을 사용.
  tdd-run 스킬 실행 전에 반드시 먼저 실행한다.
---

## STEP 0 — ONBOARDING

`mcp__plugin_serena_serena__check_onboarding_performed` 호출.
- 미완료: `mcp__plugin_serena_serena__onboarding`
- 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation`

Read 도구로 읽는다:
- `docs/{feature}/spec.md`
- `docs/{feature}/discuss.md`

spec.md가 없으면 사용자에게 알리고 중단한다.
discuss.md가 없으면 ADR 없음으로 처리하고 계속한다.

목표 해석에 도움이 된다고 판단되면 `git log --oneline -5`를 실행한다.

---

## STEP 1 — 목표 추론

spec.md(와 읽었다면 git log)를 바탕으로 이번 작업에서 사용자가 원하는 것을 해석해서 제시한다.

형식:
> 이번에 **{목표 한 줄 요약}** 을 하려는 것으로 이해했습니다.
> {spec 기반 근거 2-3줄 — 어떤 부분에서 그렇게 읽었는지 명시}
> 맞나요? 수정이 필요하면 말씀해 주세요.

사용자가 확정하면 다음 단계로 진행. 보정이 있으면 목표를 업데이트하고 재확인.

---

## STEP 2 — 비즈니스 레벨 인터뷰

확정된 목표를 기준으로 질문-답변 라운드를 반복한다. 라운드당 질문은 3개 이하로 집중한다.

**질문 선택 기준** (우선순위 순):
1. 이 기능이 "완료"됐을 때 어떤 동작·결과가 보여야 하는가
2. spec에서 발견한 모호함·모순·누락
3. 스코프 경계 — 이번에 하는 것 vs 명시적으로 제외하는 것
4. 예상 엣지케이스 (장애 상황, 순서 문제, 상태 전이, 동시성 등)

**종료 조건**: 아래 항목 모두에 확신이 생기면 인터뷰를 종료한다:
- 완료 기준이 명확한가
- 스코프가 명확한가
- 주요 엣지케이스가 식별됐는가
- 아키텍처 방향이 충분히 보이는가

아직 불확실한 항목이 남아 있으면 라운드를 계속한다.

---

## STEP 3 — 구현 계획 작성

**두 파일**을 Write 도구로 생성한다.

### 아키텍처 결정

discuss.md에 관련 ADR이 있으면 결정사항과 기각된 대안을 반영한다.
관련 ADR이 없으면 선택지와 장단점을 사용자에게 제시하고, 사용자가 선택한 후 plan에 기록한다.

### `docs/{feature}/plan.md` — 인간용 (why + context)

결정의 이유와 맥락을 담는다. 인터뷰에서 논의한 배경도 보존한다.

````markdown
# 목표

{목표 한 줄 — 구현 대상과 방향}

{왜 필요한지 1문장. 재설계/구현 후 어떻게 달라지는지 1문장}

# 아키텍처 결정

- **{결정}** _({ADR-{N}})_  
  {어떤 문제를 해결하고, 왜 이 방식인지 1-2문장}

- **{결정}** (ADR 없는 경우 생략)  
  {이유 1-2문장}

# 스코프

**In**
- {항목}

**Out**
- {항목}

# 엣지케이스 & 제약

- **{케이스}**: {처리 방향} — {이유 또는 배경}
````

### `.claude/docs/{feature}.md` — agent용 (what to do)

언어: 영문으로 작성한다.

````markdown
# Implementation Scope

{layers and components tdd-run should explore}

# Scope Out

- {items that must not be implemented}

# Constraints

- {rules that affect code correctness only — rule without reason}

# Step N — {name}

{task list (What, not How)}

Green condition: {pass criteria}
````

---

## STEP 4 — 사용자 검토 및 피드백 처리

"계획을 `docs/{feature}/plan.md`와 `.claude/docs/{feature}.md`에 저장했습니다. 검토 후 수정이 필요하면 말씀해 주세요.
확인 후 `/tdd-run` 으로 구현을 시작하세요."

수정 요청은 **비판적으로 평가**한다. 아래 경우 거절하고 근거를 설명한다:
- ADR 기각된 대안 채택 (discuss.md 참조)
- 인터뷰에서 합의한 스코프 외 확장

거절이 아닌 경우: Edit 도구로 **해당 섹션만** 수정한다 (전체 재작성 금지).
수정 후 변경 내용을 한 줄 요약하고 재검토를 요청한다.

---

## STEP 5 — 승인 후 압축

사용자가 최종 승인하거나 `/tdd-run` 진행을 명시하면 Write 도구로 두 파일을 압축해 덮어쓴다.

`docs/{feature}/plan.md`:
- **제거**: 인터뷰 대화 반복, 중복 문장
- **유지**: 목표(맥락 포함), 아키텍처 결정(이유 1-2문장 포함), 스코프 In/Out, 엣지케이스(처리 방향 + 배경 포함)

`.claude/docs/{feature}.md`:
- **제거**: 실행에 불필요한 맥락, 중복 설명
- **유지**: 구현 범위, 스코프 Out, 제약 목록, 단계별 할 일, 그린 조건
