package com.mindamd.taskextractor.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "phase", uniqueConstraints = @UniqueConstraint(columnNames = {"pipeline_id", "step_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PhaseExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(columnDefinition = "text")
    private String result;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id")
    private Pipeline pipeline;

    @Builder
    public PhaseExecution(Integer stepOrder, String result, Pipeline pipeline) {
        this.stepOrder = stepOrder;
        this.result = result;
        this.pipeline = pipeline;
    }
}
