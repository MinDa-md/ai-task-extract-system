package com.mindamd.taskextractor.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "artifact_checkpoint", uniqueConstraints = @UniqueConstraint(columnNames = {"pipeline_id", "step_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtifactCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id")
    private Pipeline pipeline;

    private ArtifactCheckpoint(Integer stepOrder, String payload, Pipeline pipeline) {
        this.stepOrder = stepOrder;
        this.payload = payload;
        this.pipeline = pipeline;
    }

    public static ArtifactCheckpoint of(Integer stepOrder, String payload, Pipeline pipeline) {
        return new ArtifactCheckpoint(stepOrder, payload, pipeline);
    }
}
