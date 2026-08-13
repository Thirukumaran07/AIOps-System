package com.aiops.backend.service.impl;


import com.aiops.backend.dto.Request.DeviceRequest;
import com.aiops.backend.dto.Response.DeviceResponse;
import com.aiops.backend.entity.Device;
import com.aiops.backend.exception.DuplicateResourceException;
import com.aiops.backend.exception.ResourceNotFoundException;
import com.aiops.backend.mapper.DeviceMapper;
import com.aiops.backend.repository.DeviceRepository;
import com.aiops.backend.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    @Override
    public DeviceResponse createDevice(DeviceRequest request) {

        if (deviceRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(
                    "Device already exists: " + request.name()
            );
        }

        Device device = deviceMapper.toEntity(request);

        Device savedDevice = deviceRepository.save(device);

        return deviceMapper.toResponse(savedDevice);
    }

    @Override
    public List<DeviceResponse> getAllDevices() {

        return deviceRepository.findAll()
                .stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    @Override
    public DeviceResponse getDeviceById(Long id) {

        Device device = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Device not found with id: " + id
                        )
                );

        return deviceMapper.toResponse(device);
    }

    @Override
    public DeviceResponse updateDevice(
            Long id,
            DeviceRequest request
    ) {

        Device device = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Device not found with id: " + id
                        )
                );

        device.setName(request.name());
        device.setIpAddress(request.ipAddress());
        device.setType(request.type());

        Device updatedDevice = deviceRepository.save(device);

        return deviceMapper.toResponse(updatedDevice);
    }

    @Override
    public void deleteDevice(Long id) {

        if (!deviceRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Device not found with id: " + id
            );
        }

        deviceRepository.deleteById(id);
    }
}