package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.PhaseExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhaseExecutionRepository extends JpaRepository<PhaseExecution, Long> {

    Optional<PhaseExecution> findByPipelineIdAndStepId(String pipelineId, String stepId);
}
