package com.mindamd.taskextractor.pipeline.util;

import com.mindamd.taskextractor.domain.entity.ArtifactCheckpoint;
import com.mindamd.taskextractor.domain.entity.Checkpoint;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.ArtifactCheckpointRepository;
import com.mindamd.taskextractor.domain.repository.CheckpointRepository;
import com.mindamd.taskextractor.pipeline.spec.StepData;
import com.mindamd.taskextractor.pipeline.spec.StepResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CheckpointManager {

    private static final int BEFORE_FIRST_STEP = -1;

    private final CheckpointRepository checkpointRepository;
    private final ArtifactCheckpointRepository artifactCheckpointRepository;
    private final StepSerializer stepSerializer;

    public CheckpointState restore(String pipelineId) {
        Optional<Checkpoint> existing = checkpointRepository.findTopByPipelineIdOrderByStepOrderDesc(pipelineId);
        if (existing.isEmpty()) {
            return new CheckpointState(BEFORE_FIRST_STEP, Optional.empty());
        }
        Checkpoint checkpoint = existing.get();
        StepData stepData = stepSerializer.deserialize(checkpoint.getResult(), StepData.class);
        return new CheckpointState(checkpoint.getStepOrder(), Optional.of(stepData));
    }

    public void save(StepResult<StepData> stepResult, int stepOrder, Pipeline pipeline) {
        StepData next = stepResult.next();
        List<StepData> artifacts = stepResult.artifacts();

        checkpointRepository.save(Checkpoint.of(stepOrder, stepSerializer.serialize(next), pipeline));

        artifacts.forEach(artifact ->
                artifactCheckpointRepository.save(
                        ArtifactCheckpoint.of(stepOrder, stepSerializer.serialize(artifact), pipeline)
                )
        );
    }
}
