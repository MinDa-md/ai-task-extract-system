package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PipelineRepository extends JpaRepository<Pipeline, String> {
    Optional<Pipeline> findByRequestKey(String requestKey);
}
