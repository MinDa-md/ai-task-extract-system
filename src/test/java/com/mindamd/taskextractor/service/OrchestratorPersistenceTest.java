package com.mindamd.taskextractor.service;

import com.mindamd.taskextractor.TestcontainersConfiguration;
import com.mindamd.taskextractor.domain.Phase;
import com.mindamd.taskextractor.domain.PhaseData;
import com.mindamd.taskextractor.domain.PipelineStatus;
import com.mindamd.taskextractor.domain.repository.PipelineExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OrchestratorPersistenceTest {

    @Autowired
    PipelineExecutionRepository repository;

    @Autowired
    TestEntityManager entityManager;

    Orchestrator orchestrator;

    static class TestPhaseData implements PhaseData {}

    static class StubPhaseA implements Phase {
        @Override
        public PhaseData execute(PhaseData input) { return input; }
        @Override
        public PhaseData restore(String serializedData) { return null; }
    }

    static class StubPhaseB implements Phase {
        @Override
        public PhaseData execute(PhaseData input) { return input; }
        @Override
        public PhaseData restore(String serializedData) { return null; }
    }

    @BeforeEach
    void setUp() {
        orchestrator = new Orchestrator(List.of(new StubPhaseA(), new StubPhaseB()), repository);
    }

    @Test
    // run 완료 후 SUCCESS와 완료 시각이 DB에 저장된다
    void runSavesSuccessStatusAndCompletedAt() {
        // when
        orchestrator.run("req-persist-1", new TestPhaseData());
        entityManager.flush();
        entityManager.clear();

        // then
        var result = repository.findById("req-persist-1");
        assertThat(result).isPresent();
        assertThat(result.get().getPipelineStatus()).isEqualTo(PipelineStatus.SUCCESS);
        assertThat(result.get().getCompletedAt()).isNotNull();
    }

    @Test
    // 마지막 Phase의 step_id·step_status가 DB에 저장된다
    void runUpdatesStepIdAndStatusPerPhase() {
        // when
        orchestrator.run("req-persist-2", new TestPhaseData());
        entityManager.flush();
        entityManager.clear();

        // then
        var result = repository.findById("req-persist-2");
        assertThat(result).isPresent();
        assertThat(result.get().getStepId()).isEqualTo("StubPhaseB");
        assertThat(result.get().getStepStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }
}
