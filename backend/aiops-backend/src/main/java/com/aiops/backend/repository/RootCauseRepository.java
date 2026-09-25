package com.aiops.backend.repository;

import com.aiops.backend.entity.RootCause;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RootCauseRepository extends JpaRepository<RootCause, Long> {

    List<RootCause> findByMetricId(Long metricId);
}