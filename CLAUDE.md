# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build          # Full build
./gradlew bootRun        # Run the application
./gradlew test           # Run all tests
./gradlew test --tests "com.mindamd.taskextractor.SomeTest"  # Run single test
```

Tests require Docker (Testcontainers spins up a MySQL container automatically).

## Key Configuration

The application requires the following to be set externally (environment variables or `application-local.yaml`):

- `spring.datasource.*` — MySQL connection details
- `encryption.secret-key` — 32-byte AES-256 key used by `Aes256Util`
- JDA bot token — for Discord integration (see `DiscordCommandListener`)

The base `application.yaml` only sets `spring.application.name=task-extractor`.

## Architecture

This is a Discord bot that extracts and summarizes meeting information from chat logs while protecting user privacy. The trigger command is `/정리해줘` (Korean: "summarize for me").

### Data Flow

```
Discord message (/정리해줘)
  → DiscordCommandListener       (retrieves last 20 messages, extracts requestKey)
  → SummaryService               (idempotency check via requestKey = Discord message ID)
  → PrivacyFilterService         (pseudonymize: real data → [NAME_1], [PHONE_1], [LOC_1])
  → GeminiApiClient              (TODO: sends pseudonymized text, gets structured summary)
  → SummaryRepository            (persist: CryptoConverter AES-256 encrypts secureDictionary)
  → buildResponseDto             (decrypt dictionary → restore original → apply DTO masking)
  → Discord response             (@PrivacyMasking serializer masks fields in JSON output)
```

### Three-Layer Privacy Model

| Layer | Where | Mechanism | Purpose |
|-------|-------|-----------|---------|
| Pseudonymization | Before LLM call | `PrivacyFilterService` regex replacement | Keep real data out of external AI APIs |
| DB Encryption | Before DB write | `CryptoConverter` + `Aes256Util` (AES-256 ECB) | Protect `secureDictionary` at rest |
| Response Masking | At JSON serialization | `@PrivacyMasking` + `PrivacyMaskingSerializer` | Partially obfuscate data in API/Discord responses |

### Package Structure

```
com.mindamd.taskextractor
├── presentation/discord/    DiscordCommandListener — entry point from JDA events
├── service/                 SummaryService — orchestrates the full flow
├── domain/
│   ├── entity/              Summary — JPA entity (MEETING_SUMMARY table)
│   ├── repository/          SummaryRepository — idempotency queries
│   └── dto/                 PrivacyMasking annotation, MaskingType enum, response DTOs
├── global/
│   ├── security/            Aes256Util, PrivacyFilterService
│   └── converter/           CryptoConverter — JPA AttributeConverter for encryption
└── infrastructure/ai/       GeminiApiClient (stub), PromptGenerator (stub)
```

**MEETING_SUMMARY:** `id`, `discord_channel_id`, `meeting_time`, `location` (pseudonymized), `participants_info` (pseudonymized), `secure_dictionary` (AES-encrypted JSON), `request_key` (UNIQUE for idempotency), `created_at`

**AUDIT_LOG** (planned): tracks admin access to sensitive data — `target_summary_id`, `admin_id`, `access_reason`, `accessed_at`

### What's Not Yet Implemented

- `GeminiApiClient.summarize()` throws `UnsupportedOperationException` — Google Gemini API call is a stub
- `PromptGenerator` is empty
- `global/config/` package is empty
- `domain/repository/JpaRepository.java` is a placeholder
