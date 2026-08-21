package com.aiops.backend.dto.Response;

import java.time.LocalDateTime;

public record MetricResponse(

        Long id,

        Long deviceId,

        String deviceName,

        Double cpuUsage,

        Double memoryUsage,

        Double diskUsage,

        Double networkUsage,

        Double latency,

        Double packetLoss,

        LocalDateTime timestamp,

        String anomalyStatus,

        Double anomalyScore
) {
}
