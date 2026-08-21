package com.aiops.backend.service;

import com.aiops.backend.entity.Metric;

public interface RootCauseService {

    String determineRootCause(Metric metric);

    String determineRecommendedAction(Metric metric);
}