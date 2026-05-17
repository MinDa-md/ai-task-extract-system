package com.mindamd.taskextractor.step.ai;

import com.mindamd.taskextractor.domain.dto.SummaryDto;
import com.mindamd.taskextractor.pipeline.spec.StepData;

import java.util.Map;

public record AiSummary(SummaryDto summary, Map<String, String> dictionary) implements StepData {}
