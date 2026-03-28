package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    // 멱등성 검증용 메서드
    boolean existsByRequestKey(String requestKey);
    Optional<Summary> findByRequestKey(String requestKey);
}