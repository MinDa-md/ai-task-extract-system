package com.mindamd.taskextractor.pipeline.spec;

import java.util.List;

public record StepResult<T extends StepData>(T next, List<StepData> artifacts) {

    public static <T extends StepData> StepResult<T> of(T next) {
        return new StepResult<>(next, List.of());
    }

    public static <T extends StepData> StepResult<T> of(T next, List<StepData> artifacts) {
        return new StepResult<>(next, artifacts);
    }
}