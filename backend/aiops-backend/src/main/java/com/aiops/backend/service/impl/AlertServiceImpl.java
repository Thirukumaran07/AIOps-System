package com.aiops.backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aiops.backend.dto.Response.AlertResponse;
import com.aiops.backend.entity.Alert;
import com.aiops.backend.entity.Device;
import com.aiops.backend.mapper.AlertMapper;
import com.aiops.backend.repository.AlertRepository;
import com.aiops.backend.repository.DeviceRepository;
import com.aiops.backend.service.AlertService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final DeviceRepository deviceRepository;
    private final AlertMapper alertMapper;

    @Override
    public AlertResponse createAlert(
            Long deviceId,
            String alertType,
            String severity,
            String message,
            Double anomalyScore,
            String rootCause,
            String recommendedAction
    ) {

        // Find device
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Device not found: " + deviceId
                        )
                );

        // Create alert
        Alert alert = Alert.builder()
                .device(device)
                .alertType(alertType)
                .severity(severity)
                .message(message)
                .anomalyScore(anomalyScore)
                .createdAt(LocalDateTime.now())
                .status("OPEN")
                .rootCause(rootCause)
                .recommendedAction(recommendedAction)
                .build();

        // Save alert
        Alert savedAlert = alertRepository.save(alert);

        // Return response
        return alertMapper.toResponse(savedAlert);
    }

    @Override
    public List<AlertResponse> getAllAlerts() {

        return alertRepository.findAll()
                .stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    @Override
    public List<AlertResponse> getAlertsByDevice(Long deviceId) {

        return alertRepository
                .findByDeviceIdOrderByCreatedAtDesc(deviceId)
                .stream()
                .map(alertMapper::toResponse)
                .toList();
    }

    @Override
    public List<AlertResponse> getOpenAlerts() {

        return alertRepository
                .findByStatusOrderByCreatedAtDesc("OPEN")
                .stream()
                .map(alertMapper::toResponse)
                .toList();
    }
}