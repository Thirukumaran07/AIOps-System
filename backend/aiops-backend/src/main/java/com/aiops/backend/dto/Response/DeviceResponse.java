package com.aiops.backend.dto.Response;

import java.time.LocalDateTime;

public record DeviceResponse(
        Long id,
        String name,
        String ipAddress,
        String type,
        String status,
        Double healthScore,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
