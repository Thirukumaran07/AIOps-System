package com.aiops.backend.service;

import com.aiops.backend.dto.Request.MetricRequest;
import com.aiops.backend.dto.Response.MetricResponse;

import java.util.List;

public interface MetricService {

    MetricResponse createMetric(MetricRequest request);

    List<MetricResponse> getAllMetrics();

    MetricResponse getMetricById(Long id);

    List<MetricResponse> getMetricsByDevice(Long deviceId);
}