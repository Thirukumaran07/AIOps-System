package com.aiops.backend.service.impl;

import com.aiops.backend.dto.Response.AlertResponse;
import com.aiops.backend.entity.Alert;
import com.aiops.backend.entity.Device;
import com.aiops.backend.mapper.AlertMapper;
import com.aiops.backend.repository.AlertRepository;
import com.aiops.backend.repository.DeviceRepository;
import com.aiops.backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final DeviceRepository deviceRepository;
    private final AlertMapper alertMapper;

    @Override
    AlertResponse createAlert(
            Long deviceId,
            String alertType,
            String severity,
            String message,
            Double anomalyScore,
            String rootCause,
            String recommendedAction
    );{

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() ->
                        new RuntimeException("Device not found: " + deviceId));

        Alert alert = Alert.builder()
                .device(device)
                .alertType(alertType)
                .severity(severity)
                .message(message)
                .anomalyScore(anomalyScore)
                .createdAt(LocalDateTime.now())
                .status("OPEN")
                .build();

        return alertMapper.toResponse(
                alertRepository.save(alert)
        );
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