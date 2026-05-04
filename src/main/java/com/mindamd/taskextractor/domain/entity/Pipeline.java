package com.mindamd.taskextractor.domain.entity;

import com.mindamd.taskextractor.domain.PipelineStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private PipelineStatus status = PipelineStatus.RUNNING;

    @OneToMany(mappedBy = "pipeline")
    private List<Checkpoint> checkpoints = new ArrayList<>();

    @OneToOne(mappedBy = "pipeline")
    private Summary summary;

    public void markFinalFailed() {
        this.status = PipelineStatus.FINAL_FAILED;
    }

    public void markCompleted() {
        this.completedAt = LocalDateTime.now();
        this.status = PipelineStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = PipelineStatus.FAILED;
    }

    public boolean isCompleted() {
        return status == PipelineStatus.SUCCESS;
    }

    public boolean isFinalFailed() { return status == PipelineStatus.FINAL_FAILED; }

    public Pipeline(String id) {
        this.id = id;
    }

}
