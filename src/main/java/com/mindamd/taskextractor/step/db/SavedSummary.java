package com.mindamd.taskextractor.step.db;

import com.mindamd.taskextractor.domain.dto.SummaryDto;
import com.mindamd.taskextractor.pipeline.spec.StepData;

public record SavedSummary(SummaryDto summary) implements StepData {}
