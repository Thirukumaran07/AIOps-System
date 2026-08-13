package com.aiops.backend.dto.Request;

import jakarta.validation.constraints.NotBlank;

public record DeviceRequest(

        @NotBlank(message = "Device name is required")
        String name,

        @NotBlank(message = "IP address is required")
        String ipAddress,

        @NotBlank(message = "Device type is required")
        String type

) {
}