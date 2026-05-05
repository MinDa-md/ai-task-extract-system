package com.mindamd.taskextractor.domain.repository;

import com.mindamd.taskextractor.domain.entity.MessageContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageContextRepository extends JpaRepository<MessageContext, Long> {

    boolean existsByRequestKey(String requestKey);

    Optional<MessageContext> findByRequestKey(String requestKey);
}
