package com.aiops.backend.repository;

import com.aiops.backend.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByMetricIdOrderByCreatedAtDesc(Long metricId);

    List<Prediction> findTop20ByOrderByCreatedAtDesc();
}

