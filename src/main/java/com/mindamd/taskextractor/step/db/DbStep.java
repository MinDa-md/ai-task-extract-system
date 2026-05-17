package com.mindamd.taskextractor.step.db;

import com.mindamd.taskextractor.domain.dto.PipelineDto;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.step.ai.AiSummary;

public class DbStep implements Step<AiSummary, SavedSummary> {

    @Override
    public Integer getStepOrder() {
        return 30;
    }

    @Override
    public SavedSummary execute(PipelineDto context, AiSummary input) {
        return null;
    }

    @Override
    public Class<SavedSummary> outputType() {
        return SavedSummary.class;
    }
}
