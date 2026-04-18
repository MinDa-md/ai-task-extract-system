package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.global.exception.RecoverableException;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class Orchestrator {

    private final List<Phase> phases;
    private final PipelineRepository repository;

    public Orchestrator(List<Phase> phases, PipelineRepository repository) {
        this.phases = phases;
        this.repository = repository;
    }

    public PhaseData run(String pipelineId, PhaseData initial) {
        MDC.put("req_id", pipelineId);
        try {
            Optional<Pipeline> existing = repository.findById(pipelineId);

            if (existing.isEmpty()) {
                Pipeline pipeline = Pipeline.builder().id(pipelineId).build();
                return executePhases(pipeline, initial);
            }

            Pipeline pipeline = existing.get();
            return switch (pipeline.getPipelineStatus()) {
                case SUCCESS -> restoreLastResult(pipeline);
                case RUNNING -> throw new IllegalStateException("Pipeline already running: " + pipelineId);
                case FINAL_FAILED -> throw new NonRecoverableException("Pipeline permanently failed: " + pipelineId);
                case FAILED -> executePhases(pipeline, initial);
                case PENDING -> throw new IllegalStateException("Unexpected PENDING status: " + pipelineId);
            };
        } finally {
            MDC.remove("req_id");
        }
    }

    private PhaseData executePhases(Pipeline pipeline, PhaseData initial) {
        pipeline.markRunning();
        repository.save(pipeline);

        PhaseData current = initial;
        try {
            for (Phase phase : phases) {
                current = phase.execute(pipeline.getId(), current);
            }
        } catch (RecoverableException e) {
            pipeline.recordFailure(PipelineStatus.FAILED);
            repository.save(pipeline);
            throw e;
        } catch (NonRecoverableException e) {
            pipeline.recordFailure(PipelineStatus.FINAL_FAILED);
            repository.save(pipeline);
            throw e;
        }

        String finalResult = phases.get(phases.size() - 1).serialize(current);
        pipeline.complete(LocalDateTime.now(), finalResult);
        repository.save(pipeline);

        return current;
    }

    private PhaseData restoreLastResult(Pipeline pipeline) {
        return phases.get(phases.size() - 1).deserialize(pipeline.getFinalResult());
    }
}
