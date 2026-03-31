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

### 파일 경로 표기 규칙

Java 소스 경로의 공통 prefix를 별칭으로 압축한다:
- `src/main/java/com/…/` → `[main]/`
- `src/test/java/com/…/` → `[test]/`

파일 목록 첫 줄에 별칭 정의를 명시하고, 이후 경로는 별칭으로만 표기한다.

### 테스트 레벨 선택 원칙

테스트는 **단위 테스트 → DataJpaTest → 통합 테스트** 순으로 낮은 레벨부터 검토한다.

| 레벨 | 적합한 검증 대상 |
|---|---|
| 단위 테스트 | 비즈니스 로직, 실행 순서, mock 호출 검증 |
| `@DataJpaTest` | JPA 엔터티 영속화, 레포지토리 쿼리, 실제 DB 상태 전이 |
| `@SpringBootTest` (통합) | Spring Security 필터 체인, 다수 레이어(비즈니스+외부의존성+DB)가 모두 얽혀 분리 불가한 경우 |

**통합 테스트(`@SpringBootTest`)는 단위 테스트와 DataJpaTest로 검증할 수 없을 때만 사용한다.**
plan.md에 통합 테스트를 포함할 경우 반드시 정당한 근거를 명시해야 한다.

### plan.md 형식

```markdown
# {milestone} 구현 계획

> [main] = src/main/java/com/{…}/
> [test] = src/test/java/com/{…}/

## 생성/수정 파일

### [create|modify] [main]/{상대경로}
- 클래스/인터페이스 선언 (어노테이션 포함)
- 필드: `{타입} {이름}`
- `{반환타입} {메서드명}({파라미터})` — 역할 한 줄

## 테스트 케이스

### {TestClassName} ({단위|DataJpa|통합})
패키지: [test]/{상대경로}
어노테이션: {어노테이션 목록}
인프라: {stub·mock·helper 한 줄 설명}

| 메서드명 | 한국어 설명 | 검증 내용 |
|---|---|---|
| {camelCase} | {한국어} | {assert 내용} |

## 주요 설계 결정

- {결정}: {이유 한 줄} (ADR-{N})
```

---

## STEP 3 — 사용자 검토 및 피드백 처리

사용자에게 설명한다:

"plan.md 를 `docs/{feature}/plan.md` 에 저장했습니다.
검토 후 수정이 필요하면 파일을 직접 편집하거나 여기서 말씀해 주세요.
확인이 끝나면 `/tdd-run {feature} {milestone}` 으로 구현을 시작하세요."

### 피드백 처리 원칙

사용자 수정 요청을 받으면 **비판적으로 평가**한다. 아래 경우 거절하고 근거를 설명한다:

- ADR의 **기각된 대안**을 채택하는 요청 (discuss.md 참조)
- 낮은 테스트 레벨로 충분한데 높은 레벨을 요구하는 경우
- 설계 복잡도를 높이지만 M2 완료 조건과 무관한 범위 확장

거절이 아닌 경우: Edit 도구로 plan.md의 **해당 섹션만** 수정한다 (전체 재작성 금지).
수정 후 변경 내용을 한 줄로 요약하고 재검토를 요청한다.

---

## STEP 4 — 사용자 승인 후 plan.md 압축

사용자가 plan.md를 최종 승인하면 tdd-run이 실행할 수 있는 최소 형식으로 압축한다.

**제거 대상** (tdd-run에 불필요):
- 설계 결정 이유 설명 (ADR 참조 번호만 유지)
- 테스트 레벨 선택 근거 서술
- `>` 인용 블록의 설계 논의 내용
- 중복되는 설명 문장

**유지 대상** (tdd-run 실행에 필수):
- 파일 경로 및 클래스/필드/메서드 선언
- 테스트 케이스 표 전체
- 어노테이션·인프라 설정
- 설계 결정 항목 (이유 제거, 결정만 유지)

압축 후 Write 도구로 plan.md를 덮어쓴다.
