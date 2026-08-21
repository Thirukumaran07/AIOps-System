package com.aiops.backend.repository;

import com.aiops.backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    List<Alert> findByStatusOrderByCreatedAtDesc(String status);
}