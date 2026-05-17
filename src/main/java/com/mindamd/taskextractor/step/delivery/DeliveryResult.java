package com.mindamd.taskextractor.step.delivery;

import com.mindamd.taskextractor.pipeline.spec.StepData;

public record DeliveryResult(boolean sent) implements StepData {}
