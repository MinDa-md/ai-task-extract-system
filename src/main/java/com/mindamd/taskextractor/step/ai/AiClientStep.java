package com.mindamd.taskextractor.step.ai;

import com.mindamd.taskextractor.domain.dto.PipelineDto;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.step.filter.AnonymizedChatLog;
import org.springframework.stereotype.Component;

@Component
public class AiClientStep implements Step<AnonymizedChatLog, AiSummary> {

    @Override
    public Integer getStepOrder() {
        return 20;
    }

    @Override
    public AiSummary execute(PipelineDto context, AnonymizedChatLog input) {
        return null;
    }

    @Override
    public Class<AiSummary> outputType() {
        return AiSummary.class;
    }
}
