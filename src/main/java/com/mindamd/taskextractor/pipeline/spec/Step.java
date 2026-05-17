package com.mindamd.taskextractor.pipeline.spec;

import com.mindamd.taskextractor.domain.dto.PipelineDto;

public interface Step<I extends StepData, O extends StepData> {

    Integer getStepOrder();

    O execute(PipelineDto context, I input);

    Class<O> outputType();
}
