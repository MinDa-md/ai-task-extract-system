package com.mindamd.taskextractor.domain.dto;

import com.mindamd.taskextractor.global.util.masking.MaskingType;
import com.mindamd.taskextractor.global.util.masking.PrivacyMasking;

public record SummaryDto(
        String meetingTime,
        @PrivacyMasking(type = MaskingType.LOCATION) String location,
        @PrivacyMasking(type = MaskingType.PHONE) String participantsInfo
) {}
