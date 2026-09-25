package com.aiops.backend.dto.Response;

import java.time.LocalDateTime;

public record RootCauseResponse(
        Long id,
        Long metricId,
        String rootCause,
        String severity,
        Double confidence,
        String recommendedAction,
        LocalDateTime createdAt
) {
}