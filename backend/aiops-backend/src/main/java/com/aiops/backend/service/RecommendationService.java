package com.aiops.backend.service;

import com.aiops.backend.dto.Response.RecommendationResponse;

import java.util.List;

public interface RecommendationService {

    List<RecommendationResponse> getAllRecommendations();

    RecommendationResponse getRecommendationById(Long rootCauseId);

    List<RecommendationResponse> getRecommendationsByMetric(Long metricId);
}