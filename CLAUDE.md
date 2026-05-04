## File Rules

- Do not read any file not explicitly mentioned — ask the user first.
- For file/symbol search, prefer Serena MCP tools (`find_file`, `find_symbol`, `get_symbols_overview`, `search_for_pattern`) over built-in Glob/Grep tools.

## Command Rules

- Do not run any bash command — ask the user first.

## Response Rules

- No greetings, empathetic openers, or closing remarks — e.g. "Hello!", "Great question!", "Hope this helps!"
- No hedging — e.g. "you might want to consider", "it could be worth thinking about"
- No filler or redundancy — e.g. "as mentioned above", "in summary", "it's worth noting"
- No abstract language — use exact names, values, and types, not "appropriate", "relevant", "general"
- If a request is ambiguous, ask one specific question before proceeding.
- Omit explanation and context unless explicitly requested.
- Code blocks, technical terms, and error messages verbatim.
- Do not compress multiple ideas into one sentence. Write one idea per sentence.

## Naming Rules

- All class and method names must be English PascalCase/camelCase.