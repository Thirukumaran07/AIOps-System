package com.aiops.backend.service;

import java.util.List;

import com.aiops.backend.dto.Response.AlertResponse;

public interface AlertService {

    AlertResponse createAlert(
            Long deviceId,
            String alertType,
            String severity,
            String message,
            Double anomalyScore,
            String rootCause,
            String recommendedAction
    );

    List<AlertResponse> getAllAlerts();

    List<AlertResponse> getAlertsByDevice(Long deviceId);

    List<AlertResponse> getOpenAlerts();

    void updateAlertStatus(Long alertId, String status);
}