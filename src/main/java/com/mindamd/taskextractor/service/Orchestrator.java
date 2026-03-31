package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.entity.PipelineExecution;
import com.mindamd.taskextractor.domain.repository.PipelineExecutionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class Orchestrator {

    private final List<Phase> phases;
    private final PipelineExecutionRepository repository;

    public Orchestrator(List<Phase> phases, PipelineExecutionRepository repository) {
        this.phases = phases;
        this.repository = repository;
    }

    public PhaseData run(String reqId, PhaseData initial) {
        PipelineExecution execution = PipelineExecution.builder()
                .reqId(reqId)
                .pipelineStatus(PipelineStatus.RUNNING)
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(execution);

        PhaseData current = initial;
        for (Phase phase : phases) {
            String stepId = phase.getClass().getSimpleName();
            execution.updateStep(stepId);
            repository.save(execution);

            current = phase.execute(current);
        }

        execution.updatePipelineStatus(PipelineStatus.SUCCESS);
        execution.complete(LocalDateTime.now());
        repository.save(execution);

        return current;
    }
}
