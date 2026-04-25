package com.mindamd.taskextractor.domain.entity;

import com.mindamd.taskextractor.global.converter.CryptoConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_info", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"discord_user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Discord snowflake — 공개 식별자이므로 평문 저장 (조회 키)
    @Column(name = "discord_user_id", nullable = false, updatable = false)
    private String discordUserId;

    // 닉네임 — 공개 표시 이름이므로 평문
    @Column(name = "discord_username")
    private String discordUsername;

    // 이하 5대 개인정보 + 계좌번호: 모두 AES-256 암호화
    @Convert(converter = CryptoConverter.class)
    @Column(name = "real_name")
    private String realName;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "email")
    private String email;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "phone")
    private String phone;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "birth_date")
    private String birthDate;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public UserInfo(String discordUserId, String discordUsername, String realName,
                    String email, String phone, String birthDate, String accountNumber) {
        this.discordUserId = discordUserId;
        this.discordUsername = discordUsername;
        this.realName = realName;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.accountNumber = accountNumber;
    }

    // 개인정보 수정용 도메인 메서드 (updated_at 자동 갱신)
    public void updatePersonalInfo(String realName, String email, String phone,
                                   String birthDate, String accountNumber) {
        if (realName != null)       this.realName = realName;
        if (email != null)          this.email = email;
        if (phone != null)          this.phone = phone;
        if (birthDate != null)      this.birthDate = birthDate;
        if (accountNumber != null)  this.accountNumber = accountNumber;
    }
}
