package com.mindamd.taskextractor.domain;

import com.mindamd.taskextractor.TestcontainersConfiguration;
import com.mindamd.taskextractor.domain.entity.PipelineExecution;
import com.mindamd.taskextractor.domain.repository.PipelineExecutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PipelineExecutionRepositoryTest {

    @Autowired
    PipelineExecutionRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void savePipelineExecution() {
        // given
        PipelineExecution execution = buildExecution("req-001", PipelineStatus.RUNNING);

        // when
        PipelineExecution saved = repository.save(execution);

        // then
        assertThat(saved.getReqId()).isEqualTo("req-001");
        assertThat(saved.getPipelineStatus()).isEqualTo(PipelineStatus.RUNNING);
    }

    @Test
    void findByReqId() {
        // given
        repository.save(buildExecution("req-002", PipelineStatus.SUCCESS));
        entityManager.flush();
        entityManager.clear();

        // when
        var found = repository.findById("req-002");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getPipelineStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }

    @Test
    void updatePipelineStatus() {
        // given
        PipelineExecution saved = repository.save(buildExecution("req-003", PipelineStatus.RUNNING));

        // when
        saved.updatePipelineStatus(PipelineStatus.SUCCESS);
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        // then
        PipelineExecution updated = repository.findById("req-003").get();
        assertThat(updated.getPipelineStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }

    @Test
    // updateStep은 stepId와 stepStatus를 갱신한다
    void updateStep() {
        // given
        PipelineExecution saved = repository.save(buildExecution("req-004", PipelineStatus.RUNNING));

        // when
        saved.updateStep("phase-1", PipelineStatus.RUNNING);
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        // then
        PipelineExecution updated = repository.findById("req-004").get();
        assertThat(updated.getStepId()).isEqualTo("phase-1");
        assertThat(updated.getStepStatus()).isEqualTo(PipelineStatus.RUNNING);
    }

    @Test
    // complete는 completedAt을 설정한다
    void complete() {
        // given
        PipelineExecution saved = repository.save(buildExecution("req-005", PipelineStatus.RUNNING));

        // when
        saved.complete(LocalDateTime.now());
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        // then
        PipelineExecution updated = repository.findById("req-005").get();
        assertThat(updated.getCompletedAt()).isNotNull();
    }

    private PipelineExecution buildExecution(String reqId, PipelineStatus status) {
        return PipelineExecution.builder()
                .reqId(reqId)
                .stepId("phase-1")
                .stepStatus(status)
                .pipelineStatus(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
