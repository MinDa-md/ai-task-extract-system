package com.mindamd.taskextractor.domain;

public enum PipelineStatus {
    PENDING, RUNNING, SUCCESS, FAILED, FINAL_FAILED;

    public boolean canTransitionTo(PipelineStatus next) {
        return switch (this) {
            case PENDING -> next == RUNNING;
            case RUNNING -> next == SUCCESS || next == FAILED || next == FINAL_FAILED;
            case FAILED -> next == RUNNING;
            case SUCCESS, FINAL_FAILED -> false;
        };
    }
}
