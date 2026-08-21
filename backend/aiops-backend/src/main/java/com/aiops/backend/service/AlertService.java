package com.aiops.backend.service;

import com.aiops.backend.dto.Response.AlertResponse;

import java.util.List;

public interface AlertService {

    AlertResponse createAlert(
            Long deviceId,
            String alertType,
            String severity,
            String message,
            Double anomalyScore
    );

    List<AlertResponse> getAllAlerts();

    List<AlertResponse> getAlertsByDevice(Long deviceId);

    List<AlertResponse> getOpenAlerts();
}