package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.ArtifactCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactCheckpointRepository extends JpaRepository<ArtifactCheckpoint, Long> {
}
