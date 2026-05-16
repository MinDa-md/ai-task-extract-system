package com.mindamd.taskextractor.domain.entity;

import com.mindamd.taskextractor.global.util.CryptoConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_context", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"request_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageContext {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_key", nullable = false, updatable = false)
    private String requestKey;

    @Column(name = "discord_channel_id", nullable = false)
    private String discordChannelId;

    @Column(name = "discord_guild_id", nullable = false)
    private String discordGuildId;

    // 원본 메시지 JSON — AES-256 암호화 후 저장
    @Convert(converter = CryptoConverter.class)
    @Column(name = "raw_messages_json", columnDefinition = "LONGTEXT", nullable = false)
    private String rawMessagesJson;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt;

    @PrePersist
    protected void onCreate() {
        this.collectedAt = LocalDateTime.now();
    }

    public MessageContext(String requestKey, String discordChannelId,
                          String discordGuildId, String rawMessagesJson) {
        this.requestKey = requestKey;
        this.discordChannelId = discordChannelId;
        this.discordGuildId = discordGuildId;
        this.rawMessagesJson = rawMessagesJson;
    }
}
