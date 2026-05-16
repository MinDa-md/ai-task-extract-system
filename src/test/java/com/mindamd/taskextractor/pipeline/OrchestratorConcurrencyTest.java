package com.mindamd.taskextractor.pipeline;

import com.mindamd.taskextractor.TestcontainersConfiguration;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.entity.PipelineStatus;
import com.mindamd.taskextractor.domain.repository.CheckpointRepository;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.global.exception.ConcurrentPipelineException;
import com.mindamd.taskextractor.pipeline.spec.Step;
import com.mindamd.taskextractor.pipeline.spec.StepData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(MockitoExtension.class)
@Import({TestcontainersConfiguration.class, OrchestratorConcurrencyTest.JpaAuditingConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrchestratorConcurrencyTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingConfig {}

    record TestData(String value) implements StepData {}

    private static final String PIPELINE_ID = "P_CONCURRENT";
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

    @AfterEach
    void cleanup() {
        checkpointRepository.deleteAll();
        pipelineRepository.deleteAll();
    }

    @Test
    void tc09_samePipelineId_concurrentRun_secondThrowsConcurrentPipelineException() throws Exception {
        CountDownLatch step1Started = new CountDownLatch(1);
        CountDownLatch step1Proceed = new CountDownLatch(1);

        when(step1.execute(eq(PIPELINE_ID), any())).thenAnswer(inv -> {
            step1Started.countDown();
            step1Proceed.await();
            return A;
        });
        when(step1.serialize(A)).thenReturn("A");
        when(step2.execute(eq(PIPELINE_ID), any())).thenReturn(B);
        when(step2.serialize(B)).thenReturn("B");
        when(step3.execute(eq(PIPELINE_ID), any())).thenReturn(C);
        when(step3.serialize(C)).thenReturn("C");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Pipeline> first = executor.submit(() -> orchestrator.run(PIPELINE_ID, INITIAL));
        step1Started.await();

        assertThatThrownBy(() -> orchestrator.run(PIPELINE_ID, INITIAL))
                .isInstanceOf(ConcurrentPipelineException.class);

        step1Proceed.countDown();
        assertThat(first.get().getStatus()).isEqualTo(PipelineStatus.SUCCESS);
        executor.shutdown();
    }
}
