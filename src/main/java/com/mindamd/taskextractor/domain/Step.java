package com.mindamd.taskextractor.domain;

public interface Step {

    Integer getStepOrder();

    PhaseData execute(String pipelineId, PhaseData input);

    String serialize(PhaseData result);

    PhaseData deserialize(String json);
}
