---
name: tdd-review
description: |
  tdd-run 구현 결과를 새 세션에서 검토·리팩토링한다. 커밋 전에 실행한다.
  "tdd-review", "구현 검토", "리뷰해줘", "코드 검토", "리팩토링해줘", "커밋 전 검토" 시 이 스킬을 사용.
  tdd-run 완료 후, commit 전에 실행한다.
---

## STEP 0 — 로드

`mcp__plugin_serena_serena__check_onboarding_performed` 호출.
- 미완료: `mcp__plugin_serena_serena__onboarding`
- 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation`

Read 도구로 읽는다:
- `docs/{feature}/plan.md`
- `docs/{feature}/discuss.md`

`git status --short`로 변경·추가된 파일 목록을 파악한다.
변경 파일의 테스트·구현 코드를 `mcp__plugin_serena_serena__read_file`로 읽는다.

---

## STEP 1 — 테스트 품질 검토

> **원칙**: 추론이 확실하지 않으면 이슈로 기록하지 않는다.

| 기준 | 심각도 | 점검 내용 |
|------|--------|-----------|
| A. 테스트 레벨 | BLOCK | SpringBoot·DataJpa 선택 근거가 실제로 정당한가. 단위 테스트로 충분한데 상위 레벨을 사용 |
| B. 블랙박스 커버리지 | BLOCK | plan.md 요구사항(기능 동작) 중 테스트로 검증되지 않은 경로 |
| C. 검증 내용 | BLOCK | assert가 의미 있는 동작을 잡는가. 구현 세부사항에 종속된 assert |
| D. 누락 케이스 | WARN | plan.md 엣지케이스·경계값 중 테스트 없는 것 |

---

## STEP 2 — 구현 품질 검토

| 기준 | 심각도 | 점검 내용 |
|------|--------|-----------|
| E. ADR 위반 | BLOCK | discuss.md의 기각된 대안을 구현이 채택 |
| F. 스코프 초과 | WARN | plan.md `Out` 항목에 해당하는 코드가 추가됨 |
| G. SOLID 위반 | WARN | S: 책임 혼재, O: 기존 public 시그니처 파괴, L: 계약 축소, I: 불필요한 메서드 노출, D: 구체 클래스 직접 생성 |

---

## STEP 3 — 이슈 출력

```
## tdd-review

| # | 심각도 | 기준 | 위치 | 문제 | 제안 |
|---|--------|------|------|------|------|
| 1 | BLOCK  | A    | …    | …    | …    |
| 2 | WARN   | G-S  | …    | …    | …    |
```

이슈가 없는 기준은 표에서 생략한다.

BLOCK이 있으면:
> 구현 차단: 위 BLOCK 이슈 수정 후 `/tdd-review` 재실행

BLOCK이 없으면 STEP 4로 진행한다.

---

## STEP 4 — 리팩토링

BLOCK이 없는 경우에만 진행한다.

변경된 구현 파일을 대상으로 리팩토링을 수행한다. **사용자 확인 없이 진행한다.**

허용: 중복 제거, 네이밍 개선, 매직 값 상수화, 불필요한 복잡도 제거, 과도한 추상화 제거
금지: 기능 추가, 동작 변경, 테스트 수정

리팩토링 후 전체 변경 파일에 대한 테스트를 실행해 회귀를 확인한다:
```
./gradlew test --tests "*{변경된테스트클래스}"
```

보고:
```
리팩토링 완료.
- {변경 파일}: {변경 내용 한 줄}
```
변경 없으면 '변경 없음'으로 표기한다.

---

## 완료

`/commit-commands:commit` 으로 커밋할 수 있습니다.
