package com.aiops.backend.mapper;

import org.springframework.stereotype.Component;

import com.aiops.backend.dto.Response.AlertResponse;
import com.aiops.backend.entity.Alert;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {

        return new AlertResponse(
                alert.getId(),
                alert.getDevice().getId(),
                alert.getDevice().getName(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getAnomalyScore(),
                alert.getCreatedAt(),
                alert.getStatus(),
                alert.getRootCause(),
                alert.getRecommendedAction()
        );
    }
}