package com.aiops.backend.service.impl;

import com.aiops.backend.dto.Request.MetricRequest;
import com.aiops.backend.dto.Response.MLPredictionResponse;
import com.aiops.backend.dto.Response.MetricResponse;
import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.exception.ResourceNotFoundException;
import com.aiops.backend.mapper.MetricMapper;
import com.aiops.backend.repository.DeviceRepository;
import com.aiops.backend.repository.MetricRepository;
import com.aiops.backend.service.AlertService;
import com.aiops.backend.service.MetricService;
import com.aiops.backend.service.PredictionService;
import com.aiops.backend.service.RootCauseService;
import com.aiops.backend.util.MetricCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricServiceImpl implements MetricService {

    private final MetricRepository metricRepository;
    private final DeviceRepository deviceRepository;
    private final MetricMapper metricMapper;
    private final MetricCalculator metricCalculator;
    private final PredictionService predictionService;
    private final AlertService alertService;
    private final RootCauseService rootCauseService;

    @Override
    public MetricResponse createMetric(MetricRequest request) {

        // 1. Find the device
        Device device = deviceRepository.findById(request.deviceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Device not found with id: " + request.deviceId()
                        )
                );

        // 2. Convert request to Metric entity
        Metric metric = metricMapper.toEntity(request, device);

        // 3. Save initial metric
        Metric savedMetric = metricRepository.save(metric);

        // 4. Send metric data to Python ML service
        MLPredictionResponse prediction =
                predictionService.predict(
                        savedMetric.getCpuUsage(),
                        savedMetric.getMemoryUsage(),
                        savedMetric.getDiskUsage(),
                        savedMetric.getLatency(),
                        savedMetric.getPacketLoss()
                );

        // 5. Print ML result
        System.out.println("========== ML PREDICTION ==========");
        System.out.println("Status: " + prediction.status());
        System.out.println("Anomaly Score: " + prediction.anomalyScore());
        System.out.println("===================================");

        // 6. Update metric with ML prediction
        savedMetric.setAnomalyStatus(prediction.status());
        savedMetric.setAnomalyScore(prediction.anomalyScore());

        // 7. Save updated metric
        savedMetric = metricRepository.save(savedMetric);

        // 8. Handle anomaly
        if ("ANOMALY".equalsIgnoreCase(prediction.status())) {

            // Determine alert severity
            String severity = determineSeverity(savedMetric);

            // Build alert message
            String message = buildAlertMessage(savedMetric);

            // Determine root cause
            String rootCause =
                    rootCauseService.determineRootCause(savedMetric);

            // Determine recommended action
            String recommendedAction =
                    rootCauseService.determineRecommendedAction(savedMetric);

            // Create alert with RCA information
            alertService.createAlert(
                    savedMetric.getDevice().getId(),
                    "NETWORK_ANOMALY",
                    severity,
                    message,
                    prediction.anomalyScore(),
                    rootCause,
                    recommendedAction
            );

            // Debug information
            System.out.println("========== ALERT CREATED ==========");
            System.out.println("Device ID: "
                    + savedMetric.getDevice().getId());
            System.out.println("Severity: " + severity);
            System.out.println("Message: " + message);
            System.out.println("Root Cause: " + rootCause);
            System.out.println("Recommended Action: "
                    + recommendedAction);
            System.out.println("===================================");
        }

        // 9. Calculate health score
        double healthScore =
                metricCalculator.calculateHealthScore(
                        request.cpuUsage(),
                        request.memoryUsage(),
                        request.diskUsage(),
                        request.latency(),
                        request.packetLoss()
                );

        // 10. Determine device status
        String status =
                metricCalculator.calculateStatus(healthScore);

        // 11. Update device health
        device.setHealthScore(healthScore);
        device.setStatus(status);

        // 12. Save updated device
        deviceRepository.save(device);

        // 13. Return response
        return metricMapper.toResponse(savedMetric);
    }

    @Override
    public List<MetricResponse> getAllMetrics() {

        return metricRepository.findAll()
                .stream()
                .map(metricMapper::toResponse)
                .toList();
    }

    @Override
    public MetricResponse getMetricById(Long id) {

        Metric metric = metricRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Metric not found with id: " + id
                        )
                );

        return metricMapper.toResponse(metric);
    }

    @Override
    public List<MetricResponse> getMetricsByDevice(Long deviceId) {

        if (!deviceRepository.existsById(deviceId)) {

            throw new ResourceNotFoundException(
                    "Device not found with id: " + deviceId
            );
        }

        return metricRepository
                .findByDeviceIdOrderByTimestampDesc(deviceId)
                .stream()
                .map(metricMapper::toResponse)
                .toList();
    }

    // Determine alert severity
    private String determineSeverity(Metric metric) {

        if (metric.getCpuUsage() >= 90
                || metric.getMemoryUsage() >= 90
                || metric.getDiskUsage() >= 90
                || metric.getLatency() >= 200
                || metric.getPacketLoss() >= 5) {

            return "CRITICAL";
        }

        if (metric.getCpuUsage() >= 80
                || metric.getMemoryUsage() >= 80
                || metric.getDiskUsage() >= 80
                || metric.getLatency() >= 100
                || metric.getPacketLoss() >= 2) {

            return "WARNING";
        }

        return "INFO";
    }

    // Build meaningful alert message
    private String buildAlertMessage(Metric metric) {

        if (metric.getCpuUsage() >= 90) {
            return "CPU usage exceeded the critical threshold.";
        }

        if (metric.getMemoryUsage() >= 90) {
            return "Memory usage exceeded the critical threshold.";
        }

        if (metric.getDiskUsage() >= 90) {
            return "Disk usage exceeded the critical threshold.";
        }

        if (metric.getLatency() >= 200) {
            return "Network latency exceeded the critical threshold.";
        }

        if (metric.getPacketLoss() >= 5) {
            return "Packet loss exceeded the critical threshold.";
        }

        return "Anomalous network behavior detected by the ML model.";
    }
}