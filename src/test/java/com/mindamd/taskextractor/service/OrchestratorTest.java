package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.repository.PipelineExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.mindamd.taskextractor.domain.entity.PipelineExecution;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrchestratorTest {

    Phase mockPhase1 = mock(Phase.class);
    Phase mockPhase2 = mock(Phase.class);
    PipelineExecutionRepository mockRepo = mock(PipelineExecutionRepository.class);
    Orchestrator orchestrator = new Orchestrator(List.of(mockPhase1, mockPhase2), mockRepo);

    List<PipelineStatus> savedPipelineStatuses = new ArrayList<>();

    static class TestPhaseData implements PhaseData {}

    @BeforeEach
    void setUp() {
        when(mockPhase1.execute(any())).thenAnswer(i -> i.getArguments()[0]);
        when(mockPhase2.execute(any())).thenAnswer(i -> i.getArguments()[0]);
        when(mockRepo.save(any())).thenAnswer(i -> {
            PipelineExecution e = i.getArgument(0);
            savedPipelineStatuses.add(e.getPipelineStatus());
            return e;
        });
    }

    @Test
    // Phase들이 List 순서대로 실행된다
    void runExecutesPhasesInOrder() {
        // when
        orchestrator.run("req-order", new TestPhaseData());

        // then
        InOrder inOrder = inOrder(mockPhase1, mockPhase2);
        inOrder.verify(mockPhase1).execute(any());
        inOrder.verify(mockPhase2).execute(any());
    }

    @Test
    // run 시작 시 RUNNING으로 저장된다
    void runSavesExecutionRecordWithRunningStatus() {
        // when
        orchestrator.run("req-running", new TestPhaseData());

        // then
        assertThat(savedPipelineStatuses).contains(PipelineStatus.RUNNING);
    }

    @Test
    // 모든 Phase 완료 후 SUCCESS로 저장된다
    void runSavesSuccessStatusAfterAllPhases() {
        // when
        orchestrator.run("req-success", new TestPhaseData());

        // then
        assertThat(savedPipelineStatuses).last().isEqualTo(PipelineStatus.SUCCESS);
    }
}
