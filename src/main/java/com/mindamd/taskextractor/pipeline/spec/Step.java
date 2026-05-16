package com.mindamd.taskextractor.pipeline.spec;

public interface Step {

    Integer getStepOrder();

    StepData execute(String pipelineId, StepData input);

    String serialize(StepData result);

    StepData deserialize(String json);
}
