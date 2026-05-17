package com.mindamd.taskextractor.step.db;

import com.mindamd.taskextractor.domain.dto.SummaryDto;
import com.mindamd.taskextractor.pipeline.spec.StepData;

import java.util.Map;

public record SavedSummary(SummaryDto summary, Map<String, String> dictionary) implements StepData {}
