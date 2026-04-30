package com.mindamd.taskextractor.domain.entity;

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
    @Column(name = "id")
    private String id;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private boolean finalFailed = false;

    @OneToMany(mappedBy = "pipeline")
    private List<PhaseExecution> phases = new ArrayList<>();

    @OneToOne(mappedBy = "pipeline")
    private Summary summary;

    public void markFinalFailed() {
        this.finalFailed = true;
    }

    public void markCompleted() {
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public Pipeline(String id) {
        this.id = id;
    }
}
