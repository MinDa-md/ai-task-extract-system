package com.mindamd.taskextractor.domain;

import com.mindamd.taskextractor.TestcontainersConfiguration;
import com.mindamd.taskextractor.domain.entity.Pipeline;
import com.mindamd.taskextractor.domain.repository.PipelineRepository;
import com.mindamd.taskextractor.domain.PipelineStatus;
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
    PipelineRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    // Pipeline을 RUNNING 상태로 저장하면 id와 status가 DB에 반영된다
    void savePipeline() {
        // given
        Pipeline pipeline = Pipeline.builder()
                .id("pipe-001")
                .pipelineStatus(PipelineStatus.RUNNING)
                .build();

        // when
        Pipeline saved = repository.save(pipeline);

        // then
        assertThat(saved.getId()).isEqualTo("pipe-001");
        assertThat(saved.getPipelineStatus()).isEqualTo(PipelineStatus.RUNNING);
    }

    @Test
    // 저장된 Pipeline을 id로 조회할 수 있다
    void findById() {
        // given
        repository.save(Pipeline.builder().id("pipe-002").pipelineStatus(PipelineStatus.SUCCESS).build());
        entityManager.flush();
        entityManager.clear();

        // when
        var found = repository.findById("pipe-002");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getPipelineStatus()).isEqualTo(PipelineStatus.SUCCESS);
    }

    @Test
    // recordFailure는 pipelineStatus를 FAILED로 전이한다
    void recordFailure() {
        // given
        Pipeline saved = repository.save(Pipeline.builder().id("pipe-003").pipelineStatus(PipelineStatus.RUNNING).build());

        // when
        saved.recordFailure(PipelineStatus.FAILED);
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        // then
        Pipeline updated = repository.findById("pipe-003").get();
        assertThat(updated.getPipelineStatus()).isEqualTo(PipelineStatus.FAILED);
    }

    @Test
    // complete는 completedAt, finalResult, status=SUCCESS를 저장한다
    void complete() {
        // given
        Pipeline saved = repository.save(Pipeline.builder().id("pipe-004").pipelineStatus(PipelineStatus.RUNNING).build());

        // when
        saved.complete(LocalDateTime.now(), "{\"key\":\"value\"}");
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        // then
        Pipeline updated = repository.findById("pipe-004").get();
        assertThat(updated.getPipelineStatus()).isEqualTo(PipelineStatus.SUCCESS);
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getFinalResult()).isEqualTo("{\"key\":\"value\"}");
    }
}
