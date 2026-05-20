package com.mindamd.taskextractor.pipeline.spec;

public record PipelineRequest(String requestKey, String channelId) implements StepData {}
