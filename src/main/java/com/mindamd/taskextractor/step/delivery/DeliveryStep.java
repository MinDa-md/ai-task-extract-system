package com.mindamd.taskextractor.step.delivery;

import com.mindamd.taskextractor.domain.dto.SummaryDto;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeliveryStep implements Step<DeliveryInput, DeliveryResult> {

    @Override
    public Integer getStepOrder() {
        return 99;
    }

    @Override
    public StepResult<DeliveryResult> execute(DeliveryInput input) {
        SummaryDto summary = input.summary().summary();
        Map<String, String> dictionary = input.dictionary().value();
        String location = restore(summary.location(), dictionary);
        String participants = restore(summary.participantsInfo(), dictionary);
        return StepResult.of(new DeliveryResult(formatMessage(summary.meetingTime(), location, participants)));
    }

    private static String restore(String text, Map<String, String> dictionary) {
        if (text == null) {
            return "미정";
        }
        String result = text;
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static String formatMessage(String meetingTime, String location, String participants) {
        return String.format("**회의 요약**\n일시: %s\n장소: %s\n참석자: %s",
                meetingTime != null ? meetingTime : "미정",
                location,
                participants);
    }
}
