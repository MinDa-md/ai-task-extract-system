---
name: tdd-green
description: |
  GREEN 단계 — 최소 구현 실행
---

## STEP 0 — 플랜 로드

1. `mcp__plugin_serena_serena__check_onboarding_performed` 호출
   - 완료: `mcp__plugin_serena_serena__prepare_for_new_conversation`
2. `docs/{feature}/plan.md` Read (`{feature}`는 대화 컨텍스트에서 파악)
3. `git log --oneline -3 -- src/main/`
   - 최근 src/main 변경 내역을 파악해 컨텍스트로 활용

읽은 내용을 바탕으로 이번 작업에서 사용자가 원하는 것을 해석해서 제시

형식:
> **{목표 한 줄 요약}**
 
---

## STEP 1 — GREEN (최소 구현)

### 코드 탐색

- `docs/{feature}/plan.md`의 `스코프 아웃` 조회 금지
- `mcp__plugin_serena_serena__find_symbol` 또는 `mcp__plugin_serena_serena__get_symbols_overview`로 관련 심볼만 핀포인트 조회
- `find_file`·`Glob`·`Grep`으로 디렉토리 전체 스캔 금지
- 범위 밖 탐색이 필요해지면 사용자에게 요청

### 구현 규칙

- 테스트를 통과하는 최소 구현만 작성
- `docs/{feature}/plan.md` `스코프 아웃` 항목 구현 금지
- 기존 패턴은 Serena 조회 결과에서 파악

### 레벨별 실행

단위 → DataJpa → 통합 순으로 진행:
1. 해당 레벨 테스트 실행
   ```
   ./gradlew test --tests "*{클래스명}"
   ```
2. 통과 시 다음 레벨로 진행
3. 실패 시 구현만 수정 (테스트 수정 금지), 레벨당 최대 2회 재시도
4. 2회 후 잔존 실패 시 즉시 중단
   - 보고: "[N]개 테스트 실패, 실패 내용: [목록]"
5. 전체 완료 
   - 보고: "GREEN 완료, [N]개 테스트 통과"

---

## 완료

보고:
```
GREEN 완료.
- [N]개 테스트 통과
```
