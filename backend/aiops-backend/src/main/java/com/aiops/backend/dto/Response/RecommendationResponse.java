package com.aiops.backend.dto.Response;

import java.time.LocalDateTime;

public record RecommendationResponse(
        Long metricId,
        Long deviceId,
        String deviceName,
        String rootCause,
        String severity,
        Double confidence,
        String recommendedAction,
        LocalDateTime createdAt
) {
}