package com.aiops.backend.controller;

import com.aiops.backend.dto.Request.MetricRequest;
import com.aiops.backend.dto.Response.MetricResponse;
import com.aiops.backend.service.MetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @PostMapping
    public ResponseEntity<MetricResponse> createMetric(
            @Valid @RequestBody MetricRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(metricService.createMetric(request));
    }

    @GetMapping
    public ResponseEntity<List<MetricResponse>> getAllMetrics() {

        return ResponseEntity.ok(
                metricService.getAllMetrics()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetricResponse> getMetricById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                metricService.getMetricById(id)
        );
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<MetricResponse>> getMetricsByDevice(
            @PathVariable Long deviceId) {

        return ResponseEntity.ok(
                metricService.getMetricsByDevice(deviceId)
        );
    }
}