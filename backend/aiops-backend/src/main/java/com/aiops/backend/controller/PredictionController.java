package com.aiops.backend.controller;

import com.aiops.backend.dto.Response.MLPredictionResponse;
import com.aiops.backend.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping("/predict")
    public MLPredictionResponse predict(
            @RequestParam double cpuUsage,
            @RequestParam double memoryUsage,
            @RequestParam double diskUsage,
            @RequestParam double latency,
            @RequestParam double packetLoss
    ) {

        return predictionService.predict(
                cpuUsage,
                memoryUsage,
                diskUsage,
                latency,
                packetLoss
        );
    }
}