package com.mindamd.taskextractor.step.filter;

import com.mindamd.taskextractor.pipeline.spec.StepData;

import java.util.Map;

public record AnonymizationDictionary(Map<String, String> value) implements StepData {}
