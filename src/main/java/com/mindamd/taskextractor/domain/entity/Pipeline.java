package com.mindamd.taskextractor.domain.entity;

import com.mindamd.taskextractor.domain.PipelineStatus;
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

    private Pipeline(String id) {
        this.id = id;
    }

    public static Pipeline of(String id) {
        return new Pipeline(id);
    }

}
