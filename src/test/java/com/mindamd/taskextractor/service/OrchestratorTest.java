package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.TestcontainersConfiguration;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.Step;
import com.mindamd.taskextractor.domain.StepData;
import com.mindamd.taskextractor.domain.entity.Checkpoint;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.CheckpointRepository;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.global.exception.RecoverableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DataJpaTest
@ExtendWith(MockitoExtension.class)
@Import({TestcontainersConfiguration.class, OrchestratorTest.JpaAuditingConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrchestratorTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingConfig {}

    record TestData(String value) implements StepData {}

    private static final String PIPELINE_ID = "P1";
    private static final TestData INITIAL = new TestData("initial");
    private static final TestData INGESTED = new TestData("ingested");
    private static final TestData A = new TestData("A");
    private static final TestData B = new TestData("B");
    private static final TestData DELIVERED = new TestData("delivered");

    @Autowired
    private PipelineRepository pipelineRepository;
    @Autowired
    private CheckpointRepository checkpointRepository;

    private Orchestrator orchestrator;

    @Mock
    private Step ingestStep, step1, step2, deliveryStep;

    @BeforeEach
    void setUp() {
        when(ingestStep.getStepOrder()).thenReturn(0);
        when(step1.getStepOrder()).thenReturn(10);
        when(step2.getStepOrder()).thenReturn(20);
        when(deliveryStep.getStepOrder()).thenReturn(99);

        orchestrator = new Orchestrator(pipelineRepository, checkpointRepository,
                List.of(ingestStep, step1, step2, deliveryStep));
    }

    @Test
    void tc01_newPipeline_allStepsRunInOrder_4CheckpointsSaved_pipelineCompleted() {
        when(ingestStep.execute(PIPELINE_ID, INITIAL)).thenReturn(INGESTED);
        when(ingestStep.serialize(INGESTED)).thenReturn("INGESTED");
        when(step1.execute(PIPELINE_ID, INGESTED)).thenReturn(A);
        when(step1.serialize(A)).thenReturn("A");
        when(step2.execute(PIPELINE_ID, A)).thenReturn(B);
        when(step2.serialize(B)).thenReturn("B");
        when(deliveryStep.execute(eq(PIPELINE_ID), any())).thenReturn(DELIVERED);
        when(deliveryStep.serialize(DELIVERED)).thenReturn("DELIVERED");

        Pipeline result = orchestrator.run(PIPELINE_ID, INITIAL);

        assertThat(result.getStatus()).isEqualTo(PipelineStatus.SUCCESS);

        List<Checkpoint> checkpoints = checkpointRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Checkpoint::getStepOrder))
                .toList();
        assertThat(checkpoints).hasSize(4);
        assertThat(checkpoints).extracting(Checkpoint::getStepOrder).containsExactly(0, 10, 20, 99);
        assertThat(checkpoints.get(0).getCreatedAt()).isBefore(checkpoints.get(1).getCreatedAt());
        assertThat(checkpoints.get(1).getCreatedAt()).isBefore(checkpoints.get(2).getCreatedAt());
        assertThat(checkpoints.get(2).getCreatedAt()).isBefore(checkpoints.get(3).getCreatedAt());

        InOrder inOrder = inOrder(ingestStep, step1, step2, deliveryStep);
        inOrder.verify(ingestStep).execute(PIPELINE_ID, INITIAL);
        inOrder.verify(step1).execute(PIPELINE_ID, INGESTED);
        inOrder.verify(step2).execute(PIPELINE_ID, A);
        inOrder.verify(deliveryStep).execute(eq(PIPELINE_ID), any());
    }

    @Test
    void tc02_step1CheckpointExists_resumesFromStep2_step2ReceivesA() {
        Pipeline pipeline = pipelineRepository.save(Pipeline.of(PIPELINE_ID));
        checkpointRepository.save(Checkpoint.of(10, "A", pipeline));
        when(step1.deserialize("A")).thenReturn(A);
        when(step2.execute(PIPELINE_ID, A)).thenReturn(B);
        when(step2.serialize(B)).thenReturn("B");
        when(deliveryStep.execute(eq(PIPELINE_ID), any())).thenReturn(DELIVERED);
        when(deliveryStep.serialize(DELIVERED)).thenReturn("DELIVERED");

        Pipeline result = orchestrator.run(PIPELINE_ID, INITIAL);

        verify(ingestStep, never()).execute(any(), any());
        verify(step1, never()).execute(any(), any());
        verify(step2).execute(PIPELINE_ID, A);
        verify(deliveryStep).execute(eq(PIPELINE_ID), any());
        assertThat(checkpointRepository.count()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }

    @Test
    void tc03_ingestCheckpointExists_resumesFromStep1_step1ReceivesIngested() {
        Pipeline pipeline = pipelineRepository.save(Pipeline.of(PIPELINE_ID));
        checkpointRepository.save(Checkpoint.of(0, "INGESTED", pipeline));
        when(ingestStep.deserialize("INGESTED")).thenReturn(INGESTED);
        when(step1.execute(PIPELINE_ID, INGESTED)).thenReturn(A);
        when(step1.serialize(A)).thenReturn("A");
        when(step2.execute(PIPELINE_ID, A)).thenReturn(B);
        when(step2.serialize(B)).thenReturn("B");
        when(deliveryStep.execute(eq(PIPELINE_ID), any())).thenReturn(DELIVERED);
        when(deliveryStep.serialize(DELIVERED)).thenReturn("DELIVERED");

        Pipeline result = orchestrator.run(PIPELINE_ID, INITIAL);

        verify(ingestStep, never()).execute(any(), any());
        verify(step1).execute(PIPELINE_ID, INGESTED);
        assertThat(checkpointRepository.count()).isEqualTo(4);
        assertThat(result.getStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }

    @Test
    void tc04_allCheckpointsExist_completedPipeline_onlyDeliveryStepReruns_checkpointCountUnchanged_completedAtUnchanged() {
        Pipeline pipeline = pipelineRepository.save(Pipeline.of(PIPELINE_ID));
        checkpointRepository.save(Checkpoint.of(0, "INGESTED", pipeline));
        checkpointRepository.save(Checkpoint.of(10, "A", pipeline));
        checkpointRepository.save(Checkpoint.of(20, "B", pipeline));
        checkpointRepository.save(Checkpoint.of(99, "DELIVERED", pipeline));
        pipeline.markCompleted();
        pipelineRepository.save(pipeline);
        LocalDateTime originalCompletedAt = pipeline.getCompletedAt();
        when(deliveryStep.execute(eq(PIPELINE_ID), any())).thenReturn(DELIVERED);

        Pipeline result = orchestrator.run(PIPELINE_ID, INITIAL);

        verify(ingestStep, never()).execute(any(), any());
        verify(step1, never()).execute(any(), any());
        verify(step2, never()).execute(any(), any());
        verify(deliveryStep).execute(eq(PIPELINE_ID), any());
        assertThat(checkpointRepository.count()).isEqualTo(4);
        assertThat(result.getCompletedAt()).isEqualTo(originalCompletedAt);
        assertThat(result.getStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }

    @Test
    void tc05_step2ThrowsNonRecoverable_pipelineFailed_noNewCheckpoint() {
        Pipeline pipeline = pipelineRepository.save(Pipeline.of(PIPELINE_ID));
        checkpointRepository.save(Checkpoint.of(10, "A", pipeline));
        when(step1.deserialize("A")).thenReturn(A);
        when(step2.execute(PIPELINE_ID, A)).thenThrow(new NonRecoverableException("fatal"));

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(NonRecoverableException.class);

        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.FAILED);
        assertThat(checkpointRepository.count()).isEqualTo(1);
    }

    @Test
    void tc06_failedPipeline_noStepsRun_nonRecoverableThrown() {
        Pipeline pipeline = Pipeline.of(PIPELINE_ID);
        pipeline.markFailed();
        pipelineRepository.save(pipeline);

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(NonRecoverableException.class);

        verify(ingestStep, never()).execute(any(), any());
        verify(step1, never()).execute(any(), any());
        verify(step2, never()).execute(any(), any());
        verify(deliveryStep, never()).execute(any(), any());
        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.FAILED);
    }

    @Test
    void tc07_ingestStepThrowsNonRecoverable_pipelineSavedAsFailed_noCheckpoints() {
        when(ingestStep.execute(PIPELINE_ID, INITIAL)).thenThrow(new NonRecoverableException("fatal"));

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(NonRecoverableException.class);

        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.FAILED);
        assertThat(checkpointRepository.count()).isEqualTo(0);
    }

    @Test
    void tc08_ingestStepThrowsRecoverable_pipelineRemainsRunning_noCheckpoints() {
        when(ingestStep.execute(PIPELINE_ID, INITIAL)).thenThrow(new RecoverableException("retry"));

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(RecoverableException.class);

        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.RUNNING);
        assertThat(checkpointRepository.count()).isEqualTo(0);
    }

    @Test
    void tc09_runCountExceeded_pipelineMarkedFailed_nonRecoverableThrown_noStepsRun() {
        Pipeline pipeline = Pipeline.of(PIPELINE_ID);
        pipeline.incrementRunCount();
        pipeline.incrementRunCount();
        pipeline.incrementRunCount();
        pipelineRepository.save(pipeline);

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(NonRecoverableException.class);

        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.FAILED);
        verify(ingestStep, never()).execute(any(), any());
        verify(step1, never()).execute(any(), any());
        verify(step2, never()).execute(any(), any());
        verify(deliveryStep, never()).execute(any(), any());
    }

}
