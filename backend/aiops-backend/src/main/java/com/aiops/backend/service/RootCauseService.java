package com.aiops.backend.service;

import com.aiops.backend.dto.Response.RootCauseResponse;
import com.aiops.backend.entity.Metric;

import java.util.List;

public interface RootCauseService {

    RootCauseResponse analyze(Metric metric);

    List<RootCauseResponse> getAll();

    RootCauseResponse getById(Long id);

    List<RootCauseResponse> getByMetricId(Long metricId);

    String determineRootCause(Metric metric);

    String determineRecommendedAction(Metric metric);
}