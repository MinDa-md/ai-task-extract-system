package com.mindamd.taskextractor.global.exception;

public class ConcurrentPipelineException extends RuntimeException {
    public ConcurrentPipelineException(String message) {
        super(message);
    }
}
