package com.mindamd.taskextractor.domain.dto;

import lombok.Builder;
import lombok.Getter;

/*
    복원된 정보들 (장소, 전화번호)
 */

@Getter
@Builder
public class SummaryResponseDto {

    private String meetingTime;

    @PrivacyMasking(type = MaskingType.LOCATION)
    private String location;

    @PrivacyMasking(type = MaskingType.PHONE)
    private String participantsInfo;

    // 디스코드 출력용 포맷팅
    public String getFormattedMessage() {
        return String.format("📅 **일정 요약 완료**\n- **시간:** %s\n- **장소:** %s\n- **참여자:** %s",
                this.meetingTime != null ? this.meetingTime : "미정",
                this.location != null ? this.location : "미정",
                this.participantsInfo != null ? this.participantsInfo : "정보 없음");
    }
}