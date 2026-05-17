package com.mindamd.taskextractor.step.filter;

import com.mindamd.taskextractor.pipeline.spec.StepData;

import java.util.Map;

public record AnonymizedChatLog(String filteredText, Map<String, String> dictionary) implements StepData {}
