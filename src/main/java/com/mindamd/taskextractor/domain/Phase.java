package com.mindamd.taskextractor.domain;

public interface Phase {

    PhaseData execute(PhaseData input);

    PhaseData restore(String serializedData);
}
