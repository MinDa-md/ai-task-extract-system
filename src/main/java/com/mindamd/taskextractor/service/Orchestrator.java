package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Step;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.entity.PhaseExecution;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.PhaseExecutionRepository;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Orchestrator {

    private final PipelineRepository pipelineRepository;
    private final PhaseExecutionRepository phaseRepository;
    private final List<Step> steps;

    public PhaseData run(String pipelineId, PhaseData initial) {
        Optional<Pipeline> existing = pipelineRepository.findById(pipelineId);
        Pipeline pipeline;
        Integer stepOrder;

        if (existing.isEmpty()) {
            pipeline = pipelineRepository.save(new Pipeline(pipelineId));
            stepOrder = 0;
        } else {
            pipeline = existing.get();
            if (pipeline.isCompleted()) {
                // TODO: 이미 해당 요청은 이전에 완료되었음을 전달
            }
            if (pipeline.isFinalFailed()) {
                throw new NonRecoverableException("Pipeline permanently failed: " + pipelineId);
            }
            stepOrder = phaseRepository.findLastCompletedStepOrder(pipelineId);
        }

        List<Step> sortedStep = steps.stream()
                .sorted(Comparator.comparingInt(Step::getStepOrder))
                .filter(step -> step.getStepOrder() > stepOrder)
                .toList();

        return execute(pipeline, sortedStep, initial);
    }

    private PhaseData execute(Pipeline pipeline, List<Step> steps, PhaseData current) {
        try {
            for(Step step : steps) {
                current = step.execute(pipeline.getId(), current);
                phaseRepository.save(PhaseExecution.builder()
                        .stepOrder(step.getStepOrder())
                        .result(step.serialize(current))
                        .pipeline(pipeline)
                        .build());
            }
        } // RecoverableException은 별도 처리 없이 그대로 전파
        catch (NonRecoverableException e) {
            pipeline.markFinalFailed();
            pipelineRepository.save(pipeline);
            throw e;
        }

        pipeline.markCompleted();
        pipelineRepository.save(pipeline);

        return current;
    }
}
