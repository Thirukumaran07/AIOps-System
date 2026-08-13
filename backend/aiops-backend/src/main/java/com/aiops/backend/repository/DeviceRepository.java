package com.aiops.backend.repository;

import com.aiops.backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByName(String name);

    boolean existsByName(String name);

    Optional<Device> findByIpAddress(String ipAddress);
}