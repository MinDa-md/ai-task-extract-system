---
name: tdd-refactor
description: |
  REFACTOR 단계 — 구현 결과 검토 및 리팩토링
---

## STEP 0 — 로드

`mcp__plugin_serena_serena__check_onboarding_performed` 호출
- 미완료: `mcp__plugin_serena_serena__onboarding`
- 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation`

Read 도구로 읽음:
- `docs/{feature}/plan.md`
- `docs/{feature}/discuss.md`

`git status --short`로 변경·추가된 파일 목록을 파악
변경 파일의 테스트·구현 코드를 `mcp__plugin_serena_serena__read_file`로 읽음

---

## STEP 1 — 테스트 품질 검토

> 원칙: 추론이 확실하지 않으면 이슈로 기록하지 않음

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
| F. 스코프 초과 | WARN | plan.md `스코프 아웃` 항목에 해당하는 코드가 추가됨 |
| G. SOLID 위반 | WARN | S: 책임 혼재, O: 기존 public 시그니처 파괴, L: 계약 축소, I: 불필요한 메서드 노출, D: 구체 클래스 직접 생성 |
| H. 레벨 일관성 | WARN | 데이터 객체가 실행 로직을 참조하는가(데이터의 실행 수준 승격), 실제로 사용되지 않는 상태·필드가 존재하는가, 구현 후 제거해도 동작에 무관한 개념이 생기지 않았는가, 각 클래스의 책임을 한 문장으로 기술할 수 있는가 |

---

## STEP 3 — 이슈 출력

```
## tdd-refactor

| # | 심각도 | 기준 | 위치 | 문제 | 제안 |
|---|--------|------|------|------|------|
| 1 | BLOCK  | A    | …    | …    | …    |
| 2 | WARN   | G-S  | …    | …    | …    |
```

BLOCK이 있으면 수정 후 STEP 4 진행:
- A·B·C: 사용자 확인 없이 수정
- E: 수정 방향을 사용자에게 제안하고 승인 후 수정

---

## STEP 4 — 리팩토링

BLOCK이 모두 해결된 후 진행

변경된 구현 파일을 대상으로 리팩토링 수행
- 허용: 중복 제거, 네이밍 개선, 매직 값 상수화, 불필요한 복잡도 제거, 과도한 추상화 제거
- 금지: 기능 추가, 동작 변경, 테스트 수정

리팩토링 후 전체 변경 파일 회귀 확인:
```
./gradlew test --tests "*{변경된테스트클래스}"
```

보고:
```
리팩토링 완료.
- {변경 파일}: {변경 내용 한 줄}
```
변경 없으면 '변경 없음'으로 표기
