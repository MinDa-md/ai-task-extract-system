package com.mindamd.taskextractor.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

class PipelineStatusTest {

    @Test
    void pendingCanTransitionToRunning() {
        assertThat(PipelineStatus.PENDING.canTransitionTo(PipelineStatus.RUNNING)).isTrue();
    }

    @Test
    void runningCanTransitionToSuccess() {
        assertThat(PipelineStatus.RUNNING.canTransitionTo(PipelineStatus.SUCCESS)).isTrue();
    }

    @Test
    void runningCanTransitionToFailed() {
        assertThat(PipelineStatus.RUNNING.canTransitionTo(PipelineStatus.FAILED)).isTrue();
    }

    @Test
    void runningCanTransitionToFinalFailed() {
        assertThat(PipelineStatus.RUNNING.canTransitionTo(PipelineStatus.FINAL_FAILED)).isTrue();
    }

    @Test
    void failedCanTransitionToRunning() {
        assertThat(PipelineStatus.FAILED.canTransitionTo(PipelineStatus.RUNNING)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(PipelineStatus.class)
    void successCannotTransitionToAny(PipelineStatus next) {
        assertThat(PipelineStatus.SUCCESS.canTransitionTo(next)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PipelineStatus.class)
    void finalFailedCannotTransitionToAny(PipelineStatus next) {
        assertThat(PipelineStatus.FINAL_FAILED.canTransitionTo(next)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = PipelineStatus.class, names = "RUNNING", mode = EXCLUDE)
    void pendingCannotTransitionToNonRunning(PipelineStatus next) {
        assertThat(PipelineStatus.PENDING.canTransitionTo(next)).isFalse();
    }

    @Test
    void runningCannotTransitionToPending() {
        assertThat(PipelineStatus.RUNNING.canTransitionTo(PipelineStatus.PENDING)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = PipelineStatus.class, names = "RUNNING", mode = EXCLUDE)
    void failedCannotTransitionToNonRunning(PipelineStatus next) {
        assertThat(PipelineStatus.FAILED.canTransitionTo(next)).isFalse();
    }
}
