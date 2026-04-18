package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrchestratorTest {

    Phase mockPhase1 = mock(Phase.class);
    Phase mockPhase2 = mock(Phase.class);
    PipelineRepository mockRepo = mock(PipelineRepository.class);
    Orchestrator orchestrator = new Orchestrator(List.of(mockPhase1, mockPhase2), mockRepo);

    List<PipelineStatus> savedPipelineStatuses = new ArrayList<>();

    static class TestPhaseData implements PhaseData {}

    @BeforeEach
    void setUp() {
        when(mockPhase1.getStepId()).thenReturn("phase1");
        when(mockPhase2.getStepId()).thenReturn("phase2");
        when(mockPhase1.execute(any(), any())).thenAnswer(i -> i.getArgument(1));
        when(mockPhase2.execute(any(), any())).thenAnswer(i -> i.getArgument(1));
        when(mockPhase2.serialize(any())).thenReturn("{}");
        when(mockRepo.findById(any())).thenReturn(Optional.empty());
        when(mockRepo.save(any())).thenAnswer(i -> {
            Pipeline p = i.getArgument(0);
            savedPipelineStatuses.add(p.getPipelineStatus());
            return p;
        });
    }

    @Test
    // phases 빈 리스트로 생성 시 예외
    void emptyPhases_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Orchestrator(List.of(), mockRepo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    // Phase들이 List 순서대로 실행된다
    void runExecutesPhasesInOrder() {
        // when
        orchestrator.run("req-order", new TestPhaseData());

        // then
        var inOrder = inOrder(mockPhase1, mockPhase2);
        inOrder.verify(mockPhase1).execute(any(), any());
        inOrder.verify(mockPhase2).execute(any(), any());
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
