package com.aiops.backend.service.impl;

import com.aiops.backend.dto.Response.MLPredictionResponse;
import com.aiops.backend.service.PredictionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

    private final RestTemplate restTemplate;

    private final String ML_URL =
            "http://localhost:5000/predict";


    @Override
    public MLPredictionResponse predict(
            double cpuUsage,
            double memoryUsage,
            double diskUsage,
            double latency,
            double packetLoss) {

        Map<String, Object> request = new HashMap<>();

        request.put("cpuUsage", cpuUsage);
        request.put("memoryUsage", memoryUsage);
        request.put("diskUsage", diskUsage);
        request.put("latency", latency);
        request.put("packetLoss", packetLoss);


        ResponseEntity<MLPredictionResponse> response =
                restTemplate.postForEntity(
                        ML_URL,
                        request,
                        MLPredictionResponse.class
                );


        return response.getBody();
    }
}