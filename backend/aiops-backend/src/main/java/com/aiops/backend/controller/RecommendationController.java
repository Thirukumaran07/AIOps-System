package com.aiops.backend.controller;

import com.aiops.backend.dto.Response.RecommendationResponse;
import com.aiops.backend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>>
    getAllRecommendations() {

        return ResponseEntity.ok(
                recommendationService.getAllRecommendations()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationResponse>
    getRecommendationById(@PathVariable Long id) {

        return ResponseEntity.ok(
                recommendationService.getRecommendationById(id)
        );
    }

    @GetMapping("/metric/{metricId}")
    public ResponseEntity<List<RecommendationResponse>>
    getRecommendationsByMetric(
            @PathVariable Long metricId) {

        return ResponseEntity.ok(
                recommendationService
                        .getRecommendationsByMetric(metricId)
        );
    }
}