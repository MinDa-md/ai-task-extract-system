package com.mindamd.taskextractor.domain.entity;

import com.mindamd.taskextractor.domain.PipelineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Pipeline {

    @Id
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_status")
    private PipelineStatus pipelineStatus;

    @Column(name = "final_result", columnDefinition = "text")
    private String finalResult;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void markRunning() {
        this.pipelineStatus = PipelineStatus.RUNNING;
    }

    public void recordFailure(PipelineStatus status) {
        this.pipelineStatus = status;
    }

    public void complete(LocalDateTime completedAt, String finalResult) {
        this.pipelineStatus = PipelineStatus.SUCCESS;
        this.completedAt = completedAt;
        this.finalResult = finalResult;
    }
}
