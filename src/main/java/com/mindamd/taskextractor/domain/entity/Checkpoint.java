package com.mindamd.taskextractor.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkpoint", uniqueConstraints = @UniqueConstraint(columnNames = {"pipeline_id", "step_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Checkpoint {

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

    private Checkpoint(Integer stepOrder, String result, Pipeline pipeline) {
        this.stepOrder = stepOrder;
        this.result = result;
        this.pipeline = pipeline;
    }

    public static Checkpoint of(Integer stepOrder, String result, Pipeline pipeline) {
        return new Checkpoint(stepOrder, result, pipeline);
    }
}
