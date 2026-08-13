package com.aiops.backend.service.impl;

import com.aiops.backend.dto.Request.MetricRequest;
import com.aiops.backend.dto.Response.MetricResponse;
import com.aiops.backend.entity.Device;
import com.aiops.backend.entity.Metric;
import com.aiops.backend.exception.ResourceNotFoundException;
import com.aiops.backend.mapper.MetricMapper;
import com.aiops.backend.repository.DeviceRepository;
import com.aiops.backend.repository.MetricRepository;
import com.aiops.backend.service.MetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricServiceImpl implements MetricService {

    private final MetricRepository metricRepository;
    private final DeviceRepository deviceRepository;
    private final MetricMapper metricMapper;

    @Override
    public MetricResponse createMetric(MetricRequest request) {

        Device device = deviceRepository.findById(request.deviceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Device not found with id: " + request.deviceId()
                        )
                );

        Metric metric = metricMapper.toEntity(request, device);

        Metric savedMetric = metricRepository.save(metric);

        return metricMapper.toResponse(savedMetric);
    }

    @Override
    public List<MetricResponse> getAllMetrics() {

        return metricRepository.findAll()
                .stream()
                .map(metricMapper::toResponse)
                .toList();
    }

    @Override
    public MetricResponse getMetricById(Long id) {

        Metric metric = metricRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Metric not found with id: " + id
                        )
                );

        return metricMapper.toResponse(metric);
    }

    @Override
    public List<MetricResponse> getMetricsByDevice(Long deviceId) {

        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException(
                    "Device not found with id: " + deviceId
            );
        }

        return metricRepository
                .findByDeviceIdOrderByTimestampDesc(deviceId)
                .stream()
                .map(metricMapper::toResponse)
                .toList();
    }
}