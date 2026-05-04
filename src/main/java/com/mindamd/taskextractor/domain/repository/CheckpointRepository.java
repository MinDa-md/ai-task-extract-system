package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    Optional<Checkpoint> findTopByPipelineIdOrderByStepOrderDesc(String pipelineId);
}
