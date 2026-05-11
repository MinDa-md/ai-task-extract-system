package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Step;
import com.mindamd.taskextractor.domain.StepData;
import com.mindamd.taskextractor.domain.entity.Checkpoint;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.CheckpointRepository;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.ConcurrentPipelineException;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class Orchestrator {

    private static final int INGEST_STEP_ORDER = 0;
    private static final int DELIVERY_STEP_ORDER = 99;

    private final PipelineRepository pipelineRepository;
    private final CheckpointRepository checkpointRepository;
    private final TreeMap<Integer, Step> steps;
    private final Set<String> runningPipelines = ConcurrentHashMap.newKeySet();

    public Orchestrator(PipelineRepository pipelineRepository,
                        CheckpointRepository checkpointRepository,
                        List<Step> steps) {
        this.pipelineRepository = pipelineRepository;
        this.checkpointRepository = checkpointRepository;
        this.steps = new TreeMap<>(steps.stream()
                .collect(Collectors.toMap(Step::getStepOrder, Function.identity())));
    }

    public Pipeline run(String pipelineId, StepData initial) {
        if (!runningPipelines.add(pipelineId)) {
            throw new ConcurrentPipelineException("Pipeline already running: " + pipelineId);
        }
        try {
            return executePipeline(pipelineId, initial);
        } finally {
            runningPipelines.remove(pipelineId);
        }
    }

    private Pipeline executePipeline(String pipelineId, StepData initial) {
        Pipeline pipeline = loadPipeline(pipelineId);

        if(pipeline.isCompleted()) {
            steps.get(DELIVERY_STEP_ORDER).execute(pipeline.getId(), initial);
            return pipeline;
        }

        if (pipeline.isRunCountExceeded()) {
            pipeline.markFailed();
            pipelineRepository.save(pipeline);
        }

        if (pipeline.isFailed()) {
            throw new NonRecoverableException("Pipeline permanently failed: " + pipelineId);
        }

        pipeline.incrementRunCount();
        pipelineRepository.save(pipeline);

        // 재시도 지점 가져오기
        ResumePoint resumePoint = loadCheckpoint(pipelineId, initial);

        // 각 단계 실행
        try {
            StepData current = resumePoint.data();
            for (Step step : steps.tailMap(resumePoint.stepOrder(), false).values()) {
                current = step.execute(pipeline.getId(), current);
                checkpointRepository.save(Checkpoint.of(step.getStepOrder(), step.serialize(current), pipeline));
            }
        } catch (NonRecoverableException e) {
            pipeline.markFailed();
            pipelineRepository.save(pipeline);
            throw e;
        }

        pipeline.markCompleted();
        pipelineRepository.save(pipeline);
        return pipeline;
    }

    private Pipeline loadPipeline(String pipelineId) {
        Optional<Pipeline> existing = pipelineRepository.findById(pipelineId);
        return existing.orElseGet(() -> pipelineRepository.save(Pipeline.of(pipelineId)));
    }

    private ResumePoint loadCheckpoint(String pipelineId, StepData initial) {
        Optional<Checkpoint> existing  = checkpointRepository.findTopByPipelineIdOrderByStepOrderDesc(pipelineId);
        // 체크포인트 없음
        if(existing.isEmpty()) {
            return new ResumePoint(INGEST_STEP_ORDER - 1, initial);
        }

        // 체크포인트 있음
        Checkpoint checkpoint = existing.get();
        Step step = steps.get(checkpoint.getStepOrder());
        StepData data = step.deserialize(checkpoint.getResult());
        return new ResumePoint(checkpoint.getStepOrder(), data);
    }

    private record ResumePoint(int stepOrder, StepData data) {}
}
