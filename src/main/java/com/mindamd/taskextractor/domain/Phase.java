package com.mindamd.taskextractor.domain;

public interface Phase {

    String getStepId();

    PhaseData execute(String pipelineId, PhaseData input);

    String serialize(PhaseData result);

    PhaseData deserialize(String json);
}
