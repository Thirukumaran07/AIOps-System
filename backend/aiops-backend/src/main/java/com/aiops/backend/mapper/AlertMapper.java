package com.aiops.backend.mapper;

import com.aiops.backend.dto.Response.AlertResponse;
import com.aiops.backend.entity.Alert;
import org.springframework.stereotype.Component;

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
                alert.getStatus()
        );
    }
}