package com.mindamd.taskextractor.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_participant",
        indexes = {
                @Index(name = "idx_mp_summary",  columnList = "summary_id"),
                @Index(name = "idx_mp_userinfo", columnList = "user_info_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class

MeetingParticipant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_info_id", nullable = false)
    private UserInfo userInfo;

    // [NAME_1], [PHONE_1] 등 — 요약본 텍스트의 가명과 실제 유저를 연결
    @Column(name = "pseudonym", nullable = false, length = 50)
    private String pseudonym;

    public MeetingParticipant(Summary summary, UserInfo userInfo, String pseudonym) {
        this.summary = summary;
        this.userInfo = userInfo;
        this.pseudonym = pseudonym;
    }
}
