package com.aiops.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "root_causes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RootCause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private Metric metric;

    @Column(name = "root_cause", nullable = false)
    private String rootCause;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "recommended_action", nullable = false)
    private String recommendedAction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}