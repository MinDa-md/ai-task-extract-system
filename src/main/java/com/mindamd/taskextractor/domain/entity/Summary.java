package com.mindamd.taskextractor.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "meeting_summary", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"request_key"}) // 멱등성: 중복 저장 방지
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Summary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_key", nullable = false, updatable = false)
    private String requestKey;

    @Column(name = "meeting_time")
    private String meetingTime; // 검색 조건이므로 평문 저장

    @Column(name = "location")
    private String location; // 가명 처리된 상태로 저장 (예: [LOC_1])

    @Column(name = "participants_info")
    private String participantsInfo; // 가명 처리된 상태로 저장 (예: [NAME_1] [PHONE_1])

    public Summary(String requestKey, String meetingTime, String location, String participantsInfo) {
        this.requestKey = requestKey;
        this.meetingTime = meetingTime;
        this.location = location;
        this.participantsInfo = participantsInfo;
    }
}