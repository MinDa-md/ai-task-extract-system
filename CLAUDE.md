## File Rules

- Do not read any file not explicitly mentioned — ask the user first.

## Command Rules

- Do not run any bash command — ask the user first.

## Implementation Rules

- Never write or modify files under `src/main` without first running `/tdd-plan` and confirming the plan. Use `/tdd-run` to execute the confirmed plan.

## Naming Rules

- All class and method names must be English PascalCase/camelCase.

## Design Rules

- Never reverse a decision recorded in `docs/orchestrator/discuss.md`.
- If an implementation require adopting a rejected alternative from an ADR, stop and explain why to the user before proceeding.
