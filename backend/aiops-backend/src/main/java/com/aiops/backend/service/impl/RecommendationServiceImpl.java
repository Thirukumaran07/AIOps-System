package com.aiops.backend.service.impl;

import com.aiops.backend.dto.Response.RecommendationResponse;
import com.aiops.backend.entity.RootCause;
import com.aiops.backend.exception.ResourceNotFoundException;
import com.aiops.backend.repository.RootCauseRepository;
import com.aiops.backend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RootCauseRepository rootCauseRepository;

    @Override
    public List<RecommendationResponse> getAllRecommendations() {

        return rootCauseRepository.findAll()
                .stream()
                .sorted((a, b) ->
                        b.getCreatedAt()
                                .compareTo(a.getCreatedAt()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public RecommendationResponse getRecommendationById(Long rootCauseId) {

        RootCause rootCause =
                rootCauseRepository.findById(rootCauseId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Recommendation not found for root cause: "
                                                + rootCauseId
                                ));

        return mapToResponse(rootCause);
    }

    @Override
    public List<RecommendationResponse> getRecommendationsByMetric(
            Long metricId) {

        return rootCauseRepository.findByMetricId(metricId)
                .stream()
                .sorted((a, b) ->
                        b.getCreatedAt()
                                .compareTo(a.getCreatedAt()))
                .map(this::mapToResponse)
                .toList();
    }

    private RecommendationResponse mapToResponse(
            RootCause rootCause) {

        return new RecommendationResponse(
                rootCause.getMetric().getId(),
                rootCause.getMetric().getDevice().getId(),
                rootCause.getMetric().getDevice().getName(),
                rootCause.getRootCause(),
                rootCause.getSeverity(),
                rootCause.getConfidence(),
                rootCause.getRecommendedAction(),
                rootCause.getCreatedAt()
        );
    }
}