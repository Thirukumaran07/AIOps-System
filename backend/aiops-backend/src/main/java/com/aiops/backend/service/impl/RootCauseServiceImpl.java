package com.aiops.backend.service.impl;

import com.aiops.backend.dto.Response.RootCauseResponse;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.entity.RootCause;
import com.aiops.backend.exception.ResourceNotFoundException;
import com.aiops.backend.repository.RootCauseRepository;
import com.aiops.backend.service.RootCauseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RootCauseServiceImpl implements RootCauseService {

    private final RootCauseRepository rootCauseRepository;

    @Override
    public RootCauseResponse analyze(Metric metric) {

        String rootCause = determineRootCause(metric);
        String severity = determineSeverity(metric, rootCause);
        double confidence = determineConfidence(rootCause);
        String recommendedAction = determineRecommendedAction(metric);

        RootCause rootCauseEntity = RootCause.builder()
                .metric(metric)
                .rootCause(rootCause)
                .severity(severity)
                .confidence(confidence)
                .recommendedAction(recommendedAction)
                .createdAt(LocalDateTime.now())
                .build();

        RootCause saved =
                rootCauseRepository.save(rootCauseEntity);

        return mapToResponse(saved);
    }

    @Override
    public List<RootCauseResponse> getAll() {

        return rootCauseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public RootCauseResponse getById(Long id) {

        RootCause rootCause =
                rootCauseRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Root cause not found: " + id
                                ));

        return mapToResponse(rootCause);
    }

    @Override
    public List<RootCauseResponse> getByMetricId(Long metricId) {

        return rootCauseRepository
                .findByMetricId(metricId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public String determineRecommendedAction(
            Metric savedMetric
    ) {

        if (savedMetric.getCpuUsage() >= 90) {

            return "Identify the process consuming high CPU and terminate the process only if it is a non-critical process; otherwise notify the administrator.";

        } else if (savedMetric.getMemoryUsage() >= 90) {

            return "Identify the process consuming excessive memory and log/notify the administrator; safely release unused resources where possible.";

        } else if (savedMetric.getDiskUsage() >= 90) {

            return "Check available storage, identify large/unnecessary files, and notify the administrator before deleting any files.";

        } else if (savedMetric.getNetworkUsage() != null && savedMetric.getNetworkUsage() >= 90) {

            return "Monitor network traffic, identify the source of high utilization, and notify the administrator regarding the network condition.";

        } else if (savedMetric.getLatency() >= 200) {

            return "Check network connectivity, identify the high-latency destination, and retry the connection.";

        } else if (savedMetric.getPacketLoss() >= 5) {

            return "Check network connectivity, retry the connection, and notify the administrator if packet loss persists.";
        }

        return "Perform detailed system and network diagnostics";
    }

    @Override
    public String determineRootCause(
            Metric savedMetric
    ) {

        if (savedMetric.getCpuUsage() >= 90) {

            return "High CPU utilization";

        } else if (savedMetric.getMemoryUsage() >= 90) {

            return "High memory utilization";

        } else if (savedMetric.getDiskUsage() >= 90) {

            return "High disk utilization";

        } else if (savedMetric.getLatency() >= 200) {

            return "High network latency";

        } else if (savedMetric.getPacketLoss() >= 5) {

            return "Network packet loss";
        }

        return "ML-detected system/network anomaly";
    }

    private String determineSeverity(
            Metric metric,
            String rootCause
    ) {

        if (metric.getCpuUsage() >= 90
                || metric.getMemoryUsage() >= 90
                || metric.getDiskUsage() >= 90
                || metric.getPacketLoss() >= 5) {

            return "HIGH";
        }

        if (metric.getLatency() >= 200) {
            return "MEDIUM";
        }

        return "MEDIUM";
    }

    private double determineConfidence(
            String rootCause
    ) {

        return switch (rootCause) {

            case "High CPU utilization",
                 "High memory utilization",
                 "High disk utilization" ->
                    95.0;

            case "High network latency",
                 "Network packet loss" ->
                    90.0;

            default ->
                    70.0;
        };
    }

    private RootCauseResponse mapToResponse(
            RootCause rootCause
    ) {

        return new RootCauseResponse(
                rootCause.getId(),
                rootCause.getMetric().getId(),
                rootCause.getRootCause(),
                rootCause.getSeverity(),
                rootCause.getConfidence(),
                rootCause.getRecommendedAction(),
                rootCause.getCreatedAt()
        );
    }
}