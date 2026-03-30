package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.PipelineExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineExecutionRepository extends JpaRepository<PipelineExecution, String> {
}
