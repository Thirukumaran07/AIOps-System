package com.aiops.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false)
    private Double cpuUsage;

    @Column(nullable = false)
    private Double memoryUsage;

    @Column(nullable = false)
    private Double diskUsage;

    @Column(nullable = false)
    private Double networkUsage;

    @Column(nullable = false)
    private Double latency;

    @Column(nullable = false)
    private Double packetLoss;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String anomalyStatus;

    @Column(nullable = false)
    private Double anomalyScore;
}