package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.RecoverableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrchestratorMdcTest {

    Phase mockPhase = mock(Phase.class);
    PipelineRepository mockRepo = mock(PipelineRepository.class);
    Orchestrator orchestrator = new Orchestrator(java.util.List.of(mockPhase), mockRepo);

    static class TestPhaseData implements PhaseData {}

    @BeforeEach
    void setUp() {
        when(mockPhase.getStepId()).thenReturn("phase1");
        when(mockPhase.serialize(any())).thenReturn("{}");
        when(mockRepo.findById(any())).thenReturn(Optional.empty());
        when(mockRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        MDC.clear();
    }

    @Test
    // run() 실행 중 MDC에 req_id가 등록됨
    void mdcContainsReqIdDuringExecution() {
        // given
        AtomicReference<String> capturedMdc = new AtomicReference<>();
        when(mockPhase.execute(any(), any())).thenAnswer(inv -> {
            capturedMdc.set(MDC.get("req_id"));
            return inv.getArgument(1);
        });

        // when
        orchestrator.run("pipeline-mdc-1", new TestPhaseData());

        // then
        assertThat(capturedMdc.get()).isEqualTo("pipeline-mdc-1");
    }

    @Test
    // run() 완료 후 MDC가 정리됨
    void mdcClearedAfterExecution() {
        // given
        when(mockPhase.execute(any(), any())).thenAnswer(inv -> inv.getArgument(1));

        // when
        orchestrator.run("pipeline-mdc-2", new TestPhaseData());

        // then
        assertThat(MDC.get("req_id")).isNull();
    }

    @Test
    // 예외 발생 시에도 MDC가 정리됨
    void mdcClearedOnException() {
        // given
        when(mockPhase.execute(any(), any())).thenThrow(new RecoverableException("timeout"));
        when(mockRepo.save(any())).thenAnswer(i -> {
            Pipeline p = i.getArgument(0);
            return p;
        });

        // when
        catchThrowable(() -> orchestrator.run("pipeline-mdc-3", new TestPhaseData()));

        // then
        assertThat(MDC.get("req_id")).isNull();
    }
}
