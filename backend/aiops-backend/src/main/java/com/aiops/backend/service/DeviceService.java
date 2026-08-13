package com.aiops.backend.service;

import com.aiops.backend.dto.Request.DeviceRequest;import com.aiops.backend.dto.Response.DeviceResponse;

import java.util.List;

public interface DeviceService {

    DeviceResponse createDevice(DeviceRequest request);

    List<DeviceResponse> getAllDevices();

    DeviceResponse getDeviceById(Long id);

    DeviceResponse updateDevice(Long id, DeviceRequest request);

    void deleteDevice(Long id);

}