package com.mindamd.taskextractor.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Pipeline {

    @Id
    private String id;

    @Column(nullable = false, updatable = false)
    private String requestKey;

    @Column(nullable = false, updatable = false)
    private String channelId;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PipelineStatus status = PipelineStatus.RUNNING;

    @Column(nullable = false)
    private Integer runCount = 0;

    public void markFailed() {
        this.status = PipelineStatus.FAILED;
    }

    public void markCompleted() {
        this.completedAt = LocalDateTime.now();
        this.status = PipelineStatus.SUCCESS;
    }

    public void incrementRunCount() {
        this.runCount++;
    }

    public boolean isCompleted() {
        return status == PipelineStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == PipelineStatus.FAILED;
    }

    public boolean isRunCountExceeded() {
        return runCount >= 3;
    }

    private Pipeline(String requestKey, String channelId) {
        this.requestKey = requestKey;
        this.channelId = channelId;
    }

    public static Pipeline of(String requestKey, String channelId) {
        return new Pipeline(requestKey, channelId);
    }
}
