package com.aiops.backend.dto.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MetricRequest(

        @NotNull(message = "Device ID is required")
        Long deviceId,

        @NotNull(message = "CPU usage is required")
        @Min(value = 0, message = "CPU usage cannot be negative")
        @Max(value = 100, message = "CPU usage cannot exceed 100")
        Double cpuUsage,

        @NotNull(message = "Memory usage is required")
        @Min(value = 0, message = "Memory usage cannot be negative")
        @Max(value = 100, message = "Memory usage cannot exceed 100")
        Double memoryUsage,

        @NotNull(message = "Disk usage is required")
        @Min(value = 0, message = "Disk usage cannot be negative")
        @Max(value = 100, message = "Disk usage cannot exceed 100")
        Double diskUsage,

        @NotNull(message = "Network usage is required")
        @Min(value = 0, message = "Network usage cannot be negative")
        Double networkUsage,

        @NotNull(message = "Latency is required")
        @Min(value = 0, message = "Latency cannot be negative")
        Double latency,

        @NotNull(message = "Packet loss is required")
        @Min(value = 0, message = "Packet loss cannot be negative")
        @Max(value = 100, message = "Packet loss cannot exceed 100")
        Double packetLoss
) {
}
