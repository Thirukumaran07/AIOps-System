package com.aiops.backend.controller;

import com.aiops.backend.dto.Request.DeviceRequest;
import com.aiops.backend.dto.Response.DeviceResponse;
import com.aiops.backend.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(
            @Valid @RequestBody DeviceRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(deviceService.createDevice(request));
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {

        return ResponseEntity.ok(
                deviceService.getAllDevices()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                deviceService.getDeviceById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequest request) {

        return ResponseEntity.ok(
                deviceService.updateDevice(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @PathVariable Long id) {

        deviceService.deleteDevice(id);

        return ResponseEntity.noContent().build();
    }
}