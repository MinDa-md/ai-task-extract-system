package com.mindamd.taskextractor.step.ai;

import com.mindamd.taskextractor.domain.dto.SummaryDto;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepResult;
import com.mindamd.taskextractor.step.filter.AnonymizedText;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AiClientStep implements Step<AnonymizedText, AiSummary> {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            채팅 로그에서 회의 관련 정보를 추출합니다.
            가명 처리된 텍스트([NAME_1], [PHONE_1] 등)는 원본 형태 그대로 추출하세요.
            """;

    @Override
    public Integer getStepOrder() {
        return 20;
    }

    @Override
    public StepResult<AiSummary> execute(AnonymizedText input) {
        SummaryDto summary = Optional.ofNullable(
                        chatClient.prompt()
                                .system(SYSTEM_PROMPT)
                                .user(input.filteredText())
                                .call()
                                .entity(SummaryDto.class))
                .orElseThrow(() -> new NonRecoverableException("AI returned empty response"));
        return StepResult.of(new AiSummary(input.requestKey(), summary));
    }
}
