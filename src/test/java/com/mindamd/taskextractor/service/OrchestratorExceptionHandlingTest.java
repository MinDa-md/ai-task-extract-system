package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.global.exception.RecoverableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrchestratorExceptionHandlingTest {

    static class TestPhaseData implements PhaseData {}

    PipelineRepository mockRepo = mock(PipelineRepository.class);
    List<PipelineStatus> savedStatuses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(mockRepo.findById(any())).thenReturn(Optional.empty());
        when(mockRepo.save(any())).thenAnswer(inv -> {
            Pipeline p = inv.getArgument(0);
            savedStatuses.add(p.getPipelineStatus());
            return p;
        });
    }

    Phase stubPhaseThrows(RuntimeException ex) {
        return new Phase() {
            @Override public String getStepId() { return "stub"; }
            @Override public PhaseData execute(String pipelineId, PhaseData input) { throw ex; }
            @Override public String serialize(PhaseData result) { return "{}"; }
            @Override public PhaseData deserialize(String json) { return new TestPhaseData(); }
        };
    }

    Orchestrator orchestratorWith(Phase phase) {
        return new Orchestrator(List.of(phase), mockRepo);
    }

    // RecoverableException → FAILED 전이
    @Test
    void recoverableException_setsPipelineStatusFailed() {
        // given
        Orchestrator orchestrator = orchestratorWith(stubPhaseThrows(new RecoverableException("fail")));

        // when
        try { orchestrator.run("pipe-1", new TestPhaseData()); } catch (RecoverableException ignored) {}

        // then
        assertThat(savedStatuses).contains(PipelineStatus.FAILED);
    }

    // NonRecoverableException → FINAL_FAILED 전이
    @Test
    void nonRecoverableException_setsPipelineStatusFinalFailed() {
        // given
        Orchestrator orchestrator = orchestratorWith(stubPhaseThrows(new NonRecoverableException("fatal")));

        // when
        try { orchestrator.run("pipe-2", new TestPhaseData()); } catch (NonRecoverableException ignored) {}

        // then
        assertThat(savedStatuses).contains(PipelineStatus.FINAL_FAILED);
    }

    // RecoverableException 재전파
    @Test
    void recoverableException_rethrows() {
        // given
        Orchestrator orchestrator = orchestratorWith(stubPhaseThrows(new RecoverableException("fail")));

        // when / then
        assertThrows(RecoverableException.class, () -> orchestrator.run("pipe-3", new TestPhaseData()));
    }

    // NonRecoverableException 재전파
    @Test
    void nonRecoverableException_rethrows() {
        // given
        Orchestrator orchestrator = orchestratorWith(stubPhaseThrows(new NonRecoverableException("fatal")));

        // when / then
        assertThrows(NonRecoverableException.class, () -> orchestrator.run("pipe-4", new TestPhaseData()));
    }

    // 미예상 예외 → 상태 변경 없이 전파
    @Test
    void unexpectedException_propagatesWithoutStatusChange() {
        // given
        Orchestrator orchestrator = orchestratorWith(stubPhaseThrows(new RuntimeException("unexpected")));

        // when
        assertThrows(RuntimeException.class, () -> orchestrator.run("pipe-5", new TestPhaseData()));

        // then
        assertThat(savedStatuses).doesNotContain(PipelineStatus.FAILED, PipelineStatus.FINAL_FAILED);
    }
}
