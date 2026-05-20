package com.mindamd.taskextractor.pipeline.util;

import com.mindamd.taskextractor.pipeline.spec.StepData;

import java.util.Optional;

public record CheckpointState(int stepOrder, Optional<StepData> input) {}
