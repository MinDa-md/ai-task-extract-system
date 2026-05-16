package com.mindamd.taskextractor.step;

import com.mindamd.taskextractor.step.filter.dto.MaskingType;
import com.mindamd.taskextractor.step.filter.dto.PrivacyMasking;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummaryResponseDto {

    private String meetingTime;

    @PrivacyMasking(type = MaskingType.LOCATION)
    private String location; // 복원된 원본 장소 (직렬화 시 마스킹 적용)

    @PrivacyMasking(type = MaskingType.PHONE)
    private String participantsInfo; // 복원된 원본 참여자 정보 (직렬화 시 전화번호 마스킹)

    // 디스코드 출력용 포맷팅
    public String getFormattedMessage() {
        return String.format("📅 **일정 요약 완료**\n- **시간:** %s\n- **장소:** %s\n- **참여자:** %s",
                this.meetingTime != null ? this.meetingTime : "미정",
                this.location != null ? this.location : "미정",
                this.participantsInfo != null ? this.participantsInfo : "정보 없음");
    }
}