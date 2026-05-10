package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRepository extends JpaRepository<Pipeline, String> {
}
