package com.aiops.backend.controller;

import com.aiops.backend.dto.Response.AlertResponse;
import com.aiops.backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<AlertResponse> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/open")
    public List<AlertResponse> getOpenAlerts() {
        return alertService.getOpenAlerts();
    }

    @GetMapping("/device/{deviceId}")
    public List<AlertResponse> getAlertsByDevice(
            @PathVariable Long deviceId
    ) {
        return alertService.getAlertsByDevice(deviceId);
    }
}