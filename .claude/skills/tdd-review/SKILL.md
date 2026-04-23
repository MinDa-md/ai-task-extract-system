---
name: tdd-review
description: |
  plan.md를 자동 검토해 구현 전 BLOCK/WARN 이슈를 발견한다. 인수는 "{feature} {milestone}" 형식 (예: "orchestrator M3").
  "plan 검토", "review plan", "tdd-review", "plan 확인해줘", "plan 문제 없어?", "plan 봐줘",
  "구현 시작해도 돼?", "plan.md 검토" 시 이 스킬을 사용.
  tdd-plan 실행 후, tdd-run 실행 전에 반드시 먼저 실행한다.
---

> **인수 형식**: `{feature} {milestone}` — 예: `orchestrator M3`

## STEP 0 — 문서 로드

Read 도구로 읽는다:
- `docs/{feature}/plan.md`
- `docs/{feature}/spec.md`
- `docs/{feature}/milestone.md`
- `docs/{feature}/discuss.md`

파일이 없으면 해당 검토 항목을 "확인 불가"로 표시하고 계속한다.

---

## STEP 1 — 검토 기준

> **원칙**: 추론이 확실하지 않으면 이슈로 기록하지 않는다. 실제 구현에 영향을 주는 이슈만 기록한다.

| 기준 | 심각도 | 점검 내용 |
|------|--------|-----------|
| A. ADR 위반 | BLOCK | discuss.md의 기각된 대안을 plan.md가 채택 |
| B. 범위 초과 | WARN | milestone.md 완료 조건과 무관한 파일 변경 |
| C. 테스트 레벨 | WARN | 비즈니스 로직 검증에 `@DataJpaTest`/`@SpringBootTest` 사용, 또는 DB 전이 검증에 단위 테스트만 사용 |
| D. 테스트 누락 | BLOCK/WARN | 새 `public` 메서드에 테스트 없음(BLOCK), 조건 분기 경로에 테스트 없음(WARN) |
| E. 시그니처 불일치 | BLOCK | 클래스 간 호출 시그니처와 피호출자 정의가 불일치 |
| F. SOLID 위반 | WARN | S: 책임 혼재, O: 기존 `public` 시그니처 파괴, L: 계약 축소, I: 불필요한 메서드 노출, D: 구체 클래스 직접 생성(`new`) |

---

## STEP 2 — 출력

```
## tdd-review: {feature} {milestone}

| # | 심각도 | 기준 | 위치 | 문제 | 제안 |
|---|--------|------|------|------|------|
| 1 | BLOCK  | E    | …    | …    | …    |
| 2 | WARN   | F-S  | …    | …    | …    |
```

이슈가 없는 기준은 표에서 생략한다.

---

BLOCK이 있으면:
> 구현 차단: 위 BLOCK 이슈 수정 후 `/tdd-review {feature} {milestone}` 재실행

BLOCK이 없으면:
> BLOCK 없음 — `/tdd-run {feature} {milestone}` 실행 가능
> (WARN N건은 선택적 개선)
