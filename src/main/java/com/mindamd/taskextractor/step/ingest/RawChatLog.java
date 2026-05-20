package com.mindamd.taskextractor.step.ingest;

import com.mindamd.taskextractor.pipeline.spec.StepData;

public record RawChatLog(String requestKey, String rawText) implements StepData {}
