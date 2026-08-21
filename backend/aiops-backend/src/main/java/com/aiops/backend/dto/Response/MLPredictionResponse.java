package com.aiops.backend.dto.Response;

public record MLPredictionResponse(
        String status,
        Double anomalyScore
) {
}