package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.TestcontainersConfiguration;
import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.PhaseExecutionRepository;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.NonRecoverableException;
import com.mindamd.taskextractor.global.exception.RecoverableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OrchestratorResumptionTest {

    // JDK dynamic proxy(Phase가 interface)로 인해 CountingStubPhase로 직접 @Autowired 불가 →
    // static 필드로 실행 횟수 추적
    static class CountingStubPhase implements Phase, Ordered {

        private final String stepId;
        private final int order;

        static int phase1Count = 0;
        static int phase2Count = 0;
        static boolean phase2FailOnNext = false;

        CountingStubPhase(String stepId, int order) {
            this.stepId = stepId;
            this.order = order;
        }

        @Override public int getOrder() { return order; }
        @Override public String getStepId() { return stepId; }

        @Override
        public PhaseData execute(String pipelineId, PhaseData input) {
            if ("phase1".equals(stepId)) phase1Count++;
            else if ("phase2".equals(stepId)) {
                phase2Count++;
                if (phase2FailOnNext) {
                    phase2FailOnNext = false;
                    throw new RecoverableException("simulated failure");
                }
            }
            return input;
        }

        @Override
        public String serialize(PhaseData result) { return "{}"; }

        @Override
        public PhaseData deserialize(String json) { return new TestPhaseData(); }
    }

    static class TestPhaseData implements PhaseData {
        @Override
        public boolean equals(Object o) { return o instanceof TestPhaseData; }
        @Override
        public int hashCode() { return 0; }
    }

    @TestConfiguration
    static class TestPhaseConfig {
        @Bean
        CountingStubPhase phase1() { return new CountingStubPhase("phase1", 1); }

        @Bean
        CountingStubPhase phase2() { return new CountingStubPhase("phase2", 2); }
    }

    @Autowired Orchestrator orchestrator;
    @Autowired PipelineRepository pipelineRepository;
    @Autowired PhaseExecutionRepository phaseExecutionRepository;

    @BeforeEach
    void reset() {
        phaseExecutionRepository.deleteAll();
        pipelineRepository.deleteAll();
        CountingStubPhase.phase1Count = 0;
        CountingStubPhase.phase2Count = 0;
        CountingStubPhase.phase2FailOnNext = false;
    }

    // 최초 실행 시 모든 Phase 실행
    @Test
    void runWhenNoExecution_executesAllPhases() {
        // when
        orchestrator.run("pipe-new", new TestPhaseData());

        // then
        assertThat(CountingStubPhase.phase1Count).isEqualTo(1);
        assertThat(CountingStubPhase.phase2Count).isEqualTo(1);
        assertThat(pipelineRepository.findById("pipe-new"))
                .isPresent()
                .get()
                .extracting(Pipeline::getPipelineStatus)
                .isEqualTo(PipelineStatus.SUCCESS);
    }

    // SUCCESS 재실행 시 Phase 미재실행, finalResult 반환
    @Test
    void runWhenSuccess_returnsSavedResult() {
        // given - 첫 번째 실행
        PhaseData firstResult = orchestrator.run("pipe-success", new TestPhaseData());
        int countAfterFirst = CountingStubPhase.phase1Count + CountingStubPhase.phase2Count;

        // when - SUCCESS 상태에서 재실행
        PhaseData secondResult = orchestrator.run("pipe-success", new TestPhaseData());

        // then
        assertThat(CountingStubPhase.phase1Count + CountingStubPhase.phase2Count).isEqualTo(countAfterFirst);
        assertThat(secondResult).isEqualTo(firstResult);
    }

    // FINAL_FAILED 재실행 차단
    @Test
    void runWhenFinalFailed_throwsNonRecoverableException() {
        // given
        Pipeline failed = Pipeline.builder()
                .id("pipe-final-failed")
                .pipelineStatus(PipelineStatus.FINAL_FAILED)
                .build();
        pipelineRepository.save(failed);

        // when / then
        assertThrows(NonRecoverableException.class,
                () -> orchestrator.run("pipe-final-failed", new TestPhaseData()));
    }

    // RUNNING 중복 요청 거절
    @Test
    void runWhenRunning_rejectsDuplicateRequest() {
        // given
        Pipeline running = Pipeline.builder()
                .id("pipe-running")
                .pipelineStatus(PipelineStatus.RUNNING)
                .build();
        pipelineRepository.save(running);

        // when / then
        assertThrows(IllegalStateException.class,
                () -> orchestrator.run("pipe-running", new TestPhaseData()));
    }

    // FAILED 재실행 시 이전 성공 Phase 스킵, 실패 Phase만 재실행
    @Test
    void runWhenFailed_resumesFromFailedPhase() {
        // given - phase2가 첫 번째 실행에서 실패
        CountingStubPhase.phase2FailOnNext = true;
        try {
            orchestrator.run("pipe-resume", new TestPhaseData());
        } catch (RecoverableException ignored) {}

        int phase1CountAfterFail = CountingStubPhase.phase1Count;
        int phase2CountAfterFail = CountingStubPhase.phase2Count;

        // when - 재실행
        orchestrator.run("pipe-resume", new TestPhaseData());

        // then - phase1은 캐시 히트로 재실행 없음, phase2만 재실행
        assertThat(CountingStubPhase.phase1Count).isEqualTo(phase1CountAfterFail);
        assertThat(CountingStubPhase.phase2Count).isEqualTo(phase2CountAfterFail + 1);
    }
}
