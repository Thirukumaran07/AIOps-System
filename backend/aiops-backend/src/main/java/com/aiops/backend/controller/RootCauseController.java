package com.aiops.backend.controller;

import com.aiops.backend.dto.Response.RootCauseResponse;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.repository.MetricRepository;
import com.aiops.backend.service.RootCauseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/root-causes")
@RequiredArgsConstructor
public class RootCauseController {

    private final RootCauseService rootCauseService;
    private final MetricRepository metricRepository;

    @PostMapping("/analyze/{metricId}")
    public ResponseEntity<RootCauseResponse> analyze(
            @PathVariable Long metricId) {

        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() ->
                        new RuntimeException("Metric not found"));

        return ResponseEntity.ok(
                rootCauseService.analyze(metric)
        );
    }

    @GetMapping
    public ResponseEntity<List<RootCauseResponse>> getAll() {

        return ResponseEntity.ok(
                rootCauseService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RootCauseResponse> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                rootCauseService.getById(id)
        );
    }

    @GetMapping("/metric/{metricId}")
    public ResponseEntity<List<RootCauseResponse>> getByMetricId(
            @PathVariable Long metricId) {

        return ResponseEntity.ok(
                rootCauseService.getByMetricId(metricId)
        );
    }
}