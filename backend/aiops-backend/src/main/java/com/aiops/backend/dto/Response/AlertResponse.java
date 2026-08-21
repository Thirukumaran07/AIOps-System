package com.aiops.backend.dto.Response;

import java.time.LocalDateTime;

public record AlertResponse(

        Long id,

        Long deviceId,

        String deviceName,

        String alertType,

        String severity,

        String message,

        Double anomalyScore,

        LocalDateTime createdAt,

        String status
) {
}