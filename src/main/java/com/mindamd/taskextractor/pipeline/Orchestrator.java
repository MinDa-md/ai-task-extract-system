package com.mindamd.taskextractor.pipeline;

import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.ConcurrentPipelineException;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.pipeline.spec.PipelineRequest;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepData;
import com.mindamd.taskextractor.pipeline.spec.StepResult;
import com.mindamd.taskextractor.pipeline.util.CheckpointManager;
import com.mindamd.taskextractor.pipeline.util.CheckpointState;
import com.mindamd.taskextractor.step.delivery.DeliveryResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class Orchestrator {

    private final PipelineRepository pipelineRepository;
    private final CheckpointManager checkpointManager;
    private final TreeMap<Integer, Step<?, ?>> steps;
    private final Set<String> runningPipelines = ConcurrentHashMap.newKeySet();

    public Orchestrator(PipelineRepository pipelineRepository,
                        CheckpointManager checkpointManager,
                        List<Step<?, ?>> steps) {
        this.pipelineRepository = pipelineRepository;
        this.checkpointManager = checkpointManager;
        this.steps = new TreeMap<>(steps.stream()
                .collect(Collectors.toMap(Step::getStepOrder, Function.identity())));
    }

    public String run(PipelineRequest request) {
        String requestKey = request.requestKey();
        if (!runningPipelines.add(requestKey)) {
            throw new ConcurrentPipelineException("Pipeline already running: " + requestKey);
        }
        try {
            return executePipeline(request);
        } finally {
            runningPipelines.remove(requestKey);
        }
    }

    private String executePipeline(PipelineRequest request) {
        Pipeline pipeline = loadPipeline(request.requestKey(), request.channelId());

        if (pipeline.isCompleted()) {
            CheckpointState state = checkpointManager.restore(pipeline.getId());
            return ((DeliveryResult) state.input().orElseThrow()).message();
        }

        if (pipeline.isFailed()) {
            throw new NonRecoverableException("Pipeline permanently failed: " + pipeline.getRequestKey());
        }

        if (pipeline.isRunCountExceeded()) {
            pipeline.markFailed();
            pipelineRepository.save(pipeline);
            throw new NonRecoverableException("Pipeline permanently failed: " + pipeline.getRequestKey());
        }

        pipeline.incrementRunCount();
        pipelineRepository.save(pipeline);

        CheckpointState state = checkpointManager.restore(pipeline.getId());
        StepData input = state.input().orElse(request);

        try {
            for (Step<?, ?> rawStep : steps.tailMap(state.stepOrder(), false).values()) {
                input = runStep(rawStep, input, pipeline);
            }
        } catch (NonRecoverableException e) {
            pipeline.markFailed();
            pipelineRepository.save(pipeline);
            throw e;
        }

        pipeline.markCompleted();
        pipelineRepository.save(pipeline);
        return ((DeliveryResult) input).message();
    }

    private Pipeline loadPipeline(String requestKey, String channelId) {
        return pipelineRepository.findByRequestKey(requestKey)
                .orElseGet(() -> pipelineRepository.save(Pipeline.of(requestKey, channelId)));
    }

    @SuppressWarnings("unchecked")
    private StepData runStep(Step<?, ?> rawStep, StepData input, Pipeline pipeline) {
        Step<StepData, StepData> step = (Step<StepData, StepData>) rawStep;
        StepResult<StepData> result = step.execute(input);
        checkpointManager.save(result, step.getStepOrder(), pipeline);
        return result.next();
    }
}
