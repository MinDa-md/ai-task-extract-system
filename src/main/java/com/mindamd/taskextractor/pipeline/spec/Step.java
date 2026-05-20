package com.mindamd.taskextractor.pipeline.spec;

public interface Step<I extends StepData, O extends StepData> {

    Integer getStepOrder();

    StepResult<O> execute(I input);
}
