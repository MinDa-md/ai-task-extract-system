erDiagram
MEETING_SUMMARY {
        BIGINT id PK "Auto Increment"
        VARCHAR discord_channel_id "평문 (검색 조건)"
        VARCHAR meeting_time "평문 (검색 조건)"
        VARCHAR location "가명 처리 (예: [LOC_1])"
        VARCHAR participants_info "가명 처리 (예: [NAME_1] [PHONE_1])"
        TEXT secure_dictionary "NULL 허용 | 가명 사전 JSON 통째로 AES-256 암호화"
        TIMESTAMP created_at "생성 일시"
    }

    AUDIT_LOG {
        BIGINT id PK "Auto Increment"
        BIGINT target_summary_id FK "조회된 요약본 ID"
        VARCHAR admin_id "조회를 수행한 관리자 ID"
        VARCHAR access_reason "조회 사유 (예: 고객 CS 처리)"
        TIMESTAMP accessed_at "조회 일시"
    }

    MEETING_SUMMARY ||--o{ AUDIT_LOG : "어드민 마스킹 해제 시 기록"