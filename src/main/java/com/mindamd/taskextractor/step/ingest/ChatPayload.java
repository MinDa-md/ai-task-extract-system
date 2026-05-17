package com.mindamd.taskextractor.step.ingest;

import com.mindamd.taskextractor.pipeline.spec.StepData;

import java.util.List;

public record ChatPayload(List<String> chatLogs) implements StepData {}
