package com.aiops.backend.mapper;

import com.aiops.backend.dto.Request.DeviceRequest;
import com.aiops.backend.dto.Response.DeviceResponse;
import com.aiops.backend.entity.Device;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public Device toEntity(DeviceRequest request) {

        return Device.builder()
                .name(request.name())
                .ipAddress(request.ipAddress())
                .type(request.type())
                .build();
    }

    public DeviceResponse toResponse(Device device) {

        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getIpAddress(),
                device.getType(),
                device.getStatus(),
                device.getHealthScore(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}