---
name: tdd-plan
description: |
  TDD 구현 전 온보딩 및 구현 계획(plan.md) 수립. 인수는 "{feature} {milestone}" 형식 (예: "orchestrator M2").
  "계획", "plan", "설계 먼저", "구현 전에 확인", "어떻게 구현될지", "tdd-plan", "M2 계획", "구현 예측" 시 이 스킬을 사용.
  tdd-run 스킬 실행 전에 반드시 먼저 실행한다.
---

> **인수 형식**: `{feature} {milestone}` — 예: `orchestrator M2`
> 첫 번째 토큰 = docs 디렉토리명, 두 번째 토큰 = 마일스톤 ID

## STEP 0 — ONBOARDING

`mcp__plugin_serena_serena__check_onboarding_performed` 를 호출한다.
- 미완료: `mcp__plugin_serena_serena__onboarding` 호출 (최초 1회 전체 스캔)
- 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation` 호출 (캐시 로드만)

Read 도구로 다음 파일을 읽는다 (`{feature}` = 첫 번째 인수):
- `docs/{feature}/spec.md`
- `docs/{feature}/milestone.md`
- `docs/{feature}/discuss.md`

`docs/{feature}/spec.md`, `docs/{feature}/milestone.md` 파일이 없으면 사용자에게 알리고 중단한다.

`git log --oneline -3` 으로 이전 완료 커밋을 파악하고, `{milestone}` 단계의 전제 조건이 충족됐는지 확인한다.

---

## STEP 1 — 컨텍스트 분석

읽은 내용에서 다음을 파악한다:
- `{milestone}` 의 **산출물** 목록과 **완료 조건**
- 관련 ADR의 결정사항 및 기각된 대안
- 이미 존재하는 코드 구조: `git log --name-only -3 --pretty=format:"" -- src/main/` 로 최근 변경된 파일 목록을 파악하고, 해당 파일들에만 `mcp__plugin_serena_serena__get_symbols_overview` 를 호출한다

---

## STEP 2 — 구현 계획 작성

`docs/{feature}/plan.md` 를 Write 도구로 생성한다.

plan.md 는 tdd-run 스킬이 사용자 확인 없이 실행할 수 있을 만큼 구체적이어야 한다.

```markdown
# {milestone} 구현 계획

## 생성/수정 파일

### [create|modify] {파일 경로}
- 클래스/인터페이스 선언 (어노테이션 포함)
- 필드 목록 (타입, 이름)
- 메서드 시그니처 목록
  - `{반환타입} {메서드명}({파라미터})` — 역할 한 줄 설명

## 테스트 케이스

### {TestClassName} ({테스트 종류: 단위/DataJpa/통합})
패키지: {패키지 경로}
어노테이션: {필요한 어노테이션 목록}
테스트 인프라: {필요한 @TestConfiguration, stub, helper 설명}

| 메서드명 | 한국어 설명 (주석용) | 검증 내용 |
|---|---|---|
| {camelCase 메서드명} | {한국어} | {assert 내용} |

## 주요 설계 결정

- {결정 사항}: {이유} (ADR-{N} 참조)
```

---

## STEP 3 — 사용자 검토 요청

사용자에게 설명한다:

"plan.md 를 `docs/{feature}/plan.md` 에 저장했습니다.
검토 후 수정이 필요하면 파일을 직접 편집하거나 여기서 말씀해 주세요.
확인이 끝나면 `/tdd-run {feature} {milestone}` 으로 구현을 시작하세요."

사용자가 대화에서 수정을 요청하면 Edit 도구로 plan.md의 해당 섹션만 수정한다 (전체 재작성 금지).
수정 후 변경 내용을 요약하고 재검토를 요청한다.
