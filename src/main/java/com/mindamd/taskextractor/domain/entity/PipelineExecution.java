package com.mindamd.taskextractor.domain.entity;

import com.mindamd.taskextractor.domain.PipelineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline_execution")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineExecution {

    @Id
    @Column(name = "req_id")
    private String reqId;

    @Column(name = "step_id")
    private String stepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_status")
    private PipelineStatus stepStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_status")
    private PipelineStatus pipelineStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void updatePipelineStatus(PipelineStatus status) {
        this.pipelineStatus = status;
    }
}
