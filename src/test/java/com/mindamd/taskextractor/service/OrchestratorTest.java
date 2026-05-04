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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private static final TestData A = new TestData("A");
    private static final TestData B = new TestData("B");
    private static final TestData C = new TestData("C");

    @Autowired
    private PipelineRepository pipelineRepository;
    @Autowired
    private CheckpointRepository checkpointRepository;

    private Orchestrator orchestrator;

    @Mock
    private Step step1, step2, step3;

    @BeforeEach
    void setUp() {
        when(step1.getStepOrder()).thenReturn(10);
        when(step2.getStepOrder()).thenReturn(20);
        when(step3.getStepOrder()).thenReturn(30);

        orchestrator = new Orchestrator(pipelineRepository, checkpointRepository, List.of(step1, step2, step3));
    }

    @Test
    void tc01_newPipeline_allStepsRunInOrder_3CheckpointsSaved_pipelineCompleted() {
        when(step1.execute(PIPELINE_ID, INITIAL)).thenReturn(A);
        when(step1.serialize(A)).thenReturn("A");
        when(step2.execute(PIPELINE_ID, A)).thenReturn(B);
        when(step2.serialize(B)).thenReturn("B");
        when(step3.execute(eq(PIPELINE_ID), any())).thenReturn(C);
        when(step3.serialize(C)).thenReturn("C");

        Pipeline result = orchestrator.run(PIPELINE_ID, INITIAL);

        assertThat(result.getStatus()).isEqualTo(PipelineStatus.SUCCESS);

        List<Checkpoint> checkpoints = checkpointRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Checkpoint::getStepOrder))
                .toList();
        assertThat(checkpoints).hasSize(3);
        assertThat(checkpoints).extracting(Checkpoint::getStepOrder).containsExactly(10, 20, 30);
        assertThat(checkpoints.get(0).getCreatedAt()).isBefore(checkpoints.get(1).getCreatedAt());
        assertThat(checkpoints.get(1).getCreatedAt()).isBefore(checkpoints.get(2).getCreatedAt());

        InOrder inOrder = inOrder(step1, step2, step3);
        inOrder.verify(step1).execute(PIPELINE_ID, INITIAL);
        inOrder.verify(step2).execute(PIPELINE_ID, A);
        inOrder.verify(step3).execute(eq(PIPELINE_ID), any());
    }

    @Test
    void tc02_step1CheckpointExists_resumesFromStep2_step2ReceivesA() {
        Pipeline pipeline = pipelineRepository.save(new Pipeline(PIPELINE_ID));
        checkpointRepository.save(Checkpoint.builder().stepOrder(10).result("A").pipeline(pipeline).build());
        when(step1.deserialize("A")).thenReturn(A);
        when(step2.execute(PIPELINE_ID, A)).thenReturn(B);
        when(step2.serialize(B)).thenReturn("B");
        when(step3.execute(eq(PIPELINE_ID), any())).thenReturn(C);
        when(step3.serialize(C)).thenReturn("C");

        Pipeline result = orchestrator.run(PIPELINE_ID, INITIAL);

        verify(step1, never()).execute(any(), any());
        verify(step2).execute(PIPELINE_ID, A);
        verify(step3).execute(eq(PIPELINE_ID), any());
        assertThat(checkpointRepository.count()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }

    @Test
    void tc03_allCheckpointsExist_onlyStep3Reruns_checkpointCountUnchanged_completedAtUnchanged() {
        Pipeline pipeline = pipelineRepository.save(new Pipeline(PIPELINE_ID));
        checkpointRepository.save(Checkpoint.builder().stepOrder(10).result("A").pipeline(pipeline).build());
        checkpointRepository.save(Checkpoint.builder().stepOrder(20).result("B").pipeline(pipeline).build());
        checkpointRepository.save(Checkpoint.builder().stepOrder(30).result("C").pipeline(pipeline).build());
        pipeline.markCompleted();
        pipelineRepository.save(pipeline);
        LocalDateTime originalCompletedAt = pipeline.getCompletedAt();
        when(step3.execute(eq(PIPELINE_ID), any())).thenReturn(C);

        Pipeline result = orchestrator.run(PIPELINE_ID, INITIAL);

        verify(step1, never()).execute(any(), any());
        verify(step2, never()).execute(any(), any());
        verify(step3).execute(eq(PIPELINE_ID), any());
        assertThat(checkpointRepository.count()).isEqualTo(3);
        assertThat(result.getCompletedAt()).isEqualTo(originalCompletedAt);
        assertThat(result.getStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }

    @Test
    void tc04_step2ThrowsNonRecoverable_pipelineFinalFailed_noNewCheckpoint() {
        Pipeline pipeline = pipelineRepository.save(new Pipeline(PIPELINE_ID));
        checkpointRepository.save(Checkpoint.builder().stepOrder(10).result("A").pipeline(pipeline).build());
        when(step1.deserialize("A")).thenReturn(A);
        when(step2.execute(PIPELINE_ID, A)).thenThrow(new NonRecoverableException("fatal"));

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(NonRecoverableException.class);

        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.FINAL_FAILED);
        assertThat(checkpointRepository.count()).isEqualTo(1);
    }

    @Test
    void tc05_finalFailedPipeline_noStepsRun_nonRecoverableThrown() {
        Pipeline pipeline = new Pipeline(PIPELINE_ID);
        pipeline.markFinalFailed();
        pipelineRepository.save(pipeline);

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(NonRecoverableException.class);

        verify(step1, never()).execute(any(), any());
        verify(step2, never()).execute(any(), any());
        verify(step3, never()).execute(any(), any());
        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.FINAL_FAILED);
    }

    @Test
    void tc06_step1ThrowsNonRecoverable_pipelineSavedAsFinalFailed_noCheckpoints() {
        when(step1.execute(PIPELINE_ID, INITIAL)).thenThrow(new NonRecoverableException("fatal"));

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(NonRecoverableException.class);

        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.FINAL_FAILED);
        assertThat(checkpointRepository.count()).isEqualTo(0);
    }

    @Test
    void tc07_step1ThrowsRecoverable_pipelineRemainsRunning_noCheckpoints() {
        when(step1.execute(PIPELINE_ID, INITIAL)).thenThrow(new RecoverableException("retry"));

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(RecoverableException.class);

        Pipeline saved = pipelineRepository.findById(PIPELINE_ID).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PipelineStatus.FAILED);
        assertThat(checkpointRepository.count()).isEqualTo(0);
    }

}
