package com.aiops.backend.mapper;

import com.aiops.backend.dto.Request.MetricRequest;
import com.aiops.backend.dto.Response.MetricResponse;
import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.Metric;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MetricMapper {

    public Metric toEntity(MetricRequest request, Device device) {

        return Metric.builder()
                .device(device)
                .cpuUsage(request.cpuUsage())
                .memoryUsage(request.memoryUsage())
                .diskUsage(request.diskUsage())
                .networkUsage(request.networkUsage())
                .latency(request.latency())
                .packetLoss(request.packetLoss())
                .timestamp(LocalDateTime.now())
                .anomalyStatus("PENDING")
                .anomalyScore(0.0)
                .build();
    }

    public MetricResponse toResponse(Metric metric) {

        return new MetricResponse(
                metric.getId(),
                metric.getDevice().getId(),
                metric.getDevice().getName(),
                metric.getCpuUsage(),
                metric.getMemoryUsage(),
                metric.getDiskUsage(),
                metric.getNetworkUsage(),
                metric.getLatency(),
                metric.getPacketLoss(),
                metric.getTimestamp(),
                metric.getAnomalyStatus(),
                metric.getAnomalyScore()
        );
    }
}