package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.MessageContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageContextRepository extends JpaRepository<MessageContext, Long> {

    // 멱등성 검증: 동일 요청 중복 저장 방지
    boolean existsByRequestKey(String requestKey);

    // 요약 재생성 등을 위한 원문 조회
    Optional<MessageContext> findByRequestKey(String requestKey);
}
