---
name: tdd-phase
description: |
  TDD(red-green-refactor) 사이클로 새 컴포넌트 구현. 인수로 구현 대상 클래스명을 받음.
  "TDD", "tdd", "테스트 설계", "테스트 케이스", "단위 테스트", "DataJpaTest", "테스트 먼저", "구현 전 테스트" 시 이 스킬을 사용.
---

> **주의**: 이 스킬은 Plan mode에서 호출하지 않는다. Plan mode는 불필요한 계획 서브에이전트를 생성해 토큰을 낭비한다.

## STEP 0 — ONBOARDING

mcp__plugin_serena_serena__check_onboarding_performed 를 호출한다.
- 온보딩이 완료되지 않은 경우: mcp__plugin_serena_serena__onboarding 을 호출한다 (최초 1회 전체 프로젝트 스캔 허용).
- 온보딩이 이미 완료된 경우: mcp__plugin_serena_serena__prepare_for_new_conversation 을 호출한다 (캐시 로드만 수행, 전체 스캔 금지).

Read 도구로 다음 파일을 순서대로 읽는다 (bash 명령 및 기타 도구 사용 금지):
- docs/$ARGUMENTS/spec.md
- docs/$ARGUMENTS/res.md
- docs/$ARGUMENTS/milestone.md

milestone.md에서 사용자가 지시한 단계만 파악한다.
파일이 하나라도 없으면 사용자에게 알리고 중단한다.

읽은 내용에서 인터페이스 계약, 입출력 타입, 예외 정책을 파악한다.

git log --oneline -5 를 실행해 이전 구현 완료 내역을 파악한다.
milestone.md의 지시 단계와 대조해 이미 완료된 단계를 확인한다.

---

## STEP 1 — RED (실패 테스트 작성)

테스트를 작성하기 전에 사용자에게 설명한다:
"다음 케이스들을 테스트할 예정입니다: [케이스 목록]. 누락된 케이스나 수정이 필요한 케이스가 있으면 말씀해 주세요."

확인 후 테스트를 Write 도구로 작성한다 (Serena create_text_file 사용 금지):

테스트 메서드는 Given-When-Then 패턴으로 작성한다:
- Given: 테스트 전제 조건 설정
- When: 테스트 대상 동작 실행
- Then: 결과 검증

**단위 테스트** ($ArgumentsTest.java)
- 정상(happy path) 시나리오
- 경계값(boundary) 시나리오
- 예외/실패 시나리오

**DataJpaTest** ($ArgumentsRepositoryTest.java) — DB 접근이 있는 경우만
- @DataJpaTest + @AutoConfigureTestDatabase(replace = NONE)
- Testcontainer로 실제 DB 기동
- 저장/조회/예외 케이스

통합 테스트가 필요하다고 판단되면, 사용자에게 먼저 요청한다:
"[구체적 이유]로 인해 통합 테스트가 필요합니다. 작성을 진행할까요?"

Write 도구로 작성한 테스트 파일을 각각 실행해 모두 FAIL인지 확인한다:
./gradlew test --tests "*[작성한 테스트 클래스명]" -q

사용자에게 설명한다:
"RED 단계 완료. 실패한 테스트: [목록]. 모두 의도한 실패입니다."

---

## STEP 2 — GREEN (최소 구현)

사용자에게 설명한다:
"RED 확인 완료. 테스트를 통과하는 최소 구현을 작성합니다."

기존 코드의 프레임워크 어노테이션/등록 규칙을 파악할 때 (bash 명령 사용 금지):
1. mcp__plugin_serena_serena__get_symbols_overview 로 유사한 기존 클래스가 있는 디렉토리를 조회한다.
2. mcp__plugin_serena_serena__find_symbol 로 참고할 심볼의 본문만 읽는다.

$ARGUMENTS 구현 파일을 Write 도구로 작성한다 (Serena create_text_file 사용 금지):
- 테스트를 통과하는 최소한의 구현만 작성
- 과도한 추상화, 미래 대비 코드 금지
- 프레임워크 어노테이션/등록 규칙을 기존 코드와 맞춤

Write 도구로 작성한 테스트 파일을 각각 실행한다 (실패 원인 파악을 위해 -q 없이 실행):
./gradlew test --tests "*[작성한 테스트 클래스명]"

실패 잔존 시 테스트가 아닌 구현을 수정한다.
수정은 최대 2회까지만 시도한다. 2회 후에도 실패가 남아있으면 구현을 중단하고 사용자에게 보고한다:
"[N]개 테스트가 2회 수정 후에도 실패합니다. 실패 내용: [목록]. 확인 후 방향을 알려주세요."

사용자에게 설명한다:
"GREEN 단계 완료. 전체 [N]개 테스트 통과. 구현 중 결정한 사항: [내용]"

---

## STEP 3 — REFACTOR

사용자에게 설명한다:
"GREEN 확인 완료. 구현 파일을 정리합니다."

다음 순서로 수행한다 (파일 전체 읽기 및 bash 사용 금지):
1. mcp__plugin_serena_serena__get_symbols_overview 로 $ARGUMENTS 구현 파일과 테스트 파일의 심볼 목록을 파악한다.
2. 수정이 필요한 심볼만 mcp__plugin_serena_serena__find_symbol 로 읽는다.

중복 제거, 네이밍 개선, 불필요한 복잡도 제거만 수행한다.
기능 추가, 추상화 추가, 테스트 구조 변경은 금지한다.

Write 도구로 작성한 테스트 파일을 각각 재실행해 회귀가 없는지 확인한다:
./gradlew test --tests "*[작성한 테스트 클래스명]"

---

## 완료

사용자에게 설명한다:
"구현 완료.
- RED: [N]개 테스트 작성
- GREEN: [N]개 통과, 주요 결정사항: [내용]
- REFACTOR: [변경 내용 또는 '변경 없음']"

/commit-commands:commit 을 호출한다.
