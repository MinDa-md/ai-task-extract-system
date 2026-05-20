package com.mindamd.taskextractor.step.ai;

import com.mindamd.taskextractor.domain.dto.SummaryDto;
import com.mindamd.taskextractor.pipeline.spec.StepData;

public record AiSummary(String requestKey, SummaryDto summary) implements StepData {}
