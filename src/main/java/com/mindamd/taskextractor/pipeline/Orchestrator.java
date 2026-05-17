package com.mindamd.taskextractor.pipeline;

import com.mindamd.taskextractor.domain.dto.PipelineDto;
import com.mindamd.taskextractor.domain.entity.Checkpoint;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.CheckpointRepository;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.ConcurrentPipelineException;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepData;
import com.mindamd.taskextractor.pipeline.spec.StepSerializer;
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
    private final StepSerializer stepSerializer;
    private final TreeMap<Integer, Step<?, ?>> steps;
    private final Set<String> runningPipelines = ConcurrentHashMap.newKeySet();

    public Orchestrator(PipelineRepository pipelineRepository,
                        CheckpointRepository checkpointRepository,
                        StepSerializer stepSerializer,
                        List<Step<?, ?>> steps) {
        this.pipelineRepository = pipelineRepository;
        this.checkpointRepository = checkpointRepository;
        this.stepSerializer = stepSerializer;
        this.steps = new TreeMap<>(steps.stream()
                .collect(Collectors.toMap(Step::getStepOrder, Function.identity())));
    }

    public Pipeline run(PipelineDto pipelineContext, StepData initial) {
        String pipelineId = pipelineContext.requestKey();
        if (!runningPipelines.add(pipelineId)) {
            throw new ConcurrentPipelineException("Pipeline already running: " + pipelineId);
        }
        try {
            return executePipeline(pipelineContext, initial);
        } finally {
            runningPipelines.remove(pipelineId);
        }
    }

    private Pipeline executePipeline(PipelineDto pipelineContext, StepData initial) {
        String pipelineId = pipelineContext.requestKey();
        Pipeline pipeline = loadPipeline(pipelineId);

        if (pipeline.isCompleted()) {
            @SuppressWarnings("unchecked")
            Step<StepData, StepData> deliveryStep = (Step<StepData, StepData>) steps.get(DELIVERY_STEP_ORDER);
            deliveryStep.execute(pipelineContext, initial);
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

        ResumePoint resumePoint = loadCheckpoint(pipelineId, initial);

        try {
            StepData current = resumePoint.data();
            for (Step<?, ?> rawStep : steps.tailMap(resumePoint.stepOrder(), false).values()) {
                @SuppressWarnings("unchecked")
                Step<StepData, StepData> step = (Step<StepData, StepData>) rawStep;
                current = step.execute(pipelineContext, current);
                checkpointRepository.save(Checkpoint.of(step.getStepOrder(), stepSerializer.serialize(current), pipeline));
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
        Optional<Checkpoint> existing = checkpointRepository.findTopByPipelineIdOrderByStepOrderDesc(pipelineId);
        if (existing.isEmpty()) {
            return new ResumePoint(INGEST_STEP_ORDER - 1, initial);
        }

        Checkpoint checkpoint = existing.get();
        @SuppressWarnings("unchecked")
        Step<StepData, StepData> step = (Step<StepData, StepData>) steps.get(checkpoint.getStepOrder());
        StepData data = stepSerializer.deserialize(checkpoint.getResult(), step.outputType());
        return new ResumePoint(checkpoint.getStepOrder(), data);
    }

    private record ResumePoint(int stepOrder, StepData data) {}
}
