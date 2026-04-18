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

`mcp__plugin_serena_serena__check_onboarding_performed` 호출.
- 미완료: `mcp__plugin_serena_serena__onboarding`
- 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation`

Read 도구로 읽는다:
- `docs/{feature}/spec.md`
- `docs/{feature}/milestone.md`
- `docs/{feature}/discuss.md`

spec.md 또는 milestone.md가 없으면 사용자에게 알리고 중단한다.

`git log --oneline -3` 으로 `{milestone}` 전제 조건 충족 여부를 확인한다.

---

## STEP 1 — 컨텍스트 분석

**탐색 제약**: git log 결과 파일 외 추가 탐색 금지. `find_file`·`Glob`·`Grep` 으로 새 파일을 검색하지 않는다. 탐색이 필요해지면 무조건 사용자에게 먼저 물어본다.

- `{milestone}` 산출물 목록과 완료 조건
- 관련 ADR의 결정사항 및 기각된 대안
- `git log --name-only -3 --pretty=format:"" -- src/main/` 로 최근 변경 파일 파악 → 해당 파일에만 `mcp__plugin_serena_serena__get_symbols_overview` 호출

---

## STEP 2 — 구현 계획 작성

`docs/{feature}/plan.md` 를 Write 도구로 생성한다.

### 파일 경로 표기 규칙

Java 소스 경로를 별칭으로 압축한다:
- `src/main/java/com/…/` → `[main]/`
- `src/test/java/com/…/` → `[test]/`

파일 목록 첫 줄에 별칭 정의를 명시하고, 이후 경로는 별칭으로만 표기한다.

### 테스트 레벨 선택

테스트는 **단위 → DataJpaTest → 통합** 순으로 낮은 레벨부터 검토한다.

| 레벨 | 적합한 검증 대상 |
|---|---|
| 단위 테스트 | 비즈니스 로직, 실행 순서, mock 호출 검증 |
| `@DataJpaTest` | JPA 영속화, 레포지토리 쿼리, DB 상태 전이 |
| `@SpringBootTest` | Security 필터 체인, 다수 레이어가 얽혀 분리 불가한 경우 |

`@SpringBootTest` 는 단위·DataJpaTest로 검증 불가할 때만 사용한다. 포함 시 plan.md에 근거를 명시한다.

### plan.md 형식

````markdown
# {milestone} 구현 계획

> [main] = src/main/java/com/{…}/
> [test] = src/test/java/com/{…}/

## 생성/수정 파일

#### `[main]/{상대경로}` · create
```
{어노테이션} class {ClassName}
  {fieldName}: {Type}  (@어노테이션 필요 시)
  {methodName}({params}): {ReturnType}  — 역할
  static {factory}({params}): {ClassName}  — 생성 팩터리
```

#### `[main]/{상대경로}` · modify
```
~ {클래스명 또는 @Table 변경 등 선언 수정}
+ {추가 필드}: {Type}
- {제거 필드}
~ {변경 필드}: {기존} → {신규}
+ {추가 메서드}({params}): {ReturnType}  — 역할
~ {변경 메서드}: {변경 내용 한 줄}
```

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
````

### 파일 표기 규칙

- `create`: 어노테이션·필드·메서드를 들여쓰기 블록으로 전부 기재
- `modify`: 델타만 기록 — `+` 추가, `-` 제거, `~` 변경. 기존 유지 항목은 기재하지 않는다
- 메서드 시그니처: `{이름}({파라미터}): {반환타입}  — 역할 한 줄`

---

## STEP 3 — 사용자 검토 및 피드백 처리

"plan.md를 `docs/{feature}/plan.md`에 저장했습니다. 검토 후 수정이 필요하면 말씀해 주세요.
확인 후 `/tdd-run {feature} {milestone}` 으로 구현을 시작하세요."

수정 요청은 **비판적으로 평가**한다. 아래 경우 거절하고 근거를 설명한다:
- ADR 기각된 대안 채택 (discuss.md 참조)
- 낮은 레벨로 충분한데 높은 레벨 요구
- {milestone} 완료 조건과 무관한 범위 확장

거절이 아닌 경우: Edit 도구로 **해당 섹션만** 수정한다 (전체 재작성 금지).
수정 후 변경 내용을 한 줄 요약하고 재검토를 요청한다.

---

## STEP 4 — 승인 후 plan.md 압축

사용자가 최종 승인하면 Write 도구로 plan.md를 압축해 덮어쓴다.

**제거**: 설계 결정 이유 설명 (ADR 번호만 유지), 테스트 레벨 선택 근거, `>` 인용 블록의 설계 논의, 중복 설명 문장  
**유지**: 파일 경로·클래스·필드·메서드 선언, 테스트 케이스 표 전체, 어노테이션·인프라 설정, 설계 결정 항목
