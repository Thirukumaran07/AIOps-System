package com.aiops.backend.repository;

import com.aiops.backend.entity.HealingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealingLogRepository
        extends JpaRepository<HealingLog, Long> {

    List<HealingLog> findByDeviceIdOrderByExecutedAtDesc(
            Long deviceId
    );

    List<HealingLog> findTop10ByOrderByExecutedAtDesc();
}