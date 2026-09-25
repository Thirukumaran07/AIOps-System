package com.aiops.backend.repository;

import com.aiops.backend.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findByDeviceIdOrderByTimestampDesc(Long deviceId);

    Optional<Metric> findFirstByDeviceIdOrderByTimestampDesc(Long deviceId);
}