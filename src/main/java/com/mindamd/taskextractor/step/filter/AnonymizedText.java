package com.mindamd.taskextractor.step.filter;

import com.mindamd.taskextractor.pipeline.spec.StepData;

public record AnonymizedText(String requestKey, String filteredText) implements StepData {}
