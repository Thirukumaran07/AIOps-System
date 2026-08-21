package com.aiops.backend.service;

import com.aiops.backend.dto.Response.MLPredictionResponse;

public interface PredictionService {

    MLPredictionResponse predict(
            double cpuUsage,
            double memoryUsage,
            double diskUsage,
            double latency,
            double packetLoss
    );
}