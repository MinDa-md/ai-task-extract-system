package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.PhaseExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhaseExecutionRepository extends JpaRepository<PhaseExecution, Long> {
    List<PhaseExecution> findAllByPipelineId(String pipelineId);

    @Query(value = "SELECT COALESCE(MAX(step_order), 0) FROM phase WHERE pipeline_id = :pipelineId", nativeQuery = true)
    Integer findLastCompletedStepOrder(@Param("pipelineId") String pipelineId);
}
