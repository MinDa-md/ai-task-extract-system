package com.mindamd.taskextractor.step.delivery;

import com.mindamd.taskextractor.pipeline.spec.StepData;
import com.mindamd.taskextractor.step.db.SavedSummary;
import com.mindamd.taskextractor.step.filter.AnonymizationDictionary;

public record DeliveryInput(SavedSummary summary, AnonymizationDictionary dictionary) implements StepData {}
