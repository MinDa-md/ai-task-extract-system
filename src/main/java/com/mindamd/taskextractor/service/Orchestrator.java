package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Step;
import com.mindamd.taskextractor.domain.StepData;
import com.mindamd.taskextractor.domain.entity.Checkpoint;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.CheckpointRepository;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.global.exception.RecoverableException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class Orchestrator {

    private final PipelineRepository pipelineRepository;
    private final CheckpointRepository checkpointRepository;
    private final TreeMap<Integer, Step> steps;

    public Orchestrator(PipelineRepository pipelineRepository,
                        CheckpointRepository checkpointRepository,
                        List<Step> steps) {
        this.pipelineRepository = pipelineRepository;
        this.checkpointRepository = checkpointRepository;
        this.steps = new TreeMap<>(steps.stream()
                .collect(Collectors.toMap(Step::getStepOrder, Function.identity())));
    }

    public Pipeline run(String pipelineId, StepData initial) {
        Pipeline pipeline = loadPipeline(pipelineId);

        // 이전에 성공한 파이프라인
        if(pipeline.isCompleted()) {
            steps.lastEntry().getValue().execute(pipeline.getId(), initial);
            return pipeline;
        }

        //  재시도 불가능 파이프라인
        if (pipeline.isFinalFailed()) {
            throw new NonRecoverableException("Pipeline permanently failed: " + pipelineId);
        }

        // 재시도 지점 가져오기
        ResumePoint resumePoint = loadCheckpoint(pipelineId, initial);

        // 각 단계 실행
        try {
            StepData current = resumePoint.data();
            for (Step step : steps.tailMap(resumePoint.stepOrder(), false).values()) {
                current = execute(pipeline, step, current);
            }
        } catch (RecoverableException e) {
            pipeline.markFailed();
            pipelineRepository.save(pipeline);
            throw e;
        } catch (NonRecoverableException e) {
            pipeline.markFinalFailed();
            pipelineRepository.save(pipeline);
            throw e;
        }

        pipeline.markCompleted();
        pipelineRepository.save(pipeline);
        return pipeline;
    }

    private Pipeline loadPipeline(String pipelineId) {
        Optional<Pipeline> existing = pipelineRepository.findById(pipelineId);
        return existing.orElseGet(() -> pipelineRepository.save(new Pipeline(pipelineId)));
    }

    private ResumePoint loadCheckpoint(String pipelineId, StepData initial) {
        Optional<Checkpoint> existing  = checkpointRepository.findTopByPipelineIdOrderByStepOrderDesc(pipelineId);
        // 체크포인트 없음
        if(existing.isEmpty()) {
            return new ResumePoint(0, initial);
        }

        // 체크포인트 있음
        Checkpoint checkpoint = existing.get();
        Step step = steps.get(checkpoint.getStepOrder());
        StepData data = step.deserialize(checkpoint.getResult());
        return new ResumePoint(checkpoint.getStepOrder(), data);
    }

    private record ResumePoint(int stepOrder, StepData data) {}

    private StepData execute(Pipeline pipeline, Step step, StepData current) {
        StepData result = step.execute(pipeline.getId(), current);
        checkpointRepository.save(Checkpoint.builder()
                .stepOrder(step.getStepOrder())
                .result(step.serialize(result))
                .pipeline(pipeline)
                .build());
        return result;
    }
}
